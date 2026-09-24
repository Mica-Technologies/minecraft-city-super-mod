package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.EntityCsmSeat;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
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
 * A piece of Residential furniture: one block, turned to face whoever places it, drawn by
 * {@code gen_furniture_residential.py}. The registry name and the box are passed in, so one
 * class serves every piece and finish; a piece with a seat (a chair, a stool, an armchair) is
 * sat on with a right-click, on Core's {@link EntityCsmSeat}, and left by sneaking.
 *
 * <p>The box and the seat are given facing north, in sixteenths; the base class turns the box
 * with the block. Upholstered pieces are cloth, the rest wood.</p>
 *
 * @since 2026.9
 */
public class BlockResidentialFurniture extends AbstractBlockRotatableNSEW {

  /** Where the seat entity sits, in blocks above the block's floor. */
  private static final double SEAT_Y = 0.1;
  /**
   * How far below the top of a seat the rider is placed, in blocks: the park bench's measured
   * fit (a seat top of 8/16 on a seat at 0.1 with a rider offset of 0.28).
   */
  private static final double RIDER_BELOW_SEAT = 0.12;
  /** The status message when the seat is taken. */
  private static final String SEAT_TAKEN = "csm.furnishings.seat.taken";

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB box;
  /** The top of the seat, in blocks above the floor, or a negative number for no seat. */
  private final double seatTop;
  /** How far forward and how far left of the block's middle the seat is, in blocks. */
  private final double seatForward;
  private final double seatLeft;

  /**
   * Constructs a piece that is not sat on.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths: {x0, y0, z0, x1, y1, z1}
   * @param upholstered  whether it is upholstered (cloth) rather than wood
   */
  public BlockResidentialFurniture(String registryName, int[] box, boolean upholstered) {
    this(registryName, box, upholstered, -16, 0, 0);
  }

  /**
   * Constructs a piece with a seat.
   *
   * @param registryName  its registry name
   * @param box           its box facing north, in sixteenths: {x0, y0, z0, x1, y1, z1}
   * @param upholstered   whether it is upholstered (cloth) rather than wood
   * @param seatTopPx     the top of the seat above the floor, in sixteenths (negative: none)
   * @param seatForwardPx how far forward of the block's middle the seat's middle is, in
   *                      sixteenths
   * @param seatLeftPx    how far to the sitter's left of the block's middle, in sixteenths
   */
  public BlockResidentialFurniture(String registryName, int[] box, boolean upholstered,
      double seatTopPx, double seatForwardPx, double seatLeftPx) {
    super(stash(registryName, upholstered), upholstered ? SoundType.CLOTH : SoundType.WOOD,
        "axe", 0, 1.5F, 3.0F, 0.0F, 0);
    this.registryName = registryName;
    this.box = new AxisAlignedBB(box[0] / 16.0, box[1] / 16.0, box[2] / 16.0, box[3] / 16.0,
        box[4] / 16.0, box[5] / 16.0);
    this.seatTop = seatTopPx / 16.0;
    this.seatForward = seatForwardPx / 16.0;
    this.seatLeft = seatLeftPx / 16.0;
    PENDING.remove();
  }

  private static Material stash(String registryName, boolean upholstered) {
    PENDING.set(registryName);
    return upholstered ? Material.CLOTH : Material.WOOD;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return box;
  }

  /**
   * The finish or fabric: the last word of the registry name ({@code sofa_navy} is navy).
   *
   * @return the finish
   */
  public String getFinish() {
    String name = getBlockRegistryName();
    return name.substring(name.lastIndexOf('_') + 1);
  }

  /**
   * Whether this piece has a seat.
   *
   * @return true for a chair, stool, armchair or sofa
   */
  public boolean hasSeat() {
    return seatTop >= 0;
  }

  /**
   * Which way a sitter faces. A piece faces whoever placed it, and so does its seat.
   *
   * @param state the block's actual state
   *
   * @return the way the sitter faces
   */
  protected EnumFacing seatFacing(IBlockState state) {
    return state.getValue(FACING);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!hasSeat() || player.isSneaking()) {
      return false;
    }
    if (world.isRemote) {
      return true;
    }
    return sit(world, pos, state, player);
  }

  /**
   * Seats the player: on the seat, facing the way the piece faces, and off again in front of
   * it. One sitter a block.
   *
   * @param world  the world
   * @param pos    the block
   * @param state  its state
   * @param player the player
   *
   * @return true: the click is used whether or not the seat was free
   */
  protected boolean sit(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
    EnumFacing front = seatFacing(state.getActualState(world, pos));
    EnumFacing left = front.rotateYCCW();
    double sx = pos.getX() + 0.5 + front.getXOffset() * seatForward
        + left.getXOffset() * seatLeft;
    double sz = pos.getZ() + 0.5 + front.getZOffset() * seatForward
        + left.getZOffset() * seatLeft;
    BlockPos out = pos.offset(front);
    EntityCsmSeat.sit(world, pos, new AxisAlignedBB(pos), SEAT_TAKEN, sx, pos.getY() + SEAT_Y,
        sz, seatTop - RIDER_BELOW_SEAT - SEAT_Y, front, out.getX() + 0.5, out.getY(),
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
    return BlockRenderLayer.SOLID;
  }
}
