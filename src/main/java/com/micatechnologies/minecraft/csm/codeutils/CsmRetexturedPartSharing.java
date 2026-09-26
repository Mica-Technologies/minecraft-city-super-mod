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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockPart;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Before the models are baked, makes the element copies Forge made to retexture CSM's variants
 * share one {@link BlockPart} per distinct element.
 *
 * <p>For every blockstate variant that sets {@code textures}, Forge's
 * {@code VanillaModelWrapper.retexture} copies the whole model, and every element in it: a new
 * {@code BlockPart} and a new {@code HashMap} of its faces, holding the very same corner vectors,
 * rotation and face objects as the element it was copied from. (It copies so that it can drop
 * the faces whose texture a variant sets to {@code ""}.) With every module that was 1.26 million
 * element copies and their face maps, about 400 MB, alive from the blockstates' loading until
 * {@link CsmUnbakedModelRelease} empties the copies after the bake: through the texture stitch
 * and the bake, the two moments the heap is fullest at launch.</p>
 *
 * <p>At the block atlas's {@link TextureStitchEvent.Pre}, which the model loader posts once every
 * blockstate and item model is loaded and before any sprite or bake, this walks the variant
 * models under {@code csm:} locations in Forge's model cache, and in each retextured copy's own
 * element list replaces every element by the first copy equal to it: the same corner, rotation
 * and face objects, the same shade flag, and the faces in the same order. That is everything a
 * bake reads from an element, compared by identity, so a shared element bakes exactly the quads,
 * in exactly the order, its copy would have; and it is what {@link CsmPartBakeCache} keys a part
 * on, so its sharing is unchanged. The element lists stay the copies' own; only their entries
 * change. A model file's own elements (a value of the cache) are never touched, nor is a list a
 * model borrows from its parent.</p>
 *
 * <p>Nothing changes an element once it is loaded: the faces are dropped inside
 * {@code retexture}, before this runs. <b>The rule this makes:</b> never change a
 * {@code BlockPart} of a CSM model, or its face map, once the blockstates are loaded; it may be
 * shared by many variants. Start the game with {@code -Dcsm.noElementSharing=true} to turn this
 * off.</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public final class CsmRetexturedPartSharing {

  /** Set to {@code true} to leave every retextured copy its own elements. */
  public static final String OFF_PROPERTY = "csm.noElementSharing";

  private static final String WRAPPER =
      "net.minecraftforge.client.model.ModelLoader$VanillaModelWrapper";

  private final Map<Class<?>, List<Field>> fieldCache = new HashMap<>();

  /**
   * Shares equal element copies, once the models are loaded and before the atlas is stitched.
   *
   * @param event the stitch event
   */
  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void onStitchPre(TextureStitchEvent.Pre event) {
    if (Boolean.getBoolean(OFF_PROPERTY)
        || event.getMap() != Minecraft.getMinecraft().getTextureMapBlocks()) {
      return;
    }
    long start = System.nanoTime();
    try {
      Class<?> wrapperClass = Class.forName(WRAPPER);
      Field modelField = wrapperClass.getDeclaredField("model");
      modelField.setAccessible(true);
      Field cacheField = ModelLoaderRegistry.class.getDeclaredField("cache");
      cacheField.setAccessible(true);
      Map<?, ?> cache = (Map<?, ?>) cacheField.get(null);

      // Model files: never changed. Everything reachable only inside a variant is a copy.
      // (A flag or uvlock variant of a file is a new wrapper round the file's own element list,
      // so it is the lists that are protected, not only the wrappers.)
      ReferenceOpenHashSet<Object> files = new ReferenceOpenHashSet<>();
      ReferenceOpenHashSet<Object> lists = new ReferenceOpenHashSet<>();
      for (Object model : cache.values()) {
        if (wrapperClass.isInstance(model)) {
          files.add(model);
          for (ModelBlock block = (ModelBlock) modelField.get(model); block != null
              && lists.add(block.getElements()); block = block.parent) {
            // With its parents, whose elements a model without its own borrows.
          }
        }
      }
      int protectedLists = lists.size();
      ReferenceOpenHashSet<Object> seen = new ReferenceOpenHashSet<>();
      List<Object> copies = new ArrayList<>();
      for (Map.Entry<?, ?> e : cache.entrySet()) {
        Object key = e.getKey();
        if (key instanceof ResourceLocation
            && CsmConstants.MOD_NAMESPACE.equals(((ResourceLocation) key).getNamespace())) {
          walk(e.getValue(), wrapperClass, seen, copies, 0);
        }
      }
      if (copies.isEmpty()) {
        return;
      }

      Map<PartKey, BlockPart> shared = new HashMap<>();
      long parts = 0;
      long replaced = 0;
      for (Object copy : copies) {
        if (files.contains(copy)) {
          continue;
        }
        ModelBlock block = (ModelBlock) modelField.get(copy);
        if (block == null) {
          continue;
        }
        List<BlockPart> elements = block.getElements();
        if (elements == null || elements.isEmpty()
            || block.parent != null && elements == block.parent.getElements()
            || !lists.add(elements)) {
          continue;
        }
        for (int i = 0; i < elements.size(); i++) {
          BlockPart part = elements.get(i);
          if (part == null) {
            continue;
          }
          parts++;
          BlockPart first = shared.putIfAbsent(new PartKey(part), part);
          if (first != null && first != part) {
            elements.set(i, first);
            replaced++;
          }
        }
      }
      Csm.getLogger().info("Shared equal retextured element copies before the stitch: {} of {} "
              + "elements in {} copies replaced by {} distinct ones, {} ms", replaced, parts,
          lists.size() - protectedLists, shared.size(), (System.nanoTime() - start) / 1_000_000L);
    } catch (Throwable t) {
      // A Forge whose internals differ: every copy keeps its own elements, which only costs memory.
      Csm.getLogger().warn("Could not share retextured element copies; models are unaffected", t);
    } finally {
      fieldCache.clear();
    }
  }

  /** An element by everything a bake reads from it, the objects compared by identity. */
  private static final class PartKey {

    private final BlockPart part;
    private final Object[] faces;
    private final int hash;

    PartKey(BlockPart part) {
      this.part = part;
      Map<EnumFacing, BlockPartFace> map = part.mapFaces;
      faces = new Object[map.size() * 2];
      int h = System.identityHashCode(part.positionFrom) * 31
          + System.identityHashCode(part.positionTo);
      h = h * 31 + System.identityHashCode(part.partRotation);
      h = h * 31 + (part.shade ? 1 : 0);
      int i = 0;
      for (Map.Entry<EnumFacing, BlockPartFace> e : map.entrySet()) {
        faces[i++] = e.getKey();
        faces[i++] = e.getValue();
        h = h * 31 + System.identityHashCode(e.getKey());
        h = h * 31 + System.identityHashCode(e.getValue());
      }
      hash = h;
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof PartKey)) {
        return false;
      }
      PartKey k = (PartKey) o;
      if (hash != k.hash || part.positionFrom != k.part.positionFrom
          || part.positionTo != k.part.positionTo || part.partRotation != k.part.partRotation
          || part.shade != k.part.shade || faces.length != k.faces.length) {
        return false;
      }
      for (int i = 0; i < faces.length; i++) {
        if (faces[i] != k.faces[i]) {
          return false;
        }
      }
      return true;
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
        // Unreadable field: its copies keep their own elements.
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
