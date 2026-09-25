package com.micatechnologies.minecraft.csm.transit.stop;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A bus stop flag: the agency's sign standing out sideways from the pole, printed on both faces,
 * with up to three route plates hung under it. It is a fitting, so it draws its own length of
 * pole (with the cap, as the top of a stop usually is).
 *
 * <p>Clicking a route plate steps its number up, 1 to 99 and then "no plate"; a sneaking click
 * steps it down. A click on the flag itself steps the top plate. Which plate is clicked is read
 * off the height of the hit, less the stack's settling. The numbers are kept by a
 * {@link TileEntityBusStopFlag}; which plates are there is actual state ({@link #ROUTE1} ...),
 * and the numbers are drawn by {@link TileEntityBusStopFlagRenderer}, white on the agency's
 * colour.</p>
 *
 * @since 2026.9
 */
public class BlockBusStopFlag extends BlockBusStopFitting implements ICsmTileEntityProvider {

  /** Whether the top route plate is there. Actual state, from the tile entity. */
  public static final PropertyBool ROUTE1 = PropertyBool.create("route1");
  /** Whether the middle route plate is there. */
  public static final PropertyBool ROUTE2 = PropertyBool.create("route2");
  /** Whether the bottom route plate is there. */
  public static final PropertyBool ROUTE3 = PropertyBool.create("route3");

  private static final PropertyBool[] ROUTES = {ROUTE1, ROUTE2, ROUTE3};

  /**
   * Where each route plate's middle is, in sixteenths above the block's bottom, facing north.
   * Written from {@code gen_transit_stops.py} (BULLET_TOPS less half of BULLET_HT); the renderer
   * centres the numbers on these and a click is sorted between them.
   */
  static final float[] PLATE_MIDDLE_Y = {5.2f, 3.2f, 1.2f};

  /** The route plates' horizontal middle (the plate runs x 9.6 to 16.6), facing north. */
  static final float PLATE_MIDDLE_X = 13.1f;

  /** The plates' two faces (z 7.85 and 8.15), facing north. */
  static final float PLATE_NORTH_Z = 7.85f;
  static final float PLATE_SOUTH_Z = 8.15f;

  /**
   * Constructs a flag.
   *
   * @param registryName its registry name, ending in the agency
   * @param box          its box facing north, in sixteenths, pole and flag together
   */
  public BlockBusStopFlag(String registryName, double[] box) {
    super(registryName, box);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, POLE, CAP, BASE, ROUTE1, ROUTE2, ROUTE3);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    IBlockState actual = super.getActualState(state, world, pos);
    TileEntity te = BusStopStack.tileEntity(world, pos);
    for (int i = 0; i < ROUTES.length; i++) {
      boolean there = te instanceof TileEntityBusStopFlag
          ? ((TileEntityBusStopFlag) te).getRoute(i) != 0 : i == 0;
      actual = actual.withProperty(ROUTES[i], there);
    }
    return actual;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    if (!world.isRemote) {
      TileEntity te = world.getTileEntity(pos);
      if (te instanceof TileEntityBusStopFlag) {
        double y = (hitY - getRoadSurfaceOffset(world, pos)) * 16.0;
        int plate = plateAt(y);
        int route = ((TileEntityBusStopFlag) te).step(plate, player.isSneaking());
        world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON,
            SoundCategory.BLOCKS, 0.3F, 0.8F);
        player.sendStatusMessage(route == 0
            ? new TextComponentTranslation("csm.transit.flag.route_none", plate + 1)
            : new TextComponentTranslation("csm.transit.flag.route", plate + 1, route), true);
      }
    }
    return true;
  }

  /**
   * The plate a click at a height hits: the flag and the top plate step the top plate, and each
   * lower plate owns the band from halfway to the plate above down.
   *
   * @param y the hit's height in sixteenths above the block's bottom, facing north
   *
   * @return the plate, 0 to 2
   */
  static int plateAt(double y) {
    for (int i = 0; i < PLATE_MIDDLE_Y.length - 1; i++) {
      if (y >= (PLATE_MIDDLE_Y[i] + PLATE_MIDDLE_Y[i + 1]) / 2.0) {
        return i;
      }
    }
    return PLATE_MIDDLE_Y.length - 1;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityBusStopFlag.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitybusstopflag";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityBusStopFlag();
  }
}
