package com.micatechnologies.minecraft.csm.parks.planters;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.BlockUtils;
import com.micatechnologies.minecraft.csm.codeutils.CsmBakedModelWrappers;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Puts {@link PlanterBakedModel} round every planter's world models, and gives the planters a
 * block colour that is their plant's, so a planted grass or fern is the biome's green.
 *
 * <p>The wrapper goes through {@link CsmBakedModelWrappers}, never a registry walk of its own:
 * that finds nothing under VintageFix's dynamic resources, and the plants would never be drawn
 * in the packs that run it. Item models ({@code inventory}) are left alone: a planter item shows
 * an empty planter.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public final class PlanterModels {

  private PlanterModels() {
  }

  /** Registers the wrapper; from the Parks client proxy's pre-initialization. */
  public static void register() {
    CsmBakedModelWrappers.register(PlanterModels::wrap);
  }

  @Nullable
  private static IBakedModel wrap(ModelResourceLocation location, IBakedModel model) {
    if (model instanceof PlanterBakedModel || "inventory".equals(location.getVariant())) {
      return null;
    }
    Block block = ForgeRegistries.BLOCKS.getValue(
        new ResourceLocation(location.getNamespace(), location.getPath()));
    return block instanceof ICsmPlanter ? new PlanterBakedModel(model, block) : null;
  }

  /**
   * The planters' block colour: the planted plant's, for the plant's tinted quads. The planters'
   * own models have no tint index, so it is never asked for them.
   */
  public static void registerColours() {
    IBlockColor colour = (state, world, pos, tintIndex) -> {
      if (world == null || pos == null) {
        return -1;
      }
      TileEntity te = BlockUtils.getTileEntitySafe(world, pos);
      IBlockState plant = te instanceof TileEntityPlanter
          ? ((TileEntityPlanter) te).getPlantState() : null;
      return plant == null ? -1
          : Minecraft.getMinecraft().getBlockColors().colorMultiplier(plant, world, pos,
              tintIndex);
    };
    List<Block> planters = new ArrayList<>();
    for (Block block : CsmRegistry.getBlocks()) {
      if (block instanceof ICsmPlanter) {
        planters.add(block);
      }
    }
    Minecraft.getMinecraft().getBlockColors()
        .registerBlockColorHandler(colour, planters.toArray(new Block[0]));
  }
}
