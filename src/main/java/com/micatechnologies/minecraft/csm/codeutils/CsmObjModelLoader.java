package com.micatechnologies.minecraft.csm.codeutils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.lang.reflect.Field;
import com.micatechnologies.minecraft.csm.CsmConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.ForgeBlockStateV1;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.client.model.obj.OBJModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Loads CSM's {@code .obj} models, in place of Forge's {@link OBJLoader}, so that their bakes can
 * be shared (see {@link CsmPartBakeCache}).
 *
 * <p>The file is read and parsed by Forge's own loader, exactly as before; this only hands back
 * the result as a {@link CsmObjModel}. Every blockstate variant makes its own copy of the model
 * ({@code process} for its custom data, {@code retexture} for its textures), and the copies stay
 * a {@code CsmObjModel}, whose {@code bake} asks the part bake cache first. The model is otherwise
 * Forge's {@link OBJModel} in every respect, and bakes Forge's {@code OBJBakedModel}.</p>
 *
 * <p>Registered only when the cache is on; then CSM does not add its domain to Forge's OBJ
 * loader, since two loaders accepting one model is an error.</p>
 *
 * <p>Every bake is handed out inside a {@link PackedObjModel}, which builds the OBJ model's quads
 * and packs their vertex data under a lock the first time they are asked for. Forge builds an
 * OBJ model's quads lazily and packs each quad's vertices lazily too, setting its "packed" flag
 * before it has written them; two chunk builder threads drawing a new model at once could take a
 * half-written quad, whose texture coordinates then pointed anywhere on the atlas, and the chunk
 * kept that until it was rebuilt. VintageFix, which bakes models as they are first drawn and
 * bakes them again after a few idle minutes, made that common: an industrial dome pendant came
 * out as a patchwork of other blocks' textures (issue #240).</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public final class CsmObjModelLoader implements ICustomModelLoader {

  /** The one instance, registered with Forge's model loader registry. */
  public static final CsmObjModelLoader INSTANCE = new CsmObjModelLoader();

  private CsmObjModelLoader() {
  }

  @Override
  public void onResourceManagerReload(IResourceManager resourceManager) {
    // Forge's OBJ loader, which reads the files, is told of the reload on its own; what was
    // parsed from the old resources is let go.
    synchronized (PARSED) {
      PARSED.clear();
    }
  }

  /** Starts the options in an OBJ model's location: {@code <file>.obj?<option>&<option>...}. */
  public static final String OPTIONS = ".obj?";

  @Override
  public boolean accepts(ResourceLocation modelLocation) {
    if (!CsmConstants.MOD_NAMESPACE.equals(modelLocation.getNamespace())) {
      return false;
    }
    String path = modelLocation.getPath();
    return path.endsWith(".obj") || path.contains(OPTIONS);
  }

  /**
   * Loads an OBJ model, applying the options its location may carry.
   *
   * <p>A vanilla multipart rule names a model, a rotation and uvlock, nothing else, so it cannot
   * retexture an OBJ model or set Forge's custom data on it as a Forge variant does. Those
   * multipart blockstates (the generators' {@code forge_multipart.convert_obj}) put them in the
   * location instead, after the file name: {@code csm:a/b.obj?flip-v&body=csm:blocks/x}. Each
   * option is {@code key} or {@code key=value}: {@code flip-v}, {@code ambient} and {@code gui3d}
   * are Forge's OBJ custom data (a bare key means {@code true}); {@code transform} names one of
   * Forge's transform presets ({@code forge:default-block}), which an item variant uses for its
   * display transforms; every other key is a material, retextured as a Forge variant's
   * {@code "#key"} texture is. The file is parsed once, however many option sets name it
   * ({@link #parsed}), and every copy shares its faces, so a shape drawn in six colours holds its
   * geometry once.</p>
   */
  @Override
  public IModel loadModel(ResourceLocation modelLocation) throws Exception {
    String path = modelLocation.getPath();
    int at = path.indexOf(OPTIONS);
    if (at < 0) {
      return CsmObjModel.wrap(parsed(modelLocation));
    }
    ResourceLocation file = new ResourceLocation(modelLocation.getNamespace(),
        path.substring(0, at + OPTIONS.length() - 1));
    IModel model = CsmObjModel.wrap(parsed(file));
    ImmutableMap.Builder<String, String> custom = ImmutableMap.builder();
    ImmutableMap.Builder<String, String> textures = ImmutableMap.builder();
    IModelState transform = null;
    boolean anyCustom = false;
    boolean anyTexture = false;
    for (String option : path.substring(at + OPTIONS.length()).split("&")) {
      if (option.isEmpty()) {
        continue;
      }
      int eq = option.indexOf('=');
      String key = eq < 0 ? option : option.substring(0, eq);
      String value = eq < 0 ? "true" : option.substring(eq + 1);
      switch (key) {
        case "flip-v":
        case "ambient":
        case "gui3d":
          custom.put(key, value);
          anyCustom = true;
          break;
        case "transform":
          transform = ForgeBlockStateV1.Transforms.get(value).orElseThrow(
              () -> new IllegalArgumentException("unknown transform " + value + " in "
                  + modelLocation));
          break;
        default:
          textures.put("#" + key, value);
          anyTexture = true;
          break;
      }
    }
    if (anyCustom) {
      model = model.process(custom.build());
    }
    if (anyTexture) {
      model = model.retexture(textures.build());
    }
    if (transform != null && model instanceof CsmObjModel) {
      model = ((CsmObjModel) model).withDefaultState(transform);
    }
    return model;
  }

  @Override
  public String toString() {
    return "CsmObjModelLoader";
  }

  /**
   * Each file's parse, by file, until the next resource reload. Forge's OBJ loader reads and
   * parses the file again on every call; the model registry used to call it once a file, but a
   * file named with several option sets is several locations, each loaded on its own.
   */
  private static final Map<ResourceLocation, IModel> PARSED = new HashMap<>();

  /**
   * Forge's parse of a file, read once until the next resource reload, its equal vertices made
   * one object.
   *
   * <p>Forge's OBJ parser gives every corner of every face a vertex of its own, each with its
   * own copy of the position, though the file names each position, texture coordinate and
   * normal once and faces refer to them by number: a triangle mesh holds every position about
   * six times. The parsed model is kept for as long as the game runs (its bakes build their
   * quads lazily, when first drawn), so that was most of the memory CSM's OBJ models held:
   * 2.3 million vertices and their positions for 750 thousand faces. Corners equal in
   * position, texture coordinate, normal and material are now one vertex. A face's vertices
   * are only read once parsed: every bake transforms each face into a new one
   * ({@code Group.applyTransform}); the one write, a missing texture coordinate filled in per
   * corner, is why a corner without one is left alone.</p>
   */
  private static IModel parsed(ResourceLocation file) throws Exception {
    synchronized (PARSED) {
      IModel model = PARSED.get(file);
      if (model == null) {
        model = OBJLoader.INSTANCE.loadModel(file);
        if (model instanceof OBJModel) {
          shareVertices(((OBJModel) model).getMatLib());
        }
        PARSED.put(file, model);
      }
      return model;
    }
  }

  private static void shareVertices(OBJModel.MaterialLibrary lib) {
    Map<VertexKey, OBJModel.Vertex> seen = new HashMap<>();
    for (OBJModel.Group group : lib.getGroups().values()) {
      for (OBJModel.Face face : group.getFaces()) {
        OBJModel.Vertex[] verts = face.getVertices();
        if (verts == null) {
          continue;
        }
        for (int i = 0; i < verts.length; i++) {
          OBJModel.Vertex v = verts[i];
          if (v == null || !v.hasTextureCoordinate()) {
            continue;
          }
          OBJModel.Vertex shared = seen.putIfAbsent(new VertexKey(v), v);
          if (shared != null) {
            verts[i] = shared;
          }
        }
      }
    }
  }

  /** A parsed vertex by value: its position, and its texture coordinate, normal and material. */
  private static final class VertexKey {

    private final float x;
    private final float y;
    private final float z;
    private final float w;
    private final Object uv;
    private final Object normal;
    private final Object material;
    private final int hash;

    VertexKey(OBJModel.Vertex v) {
      this.x = v.getPos().x;
      this.y = v.getPos().y;
      this.z = v.getPos().z;
      this.w = v.getPos().w;
      // The parser hands every corner the same texture coordinate and normal objects for the
      // same index in the file, so identity is the file's own numbering.
      this.uv = v.getTextureCoordinate();
      this.normal = v.hasNormal() ? v.getNormal() : null;
      this.material = v.getMaterial();
      this.hash = Objects.hash(Float.floatToIntBits(x), Float.floatToIntBits(y),
          Float.floatToIntBits(z), Float.floatToIntBits(w), System.identityHashCode(uv),
          System.identityHashCode(normal), System.identityHashCode(material));
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof VertexKey)) {
        return false;
      }
      VertexKey k = (VertexKey) o;
      return Float.floatToIntBits(x) == Float.floatToIntBits(k.x)
          && Float.floatToIntBits(y) == Float.floatToIntBits(k.y)
          && Float.floatToIntBits(z) == Float.floatToIntBits(k.z)
          && Float.floatToIntBits(w) == Float.floatToIntBits(k.w)
          && uv == k.uv && normal == k.normal && material == k.material;
    }

    @Override
    public int hashCode() {
      return hash;
    }
  }

  /** Forge's OBJ model, whose copies stay this class and whose bakes are shared. */
  public static final class CsmObjModel extends OBJModel {

    /**
     * The model's own default state when a location option set one ({@code transform}); null
     * for Forge's (the identity).
     */
    private IModelState defaultState;

    CsmObjModel(OBJModel from) throws IllegalAccessException {
      super(from.getMatLib(),
          (ResourceLocation) CsmPartBakeCache.objLocationField().get(from));
      CsmPartBakeCache.objCustomDataField().set(this,
          CsmPartBakeCache.objCustomDataField().get(from));
      if (from instanceof CsmObjModel) {
        this.defaultState = ((CsmObjModel) from).defaultState;
      }
    }

    static IModel wrap(IModel model) {
      if (model instanceof OBJModel && !(model instanceof CsmObjModel)) {
        try {
          return new CsmObjModel((OBJModel) model);
        } catch (IllegalAccessException e) {
          return model;
        }
      }
      return model;
    }

    private IModel keepState(IModel copy) {
      if (defaultState != null && copy instanceof CsmObjModel && copy != this) {
        ((CsmObjModel) copy).defaultState = defaultState;
      }
      return copy;
    }

    /**
     * A copy of this model whose default state is {@code state}: a Forge variant's
     * {@code "transform"}, which the variant would otherwise have composed with the model's.
     */
    CsmObjModel withDefaultState(IModelState state) {
      try {
        CsmObjModel copy = new CsmObjModel(this);
        copy.defaultState = state;
        return copy;
      } catch (IllegalAccessException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public IModelState getDefaultState() {
      return defaultState != null ? defaultState : super.getDefaultState();
    }

    @Override
    public IModel process(ImmutableMap<String, String> customData) {
      return keepState(wrap(super.process(customData)));
    }

    @Override
    public IModel retexture(ImmutableMap<String, String> textures) {
      return keepState(wrap(super.retexture(textures)));
    }

    @Override
    public IBakedModel bake(IModelState state, VertexFormat format,
        Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
      return CsmPartBakeCache.instance().bakeObj(this, state, format, bakedTextureGetter,
          () -> new PackedObjModel(super.bake(state, format, bakedTextureGetter),
              unshadedSprites(bakedTextureGetter), particleSprite(bakedTextureGetter)));
    }

    /**
     * The sprite Forge's OBJ bake reports as its particle once its quads are built: the sprite of
     * the last face it builds (groups in the library's order, faces in each group's). Forge
     * answers its white sprite until then, which is what a multipart model, reading its first
     * part's particle when it is made, would keep for good, so every block broken would scatter
     * white. Answering it from the start gives the multipart model the sprite a Forge variant
     * shows after its first draw.
     */
    @Nullable
    private TextureAtlasSprite particleSprite(
        Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
      OBJModel.MaterialLibrary lib = getMatLib();
      OBJModel.Face last = null;
      for (OBJModel.Group group : lib.getGroups().values()) {
        for (OBJModel.Face face : group.getFaces()) {
          last = face;
        }
      }
      if (last == null) {
        return null;
      }
      OBJModel.Material material = lib.getMaterial(last.getMaterialName());
      if (material == null) {
        return null;
      }
      if (material.isWhite()) {
        return net.minecraftforge.client.model.ModelLoader.White.INSTANCE;
      }
      if (material.getTexture().getTextureLocation().getPath().startsWith("#")) {
        return bakedTextureGetter.apply(new ResourceLocation("missingno"));
      }
      return bakedTextureGetter.apply(material.getTexture().getTextureLocation());
    }

    /**
     * The sprites of this model's {@code unshaded_*} materials, as retextured for this variant.
     * Forge's OBJ loader keeps no custom data of its own, so a material's name is how a model
     * asks for it: a material called {@code unshaded_lens} is drawn without directional shading.
     */
    private Set<TextureAtlasSprite> unshadedSprites(
        Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
      Set<TextureAtlasSprite> out = Collections.newSetFromMap(new IdentityHashMap<>());
      OBJModel.MaterialLibrary lib = getMatLib();
      for (String name : lib.getMaterialNames()) {
        if (name.startsWith(UNSHADED_PREFIX)) {
          OBJModel.Material material = lib.getMaterial(name);
          out.add(bakedTextureGetter.apply(material.getTexture().getTextureLocation()));
        }
      }
      return out;
    }
  }

  /**
   * An OBJ material whose name starts with this is baked without directional ("diffuse")
   * shading, as a JSON element with {@code "shade": false} is. For glass that is meant to read
   * as lit: a face pointing straight down is otherwise drawn at half brightness, so a street
   * light's lens, seen from the road, could never be brighter than a dull tan.
   */
  public static final String UNSHADED_PREFIX = "unshaded_";

  /**
   * Forge's {@code OBJBakedModel.quads}, the list its {@code getQuads} hands out for any state
   * without OBJ state; {@code null} if this Forge has no such field, and then the quads are left
   * as Forge built them.
   */
  private static final Field OBJ_BAKED_QUADS = openObjBakedQuads();

  private static Field openObjBakedQuads() {
    try {
      Field f = OBJModel.OBJBakedModel.class.getDeclaredField("quads");
      f.setAccessible(true);
      return f;
    } catch (ReflectiveOperationException | RuntimeException e) {
      return null;
    }
  }

  /**
   * An OBJ bake whose quads are built and fully packed, once and on one thread, before any
   * caller sees them.
   *
   * <p>Forge builds an OBJ quad as an {@code UnpackedBakedQuad}, which keeps its unpacked
   * {@code float[4][elements][4]} (about 900 bytes) for good after packing it into the vanilla
   * {@code int[]}. Once packed, each is swapped for a plain {@link BakedQuad} on the same
   * {@code int[]} (the same quad, field for field, as {@code CsmQuadSharing} would see it), and
   * the list is written back into the OBJ bake, so the unpacked arrays are freed. A plain quad
   * reaches Forge's lighting pipeline through {@code LightUtil.putBakedQuad}, which reads the
   * packed data: what is drawn does not change. Measured on the street lights (#99): those
   * arrays were most of the 27.5 MB the OBJ fixtures added.</p>
   */
  public static final class PackedObjModel extends BakedModelWrapper<IBakedModel> {

    private volatile boolean ready;

    /** Sprites whose quads are drawn without directional shading; usually empty. */
    private final Set<TextureAtlasSprite> unshaded;

    /**
     * Only when the quads could not be written back: the model's own list, and the same list
     * with the unshaded quads swapped in.
     */
    private List<BakedQuad> ownQuads;
    private List<BakedQuad> ownQuadsUnshaded;

    /** The particle sprite Forge's bake reports once built; null to ask the bake. */
    @Nullable
    private final TextureAtlasSprite particle;

    PackedObjModel(IBakedModel baked, Set<TextureAtlasSprite> unshaded,
        @Nullable TextureAtlasSprite particle) {
      super(baked);
      this.unshaded = unshaded;
      this.particle = particle;
    }

    @Override
    @Nonnull
    public TextureAtlasSprite getParticleTexture() {
      return particle != null ? particle : originalModel.getParticleTexture();
    }

    @Override
    @Nonnull
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side,
        long rand) {
      if (!ready) {
        synchronized (this) {
          if (!ready) {
            build();
            ready = true;
          }
        }
      }
      List<BakedQuad> quads = originalModel.getQuads(state, side, rand);
      // Forge's OBJ bake hands back the same list for every state that carries no OBJ state.
      return ownQuads != null && quads == ownQuads ? ownQuadsUnshaded : quads;
    }

    private void build() {
      // The model's own quads, the ones any state without OBJ state gets.
      List<BakedQuad> own = originalModel.getQuads(null, null, 0L);
      List<BakedQuad> packed = new ArrayList<>(own.size());
      for (BakedQuad quad : own) {
        boolean diffuse = quad.shouldApplyDiffuseLighting()
            && !unshaded.contains(quad.getSprite());
        boolean plain = quad.getClass() == BakedQuad.class
            && diffuse == quad.shouldApplyDiffuseLighting();
        packed.add(plain ? quad
            : new BakedQuad(quad.getVertexData(), quad.getTintIndex(), quad.getFace(),
                quad.getSprite(), diffuse, quad.getFormat()));
      }
      if (OBJ_BAKED_QUADS != null && originalModel instanceof OBJModel.OBJBakedModel) {
        try {
          OBJ_BAKED_QUADS.set(originalModel, ImmutableList.copyOf(packed));
          return;
        } catch (IllegalAccessException | RuntimeException e) {
          // fall through: keep Forge's list and swap in ours on the way out
        }
      }
      if (!unshaded.isEmpty()) {
        ownQuads = own;
        ownQuadsUnshaded = Collections.unmodifiableList(packed);
      }
    }
  }
}
