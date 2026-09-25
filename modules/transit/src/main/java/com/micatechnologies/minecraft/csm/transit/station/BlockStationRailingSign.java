package com.micatechnologies.minecraft.csm.transit.station;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.transit.platform.PlatformSigns;
import com.micatechnologies.minecraft.csm.transit.platform.TileEntityPlatformSign;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The stair entrance's railing carrying a name plate on both faces: SUBWAY, METRO or a station
 * name ({@link StationLegends}), stepped by a click with an empty hand, back by a sneaking one.
 * The plate hangs only on a straight run (railing on two opposite sides and nothing on the
 * others); anywhere else the block is plain railing. The value lives in the platform signs'
 * {@link TileEntityPlatformSign} and reaches the model as {@link #LEGEND}.
 *
 * @since 2026.9
 */
public class BlockStationRailingSign extends BlockStationRailing implements
    ICsmTileEntityProvider {

  /** What the plate reads, counting from 1. */
  public static final PropertyInteger LEGEND = PropertyInteger.create("legend", 1,
      StationLegends.LEGENDS.length);

  /**
   * Constructs a railing with a name plate.
   *
   * @param registryName its registry name
   */
  public BlockStationRailingSign(String registryName) {
    super(registryName, false);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, NORTH, EAST, WEST, SOUTH, LEGEND);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos) {
    return super.getActualState(state, world, pos).withProperty(LEGEND,
        PlatformSigns.valueAt(world, pos, StationLegends.LEGENDS.length));
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !player.getHeldItem(hand).isEmpty()) {
      return super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
    }
    if (!world.isRemote) {
      StationLegends.step(world, pos, player, StationLegends.LEGENDS.length, false);
    }
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityPlatformSign.class;
  }

  @Override
  public String getTileEntityName() {
    return PlatformSigns.TILE_ENTITY_NAME;
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityPlatformSign();
  }
}
