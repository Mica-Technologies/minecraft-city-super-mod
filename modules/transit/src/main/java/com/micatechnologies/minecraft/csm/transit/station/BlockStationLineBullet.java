package com.micatechnologies.minecraft.csm.transit.station;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformFixture;
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
 * A line bullet: a round enamel plate on the wall in a line's colour with its letter or number.
 * A click steps through the sixteen invented lines in {@link #LINES}, a sneaking click back
 * ({@link PlatformSigns}); the blockstate swaps the plate's texture. The first four are the
 * platform line strip's (1, 4, 7, E in the agencies' colours) with a sister line each; the order
 * is the generator's ({@code LINES} in {@code gen_transit_stations.py}).
 *
 * @since 2026.9
 */
public class BlockStationLineBullet extends BlockPlatformFixture implements
    ICsmTileEntityProvider {

  /** What each line's bullet reads, in the order a click steps through them. */
  public static final String[] LINES = {"1", "2", "4", "5", "7", "8", "E", "F", "A", "C", "K",
      "M", "S", "T", "X", "Z"};

  /** Which line the bullet shows, counting from 1. */
  public static final PropertyInteger LINE = PropertyInteger.create("line", 1, LINES.length);

  /**
   * Constructs a line bullet.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockStationLineBullet(String registryName, double[] box) {
    super(registryName, box);
    setDefaultState(getDefaultState().withProperty(LINE, 1));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, LINE);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state.withProperty(LINE, PlatformSigns.valueAt(world, pos, LINES.length));
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand == EnumHand.MAIN_HAND && !world.isRemote) {
      PlatformSigns.step(world, pos, player, LINES.length, "csm.transit.line", LINES);
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
