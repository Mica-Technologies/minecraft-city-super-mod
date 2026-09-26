package com.micatechnologies.minecraft.csm.codeutils;

import com.google.common.cache.ForwardingLoadingCache;
import com.google.common.cache.LoadingCache;
import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.CsmConstants;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockPart;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.ModelStateComposition;
import net.minecraftforge.client.model.MultiModelState;
import net.minecraftforge.client.model.SimpleModelState;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.client.model.obj.OBJModel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Bakes each distinct part of CSM's models once, and hands the same baked part to every
 * blockstate variant that names it.
 *
 * <p>Forge bakes one model per variant, and a variant's model is built of parts: its base model
 * and each of its sub-models, each retextured, rotated and flagged for that variant. Those parts
 * repeat. Across every module Forge baked about 244 thousand parts of which only about 50
 * thousand differ (80%; 91% of the OBJ parts), because a variant that differs from another in
 * one sub-model bakes all the others again. A whole variant rarely repeats (20%), so the cache is
 * keyed on parts, not variants.</p>
 *
 * <p>A part is baked in one of two places, and each gets its own hook. Both are active only
 * while the models are being baked: from the block atlas's {@link TextureStitchEvent.Pre}, which
 * the model loader posts just before its bake loop, to {@link ModelBakeEvent}, which follows it.
 * Outside that window every bake goes to Forge unchanged, and the cache holds nothing.</p>
 * <ul>
 *   <li><b>JSON models.</b> Forge's {@code VanillaModelWrapper.bake} already routes every bake
 *   through a small Guava cache in {@code ModelLoader.VanillaLoader} (50 entries for 100 ms,
 *   keyed on the wrapper object, so it almost never hits). That cache field is replaced by one
 *   that forwards to it, and answers from this cache for wrappers whose model file is under
 *   {@code csm:}.</li>
 *   <li><b>OBJ models.</b> CSM's {@code .obj} files are loaded by {@link CsmObjModelLoader}
 *   instead of Forge's {@link OBJLoader}: it loads them through Forge's loader and hands back a
 *   {@link CsmObjModelLoader.CsmObjModel}, an {@link OBJModel} whose copies (every variant
 *   retextures or processes its own) stay that class, and whose {@code bake} asks this cache.</li>
 * </ul>
 *
 * <p>The key is everything the bake reads, compared by value where Forge builds a fresh object
 * per variant and by identity where every variant shares the object read from the file:</p>
 * <ul>
 *   <li>for a JSON part, its elements (their corners, rotation and face objects, which the
 *   retexture copies share with the file's model, and each face map in order), the resolved
 *   texture map, the parent model, the ambient occlusion and gui3d flags, the display transforms
 *   by value, the item overrides, uvlock and the armature (only the empty one is cached);</li>
 *   <li>for an OBJ part, the file's groups (shared by every copy), each material's texture,
 *   colour and texture placement by value, and the custom data (ambient, gui3d, flip-v);</li>
 *   <li>for both, the vertex format, the texture getter, and the model state as the bake sees
 *   it: the transform it answers for the whole model and for each of the nine display
 *   perspectives, by value.</li>
 * </ul>
 * <p>That last is only the whole of what the bake reads from a state if the state cannot also
 * hide a named part or move one joint, which Forge's own states answer for with an empty result.
 * So a part is cached only when its state is built entirely of the states Forge's blockstates
 * make ({@link TRSRTransformation}, {@link ModelRotation}, {@link ItemTransformVec3f}, a
 * {@link SimpleModelState} of perspective transforms, and {@link ModelStateComposition},
 * {@link MultiModelState} and its part states over those). Anything else, an {@code OBJState}
 * for instance, is baked by Forge as before.</p>
 *
 * <p>What sharing a baked part changes: the part's baked object is now the same for several
 * variants, so the unbaked model a baked JSON part keeps (Forge's animation support holds it)
 * is the first variant's, which is equal to the others in everything the bake read. Nothing
 * that is drawn differs. {@code -Dcsm.noPartBakeCache=true} turns the whole mechanism off and
 * loads CSM's OBJ files through Forge's own loader again.</p>
 *
 * <p><b>The rules this makes:</b> a CSM baked model may be the same object for many states and
 * items, so never change one (already a rule for its quad lists), and never key per-state data
 * on a baked model's identity expecting one model per state. And never add another way of
 * loading CSM's {@code .obj} files: they must go through {@link CsmObjModelLoader}.</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public final class CsmPartBakeCache {

  /** Set to {@code true} to bake every part as Forge does, one bake per variant. */
  public static final String DISABLE_PROPERTY = "csm.noPartBakeCache";

  private static final String LOADER = "net.minecraftforge.client.model.ModelLoader";
  private static final TransformType[] PERSPECTIVES = TransformType.values();

  private static final CsmPartBakeCache INSTANCE = new CsmPartBakeCache();

  // Forge internals, all read by their Forge names (Forge's own classes are not obfuscated).
  private Field keyModel;
  private Field keyState;
  private Field keyFormat;
  private Field keyGetter;
  private Field wrapperLocation;
  private Field wrapperModel;
  private Field wrapperUvlock;
  private Field wrapperAnimation;
  private Field animationJoints;
  private Class<?> wrapperClass;
  private Field compositionFirst;
  private Field compositionSecond;
  private Field multiStates;
  private Class<?> partStateClass;
  private Field partStateState;
  private Field simpleMap;
  private Field objLocation;
  private Field objCustomData;
  private Field objMaterials;
  private Field customAmbient;
  private Field customGui3d;
  private Field customFlipV;

  private boolean vanillaHooked;
  private volatile Thread bakeThread;
  private final Map<Key, IBakedModel> baked = new HashMap<>();
  private final IdentityHashMap<Object, Boolean> knownStates = new IdentityHashMap<>();
  private long startNanos;
  private long jsonAsked;
  private long jsonBaked;
  private long objAsked;
  private long objBaked;
  private long notCached;
  private long otherMods;

  private CsmPartBakeCache() {
  }

  /**
   * Installs both hooks. Called from Core's client pre-init, before any model is loaded. If the
   * cache is disabled, or Forge's internals are not as expected, CSM's OBJ files go to Forge's
   * own loader and every bake is Forge's.
   */
  public static void install() {
    if (Boolean.getBoolean(DISABLE_PROPERTY)) {
      OBJLoader.INSTANCE.addDomain(CsmConstants.MOD_NAMESPACE);
      Csm.getLogger().info("Part bake cache off (-D{}=true)", DISABLE_PROPERTY);
      return;
    }
    INSTANCE.hookVanilla();
    try {
      INSTANCE.objFields();
      ModelLoaderRegistry.registerLoader(CsmObjModelLoader.INSTANCE);
    } catch (Throwable t) {
      Csm.getLogger().warn("Part bake cache: OBJ models are baked per variant", t);
      OBJLoader.INSTANCE.addDomain(CsmConstants.MOD_NAMESPACE);
    }
    MinecraftForge.EVENT_BUS.register(INSTANCE);
  }

  /** Replaces the vanilla loader's bake cache with one that forwards to it. */
  @SuppressWarnings("unchecked")
  private void hookVanilla() {
    try {
      Class<?> keyClass = Class.forName(LOADER + "$BakedModelCacheKey");
      keyModel = open(keyClass, "model");
      keyState = open(keyClass, "state");
      keyFormat = open(keyClass, "format");
      keyGetter = open(keyClass, "bakedTextureGetter");
      wrapperClass = Class.forName(LOADER + "$VanillaModelWrapper");
      wrapperLocation = open(wrapperClass, "location");
      wrapperModel = open(wrapperClass, "model");
      wrapperUvlock = open(wrapperClass, "uvlock");
      wrapperAnimation = open(wrapperClass, "animation");
      animationJoints = open(
          Class.forName("net.minecraftforge.client.model.animation.ModelBlockAnimation"),
          "joints");
      compositionFirst = open(ModelStateComposition.class, "first");
      compositionSecond = open(ModelStateComposition.class, "second");
      multiStates = open(MultiModelState.class, "states");
      partStateClass = Class.forName("net.minecraftforge.client.model.MultiModelState$PartState");
      partStateState = open(partStateClass, "state");
      simpleMap = open(SimpleModelState.class, "map");

      Class<?> vanillaLoader = Class.forName(LOADER + "$VanillaLoader");
      Object loader = vanillaLoader.getEnumConstants()[0];
      Field cacheField = open(vanillaLoader, "modelCache");
      LoadingCache<Object, IBakedModel> forge =
          (LoadingCache<Object, IBakedModel>) cacheField.get(loader);
      if (!(forge instanceof Forwarding)) {
        cacheField.set(loader, new Forwarding(forge));
      }
      vanillaHooked = true;
    } catch (Throwable t) {
      Csm.getLogger().warn("Part bake cache: JSON models are baked per variant", t);
    }
  }

  private void objFields() throws ReflectiveOperationException {
    objLocation = open(OBJModel.class, "modelLocation");
    objCustomData = open(OBJModel.class, "customData");
    objMaterials = open(OBJModel.MaterialLibrary.class, "materials");
    Class<?> custom = Class.forName("net.minecraftforge.client.model.obj.OBJModel$CustomData");
    customAmbient = open(custom, "ambientOcclusion");
    customGui3d = open(custom, "gui3d");
    customFlipV = open(custom, "flipV");
  }

  static Field objLocationField() {
    return INSTANCE.objLocation;
  }

  static Field objCustomDataField() {
    return INSTANCE.objCustomData;
  }

  /**
   * Opens the window: the block atlas is stitched just before the model loader's bake loop.
   *
   * @param event the stitch event
   */
  @SubscribeEvent
  public void onStitchPre(TextureStitchEvent.Pre event) {
    if (event.getMap() != Minecraft.getMinecraft().getTextureMapBlocks()) {
      return;
    }
    baked.clear();
    knownStates.clear();
    jsonAsked = jsonBaked = objAsked = objBaked = notCached = otherMods = 0;
    startNanos = System.nanoTime();
    bakeThread = Thread.currentThread();
  }

  /**
   * Closes the window before any bake handler runs, and lets go of everything cached.
   *
   * @param event the bake event
   */
  @SubscribeEvent(priority = EventPriority.HIGHEST)
  public void onModelBake(ModelBakeEvent event) {
    if (bakeThread == null) {
      return;
    }
    bakeThread = null;
    Csm.getLogger().info("Part bake cache: JSON parts {} asked, {} baked; OBJ parts {} asked, {} "
            + "baked; {} not cacheable, {} other mods' passed on; {} distinct kept, {} ms from "
            + "stitch to bake event", jsonAsked, jsonBaked, objAsked, objBaked, notCached,
        otherMods, baked.size(), (System.nanoTime() - startNanos) / 1_000_000L);
    baked.clear();
    knownStates.clear();
  }

  private boolean open() {
    return bakeThread == Thread.currentThread();
  }

  // ---- JSON models --------------------------------------------------------------------------

  /** The bake of a vanilla wrapper, or null to let Forge's cache bake it. */
  private IBakedModel bakeJson(Object cacheKey, LoadingCache<Object, IBakedModel> forge) {
    if (!open()) {
      return null;
    }
    Key key;
    try {
      Object wrapper = keyModel.get(cacheKey);
      ResourceLocation location = (ResourceLocation) wrapperLocation.get(wrapper);
      if (location == null || !CsmConstants.MOD_NAMESPACE.equals(location.getNamespace())) {
        otherMods++;
        return null;
      }
      key = jsonKey(wrapper, (IModelState) keyState.get(cacheKey),
          (VertexFormat) keyFormat.get(cacheKey), keyGetter.get(cacheKey));
    } catch (ReflectiveOperationException | RuntimeException e) {
      key = null;
    }
    if (key == null) {
      notCached++;
      return null;
    }
    jsonAsked++;
    IBakedModel model = baked.get(key);
    if (model == null) {
      model = forge.getUnchecked(cacheKey);
      baked.put(key, model);
      jsonBaked++;
    }
    return model;
  }

  private Key jsonKey(Object wrapper, IModelState state, VertexFormat format, Object getter)
      throws ReflectiveOperationException {
    ModelBlock model = (ModelBlock) wrapperModel.get(wrapper);
    Object animation = wrapperAnimation.get(wrapper);
    if (model == null || animation == null
        || !((Map<?, ?>) animationJoints.get(animation)).isEmpty()) {
      return null;
    }
    KeyBuilder b = new KeyBuilder('J');
    if (!state(b, state)) {
      return null;
    }
    b.id(format).id(getter).id(animation).id(model.parent);
    b.bit(wrapperUvlock.getBoolean(wrapper) ? 1 : 0);
    b.val(model.textures);
    b.bit(model.isAmbientOcclusion() ? 1 : 0).bit(model.isGui3d() ? 1 : 0);
    ItemCameraTransforms camera = model.getAllTransforms();
    for (TransformType type : PERSPECTIVES) {
      ItemTransformVec3f t = camera.getTransform(type);
      b.vec(t.rotation.x, t.rotation.y, t.rotation.z)
          .vec(t.translation.x, t.translation.y, t.translation.z)
          .vec(t.scale.x, t.scale.y, t.scale.z);
    }
    List<ItemOverride> overrides = model.getOverrides();
    b.bit(overrides.size());
    for (ItemOverride o : overrides) {
      b.id(o);
    }
    List<BlockPart> elements = model.getElements();
    b.bit(elements.size());
    for (BlockPart part : elements) {
      b.id(part.positionFrom).id(part.positionTo).id(part.partRotation);
      b.bit(part.shade ? 1 : 0).bit(part.mapFaces.size());
      for (Map.Entry<EnumFacing, BlockPartFace> face : part.mapFaces.entrySet()) {
        b.id(face.getKey()).id(face.getValue());
      }
    }
    return b.build();
  }

  // ---- OBJ models ---------------------------------------------------------------------------

  /** The bake of one of CSM's OBJ models: cached if its key can be made, else Forge's own. */
  IBakedModel bakeObj(OBJModel model, IModelState state, VertexFormat format,
      Function<ResourceLocation, TextureAtlasSprite> getter, Supplier<IBakedModel> bake) {
    if (!open()) {
      return bake.get();
    }
    Key key;
    try {
      key = objKey(model, state, format, getter);
    } catch (ReflectiveOperationException | RuntimeException e) {
      key = null;
    }
    if (key == null) {
      notCached++;
      return bake.get();
    }
    objAsked++;
    IBakedModel baked = this.baked.get(key);
    if (baked == null) {
      baked = bake.get();
      this.baked.put(key, baked);
      objBaked++;
    }
    return baked;
  }

  static CsmPartBakeCache instance() {
    return INSTANCE;
  }

  private Key objKey(OBJModel model, IModelState state, VertexFormat format, Object getter)
      throws ReflectiveOperationException {
    KeyBuilder b = new KeyBuilder('O');
    if (!state(b, state)) {
      return null;
    }
    OBJModel.MaterialLibrary lib = model.getMatLib();
    b.id(format).id(getter).id(lib.getGroups());
    b.val(objLocation.get(model));
    Object custom = objCustomData.get(model);
    b.bit(customAmbient.getBoolean(custom) ? 1 : 0).bit(customGui3d.getBoolean(custom) ? 1 : 0)
        .bit(customFlipV.getBoolean(custom) ? 1 : 0);
    Map<?, ?> materials = (Map<?, ?>) objMaterials.get(lib);
    b.bit(materials.size());
    for (Map.Entry<?, ?> e : materials.entrySet()) {
      OBJModel.Material m = (OBJModel.Material) e.getValue();
      b.val(e.getKey()).val(m.getName());
      Vector4f c = m.getColor();
      if (c == null) {
        b.bit(0);
      } else {
        b.bit(1).vec(c.x, c.y, c.z).vec(c.w, 0, 0);
      }
      OBJModel.Texture t = m.getTexture();
      b.val(t.getPath());
      Vector2f p = t.getPosition();
      Vector2f s = t.getScale();
      b.vec(p == null ? Float.NaN : p.x, p == null ? Float.NaN : p.y, t.getRotation())
          .vec(s == null ? Float.NaN : s.x, s == null ? Float.NaN : s.y, 0);
    }
    return b.build();
  }

  // ---- Model states -------------------------------------------------------------------------

  /**
   * Adds what a bake reads from {@code state} to the key: its transform for the whole model and
   * for each display perspective. False if the state could also answer for a named part (hide
   * one, move a joint), in which case the part is not cached.
   */
  private boolean state(KeyBuilder b, IModelState state) throws IllegalAccessException {
    if (!known(state, 0)) {
      return false;
    }
    transform(b, state.apply(Optional.empty()));
    for (TransformType type : PERSPECTIVES) {
      transform(b, state.apply(Optional.of(type)));
    }
    return true;
  }

  private static void transform(KeyBuilder b, Optional<TRSRTransformation> t) {
    if (!t.isPresent()) {
      b.bit(0);
      return;
    }
    Matrix4f m = t.get().getMatrix();
    b.bit(1).vec(m.m00, m.m01, m.m02).vec(m.m03, m.m10, m.m11).vec(m.m12, m.m13, m.m20)
        .vec(m.m21, m.m22, m.m23).vec(m.m30, m.m31, m.m32).vec(m.m33, 0, 0);
  }

  /**
   * Whether a state is made only of Forge's own states, which answer for any named part (a
   * hidden part, a joint) with nothing. Decided once per object for the shared containers.
   */
  private boolean known(Object state, int depth) throws IllegalAccessException {
    if (state == null || depth > 32) {
      return false;
    }
    if (state instanceof TRSRTransformation || state instanceof ModelRotation
        || state instanceof ItemTransformVec3f) {
      return true;
    }
    Class<?> type = state.getClass();
    if (type == ModelStateComposition.class) {
      return known(compositionFirst.get(state), depth + 1)
          && known(compositionSecond.get(state), depth + 1);
    }
    if (type == partStateClass) {
      return known(partStateState.get(state), depth + 1);
    }
    if (type == MultiModelState.class || type == SimpleModelState.class) {
      Boolean k = knownStates.get(state);
      if (k == null) {
        k = Boolean.TRUE;
        if (type == SimpleModelState.class) {
          for (Object part : ((Map<?, ?>) simpleMap.get(state)).keySet()) {
            if (!(part instanceof TransformType)) {
              k = Boolean.FALSE;
              break;
            }
          }
        } else {
          for (Object inner : ((Map<?, ?>) multiStates.get(state)).values()) {
            if (!known(inner, depth + 1)) {
              k = Boolean.FALSE;
              break;
            }
          }
        }
        knownStates.put(state, k);
      }
      return k;
    }
    return false;
  }

  private static Field open(Class<?> type, String name) throws NoSuchFieldException {
    Field f = type.getDeclaredField(name);
    f.setAccessible(true);
    return f;
  }

  /** Forge's vanilla-wrapper bake cache, with this cache in front of it for CSM's parts. */
  private static final class Forwarding extends
      ForwardingLoadingCache.SimpleForwardingLoadingCache<Object, IBakedModel> {

    private final LoadingCache<Object, IBakedModel> forge;

    Forwarding(LoadingCache<Object, IBakedModel> forge) {
      super(forge);
      this.forge = forge;
    }

    @Override
    public IBakedModel getUnchecked(Object key) {
      IBakedModel m = INSTANCE.bakeJson(key, forge);
      return m != null ? m : forge.getUnchecked(key);
    }

    @Override
    public IBakedModel get(Object key) throws ExecutionException {
      IBakedModel m = INSTANCE.bakeJson(key, forge);
      return m != null ? m : forge.get(key);
    }

    @Override
    public IBakedModel apply(Object key) {
      return getUnchecked(key);
    }
  }

  /** Collects a key: objects compared by identity, objects compared by value, and ints. */
  private static final class KeyBuilder {

    private final List<Object> ids = new ArrayList<>(32);
    private final List<Object> vals = new ArrayList<>(4);
    private final IntArrayList bits = new IntArrayList(96);

    KeyBuilder(char kind) {
      bits.add(kind);
    }

    KeyBuilder id(Object o) {
      ids.add(o);
      return this;
    }

    KeyBuilder val(Object o) {
      vals.add(o);
      return this;
    }

    KeyBuilder bit(int i) {
      bits.add(i);
      return this;
    }

    /** Three floats by their bits, so -0 and 0 (and NaN payloads) stay apart. */
    KeyBuilder vec(float x, float y, float z) {
      bits.add(Float.floatToRawIntBits(x));
      bits.add(Float.floatToRawIntBits(y));
      bits.add(Float.floatToRawIntBits(z));
      return this;
    }

    Key build() {
      return new Key(ids.toArray(), vals.toArray(), bits.toIntArray());
    }
  }

  /** A part's bake inputs; equal keys bake equal models. */
  private static final class Key {

    private final Object[] ids;
    private final Object[] vals;
    private final int[] bits;
    private final int hash;

    Key(Object[] ids, Object[] vals, int[] bits) {
      this.ids = ids;
      this.vals = vals;
      this.bits = bits;
      int h = Arrays.hashCode(bits);
      for (Object o : ids) {
        h = h * 31 + System.identityHashCode(o);
      }
      this.hash = h * 31 + Arrays.hashCode(vals);
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Key)) {
        return false;
      }
      Key k = (Key) o;
      if (k.hash != hash || k.ids.length != ids.length || !Arrays.equals(k.bits, bits)) {
        return false;
      }
      for (int i = 0; i < ids.length; i++) {
        if (ids[i] != k.ids[i]) {
          return false;
        }
      }
      return Arrays.equals(vals, k.vals);
    }
  }
}
