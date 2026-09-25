package com.micatechnologies.minecraft.csm.transit.airport;

import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformFixture;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A piece of jet bridge: a length of tunnel, the cab at the aircraft end, or the rotunda the
 * tunnel swings from. Its facing is the way to the aircraft (the model's north).
 *
 * <p>A piece is drawn by one block but is wider and taller than it: the corridor is two blocks
 * wide and two tall, centred on the block, so a player walks down the middle of a line of these
 * blocks with the walls half a block out either side; the rotunda is three blocks across on its
 * block. A block may draw and collide past its own cell by up to a block, which is as far as the
 * game looks for another block's collision boxes, so the walls, roof and the rotunda's floor are
 * solid without any other block being placed. What the player aims at is the floor alone, so from
 * inside the corridor anything else there can still be clicked.</p>
 *
 * <p>Pieces join along the facing's axis ({@link #AHEAD}, {@link #BEHIND}, actual state): a
 * tunnel or cab next to it, or a rotunda two blocks away (the rotunda's collar reaches the edge of
 * its three blocks). Where a piece does not continue it draws a frame round the open end. The cab
 * is open at the front, where the canopy's bellows would meet a door, and a safety bar across the
 * bumper stops a player walking off it.</p>
 *
 * @since 2026.9
 */
public class BlockJetBridge extends BlockPlatformFixture {

  /** What the piece is. */
  public enum Kind {
    TUNNEL, CAB, ROTUNDA
  }

  /** The bridge continues towards the aircraft. */
  public static final PropertyBool AHEAD = PropertyBool.create("ahead");
  /** The bridge continues towards the terminal. */
  public static final PropertyBool BEHIND = PropertyBool.create("behind");

  /** The light inside the bridge. */
  private static final int LIGHT = 9;

  // the boxes, facing north, in sixteenths: gen_transit_airside.py's JB_* and ROT numbers
  private static final double[][] TUNNEL = {
      {-8, 0, 0, 24, 1.5, 16}, {-8, 1.5, 0, -6.5, 30.5, 16}, {22.5, 1.5, 0, 24, 30.5, 16},
      {-8, 30.5, 0, 24, 31.6, 16}};
  private static final double[][] CAB = {
      {-10, 0, 0, 26, 1.5, 16}, {-10, 1.5, 0, -8.5, 30.5, 16}, {24.5, 1.5, 0, 26, 30.5, 16},
      {-10, 30.5, 0, 26, 31.6, 16}, {-7.8, 0, -5.4, 23.8, 1.5, 0},
      {-7.8, 1.5, -4.8, 23.8, 24, -3.8}};
  private static final double[][] ROTUNDA = {
      {-8, 0, -16, 24, 1.5, 32}, {-14, 0, -8, -8, 1.5, 24}, {24, 0, -8, 30, 1.5, 24},
      {-14, 1.5, -8, -12.5, 30.5, 24}, {28.5, 1.5, -8, 30, 30.5, 24},
      {-8, 1.5, -16, -6.5, 30.5, -14}, {22.5, 1.5, -16, 24, 30.5, -14},
      {-8, 1.5, 30, -6.5, 30.5, 32}, {22.5, 1.5, 30, 24, 30.5, 32},
      {-14, 1.5, -14, -11, 30.5, -11}, {27, 1.5, -14, 30, 30.5, -11},
      {-14, 1.5, 27, -11, 30.5, 30}, {27, 1.5, 27, 30, 30.5, 30},
      {-14, 30.5, -16, 30, 31.6, 32}};
  private static final double[] TUNNEL_FLOOR = {-8, 0, 0, 24, 1.5, 16};
  private static final double[] CAB_FLOOR = {-10, 0, -5.4, 26, 1.5, 16};
  private static final double[] ROTUNDA_FLOOR = {-14, 0, -16, 30, 1.5, 32};

  private final Kind kind;
  private final AxisAlignedBB[] boxes;
  private final AxisAlignedBB floor;

  /**
   * Constructs a piece of jet bridge.
   *
   * @param registryName its registry name
   * @param kind         what it is
   */
  public BlockJetBridge(String registryName, Kind kind) {
    super(registryName, floorOf(kind), false, LIGHT);
    this.kind = kind;
    double[][] from = kind == Kind.TUNNEL ? TUNNEL : kind == Kind.CAB ? CAB : ROTUNDA;
    this.boxes = new AxisAlignedBB[from.length];
    for (int i = 0; i < from.length; i++) {
      this.boxes[i] = box(from[i]);
    }
    this.floor = box(floorOf(kind));
    setDefaultState(getDefaultState().withProperty(AHEAD, false).withProperty(BEHIND, false));
  }

  private static double[] floorOf(Kind kind) {
    return kind == Kind.TUNNEL ? TUNNEL_FLOOR : kind == Kind.CAB ? CAB_FLOOR : ROTUNDA_FLOOR;
  }

  private static AxisAlignedBB box(double[] b) {
    return new AxisAlignedBB(b[0] / 16.0, b[1] / 16.0, b[2] / 16.0, b[3] / 16.0, b[4] / 16.0,
        b[5] / 16.0);
  }

  /** What the piece is. */
  public Kind getKind() {
    return kind;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, AHEAD, BEHIND);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);
    return state.withProperty(AHEAD, continues(world, pos, facing, facing))
        .withProperty(BEHIND, continues(world, pos, facing.getOpposite(), facing));
  }

  /**
   * Whether the bridge carries on from {@code pos} towards {@code dir}: a tunnel or cab on the
   * same axis right beyond this piece's edge, or a rotunda whose collar reaches that edge.
   */
  private boolean continues(IBlockAccess world, BlockPos pos, EnumFacing dir, EnumFacing facing) {
    int edge = kind == Kind.ROTUNDA ? 2 : 1;
    IBlockState next = world.getBlockState(pos.offset(dir, edge));
    if (next.getBlock() instanceof BlockJetBridge
        && ((BlockJetBridge) next.getBlock()).kind != Kind.ROTUNDA
        && next.getValue(FACING).getAxis() == facing.getAxis()) {
      return true;
    }
    IBlockState far = world.getBlockState(pos.offset(dir, edge + 1));
    return far.getBlock() instanceof BlockJetBridge
        && ((BlockJetBridge) far.getBlock()).kind == Kind.ROTUNDA
        && far.getValue(FACING).getAxis() == facing.getAxis();
  }

  /** What the player aims at: the floor, so the corridor's inside can still be clicked. */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return floor;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity,
      boolean isActualState) {
    EnumFacing facing = state.getValue(FACING);
    for (AxisAlignedBB b : boxes) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes,
          RotationUtils.rotateBoundingBoxByFacing(b, facing));
    }
  }
}
