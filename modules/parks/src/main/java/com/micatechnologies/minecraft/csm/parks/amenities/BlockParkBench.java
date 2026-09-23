package com.micatechnologies.minecraft.csm.parks.amenities;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.EntityCsmSeat;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A park bench or picnic table: one block of seat, placed side by side into a run. The frame
 * (legs and arms) is drawn only at the two ends of a run -- {@link #LEFT} and {@link #RIGHT} say
 * whether the same block, facing the same way, continues on that side -- so three placed in a
 * row are one long bench, not three benches pushed together.
 *
 * <p>Left and right are the sitter's, facing {@link #FACING}. Both are actual state, never
 * stored; the multipart blockstate picks the frame from them ({@code gen_park_amenities.py}).</p>
 *
 * <p>Right-click to sit, on Core's {@link EntityCsmSeat}; sneak to get up. A bench seats one a
 * block, so a run of three seats three. A picnic table seats one on each side, on the bench
 * nearer the player, facing the table.</p>
 *
 * @since 2026.9
 */
public class BlockParkBench extends AbstractBlockRotatableNSEW {

  /** The run continues to the sitter's left. */
  public static final PropertyBool LEFT = PropertyBool.create("left");
  /** The run continues to the sitter's right. */
  public static final PropertyBool RIGHT = PropertyBool.create("right");

  /** Where the seat entity sits, in blocks above the block's floor. */
  private static final double SEAT_Y = 0.1;
  /**
   * How far above the seat entity the rider sits, set so the rider is on the slats: a bench seat
   * is 8/16 up, a picnic table's bench 7.5/16 (the portable toilet's seat is 7.5/16 on 0.1 + 0.25).
   */
  private static final double BENCH_RIDER_OFFSET = 0.28;
  private static final double TABLE_RIDER_OFFSET = 0.25;
  /** How far a picnic table's bench sits from the block's middle, either side, in blocks. */
  private static final double TABLE_BENCH = 6.0 / 16.0;
  /** Around each seat, how close another seat makes it taken, in blocks. */
  private static final double SEAT_ROOM = 0.25;

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB box;

  /**
   * Constructs a bench.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths: {x0, y0, z0, x1, y1, z1}
   */
  public BlockParkBench(String registryName, int[] box) {
    super(stash(registryName), SoundType.WOOD, "axe", 0, 1.5F, 3.0F, 0.0F, 0);
    this.registryName = registryName;
    this.box = new AxisAlignedBB(box[0] / 16.0, box[1] / 16.0, box[2] / 16.0, box[3] / 16.0,
        box[4] / 16.0, box[5] / 16.0);
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.WOOD;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, LEFT, RIGHT);
  }

  @Override
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
    IBlockState s = super.getActualState(state, world, pos);
    EnumFacing facing = s.getValue(FACING);
    return s.withProperty(LEFT, continues(world, pos, facing, facing.rotateYCCW()))
        .withProperty(RIGHT, continues(world, pos, facing, facing.rotateY()));
  }

  private boolean continues(IBlockAccess world, BlockPos pos, EnumFacing facing,
      EnumFacing side) {
    IBlockState other = world.getBlockState(pos.offset(side));
    return other.getBlock() == this && other.getValue(FACING) == facing;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return box;
  }

  // --- sitting ---

  private boolean isTable() {
    return getBlockRegistryName().startsWith("picnic_table");
  }

  /**
   * How far forward of the block's middle a bench's seat is, in blocks: the middle of its slats
   * (a backed bench's slats run z 2 to 9.5 of 16 facing north, so its seat is forward of the
   * middle; a backless bench's are centred).
   */
  private double benchForward() {
    return getBlockRegistryName().contains("backless") ? 0.0 : 2.5 / 16.0;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (world.isRemote) {
      return true;
    }
    EnumFacing front = state.getValue(FACING);
    double cx = pos.getX() + 0.5;
    double cz = pos.getZ() + 0.5;
    EnumFacing faces;
    double offset;
    if (isTable()) {
      // Sit on the bench on the player's side of the table, facing across it.
      double toPlayer = (player.posX - cx) * front.getXOffset()
          + (player.posZ - cz) * front.getZOffset();
      faces = toPlayer >= 0 ? front.getOpposite() : front;
      offset = TABLE_BENCH;
    } else {
      faces = front;
      offset = benchForward();
    }
    // A bench seat is forward of the middle toward the way the sitter faces; a table's bench is
    // behind the sitter, away from the table.
    double along = isTable() ? -offset : offset;
    double sx = cx + faces.getXOffset() * along;
    double sz = cz + faces.getZOffset() * along;
    BlockPos out = isTable() ? pos.offset(faces.getOpposite()) : pos.offset(faces);
    AxisAlignedBB room = new AxisAlignedBB(sx - SEAT_ROOM, pos.getY(), sz - SEAT_ROOM,
        sx + SEAT_ROOM, pos.getY() + 1, sz + SEAT_ROOM);
    EntityCsmSeat.sit(world, pos, room, "csm.parks.seat.taken", sx, pos.getY() + SEAT_Y, sz,
        isTable() ? TABLE_RIDER_OFFSET : BENCH_RIDER_OFFSET, faces, out.getX() + 0.5, out.getY(),
        out.getZ() + 0.5, player);
    return true;
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
}
