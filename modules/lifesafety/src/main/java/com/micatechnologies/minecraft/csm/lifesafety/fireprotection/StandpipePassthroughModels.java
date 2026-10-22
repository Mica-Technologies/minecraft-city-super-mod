package com.micatechnologies.minecraft.csm.lifesafety.fireprotection;

import com.micatechnologies.minecraft.csm.codeutils.CsmBakedModelWrappers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Draws a dressed {@link BlockStandpipePassthrough} as the block it copies, baked into the chunk
 * mesh: the copied block's own model's quads, in whichever pass that block draws in, with the
 * pass-through's trim plates (its multipart, {@code covered=true}) in the cutout pass. Nothing is
 * built per frame and nothing is cached: the quads are the copied model's own lists.
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public final class StandpipePassthroughModels {

  private StandpipePassthroughModels() {
  }

  /** Registers the wrapper; called from the Life Safety client proxy's pre-initialization. */
  public static void register() {
    CsmBakedModelWrappers.register(StandpipePassthroughModels::wrap);
  }

  @Nullable
  private static IBakedModel wrap(ModelResourceLocation location, IBakedModel model) {
    if (model instanceof Dressed || "inventory".equals(location.getVariant())
        || !"csm".equals(location.getNamespace())) {
      return null;
    }
    Block block = ForgeRegistries.BLOCKS.getValue(
        new ResourceLocation(location.getNamespace(), location.getPath()));
    return block instanceof BlockStandpipePassthrough ? new Dressed(model) : null;
  }

  @Nullable
  private static IBlockState mimic(@Nullable IBlockState state) {
    return state instanceof IExtendedBlockState
        ? ((IExtendedBlockState) state).getValue(BlockStandpipePassthrough.MIMIC) : null;
  }

  /** The pass-through's model, with the copied block's quads added when it is dressed. */
  private static final class Dressed extends BakedModelWrapper<IBakedModel> {

    Dressed(IBakedModel original) {
      super(original);
    }

    @Override
    @Nonnull
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side,
        long rand) {
      BlockRenderLayer layer = MinecraftForgeClient.getRenderLayer();
      List<BakedQuad> own = layer == null || layer == BlockRenderLayer.CUTOUT
          ? originalModel.getQuads(state, side, rand) : Collections.emptyList();
      IBlockState mimic = mimic(state);
      if (mimic == null || (layer != null && !mimic.getBlock().canRenderInLayer(mimic, layer))) {
        return own;
      }
      List<BakedQuad> copied = Minecraft.getMinecraft().getBlockRendererDispatcher()
          .getModelForState(mimic).getQuads(mimic, side, rand);
      if (own.isEmpty()) {
        return copied;
      }
      List<BakedQuad> out = new ArrayList<>(own.size() + copied.size());
      out.addAll(own);
      out.addAll(copied);
      return out;
    }

    @Override
    public boolean isAmbientOcclusion(@Nonnull IBlockState state) {
      IBlockState mimic = mimic(state);
      return mimic != null
          ? Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(mimic)
          .isAmbientOcclusion(mimic)
          : originalModel.isAmbientOcclusion(state);
    }
  }
}
