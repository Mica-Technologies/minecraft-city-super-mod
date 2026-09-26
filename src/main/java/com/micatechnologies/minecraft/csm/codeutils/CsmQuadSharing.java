package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.CsmConstants;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.SimpleBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.obj.OBJModel;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Makes CSM's baked models share their identical quads, once baking is done.
 *
 * <p>Forge bakes one model per blockstate variant, and every variant that names a submodel bakes
 * that submodel again, even with the same transform and textures. So the same quad exists many
 * times over: across all modules about 7 million baked quads hold fewer than 800 thousand
 * distinct ones. Each copy is a {@link BakedQuad} and its own 28-int vertex array, about 170
 * bytes, and together the copies were about a gigabyte of the heap.</p>
 *
 * <p>After the bake ({@link ModelBakeEvent}, at the lowest priority so every other handler has
 * already wrapped or replaced what it wanted), this walks every baked model under a {@code csm:}
 * location and, in each {@link SimpleBakedModel}'s quad lists, replaces every quad with the first
 * quad seen with the same content: the same vertex data, tint index, face, sprite, diffuse
 * lighting flag and vertex format. The lists are the builder's {@code ArrayList}s, so they are
 * edited in place and no model is replaced. Nothing on screen can change: the quads a model hands
 * out are equal, field for field, to the ones it held before.</p>
 *
 * <p>What it leaves alone:</p>
 * <ul>
 *   <li>Forge's OBJ models ({@link OBJModel.OBJBakedModel}). They build their quads lazily, on
 *   the first {@code getQuads}, and are not touched.</li>
 *   <li>Any quad class other than {@link BakedQuad} itself (Forge's {@code UnpackedBakedQuad},
 *   the retextured quads), and any list that is not an {@code ArrayList}.</li>
 *   <li>CSM's own baked model classes (doors, tree logs and leaves): they build and cache their
 *   quads on demand, so the walk does not look inside them.</li>
 * </ul>
 *
 * <p><b>The rule this makes:</b> a baked quad is shared, so never write into the array
 * {@link BakedQuad#getVertexData()} returns. Nothing in CSM, Forge or vanilla does; code that
 * needs changed vertices builds a new quad. Code that compares quads by identity (the ceiling
 * fan's blades are the quads its running model does not have) keeps working, since two quads
 * that are now one instance were equal in every field before.</p>
 *
 * <p>The same pass points every <em>empty</em> quad list and item override list at one shared
 * empty list. A {@link SimpleBakedModel} has seven quad lists (one per face and the general one)
 * and most are empty, and every baked model has an override list that almost never has an
 * override, each its own {@code ArrayList}: 1.2 million empty lists, about 27 MB. <b>So never
 * add to a list a baked model hands out</b>, which was already wrong, since the list is the
 * model's own.</p>
 *
 * <p>The dedup maps live only for the pass. A resource reload bakes new models and posts the
 * event again, so the pass reruns on them.</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public final class CsmQuadSharing {

  /** Content equality over everything a {@link BakedQuad} holds. */
  private static final Hash.Strategy<BakedQuad> CONTENT = new Hash.Strategy<BakedQuad>() {
    @Override
    public int hashCode(BakedQuad q) {
      if (q == null) {
        return 0;
      }
      int h = Arrays.hashCode(q.getVertexData());
      h = h * 31 + q.getTintIndex();
      h = h * 31 + (q.getFace() == null ? -1 : q.getFace().ordinal());
      h = h * 31 + System.identityHashCode(q.getSprite());
      h = h * 31 + (q.shouldApplyDiffuseLighting() ? 1 : 0);
      return h;
    }

    @Override
    public boolean equals(BakedQuad a, BakedQuad b) {
      if (a == b) {
        return true;
      }
      if (a == null || b == null) {
        return false;
      }
      return a.getTintIndex() == b.getTintIndex()
          && a.getFace() == b.getFace()
          && a.getSprite() == b.getSprite()
          && a.shouldApplyDiffuseLighting() == b.shouldApplyDiffuseLighting()
          && Objects.equals(a.getFormat(), b.getFormat())
          && Arrays.equals(a.getVertexData(), b.getVertexData());
    }
  };

  /** The one empty list every empty quad list and override list is pointed at. */
  private static final List<Object> EMPTY = Collections.emptyList();

  private final Map<Class<?>, List<Field>> fieldCache = new HashMap<>();
  private final Map<Class<?>, Boolean> modelHelpers = new HashMap<>();
  private Field generalQuadsField;
  private Field faceQuadsField;
  private Field overridesField;
  private long emptied;
  private Object2ObjectOpenCustomHashMap<BakedQuad, BakedQuad> canonical;
  private ReferenceOpenHashSet<Object> visited;
  private long seen;
  private long replaced;
  private long skippedModels;

  /**
   * Runs the pass on the freshly baked registry.
   *
   * @param event the bake event
   */
  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void onModelBake(ModelBakeEvent event) {
    long start = System.nanoTime();
    canonical = new Object2ObjectOpenCustomHashMap<>(1 << 20, CONTENT);
    visited = new ReferenceOpenHashSet<>(1 << 19);
    seen = 0;
    replaced = 0;
    skippedModels = 0;
    emptied = 0;
    try {
      generalQuadsField = ObfuscationReflectionHelper.findField(SimpleBakedModel.class,
          "field_177563_a");
      faceQuadsField = ObfuscationReflectionHelper.findField(SimpleBakedModel.class,
          "field_177561_b");
      overridesField = ObfuscationReflectionHelper.findField(ItemOverrideList.class,
          "field_188023_b");
      IRegistry<ModelResourceLocation, IBakedModel> registry = event.getModelRegistry();
      for (ModelResourceLocation key : registry.getKeys()) {
        if (CsmConstants.MOD_NAMESPACE.equals(key.getNamespace())) {
          visit(registry.getObject(key), 0);
        }
      }
      Csm.getLogger().info(
          "Shared identical baked quads: {} quads in {} models, {} replaced by an equal quad, "
              + "{} distinct kept ({} model classes skipped); {} empty lists shared, {} ms",
          seen, visited.size(), replaced, canonical.size(), skippedModels, emptied,
          (System.nanoTime() - start) / 1_000_000L);
    } catch (Throwable t) {
      // Sharing is only a saving; a model class this does not understand must never stop the
      // game loading. Quads already replaced are equal to the ones they replaced.
      Csm.getLogger().warn("Baked quad sharing stopped early; models are unaffected", t);
    } finally {
      canonical = null;
      visited = null;
      fieldCache.clear();
      modelHelpers.clear();
    }
  }

  private void visit(Object model, int depth) {
    if (!(model instanceof IBakedModel) || depth > 16 || !visited.add(model)) {
      return;
    }
    if (model instanceof OBJModel.OBJBakedModel) {
      return;
    }
    Class<?> type = model.getClass();
    if (type == SimpleBakedModel.class) {
      share((SimpleBakedModel) model);
      return;
    }
    String name = type.getName();
    if (!name.startsWith("net.minecraft.") && !name.startsWith("net.minecraftforge.")) {
      // CSM's own and other mods' baked models build and cache quads their own way.
      skippedModels++;
      return;
    }
    // A wrapper or container (Forge's perspective wrapper, MultiModel.Baked, weighted and
    // multipart models): look for the models it holds.
    for (Field f : fieldsOf(type)) {
      Object value;
      try {
        value = f.get(model);
      } catch (Throwable e) {
        continue;
      }
      visitValue(value, depth + 1);
    }
  }

  private void visitValue(Object value, int depth) {
    if (value == null || depth > 16) {
      return;
    }
    if (value instanceof IBakedModel) {
      visit(value, depth);
    } else if (value instanceof ItemOverrideList) {
      shareOverrides((ItemOverrideList) value);
    } else if (value instanceof Map) {
      for (Object v : ((Map<?, ?>) value).values()) {
        visitValue(v, depth + 1);
      }
    } else if (value instanceof Collection) {
      for (Object v : (Collection<?>) value) {
        if (v instanceof BakedQuad) {
          return; // a quad list outside a SimpleBakedModel: left as it is
        }
        visitValue(v, depth + 1);
      }
    } else if (value instanceof Pair) {
      visitValue(((Pair<?, ?>) value).getLeft(), depth + 1);
      visitValue(((Pair<?, ?>) value).getRight(), depth + 1);
    } else if (isModelHelper(value.getClass())) {
      // WeightedBakedModel's entries: a helper class that belongs to a model class.
      for (Field f : fieldsOf(value.getClass())) {
        try {
          visitValue(f.get(value), depth + 1);
        } catch (Throwable ignored) {
          // Unreadable field: nothing to share there.
        }
      }
    }
  }

  /**
   * Whether a class is a helper nested in a model class. Decided once per class, since
   * {@code getEnclosingClass()} is slow and the walk asks it of millions of values.
   */
  private boolean isModelHelper(Class<?> type) {
    Boolean known = modelHelpers.get(type);
    if (known == null) {
      Class<?> enclosing = type.getEnclosingClass();
      known = enclosing != null && IBakedModel.class.isAssignableFrom(enclosing);
      modelHelpers.put(type, known);
    }
    return known;
  }

  private void share(SimpleBakedModel model) {
    share(model.getQuads(null, null, 0L));
    for (EnumFacing side : EnumFacing.values()) {
      share(model.getQuads(null, side, 0L));
    }
    try {
      List<?> general = (List<?>) generalQuadsField.get(model);
      if (general != null && general != EMPTY && general.isEmpty()) {
        generalQuadsField.set(model, EMPTY);
        emptied++;
      }
      @SuppressWarnings("unchecked")
      Map<EnumFacing, List<BakedQuad>> faces =
          (Map<EnumFacing, List<BakedQuad>>) faceQuadsField.get(model);
      if (faces instanceof EnumMap) {
        for (Map.Entry<EnumFacing, List<BakedQuad>> e : faces.entrySet()) {
          List<BakedQuad> list = e.getValue();
          if (list != null && list != (Object) EMPTY && list.isEmpty()) {
            e.setValue(Collections.emptyList());
            emptied++;
          }
        }
      }
    } catch (IllegalAccessException | RuntimeException ignored) {
      // A model built some other way keeps its own lists.
    }
    shareOverrides(model.getOverrides());
  }

  private void shareOverrides(ItemOverrideList overrides) {
    if (overrides == null) {
      return;
    }
    try {
      List<?> list = (List<?>) overridesField.get(overrides);
      if (list != null && list != EMPTY && list.isEmpty()) {
        overridesField.set(overrides, EMPTY);
        emptied++;
      }
    } catch (IllegalAccessException | RuntimeException ignored) {
      // Kept as it is.
    }
  }

  private void share(List<BakedQuad> quads) {
    if (!(quads instanceof ArrayList)) {
      return;
    }
    ArrayList<BakedQuad> list = (ArrayList<BakedQuad>) quads;
    for (int i = 0; i < list.size(); i++) {
      BakedQuad q = list.get(i);
      if (q == null || q.getClass() != BakedQuad.class) {
        continue;
      }
      seen++;
      BakedQuad first = canonical.get(q);
      if (first == null) {
        canonical.put(q, q);
      } else if (first != q) {
        list.set(i, first);
        replaced++;
      }
    }
    list.trimToSize();
  }

  private List<Field> fieldsOf(Class<?> type) {
    List<Field> fields = fieldCache.get(type);
    if (fields != null) {
      return fields;
    }
    fields = new ArrayList<>();
    for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
      for (Field f : c.getDeclaredFields()) {
        if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()
            || f.getType() == String.class) {
          continue;
        }
        try {
          f.setAccessible(true);
          fields.add(f);
        } catch (Throwable ignored) {
          // A field the JVM will not open holds nothing this pass needs.
        }
      }
    }
    fieldCache.put(type, fields);
    return fields;
  }
}
