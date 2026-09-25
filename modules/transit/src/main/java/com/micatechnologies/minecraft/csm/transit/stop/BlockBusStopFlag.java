package com.micatechnologies.minecraft.csm.transit.stop;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficsigns.BlockTrafficSign;
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
 * A bus stop flag: the agency's sign, printed on both faces, with up to three route plates hung
 * under it on the same post. It is a road sign -- a {@link BlockTrafficSign} with its own models
 * -- so it stands on Roads' sign posts and does everything a road sign does: the eight facings,
 * the facing taken from the sign or post below, the extension post onto a slab or through a
 * guardrail, the setback in front of a signal arm or under a span wire, and the back-to-back
 * pairing that puts two flags on one post.
 *
 * <p>The flag stands on the top of its block and above it, like the road signs' tall plates; the
 * route plates hang in the block under the flag, where a click can reach them. Clicking a route
 * plate steps its number up, 1 to 99 and then "no plate"; a sneaking click steps it down. A click
 * on the flag itself steps the top plate. The numbers are kept by a
 * {@link TileEntityBusStopFlag}; which plates are there is actual state ({@link #ROUTE1} ...),
 * which the blockstate turns into the plate's texture or a clear one, so the plates are baked in
 * every shift model; the numbers are drawn by {@link TileEntityBusStopFlagRenderer}.</p>
 *
 * @since 2026.9
 */
public class BlockBusStopFlag extends BlockTrafficSign implements ICsmTileEntityProvider {

  /** Whether the top route plate is there. Actual state, from the tile entity. */
  public static final PropertyBool ROUTE1 = PropertyBool.create("route1");
  /** Whether the middle route plate is there. */
  public static final PropertyBool ROUTE2 = PropertyBool.create("route2");
  /** Whether the bottom route plate is there. */
  public static final PropertyBool ROUTE3 = PropertyBool.create("route3");

  private static final PropertyBool[] ROUTES = {ROUTE1, ROUTE2, ROUTE3};

  /**
   * Where each route plate's middle is, in sixteenths above the block's bottom. Written from
   * {@code gen_transit_stops.py} (PLATE_TOPS less half of PLATE_HT); the renderer centres the
   * numbers on these and a click is sorted between them.
   */
  static final float[] PLATE_MIDDLE_Y = {6.65f, 3.95f, 1.25f};

  /**
   * Where a route plate's number is centred, in sixteenths from the plate's left edge as it is
   * read: the middle of the part the plate's bus pictogram leaves free (texels 16 to 64 of 64 on
   * a plate 10 wide). The plates run x 3 to 13, centred on the post.
   */
  static final float NUMBER_FROM_LEFT = 6.25f;

  /** A plate's width, in sixteenths. */
  static final float PLATE_WIDTH = 10.0f;

  /** The plates' two faces in the unshifted model: the front at z 0, the back at 0.5. */
  static final float FACE_FRONT_Z = 0.0f;
  static final float FACE_BACK_Z = 0.5f;

  private final BusAgency agency;

  /**
   * Constructs a flag.
   *
   * @param registryName its registry name, ending in the agency
   */
  public BlockBusStopFlag(String registryName) {
    super(registryName);
    this.agency = BusAgency.ofRegistryName(registryName);
  }

  /**
   * The agency whose flag this is, from the end of its registry name.
   *
   * @return the agency
   */
  public BusAgency getAgency() {
    return agency;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, DOWNWARD, SHIFT, ROUTE1, ROUTE2, ROUTE3);
  }

  /**
   * The road sign's shift and extension post, plus which route plates are there. Falls back to
   * the top plate alone when the tile entity is not there yet: this runs during chunk load before
   * tile entities are attached, and a missing one must not throw.
   */
  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos) {
    IBlockState actual = super.getActualState(state, world, pos);
    TileEntity te = BusStopSigns.tileEntity(world, pos);
    for (int i = 0; i < ROUTES.length; i++) {
      boolean there = te instanceof TileEntityBusStopFlag
          ? ((TileEntityBusStopFlag) te).getRoute(i) != 0 : i == 0;
      actual = actual.withProperty(ROUTES[i], there);
    }
    return actual;
  }

  /**
   * Steps the clicked route plate, and consumes the click on both sides, as the other
   * configurable signs do: returning false on the server would let the held item be used.
   */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    if (!world.isRemote) {
      TileEntity te = world.getTileEntity(pos);
      if (te instanceof TileEntityBusStopFlag) {
        int plate = plateAt(hitY * 16.0);
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
   * @param y the hit's height in sixteenths above the block's bottom
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
