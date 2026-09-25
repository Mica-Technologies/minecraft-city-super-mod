package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.CsmConstants;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.block.model.BlockPart;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Once baking is done, lets go of the element copies Forge made to retexture CSM's models.
 *
 * <p>For every blockstate variant that sets {@code textures}, Forge's
 * {@code VanillaModelWrapper.retexture} copies the whole model: a new {@link ModelBlock} with a
 * new {@link BlockPart} and a new face map for every element. The copy is only needed to bake
 * that variant, but the baked model keeps it reachable afterwards: the baked wrapper holds the
 * {@code VanillaModelWrapper} it came from (for Forge's animation support, which re-bakes from
 * it), and so do the unbaked variant models Forge caches until the next reload. With every
 * module that came to 138 thousand model copies holding 1.3 million elements, about 440 MB.</p>
 *
 * <p>After the bake ({@link ModelBakeEvent}, lowest priority) this walks the unbaked variant
 * models under {@code csm:} locations in Forge's model cache, finds each retextured
 * {@code VanillaModelWrapper} copy, and empties its element list. The objects stay where they
 * are; only the elements they no longer use are released. A model that is itself in the cache
 * (a model file, not a per-variant copy) is never touched, nor is any element list one of them
 * uses, so everything that can still be asked for a bake after this is intact.</p>
 *
 * <p>What would need the released elements is a second bake of the same copy. Forge does that
 * in two places only: its animation state machine (a block with the {@code AnimationProperty}
 * unlisted property, or an item with the animation capability), which no CSM block or item
 * uses; and nothing else, since a resource reload makes a new model loader, clears the cache
 * and loads every model again from its file. <b>The rule this makes:</b> never bake a CSM
 * variant's unbaked model again after the bake event; ask the model manager for the baked one,
 * or load the model file afresh through {@link ModelLoaderRegistry}.</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public final class CsmUnbakedModelRelease {

  private static final String WRAPPER =
      "net.minecraftforge.client.model.ModelLoader$VanillaModelWrapper";

  private final Map<Class<?>, List<Field>> fieldCache = new HashMap<>();

  /**
   * Runs the release on the model cache the bake just used.
   *
   * @param event the bake event
   */
  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void onModelBake(ModelBakeEvent event) {
    long start = System.nanoTime();
    try {
      Class<?> wrapperClass = Class.forName(WRAPPER);
      Field modelField = wrapperClass.getDeclaredField("model");
      modelField.setAccessible(true);
      Field cacheField = ModelLoaderRegistry.class.getDeclaredField("cache");
      cacheField.setAccessible(true);
      Map<?, ?> cache = (Map<?, ?>) cacheField.get(null);

      // Everything a model file in the cache uses stays: its wrapper, its model, its elements.
      ReferenceOpenHashSet<Object> keep = new ReferenceOpenHashSet<>();
      for (Object model : cache.values()) {
        if (wrapperClass.isInstance(model)) {
          keep.add(model);
          // With its parents, whose elements a model without its own resolves to.
          for (ModelBlock block = (ModelBlock) modelField.get(model); block != null
              && keep.add(block); block = block.parent) {
            keep.add(block.getElements());
          }
        }
      }

      // The per-variant models under csm: locations, and every wrapper copy inside them.
      ReferenceOpenHashSet<Object> seen = new ReferenceOpenHashSet<>();
      List<Object> copies = new ArrayList<>();
      for (Map.Entry<?, ?> e : cache.entrySet()) {
        Object key = e.getKey();
        if (key instanceof ResourceLocation
            && CsmConstants.MOD_NAMESPACE.equals(((ResourceLocation) key).getNamespace())) {
          walk(e.getValue(), wrapperClass, seen, copies, 0);
        }
      }

      long models = 0;
      long elements = 0;
      ReferenceOpenHashSet<Object> cleared = new ReferenceOpenHashSet<>();
      for (Object copy : copies) {
        if (keep.contains(copy)) {
          continue;
        }
        ModelBlock block = (ModelBlock) modelField.get(copy);
        if (block == null || keep.contains(block)) {
          continue;
        }
        List<BlockPart> parts = block.getElements();
        if (parts == null || keep.contains(parts) || parts.isEmpty()
            || block.parent != null && parts == block.parent.getElements()
            || !cleared.add(parts)) {
          continue;
        }
        models++;
        elements += parts.size();
        parts.clear();
        if (parts instanceof ArrayList) {
          ((ArrayList<BlockPart>) parts).trimToSize();
        }
      }
      Csm.getLogger().info("Released the retextured model copies Forge kept after baking: "
              + "{} element lists ({} elements) from {} wrapper copies, {} ms", models, elements,
          copies.size(), (System.nanoTime() - start) / 1_000_000L);
    } catch (Throwable t) {
      // A Forge whose internals differ: keep the copies, which only costs memory.
      Csm.getLogger().warn("Could not release retextured model copies; models are unaffected",
          t);
    } finally {
      fieldCache.clear();
    }
  }

  private void walk(Object node, Class<?> wrapperClass, ReferenceOpenHashSet<Object> seen,
      List<Object> copies, int depth) {
    if (node == null || depth > 12) {
      return;
    }
    if (node instanceof Collection) {
      for (Object o : (Collection<?>) node) {
        walk(o, wrapperClass, seen, copies, depth + 1);
      }
      return;
    }
    if (node instanceof Map) {
      for (Object o : ((Map<?, ?>) node).values()) {
        walk(o, wrapperClass, seen, copies, depth + 1);
      }
      return;
    }
    if (node instanceof Pair) {
      walk(((Pair<?, ?>) node).getLeft(), wrapperClass, seen, copies, depth + 1);
      walk(((Pair<?, ?>) node).getRight(), wrapperClass, seen, copies, depth + 1);
      return;
    }
    if (!(node instanceof IModel) || !seen.add(node)) {
      return;
    }
    if (wrapperClass.isInstance(node)) {
      copies.add(node);
      return;
    }
    // Forge's container models (weighted variants, multi-part, multipart): the models inside.
    if (!node.getClass().getName().startsWith("net.minecraftforge.client.model.")) {
      return;
    }
    for (Field f : fieldsOf(node.getClass())) {
      try {
        walk(f.get(node), wrapperClass, seen, copies, depth + 1);
      } catch (Throwable ignored) {
        // Unreadable field: nothing to release there.
      }
    }
  }

  private List<Field> fieldsOf(Class<?> type) {
    List<Field> fields = fieldCache.get(type);
    if (fields != null) {
      return fields;
    }
    fields = new ArrayList<>();
    for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
      for (Field f : c.getDeclaredFields()) {
        Class<?> t = f.getType();
        if (Modifier.isStatic(f.getModifiers()) || t.isPrimitive()) {
          continue;
        }
        if (IModel.class.isAssignableFrom(t) || Collection.class.isAssignableFrom(t)
            || Map.class.isAssignableFrom(t) || Pair.class.isAssignableFrom(t)
            || t == Object.class) {
          try {
            f.setAccessible(true);
            fields.add(f);
          } catch (Throwable ignored) {
            // Skip a field the JVM will not open.
          }
        }
      }
    }
    fieldCache.put(type, fields);
    return fields;
  }
}
