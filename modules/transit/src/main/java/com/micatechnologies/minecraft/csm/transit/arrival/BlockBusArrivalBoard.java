package com.micatechnologies.minecraft.csm.transit.arrival;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A large bus arrival board (3 x 1 or 5 x 2 blocks) or the info totem (1 x 3), placed whole by
 * its item. The block itself is the board's <em>master</em>, at its bottom-centre cell: it holds
 * the tile entity ({@link TileEntityBusArrivalBoard}, the board's data) and the renderer draws the
 * whole board from it ({@link TileEntityBusArrivalBoardRenderer}). Invisible parts
 * ({@link BlockBusArrivalBoardPart}) fill its other cells, so the board is solid and clicked
 * anywhere.
 *
 * <p><b>Placing.</b> The item picks the mount from the face it is used on ({@link BoardMount}):
 * <ul>
 *   <li>a block's underside: <b>hung</b>, the board's top row just under that block, on two rods
 *   up to it;</li>
 *   <li>the top of the ground: on a <b>post</b> (the 5 x 2 two), the board's bottom
 *   {@value #POST_RISE} blocks up, the posts' cells parts of the board;</li>
 *   <li>a wall's side: flat on the <b>wall</b>, one-sided, facing out of it.</li>
 * </ul>
 * The totem always stands on the ground. Otherwise the board faces the placer. Every cell must be
 * free (air or replaceable) or nothing is placed, and the action bar says which cell is in the
 * way. A {@code /setblock} of the master alone fills what cells it can.</p>
 *
 * <p><b>Metadata</b> is the facing's horizontal index (S 0, W 1, N 2, E 3) in bits 0-1 and the
 * mount's ordinal in bits 2-3; everything else is the tile entity's. Breaking any cell breaks the
 * whole board; only the master drops its item.</p>
 *
 * <p><b>Frames.</b> Cells are named by {@code lx} (to the reader's right, master 0) and {@code dy}
 * (up, master 0). Boxes are given in the model frame facing north, in sixteenths, and turned with
 * the facing (the front is north, a wall behind is south).</p>
 *
 * @since 2026.11
 */
public class BlockBusArrivalBoard extends AbstractBlockRotatableNSEW
    implements ICsmTileEntityProvider {

  /** The board's GUI: global across the mod (see {@code TransitGuiProvider}). */
  public static final int GUI_ID = 80;

  /** How far a post raises a board's bottom above the ground it stands on, in blocks. */
  public static final int POST_RISE = 2;

  public static final PropertyEnum<BoardMount> MOUNT = PropertyEnum.create("mount",
      BoardMount.class);

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  /** A board cell's slab, two-sided (in the block's middle) and on a wall (against it). */
  private static final AxisAlignedBB SLAB = box(0, 0, 6, 16, 16, 10);
  private static final AxisAlignedBB SLAB_WALL = box(0, 0, 13, 16, 16, 16);
  private static final AxisAlignedBB POST = box(6, 0, 6, 10, 16, 10);
  private static final AxisAlignedBB TOTEM_BASE = box(1, 0, 4, 15, 16, 12);
  private static final AxisAlignedBB TOTEM_BODY = box(2, 0, 5.5, 14, 16, 10.5);

  private final String registryName;
  private final int width;
  private final int height;
  private final boolean totem;

  /**
   * Constructs a board.
   *
   * @param registryName its registry name
   * @param width        its width in blocks, odd (3, 5; the totem 1)
   * @param height       its height in blocks (1, 2; the totem 3)
   * @param totem        whether it is the info totem
   */
  public BlockBusArrivalBoard(String registryName, int width, int height, boolean totem) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 2.0F, 6.0F, 4 / 15.0F, 0, false);
    this.registryName = registryName;
    this.width = width;
    this.height = height;
    this.totem = totem;
    PENDING.remove();
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(MOUNT, totem ? BoardMount.FLOOR : BoardMount.HUNG));
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  private static AxisAlignedBB box(double x0, double y0, double z0, double x1, double y1,
      double z1) {
    return new AxisAlignedBB(x0 / 16, y0 / 16, z0 / 16, x1 / 16, y1 / 16, z1 / 16);
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  /** The board's width, in blocks. */
  public int getWidth() {
    return width;
  }

  /** The board's height, in blocks (the totem's whole height). */
  public int getHeight() {
    return height;
  }

  /** Whether this is the info totem. */
  public boolean isTotem() {
    return totem;
  }

  // --- state ----------------------------------------------------------------------------------

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, MOUNT);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(MOUNT, BoardMount.byOrdinal((meta >> 2) & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | state.getValue(MOUNT).ordinal() << 2;
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    BoardMount mount = mountFor(side);
    EnumFacing facing = mount == BoardMount.WALL ? side
        : placer.getHorizontalFacing().getOpposite();
    return getDefaultState().withProperty(FACING, facing).withProperty(MOUNT, mount);
  }

  /** The mount the item makes when used on a face. */
  BoardMount mountFor(EnumFacing side) {
    if (totem) {
      return BoardMount.FLOOR;
    }
    if (side == EnumFacing.DOWN) {
      return BoardMount.HUNG;
    }
    return side == EnumFacing.UP ? BoardMount.POST : BoardMount.WALL;
  }

  // --- cells ----------------------------------------------------------------------------------

  /** The reader's right for a board facing this way: they face the board. */
  public static EnumFacing rightOf(EnumFacing facing) {
    return facing.rotateYCCW();
  }

  /** The lateral offsets (to the reader's right) of a board's posts. */
  public int[] postColumns() {
    return width >= 5 ? new int[]{-(width / 2), width / 2} : new int[]{0};
  }

  /**
   * The board's cells after the master, as {lx, dy} pairs: its own rectangle, then any post
   * cells below it.
   *
   * @param mount the mount
   *
   * @return the cells
   */
  public List<int[]> cells(BoardMount mount) {
    List<int[]> out = new ArrayList<>();
    int half = width / 2;
    for (int dy = 0; dy < height; dy++) {
      for (int lx = -half; lx <= half; lx++) {
        if (lx != 0 || dy != 0) {
          out.add(new int[]{lx, dy});
        }
      }
    }
    if (mount == BoardMount.POST && !totem) {
      for (int lx : postColumns()) {
        for (int dy = -1; dy >= -POST_RISE; dy--) {
          out.add(new int[]{lx, dy});
        }
      }
    }
    return out;
  }

  /** A cell's world position. */
  public static BlockPos cellPos(BlockPos master, EnumFacing facing, int lx, int dy) {
    return master.offset(rightOf(facing), lx).up(dy);
  }

  /**
   * The step from a cell towards the master: down a board's rows, up its posts, then along the
   * bottom row.
   */
  static EnumFacing stepToMaster(EnumFacing facing, int lx, int dy) {
    if (dy > 0) {
      return EnumFacing.DOWN;
    }
    if (dy < 0) {
      return EnumFacing.UP;
    }
    return lx > 0 ? rightOf(facing).getOpposite() : rightOf(facing);
  }

  /**
   * Which cell of the board at {@code master} a position is.
   *
   * @return {lx, dy}, or null if the board does not cover it
   */
  @Nullable
  int[] cellOf(BlockPos master, IBlockState state, BlockPos cell) {
    EnumFacing facing = state.getValue(FACING);
    for (int[] c : cells(state.getValue(MOUNT))) {
      if (cellPos(master, facing, c[0], c[1]).equals(cell)) {
        return c;
      }
    }
    return null;
  }

  /**
   * Fills every free cell of the board with a part.
   *
   * @return the parts placed
   */
  public int fillParts(World world, BlockPos master, IBlockState state) {
    Block part = CsmRegistry.getBlock(BlockBusArrivalBoardPart.REGISTRY_NAME);
    if (part == null) {
      return 0;
    }
    EnumFacing facing = state.getValue(FACING);
    int placed = 0;
    for (int[] c : cells(state.getValue(MOUNT))) {
      BlockPos at = cellPos(master, facing, c[0], c[1]);
      IBlockState here = world.getBlockState(at);
      if (here.getBlock() instanceof BlockBusArrivalBoardPart
          || !here.getBlock().isReplaceable(world, at)) {
        continue;
      }
      world.setBlockState(at, part.getDefaultState().withProperty(BlockBusArrivalBoardPart.TOWARD,
          stepToMaster(facing, c[0], c[1])), 3);
      placed++;
    }
    return placed;
  }

  // --- shape ----------------------------------------------------------------------------------

  /**
   * A cell's box in the model frame facing north.
   *
   * @param mount the board's mount
   * @param lx    the cell's lateral offset
   * @param dy    the cell's height above the master
   *
   * @return the box
   */
  AxisAlignedBB cellBox(BoardMount mount, int lx, int dy) {
    if (totem) {
      return dy == 0 ? TOTEM_BASE : TOTEM_BODY;
    }
    if (dy < 0) {
      return POST;
    }
    return mount == BoardMount.WALL ? SLAB_WALL : SLAB;
  }

  /** A cell's box, turned to the board's facing, relative to the cell. */
  AxisAlignedBB turnedCellBox(IBlockState masterState, int lx, int dy) {
    return RotationUtils.rotateBoundingBoxByFacing(
        cellBox(masterState.getValue(MOUNT), lx, dy), masterState.getValue(FACING));
  }

  /**
   * The whole board's outline in the world, for the selection box: its rectangle of cells and
   * any posts, at the slab's depth.
   *
   * @param master the master's position
   * @param state  its state
   *
   * @return the outline
   */
  public AxisAlignedBB worldOutline(BlockPos master, IBlockState state) {
    BoardMount mount = state.getValue(MOUNT);
    AxisAlignedBB slab = cellBox(mount, 0, totem ? 1 : 0);
    double half = width / 2.0;
    double bottom = mount == BoardMount.POST && !totem ? -POST_RISE : 0;
    AxisAlignedBB north = new AxisAlignedBB(0.5 - half, bottom, totem ? TOTEM_BASE.minZ : slab.minZ,
        0.5 + half, height, totem ? TOTEM_BASE.maxZ : slab.maxZ);
    return RotationUtils.rotateBoundingBoxByFacing(north, state.getValue(FACING)).offset(master);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return cellBox(state.getValue(MOUNT), 0, 0);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos) {
    return worldOutline(pos, state);
  }

  // --- placing, breaking, clicking ------------------------------------------------------------

  @Override
  public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
    super.onBlockAdded(world, pos, state);
    if (!world.isRemote) {
      fillParts(world, pos, state);
    }
  }

  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
    if (!world.isRemote && !BlockBusArrivalBoardPart.DEMOLISHING.get()) {
      BlockBusArrivalBoardPart.DEMOLISHING.set(true);
      try {
        EnumFacing facing = state.getValue(FACING);
        for (int[] c : cells(state.getValue(MOUNT))) {
          BlockPos at = cellPos(pos, facing, c[0], c[1]);
          if (world.getBlockState(at).getBlock() instanceof BlockBusArrivalBoardPart) {
            world.setBlockToAir(at);
          }
        }
      } finally {
        BlockBusArrivalBoardPart.DEMOLISHING.set(false);
      }
    }
    super.breakBlock(world, pos, state);
  }

  /** An empty-handed click anywhere on the board opens its editor. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (world.isRemote) {
      player.openGui(Csm.instance, GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
    }
    return true;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
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
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }

  // --- tile entity ----------------------------------------------------------------------------

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityBusArrivalBoard.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitybusarrivalboard";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityBusArrivalBoard();
  }

  @Override
  protected ItemBlock createItemBlock() {
    return new ItemBoard(this);
  }

  /**
   * The board's item: moves the master to where the mount puts it and places the board only if
   * every one of its cells is free.
   */
  public static class ItemBoard extends ItemBlock {

    public ItemBoard(BlockBusArrivalBoard block) {
      super(block);
    }

    @Override
    public boolean placeBlockAt(@Nonnull ItemStack stack, @Nonnull EntityPlayer player,
        World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ,
        @Nonnull IBlockState newState) {
      BlockBusArrivalBoard board = (BlockBusArrivalBoard) block;
      BoardMount mount = newState.getValue(MOUNT);
      EnumFacing facing = newState.getValue(FACING);
      // vanilla hands over the cell beside the face clicked
      BlockPos master = pos;
      if (mount == BoardMount.HUNG) {
        master = pos.down(board.height - 1);
      } else if (mount == BoardMount.POST) {
        master = pos.up(POST_RISE);
      }
      List<BlockPos> need = new ArrayList<>();
      need.add(master);
      for (int[] c : board.cells(mount)) {
        need.add(cellPos(master, facing, c[0], c[1]));
      }
      for (BlockPos cell : need) {
        if (cell.getY() < 0 || cell.getY() >= world.getHeight()
            || !world.getBlockState(cell).getBlock().isReplaceable(world, cell)
            || !player.canPlayerEdit(cell, side, stack)) {
          if (!world.isRemote) {
            player.sendStatusMessage(new TextComponentTranslation(
                "csm.transit.arrival_board.blocked", cell.getX(), cell.getY(), cell.getZ()),
                true);
          }
          return false;
        }
      }
      return super.placeBlockAt(stack, player, world, master, side, hitX, hitY, hitZ, newState);
    }
  }
}
