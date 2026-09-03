package com.micatechnologies.minecraft.csm.lighting;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole.TRAFFIC_POLE_COLOR;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractBrightLightPoleColored extends AbstractBrightLight {

  public static final PropertyEnum<TRAFFIC_POLE_COLOR> COLOR =
      PropertyEnum.create("color", TRAFFIC_POLE_COLOR.class);

  public static final TRAFFIC_POLE_COLOR DEFAULT_COLOR = TRAFFIC_POLE_COLOR.BLACK;

  /**
   * Creates a new {@link BlockStateContainer} for the block with the required properties for
   * rotation and state.
   *
   * @return a new {@link BlockStateContainer} for the block
   *
   * @see Block#createBlockState()
   * @since 1.0
   */
  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, STATE, COLOR);
  }

  @Override
  @SuppressWarnings("deprecation")
  public @NotNull IBlockState getActualState(@NotNull IBlockState state,
      @NotNull IBlockAccess worldIn,
      @NotNull BlockPos pos) {
    // Check if block below is a traffic pole
    AbstractBlockTrafficPole trafficPoleOrNull = getBlockBelowIfTrafficPole(worldIn, pos);
    TRAFFIC_POLE_COLOR color = DEFAULT_COLOR;
    if (trafficPoleOrNull != null) {
      color = trafficPoleOrNull.getTrafficPoleColor();
    }
    return state.withProperty(COLOR, color);
  }

  public static AbstractBlockTrafficPole getBlockBelowIfTrafficPole(IBlockAccess worldIn,
      BlockPos pos) {
    IBlockState stateOfBlockBelow = worldIn.getBlockState(pos.down());
    return (stateOfBlockBelow.getBlock() instanceof AbstractBlockTrafficPole)
        ? (AbstractBlockTrafficPole) stateOfBlockBelow.getBlock()
        : null;
  }
}
