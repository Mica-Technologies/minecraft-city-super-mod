package com.micatechnologies.minecraft.csm.codeutils;

import com.google.common.collect.ImmutableMap;
import com.micatechnologies.minecraft.csm.CsmConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
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
    // Forge's OBJ loader, which reads the files, is told of the reload on its own.
  }

  @Override
  public boolean accepts(ResourceLocation modelLocation) {
    return CsmConstants.MOD_NAMESPACE.equals(modelLocation.getNamespace())
        && modelLocation.getPath().endsWith(".obj");
  }

  @Override
  public IModel loadModel(ResourceLocation modelLocation) throws Exception {
    IModel model = OBJLoader.INSTANCE.loadModel(modelLocation);
    return model instanceof OBJModel ? new CsmObjModel((OBJModel) model) : model;
  }

  @Override
  public String toString() {
    return "CsmObjModelLoader";
  }

  /** Forge's OBJ model, whose copies stay this class and whose bakes are shared. */
  public static final class CsmObjModel extends OBJModel {

    CsmObjModel(OBJModel from) throws IllegalAccessException {
      super(from.getMatLib(),
          (ResourceLocation) CsmPartBakeCache.objLocationField().get(from));
      CsmPartBakeCache.objCustomDataField().set(this,
          CsmPartBakeCache.objCustomDataField().get(from));
    }

    private static IModel wrap(IModel model) {
      if (model instanceof OBJModel && !(model instanceof CsmObjModel)) {
        try {
          return new CsmObjModel((OBJModel) model);
        } catch (IllegalAccessException e) {
          return model;
        }
      }
      return model;
    }

    @Override
    public IModel process(ImmutableMap<String, String> customData) {
      return wrap(super.process(customData));
    }

    @Override
    public IModel retexture(ImmutableMap<String, String> textures) {
      return wrap(super.retexture(textures));
    }

    @Override
    public IBakedModel bake(IModelState state, VertexFormat format,
        Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
      return CsmPartBakeCache.instance().bakeObj(this, state, format, bakedTextureGetter,
          () -> new PackedObjModel(super.bake(state, format, bakedTextureGetter),
              unshadedSprites(bakedTextureGetter)));
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
   * An OBJ bake whose quads are built and fully packed, once and on one thread, before any
   * caller sees them.
   */
  public static final class PackedObjModel extends BakedModelWrapper<IBakedModel> {

    private volatile boolean ready;

    /** Sprites whose quads are drawn without directional shading; usually empty. */
    private final Set<TextureAtlasSprite> unshaded;

    /** The model's own quads, and the same list with the unshaded quads swapped in. */
    private List<BakedQuad> ownQuads;
    private List<BakedQuad> ownQuadsUnshaded;

    PackedObjModel(IBakedModel baked, Set<TextureAtlasSprite> unshaded) {
      super(baked);
      this.unshaded = unshaded;
    }

    @Override
    @Nonnull
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side,
        long rand) {
      if (!ready) {
        synchronized (this) {
          if (!ready) {
            // The model's own quads, the ones any state without OBJ state gets.
            List<BakedQuad> own = originalModel.getQuads(null, null, 0L);
            for (BakedQuad quad : own) {
              quad.getVertexData();
            }
            if (!unshaded.isEmpty()) {
              List<BakedQuad> copy = new ArrayList<>(own.size());
              for (BakedQuad quad : own) {
                copy.add(unshaded.contains(quad.getSprite())
                    ? new BakedQuad(quad.getVertexData(), quad.getTintIndex(), quad.getFace(),
                        quad.getSprite(), false, quad.getFormat())
                    : quad);
              }
              ownQuads = own;
              ownQuadsUnshaded = Collections.unmodifiableList(copy);
            }
            ready = true;
          }
        }
      }
      List<BakedQuad> quads = originalModel.getQuads(state, side, rand);
      // Forge's OBJ bake hands back the same list for every state that carries no OBJ state.
      return quads == ownQuads ? ownQuadsUnshaded : quads;
    }
  }
}
