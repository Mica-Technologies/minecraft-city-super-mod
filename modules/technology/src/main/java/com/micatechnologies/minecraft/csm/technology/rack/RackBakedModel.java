package com.micatechnologies.minecraft.csm.technology.rack;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A fillable rack's model: the frame its blockstate draws, with the units its tile entity holds
 * added as quads baked into the chunk mesh, so nothing about them runs per frame.
 *
 * <p>The units arrive through {@link AbstractBlockRack#CONTENTS}. They are opaque, so they are
 * added in every pass but the translucent one, where a cabinet draws only its door's glass.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class RackBakedModel extends BakedModelWrapper<IBakedModel> {

  private final AbstractBlockRack block;

  public RackBakedModel(IBakedModel original, AbstractBlockRack block) {
    super(original);
    this.block = block;
  }

  @Override
  @Nonnull
  public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side,
      long rand) {
    List<BakedQuad> base = originalModel.getQuads(state, side, rand);
    if (side != null || !(state instanceof IExtendedBlockState)
        || MinecraftForgeClient.getRenderLayer() == BlockRenderLayer.TRANSLUCENT) {
      return base;
    }
    RackContents contents = ((IExtendedBlockState) state).getValue(AbstractBlockRack.CONTENTS);
    if (contents == null || contents.isEmpty()) {
      return base;
    }
    List<BakedQuad> units = RackModels.quads(block.getFrame(), contents,
        state.getValue(AbstractBlockRack.FACING), block.cellOf(state));
    if (units.isEmpty()) {
      return base;
    }
    List<BakedQuad> out = new ArrayList<>(base.size() + units.size());
    out.addAll(base);
    out.addAll(units);
    return out;
  }
}
