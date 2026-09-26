package com.micatechnologies.minecraft.csm.codeutils;

import com.google.common.collect.ImmutableMap;
import com.micatechnologies.minecraft.csm.CsmConstants;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
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
          () -> super.bake(state, format, bakedTextureGetter));
    }
  }
}
