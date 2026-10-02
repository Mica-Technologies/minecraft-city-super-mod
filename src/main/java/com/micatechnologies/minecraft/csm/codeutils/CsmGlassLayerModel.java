package com.micatechnologies.minecraft.csm.codeutils;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A glass-fronted block's model ({@link ICsmGlassFronted}), split between the passes: in the
 * translucent pass only its glass faces, in the cutout pass the rest ({@link CsmGlassLayer}).
 * With no pass set -- an item in a hand or a slot, the breaking overlay -- the model is whole.
 *
 * <p>Put in place through {@link CsmBakedModelWrappers}, so it is applied in a pack running
 * VintageFix's dynamic resources too; left unwrapped there, the block would be drawn whole in
 * both passes and its glass opaque in the cutout one (issue #242, for the glazed doors).</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class CsmGlassLayerModel extends BakedModelWrapper<IBakedModel> {

  public CsmGlassLayerModel(IBakedModel original) {
    super(original);
  }

  @Override
  @Nonnull
  public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side,
      long rand) {
    List<BakedQuad> quads = super.getQuads(state, side, rand);
    BlockRenderLayer layer = MinecraftForgeClient.getRenderLayer();
    if (layer == null || quads.isEmpty()) {
      return quads;
    }
    List<BakedQuad> out = new ArrayList<>(quads.size());
    for (BakedQuad quad : quads) {
      if (CsmGlassLayer.belongsIn(quad.getSprite().getIconName(), layer)) {
        out.add(quad);
      }
    }
    return out;
  }

  /**
   * Puts the split model in place of a glass-fronted block's world model as it is baked.
   * Registered with {@link CsmBakedModelWrappers} by Core's client proxy.
   *
   * @param location the model's location
   * @param model    the baked model
   * @return the split model, or {@code null} if this is not a glass-fronted block's world model
   */
  @Nullable
  public static IBakedModel wrap(ModelResourceLocation location, IBakedModel model) {
    if (model instanceof CsmGlassLayerModel || "inventory".equals(location.getVariant())) {
      return null;
    }
    Block block = ForgeRegistries.BLOCKS.getValue(
        new ResourceLocation(location.getNamespace(), location.getPath()));
    if (!(block instanceof ICsmGlassFronted) || !((ICsmGlassFronted) block).isGlassFronted()) {
      return null;
    }
    return new CsmGlassLayerModel(model);
  }
}
