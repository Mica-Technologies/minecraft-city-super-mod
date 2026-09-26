package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.CsmConstants;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.obj.OBJModel;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Lets go of the pixel copies CSM's still sprites keep in the Java heap, as soon as the block
 * atlas is uploaded.
 *
 * <p>Vanilla keeps every sprite's pixels, at every mipmap level, in
 * {@link TextureAtlasSprite}'s frame data after it has uploaded them to the atlas texture. Only
 * two things read them afterwards: the animation tick, which uploads the next frame of an
 * animated sprite, and a bake that turns a sprite's pixels into geometry (an item drawn from its
 * texture, a model whose root is {@code builtin/generated}, baked by Forge's
 * {@code ItemLayerModel}). The atlas is in video memory and draws from there. With every module
 * CSM's copies are about 240 MB of the heap that nothing uses.</p>
 *
 * <p>They go in two steps:</p>
 * <ol>
 *   <li><b>At the block atlas's {@link TextureStitchEvent.Post}</b>, which the model loader
 *   posts once the atlas is uploaded and before its bake loop, every {@code csm:} still sprite
 *   is released except those a bake may still read: the textures of every model whose root is
 *   {@code builtin/generated}, and every texture of any model this class does not know (another
 *   loader's model could read pixels too). What it knows are vanilla JSON models, OBJ models and
 *   Forge's variant containers, which only read a sprite's position. The models are read from
 *   Forge's model cache and the loader's variant map, which hold every model the loop will
 *   bake. This is what lowers the launch peak: the pixels are gone while the models bake,
 *   which is when the heap is fullest.</li>
 *   <li><b>After the bake</b> ({@link ModelBakeEvent}, lowest priority, so every bake that reads
 *   pixels has run), the sprites kept in step 1 are released too.</li>
 * </ol>
 *
 * <p>Animated sprites keep theirs. A sprite's size, position and UVs are separate fields and are
 * untouched. Other mods' and vanilla's sprites are left alone, since some of them are baked
 * again later (Forge's dynamic bucket reads its sprites' pixels each time it meets a new fluid).
 * Step 1 does nothing on a stitch outside a model load (no CSM model in Forge's cache). Start
 * the game with {@code -Dcsm.lateSpriteRelease=true} to skip step 1 and release everything after
 * the bake, as before.</p>
 *
 * <p>A resource reload stitches a new atlas from new sprite objects, reading every texture from
 * its file again, and both steps run again. Changing the mipmap level in the video settings is
 * such a reload.</p>
 *
 * <p><b>The rules this makes:</b> never read a CSM sprite's pixels once the block atlas is
 * uploaded ({@link TextureAtlasSprite#getFrameTextureData(int)} on a still sprite: its frame
 * count is 0), except in the bake of a {@code builtin/generated} model; code that needs a
 * texture's pixels reads the PNG from the resource manager itself, and nothing in CSM reads an
 * atlas sprite's pixels today. A new kind of model that reads pixels as it bakes needs nothing
 * here, since a model of a kind not known here keeps all of its sprites through the bake.</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public final class CsmSpriteDataRelease {

  /** Set to {@code true} to release every still sprite only after the bake. */
  public static final String LATE_PROPERTY = "csm.lateSpriteRelease";

  private static final String LOADER = "net.minecraftforge.client.model.ModelLoader";

  /** The name vanilla gives the root model of every {@code builtin/generated} item model. */
  private static final String GENERATION_MARKER = "generation marker";

  /** Forge's variant containers: they bake the models inside them and read no pixels. */
  private static final Set<String> CONTAINERS = new HashSet<>();

  static {
    CONTAINERS.add(LOADER + "$WeightedRandomModel");
    CONTAINERS.add(LOADER + "$MultipartModel");
    CONTAINERS.add("net.minecraftforge.client.model.MultiModel");
  }

  private final Map<Class<?>, List<Field>> fieldCache = new HashMap<>();

  /**
   * Step 1: once the block atlas is uploaded, releases every CSM still sprite no bake reads.
   *
   * @param event the stitch event
   */
  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void onStitchPost(TextureStitchEvent.Post event) {
    TextureMap atlas = event.getMap();
    if (Boolean.getBoolean(LATE_PROPERTY)
        || atlas != Minecraft.getMinecraft().getTextureMapBlocks()) {
      return;
    }
    long start = System.nanoTime();
    try {
      Set<String> keep = new HashSet<>();
      if (!pixelReaderTextures(keep)) {
        return;
      }
      int[] counts = new int[3];
      long pixels = release(atlas, keep, counts);
      Csm.getLogger().info("Released the pixel data of {} still CSM sprites ({} Mpx at full size) "
              + "once the atlas was uploaded; {} kept until after the bake for the models that "
              + "read pixels as they bake, {} animated sprites keep theirs; {} ms", counts[0],
          pixels / 1_000_000L, counts[1], counts[2], (System.nanoTime() - start) / 1_000_000L);
    } catch (Throwable t) {
      // A Forge whose internals differ: step 2 releases everything after the bake instead.
      Csm.getLogger().warn("Could not release sprite pixel data at upload; it is released after "
          + "the bake instead", t);
    } finally {
      fieldCache.clear();
    }
  }

  /**
   * Step 2: after the bake, releases the CSM still sprites step 1 kept (or all of them).
   *
   * @param event the bake event
   */
  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void onModelBake(ModelBakeEvent event) {
    long start = System.nanoTime();
    try {
      int[] counts = new int[3];
      long pixels = release(Minecraft.getMinecraft().getTextureMapBlocks(), null, counts);
      Csm.getLogger().info("Released the pixel data of {} still CSM sprites ({} Mpx at full "
              + "size) after the bake; {} animated sprites keep theirs, {} ms", counts[0],
          pixels / 1_000_000L, counts[2], (System.nanoTime() - start) / 1_000_000L);
    } catch (Throwable t) {
      // An atlas whose internals differ: keep the data, which only costs memory.
      Csm.getLogger().warn("Could not release sprite pixel data; textures are unaffected", t);
    }
  }

  /**
   * Clears the frame data of every CSM still sprite in the atlas that still has it and is not
   * named in {@code keep}.
   *
   * @param keep the sprites to leave alone, or null for none
   * @param counts released, kept and animated sprites, filled in
   * @return the pixels released, at full size
   */
  private static long release(TextureMap atlas, Set<String> keep, int[] counts)
      throws ReflectiveOperationException {
    @SuppressWarnings("unchecked")
    Map<String, TextureAtlasSprite> uploaded = (Map<String, TextureAtlasSprite>)
        ObfuscationReflectionHelper.findField(TextureMap.class, "field_94252_e").get(atlas);
    String prefix = CsmConstants.MOD_NAMESPACE + ":";
    long pixels = 0;
    for (TextureAtlasSprite sprite : uploaded.values()) {
      if (sprite == null || !sprite.getIconName().startsWith(prefix)
          || sprite.getFrameCount() == 0) {
        continue;
      }
      if (sprite.hasAnimationMetadata()) {
        counts[2]++;
        continue;
      }
      if (keep != null && keep.contains(sprite.getIconName())) {
        counts[1]++;
        continue;
      }
      pixels += (long) sprite.getIconWidth() * sprite.getIconHeight();
      sprite.clearFramesTextureData();
      counts[0]++;
    }
    return pixels;
  }

  /**
   * Collects the textures of every model the coming bake may read the pixels of.
   *
   * @param keep the texture names, filled in
   * @return false if no CSM model is loaded (a stitch outside a model load), so nothing is known
   */
  private boolean pixelReaderTextures(Set<String> keep) throws ReflectiveOperationException {
    Class<?> wrapperClass = Class.forName(LOADER + "$VanillaModelWrapper");
    Field modelField = wrapperClass.getDeclaredField("model");
    modelField.setAccessible(true);
    Field cacheField = ModelLoaderRegistry.class.getDeclaredField("cache");
    cacheField.setAccessible(true);
    Map<?, ?> cache = (Map<?, ?>) cacheField.get(null);

    boolean csmLoaded = false;
    for (Object key : cache.keySet()) {
      if (key instanceof ResourceLocation
          && CsmConstants.MOD_NAMESPACE.equals(((ResourceLocation) key).getNamespace())) {
        csmLoaded = true;
        break;
      }
    }
    if (!csmLoaded) {
      return false;
    }

    ReferenceOpenHashSet<Object> seen = new ReferenceOpenHashSet<>();
    Set<ResourceLocation> textures = new HashSet<>();
    walk(cache.values(), wrapperClass, modelField, seen, textures, 0);
    // The loader's variant map: every model its bake loop bakes (the cache holds them too).
    Class<?> vanillaLoader = Class.forName(LOADER + "$VanillaLoader");
    Field loaderField = vanillaLoader.getDeclaredField("loader");
    loaderField.setAccessible(true);
    Object loader = loaderField.get(vanillaLoader.getEnumConstants()[0]);
    if (loader != null) {
      Field stateModels = ModelLoader.class.getDeclaredField("stateModels");
      stateModels.setAccessible(true);
      walk(((Map<?, ?>) stateModels.get(loader)).values(), wrapperClass, modelField, seen,
          textures, 0);
    }
    for (ResourceLocation texture : textures) {
      keep.add(texture.toString());
    }
    return true;
  }

  private void walk(Object node, Class<?> wrapperClass, Field modelField,
      ReferenceOpenHashSet<Object> seen, Set<ResourceLocation> keep, int depth) {
    if (node == null || depth > 16) {
      return;
    }
    if (node instanceof Collection) {
      for (Object o : (Collection<?>) node) {
        walk(o, wrapperClass, modelField, seen, keep, depth + 1);
      }
      return;
    }
    if (node instanceof Map) {
      for (Object o : ((Map<?, ?>) node).values()) {
        walk(o, wrapperClass, modelField, seen, keep, depth + 1);
      }
      return;
    }
    if (node instanceof Pair) {
      walk(((Pair<?, ?>) node).getLeft(), wrapperClass, modelField, seen, keep, depth + 1);
      walk(((Pair<?, ?>) node).getRight(), wrapperClass, modelField, seen, keep, depth + 1);
      return;
    }
    if (!(node instanceof IModel) || !seen.add(node)) {
      return;
    }
    IModel model = (IModel) node;
    if (wrapperClass.isInstance(node)) {
      // A JSON model: only one drawn from its texture reads pixels.
      try {
        if (readsPixels((ModelBlock) modelField.get(node))) {
          keepAll(model, keep);
        }
      } catch (Throwable t) {
        keepAll(model, keep);
      }
      return;
    }
    if (node instanceof OBJModel) {
      return;
    }
    if (!CONTAINERS.contains(node.getClass().getName())) {
      // A kind of model not known here: it may read the pixels of anything it names.
      keepAll(model, keep);
    }
    for (Field f : fieldsOf(node.getClass())) {
      try {
        walk(f.get(node), wrapperClass, modelField, seen, keep, depth + 1);
      } catch (Throwable ignored) {
        // Unreadable field: its models are not known, but a container reads no pixels itself.
      }
    }
  }

  private static void keepAll(IModel model, Set<ResourceLocation> keep) {
    try {
      keep.addAll(model.getTextures());
    } catch (Throwable ignored) {
      // A model whose textures cannot be listed could not have had them stitched either.
    }
  }

  /**
   * Whether a JSON model is baked from its textures' pixels: its root is {@code
   * builtin/generated}, or a parent is not resolved yet and so could be.
   */
  private static boolean readsPixels(ModelBlock block) {
    if (block == null) {
      return false;
    }
    ModelBlock b = block;
    for (int guard = 0; guard < 64; guard++) {
      if (b.parent == null) {
        return b.getParentLocation() != null || GENERATION_MARKER.equals(b.name);
      }
      b = b.parent;
    }
    return true;
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
