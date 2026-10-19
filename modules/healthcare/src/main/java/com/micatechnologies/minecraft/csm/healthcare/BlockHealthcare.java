package com.micatechnologies.minecraft.csm.healthcare;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.EntityCsmSeat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A Healthcare piece: one block, turned to face the room, drawn from a baked model that
 * {@code gen_healthcare_wards.py} writes. One class serves every simple piece; the registry
 * name, the box and the material are passed in.
 *
 * <p><b>Metadata</b> (so a generator can place any state with {@code /setblock}):</p>
 * <ul>
 *   <li>a {@link Mount#FLOOR floor} piece stores its facing in bits 0-1 as
 *   {@link AbstractBlockRotatableNSEW} does (S 0, W 1, N 2, E 3) and up to two flags in bits 2
 *   and 3;</li>
 *   <li>a {@link Mount#WALL wall} piece stores its facing in bits 0-2 as the facing's own index
 *   (N 2, S 3, W 4, E 5; 0 and 1 read as north) and one flag in bit 3.</li>
 * </ul>
 * <p>The facing is the way the front faces: towards whoever placed it, or out of the wall it is
 * hung on. A subclass names its flags in {@link #storedFlags()} and any property computed from
 * the neighbours in {@link #actualProperties()}.</p>
 *
 * <p>A piece with a seat (a chair, the recliner) is sat on with a right-click on Core's
 * {@link EntityCsmSeat} and left by sneaking.</p>
 *
 * @since 2026.10
 */
public class BlockHealthcare extends AbstractBlockRotatableNSEW {

  /** How a piece is mounted, which decides how its facing is stored and chosen. */
  public enum Mount {
    /** Stands on the floor: facing in two bits, from the placer. */
    FLOOR,
    /** Hangs on a wall: facing in three bits, from the face clicked. */
    WALL
  }

  /** A flag for a screen that is on. */
  public static final PropertyBool ON = PropertyBool.create("on");

  private static final PropertyBool[] NO_FLAGS = new PropertyBool[0];
  private static final IProperty<?>[] NO_PROPERTIES = new IProperty<?>[0];

  /** Where the seat entity sits, in blocks above the block's floor. */
  private static final double SEAT_Y = 0.1;
  /** How far below the top of a seat the rider is placed (the park bench's fit). */
  private static final double RIDER_BELOW_SEAT = 0.12;
  /** The status message when a seat is taken. */
  private static final String SEAT_TAKEN = "csm.healthcare.seat.taken";

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB box;
  private final Mount mount;
  private final boolean collides;
  private double seatTop = -1;
  private double seatForward;

  /**
   * Constructs a piece.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths: {x0, y0, z0, x1, y1, z1}
   * @param material     what it is made of
   * @param mount        floor or wall
   * @param collides     whether it can be walked into (false: a box to aim at only)
   */
  public BlockHealthcare(String registryName, int[] box, HealthcareMaterial material, Mount mount,
      boolean collides) {
    super(stash(registryName, material), material.getSound(), material.getTool(), 0,
        material.getHardness(), material.getHardness() * 2.0F, 0.0F, 0, false);
    this.registryName = registryName;
    this.box = new AxisAlignedBB(box[0] / 16.0, box[1] / 16.0, box[2] / 16.0, box[3] / 16.0,
        box[4] / 16.0, box[5] / 16.0);
    this.mount = mount;
    this.collides = collides;
    IBlockState base = this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH);
    for (PropertyBool flag : storedFlags()) {
      base = base.withProperty(flag, false);
    }
    setDefaultState(defaultExtras(base));
    PENDING.remove();
  }

  private static net.minecraft.block.material.Material stash(String registryName,
      HealthcareMaterial material) {
    PENDING.set(registryName);
    return material.getMaterial();
  }

  /**
   * Gives the piece a seat.
   *
   * @param seatTopPx     the top of the seat above the floor, in sixteenths
   * @param seatForwardPx how far forward of the block's middle the seat's middle is
   *
   * @return this piece
   */
  public BlockHealthcare withSeat(double seatTopPx, double seatForwardPx) {
    this.seatTop = seatTopPx / 16.0;
    this.seatForward = seatForwardPx / 16.0;
    return this;
  }

  /**
   * The flags stored in metadata after the facing, in bit order. Called while the block is
   * being constructed, so it must return constants.
   *
   * @return the flags (at most two on the floor, one on a wall)
   */
  protected PropertyBool[] storedFlags() {
    return NO_FLAGS;
  }

  /**
   * Properties computed from the neighbours in {@link #getActualState}, never stored. Called
   * while the block is being constructed, so it must return constants.
   *
   * @return the properties
   */
  protected IProperty<?>[] actualProperties() {
    return NO_PROPERTIES;
  }

  /**
   * Gives the actual-state properties their default values on the default state.
   *
   * @param state the default state with the facing and flags set
   *
   * @return the state
   */
  protected IBlockState defaultExtras(IBlockState state) {
    return state;
  }

  public Mount getMount() {
    return mount;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    List<IProperty<?>> props = new ArrayList<>();
    props.add(FACING);
    Collections.addAll(props, storedFlags());
    Collections.addAll(props, actualProperties());
    return new CsmBlockStateContainer(this, props.toArray(new IProperty<?>[0]));
  }

  // --- metadata ------------------------------------------------------------------------------

  private int facingBits() {
    return mount == Mount.WALL ? 3 : 2;
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    EnumFacing facing;
    if (mount == Mount.WALL) {
      int index = meta & 7;
      facing = index >= 2 && index <= 5 ? EnumFacing.byIndex(index) : EnumFacing.NORTH;
    } else {
      facing = EnumFacing.byHorizontalIndex(meta & 3);
    }
    IBlockState state = getDefaultState().withProperty(FACING, facing);
    PropertyBool[] flags = storedFlags();
    for (int i = 0; i < flags.length; i++) {
      state = state.withProperty(flags[i], (meta >> (facingBits() + i) & 1) != 0);
    }
    return state;
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    EnumFacing facing = state.getValue(FACING);
    int meta = mount == Mount.WALL ? facing.getIndex() : facing.getHorizontalIndex();
    PropertyBool[] flags = storedFlags();
    for (int i = 0; i < flags.length; i++) {
      if (state.getValue(flags[i])) {
        meta |= 1 << (facingBits() + i);
      }
    }
    return meta;
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing front = mount == Mount.WALL && facing.getAxis().isHorizontal()
        ? facing : placer.getHorizontalFacing().getOpposite();
    return getDefaultState().withProperty(FACING, front);
  }

  // --- shape ---------------------------------------------------------------------------------

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return box;
  }

  @Nullable
  @Override
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return collides ? super.getCollisionBoundingBox(state, source, pos) : NULL_AABB;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess world, @Nonnull IBlockState state,
      @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }

  // --- the seat ------------------------------------------------------------------------------

  /**
   * Whether this piece has a seat.
   *
   * @return true for a chair or the recliner
   */
  public boolean hasSeat() {
    return seatTop >= 0;
  }

  /**
   * The top of the seat in {@code state}, in blocks above the block's floor: the fixed seat
   * height, or more for a seat that can stand on a riser.
   *
   * @param state the block's state
   *
   * @return the seat's top
   */
  protected double seatTop(IBlockState state) {
    return seatTop;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!hasSeat() || player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      EnumFacing front = state.getValue(FACING);
      sit(world, pos, front, seatTop(state), seatForward, player);
    }
    return true;
  }

  /**
   * Seats a player in the block at {@code pos}, facing {@code front}, and lets them off in front
   * of it. One sitter a block.
   *
   * @param world   the world
   * @param pos     the block
   * @param front   the way the sitter faces
   * @param top     the top of the seat above the block's floor, in blocks
   * @param forward how far towards {@code front} of the block's middle the seat is, in blocks
   * @param player  the player
   */
  public static void sit(World world, BlockPos pos, EnumFacing front, double top, double forward,
      EntityPlayer player) {
    double sx = pos.getX() + 0.5 + front.getXOffset() * forward;
    double sz = pos.getZ() + 0.5 + front.getZOffset() * forward;
    BlockPos out = pos.offset(front);
    EntityCsmSeat.sit(world, pos, new AxisAlignedBB(pos), SEAT_TAKEN, sx, pos.getY() + SEAT_Y,
        sz, top - RIDER_BELOW_SEAT - SEAT_Y, front, out.getX() + 0.5, out.getY(),
        out.getZ() + 0.5, player);
  }
}
