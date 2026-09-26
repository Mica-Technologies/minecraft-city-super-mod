package com.micatechnologies.minecraft.csm.transit.stop;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficsigns.BlockTrafficSign;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import com.micatechnologies.minecraft.csm.codeutils.SignShift;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A bus stop flag: the agency's sign, printed on both faces, with up to three route plates hung
 * under it, the lot hanging off the side of the post as NYC's bus stop flags do: the post runs up
 * one edge of the sign, and the sign's edge is bolted across the post's front. It is a road sign -- a {@link BlockTrafficSign} with its own models
 * -- so it stands on Roads' sign posts and does everything a road sign does: the eight facings,
 * the facing taken from the sign or post below, the extension post onto a slab or through a
 * guardrail, the setback in front of a signal arm or under a span wire, and the back-to-back
 * pairing that puts two flags on one post.
 *
 * <p>The flag stands on the top of its block and above it, like the road signs' tall plates; the
 * route plates hang in the block under the flag, where a click can reach them. Clicking a route
 * plate steps its number up, 1 to 99 and then "no plate"; a sneaking click steps it down. A click
 * on the flag itself moves it to the other side of the post: reaching to the reader's right (the
 * post at its left edge, the default) or left. The side and the numbers are kept by a
 * {@link TileEntityBusStopFlag}; which plates are there is actual state ({@link #ROUTE1} ...),
 * which the blockstate turns into the plate's texture or a clear one, so the plates are baked in
 * every shift model; the numbers are drawn by {@link TileEntityBusStopFlagRenderer}. The side is
 * actual state too, crossed with the shift in {@link #HANG}, which picks the model.</p>
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
   * The shift and the side together, the one property that picks the flag's model (see
   * {@link BusStopFlagHang}). Actual state, from the sign system's shift and the tile entity.
   */
  public static final PropertyEnum<BusStopFlagHang> HANG =
      PropertyEnum.create("hang", BusStopFlagHang.class);

  /**
   * The flag's span across the model, in sixteenths, hanging to the reader's right and left:
   * {@code SIDES} in {@code gen_transit_stops.py}. The model faces north and the sign frame
   * mirrors x, so the reader's right is the model's low x; either way the sign's inner edge
   * ends at the post's middle, across its front (see the generator for why not further).
   */
  static final float[] FLAG_X_RIGHT = {-2.0f, 8.0f};
  static final float[] FLAG_X_LEFT = {8.0f, 18.0f};

  /** The post's sides, in sixteenths: its widest bar, x 6.5 to 9.5. */
  private static final double POST_X0 = 6.5;
  private static final double POST_X1 = 9.5;

  /**
   * Where a click turns from the top route plate (its top at 7.9) to the flag (its bottom at
   * 8.3), in sixteenths above the block's bottom.
   */
  static final double FLAG_FROM_Y = 8.1;

  /** What {@link #plateAt} returns for a click on the flag rather than a plate. */
  static final int FLAG = -1;

  /**
   * Where each route plate's middle is, in sixteenths above the block's bottom. Written from
   * {@code gen_transit_stops.py} (PLATE_TOPS less half of PLATE_HT); the renderer centres the
   * numbers on these and a click is sorted between them.
   */
  static final float[] PLATE_MIDDLE_Y = {6.65f, 3.95f, 1.25f};

  /**
   * Where a route plate's number is centred, in sixteenths from the plate's left edge as it is
   * read: the middle of the part the plate's bus pictogram leaves free (texels 16 to 64 of 64 on
   * a plate 10 wide). The plates span {@link #FLAG_X_RIGHT} or {@link #FLAG_X_LEFT}.
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
    return new CsmBlockStateContainer(this, FACING, DOWNWARD, SHIFT, HANG, ROUTE1, ROUTE2,
        ROUTE3);
  }

  /**
   * Registers the item model, and a state mapper that leaves {@link #SHIFT} out of the model
   * location. The flag's model is picked by {@link #HANG}, which already carries the shift, so
   * the blockstate has no {@code shift} variants and a location per shift would be three
   * identical variants: without the mapper every flag has 2,304 model locations, with it 768.
   */
  @Override
  @SideOnly(Side.CLIENT)
  public void registerModels() {
    super.registerModels();
    ModelLoader.setCustomStateMapper(this, new StateMap.Builder().ignore(SHIFT).build());
  }

  /**
   * The middle of the flag and its plates across the model, in sixteenths.
   *
   * @param left whether the flag hangs to the reader's left
   *
   * @return the model x of the middle
   */
  static float middleX(boolean left) {
    float[] span = left ? FLAG_X_LEFT : FLAG_X_RIGHT;
    return (span[0] + span[1]) / 2.0f;
  }

  /**
   * The road sign's shift and extension post, plus which route plates are there and which side
   * the flag hangs. Falls back to the top plate alone, hanging right, when the tile entity is not
   * there yet: this runs during chunk load before tile entities are attached, and a missing one
   * must not throw.
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
    boolean left = te instanceof TileEntityBusStopFlag && ((TileEntityBusStopFlag) te).isLeft();
    return actual.withProperty(HANG, BusStopFlagHang.of(actual.getValue(SHIFT), left));
  }

  /**
   * The road sign's box, narrowed across to the flag and the post: the flag reaches a sixteenth
   * past the block on its outer side and leaves the far side of the post empty, and a box over
   * the empty side would take clicks meant for whatever stands there. Read off {@link #HANG},
   * which is right whenever the state is the actual one, as it is for the outline and the
   * collision.
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    AxisAlignedBB box = super.getBlockBoundingBox(state, source, pos);
    boolean left = state.getValue(HANG).isLeft();
    double x0 = left ? POST_X0 : FLAG_X_RIGHT[0];
    double x1 = left ? FLAG_X_LEFT[1] : POST_X1;
    return new AxisAlignedBB(x0 / 16.0, box.minY, box.minZ, x1 / 16.0, box.maxY, box.maxZ);
  }

  /**
   * The road sign's collision, narrowed across as {@link #getBlockBoundingBox} is: set back, it is
   * the thin slab at the flag's plane that a road sign's is, only as wide as the flag and post.
   */
  @Override
  @SuppressWarnings("deprecation")
  @Nullable
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess worldIn,
      BlockPos pos) {
    IBlockState actual = state.getActualState(worldIn, pos);
    if (actual.getValue(SHIFT) != SignShift.SETBACK) {
      return super.getCollisionBoundingBox(state, worldIn, pos);
    }
    AxisAlignedBB box = getBlockBoundingBox(actual, worldIn, pos);
    return RotationUtils.rotateBoundingBoxByFacing(
        new AxisAlignedBB(box.minX, box.minY, 0.75, box.maxX, box.maxY, 0.8125),
        actual.getValue(FACING));
  }

  /**
   * Steps the clicked route plate, or moves the flag to the other side of the post when the flag
   * itself is clicked, and consumes the click on both sides, as the other configurable signs do:
   * returning false on the server would let the held item be used.
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
        TileEntityBusStopFlag flag = (TileEntityBusStopFlag) te;
        int plate = plateAt(hitY * 16.0);
        world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON,
            SoundCategory.BLOCKS, 0.3F, 0.8F);
        if (plate == FLAG) {
          player.sendStatusMessage(new TextComponentTranslation(flag.flipSide()
              ? "csm.transit.flag.side_left" : "csm.transit.flag.side_right"), true);
          return true;
        }
        int route = flag.step(plate, player.isSneaking());
        player.sendStatusMessage(route == 0
            ? new TextComponentTranslation("csm.transit.flag.route_none", plate + 1)
            : new TextComponentTranslation("csm.transit.flag.route", plate + 1, route), true);
      }
    }
    return true;
  }

  /**
   * What a click at a height hits: the flag above {@link #FLAG_FROM_Y}, and below it the plates,
   * each lower plate owning the band from halfway to the plate above down.
   *
   * @param y the hit's height in sixteenths above the block's bottom
   *
   * @return {@link #FLAG}, or the plate, 0 to 2
   */
  static int plateAt(double y) {
    if (y >= FLAG_FROM_Y) {
      return FLAG;
    }
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
