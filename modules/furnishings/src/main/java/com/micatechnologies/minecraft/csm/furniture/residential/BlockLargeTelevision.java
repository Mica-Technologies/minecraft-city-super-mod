package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A big flat-screen TV, several blocks wide and two high: on the wall, or standing on a centre
 * pedestal on the floor or on a run of TV stands. It changes channel as the other TVs do
 * ({@link TvChannel}, kept by a {@link TileEntityTelevision} in every block), and a click on any
 * of its blocks changes them all. {@code gen_furniture_living.py} draws it whole and cuts it into
 * one model per block, the picture spanned across all of them, so each block is lit from its own
 * position while the picture stays one image.
 *
 * <p>Its blocks are placed together by its item, centred on the block clicked and rising from
 * it, and only where every one is free; breaking any of them breaks the TV, and only the block
 * broken drops it. The column (counted along {@code facing.rotateY()}, as a two-block piece's
 * second block is) is stored with the facing; the row is actual state, counted from the TV's
 * blocks under it, and with it the {@link #CELL} ({@code row * cols + col}) that picks the
 * model. A stand TV stands on whatever is under its pedestal's column, floor or TV stand
 * ({@link BlockCounterPiece#REST}, two of its values), and all its blocks drop with it.</p>
 *
 * @since 2026.10
 */
public class BlockLargeTelevision extends BlockResidentialFurniture
    implements ICsmTileEntityProvider {

  /** The rests a big stand TV is drawn for: anything else is drawn as the floor. */
  public static final PropertyEnum<SurfaceRest> REST = PropertyEnum.create("rest",
      SurfaceRest.class, SurfaceRest.FLOOR, SurfaceRest.TV_STAND);

  private static final PropertyInteger[] CELLS = new PropertyInteger[17];
  /** How far down a column is searched for the TV's own blocks when counting the row. */
  private static final int MAX_STACK = 32;

  private static final ThreadLocal<int[]> PENDING = new ThreadLocal<>();
  /** Set while a TV's other blocks are being cleared, so they do not clear it again. */
  private static final ThreadLocal<Boolean> CLEARING = ThreadLocal.withInitial(() -> false);

  private final int cols;
  private final int rows;
  private final boolean wall;
  private final PropertyInteger cell;
  /** Each block's box facing north, by cell. */
  private final AxisAlignedBB[] boxes;

  /**
   * Constructs a big TV.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north across all its blocks, in sixteenths: x from 0 to
   *                     16 * cols, y from 0 to 16 * rows
   * @param cols         how many blocks wide it is, 2 to 4
   * @param rows         how many blocks high it is, at most 4
   * @param wall         whether it hangs on the wall rather than standing
   */
  public BlockLargeTelevision(String registryName, int[] box, int cols, int rows, boolean wall) {
    super(pend(registryName, cols, rows, wall), box, Material.WOOD, SoundType.METAL, 0.8F);
    this.cols = cols;
    this.rows = rows;
    this.wall = wall;
    this.cell = cellProperty(cols * rows);
    this.boxes = new AxisAlignedBB[cols * rows];
    for (int i = 0; i < boxes.length; i++) {
      boxes[i] = cellBox(box, i % cols, i / cols);
    }
    IBlockState def = getDefaultState().withProperty(cell, 0)
        .withProperty(BlockTelevision.CHANNEL, TvChannel.OFF);
    setDefaultState(wall ? def : def.withProperty(REST, SurfaceRest.FLOOR));
    PENDING.remove();
  }

  private static String pend(String registryName, int cols, int rows, boolean wall) {
    if (cols < 1 || cols > 4 || rows < 1 || rows > 4) {
      throw new IllegalArgumentException("a big TV is 1 to 4 blocks each way: " + registryName);
    }
    PENDING.set(new int[]{cols, rows, wall ? 1 : 0});
    return registryName;
  }

  private static synchronized PropertyInteger cellProperty(int count) {
    if (CELLS[count] == null) {
      CELLS[count] = PropertyInteger.create("cell", 0, count - 1);
    }
    return CELLS[count];
  }

  private static AxisAlignedBB cellBox(int[] whole, int col, int row) {
    double x0 = Math.max(whole[0], col * 16) - col * 16;
    double x1 = Math.min(whole[3], col * 16 + 16) - col * 16;
    double y0 = Math.max(whole[1], row * 16) - row * 16;
    double y1 = Math.min(whole[4], row * 16 + 16) - row * 16;
    if (x1 <= x0) {
      x0 = 0;
      x1 = 16;
    }
    if (y1 <= y0) {
      y0 = 0;
      y1 = 16;
    }
    return new AxisAlignedBB(x0 / 16.0, y0 / 16.0, whole[2] / 16.0, x1 / 16.0, y1 / 16.0,
        whole[5] / 16.0);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    // Called from Block's constructor, before this block's fields are set.
    int[] spec = PENDING.get();
    List<IProperty<?>> props = new ArrayList<>();
    props.add(FACING);
    props.add(cellProperty(spec[0] * spec[1]));
    props.add(BlockTelevision.CHANNEL);
    if (spec[2] == 0) {
      props.add(REST);
    }
    return new CsmBlockStateContainer(this, props.toArray(new IProperty<?>[0]));
  }

  /**
   * How many blocks wide: from the field once constructed, from the pending spec while Block's
   * constructor runs (vanilla's {@code setHarvestLevel} asks every state for its metadata before
   * this block's fields are set).
   */
  private int colsNow() {
    if (cols > 0) {
      return cols;
    }
    int[] spec = PENDING.get();
    if (spec == null) {
      throw new IllegalStateException("big TV state read before its size is known");
    }
    return spec[0];
  }

  /** The cell property, likewise usable during construction. */
  private PropertyInteger cellNow() {
    if (cell != null) {
      return cell;
    }
    int[] spec = PENDING.get();
    if (spec == null) {
      throw new IllegalStateException("big TV state read before its size is known");
    }
    return cellProperty(spec[0] * spec[1]);
  }

  /**
   * The metadata of a block facing {@code horizontalIndex} in column {@code col}: the facing in
   * the low two bits, the column in the high two.
   *
   * @param horizontalIndex the facing's horizontal index, 0 to 3
   * @param col             the column, 0 to 3
   *
   * @return the metadata
   */
  static int packMeta(int horizontalIndex, int col) {
    return (horizontalIndex & 3) | ((col & 3) << 2);
  }

  /**
   * The column stored in {@code meta}, or 0 for one past the TV's width.
   *
   * @param meta the metadata
   * @param cols how many blocks wide the TV is
   *
   * @return the column
   */
  static int colOfMeta(int meta, int cols) {
    int col = (meta >> 2) & 3;
    return col < cols ? col : 0;
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(cellNow(), colOfMeta(meta, colsNow()));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return packMeta(state.getValue(FACING).getHorizontalIndex(),
        state.getValue(cellNow()) % colsNow());
  }

  /**
   * The way along the TV's columns: column 1 is this way of column 0.
   *
   * @param state any of its blocks' states
   *
   * @return the direction
   */
  private static EnumFacing along(IBlockState state) {
    return state.getValue(FACING).rotateY();
  }

  /** Whether {@code other} is a block of the same TV's column {@code col}, facing as it does. */
  private boolean sameColumn(IBlockState other, EnumFacing facing, int col) {
    return other.getBlock() == this && other.getValue(FACING) == facing
        && other.getValue(cell) % cols == col;
  }

  /**
   * The row of the block at {@code pos}: its TV's blocks of the same column under it, counted
   * until something else, taken round the TV's height (a TV stacked on another the same way
   * continues the column).
   */
  private int rowAt(IBlockAccess world, BlockPos pos, IBlockState state) {
    EnumFacing facing = state.getValue(FACING);
    int col = state.getValue(cell) % cols;
    int below = 0;
    BlockPos p = pos.down();
    while (below < MAX_STACK && p.getY() >= 0 && sameColumn(world.getBlockState(p), facing,
        col)) {
      below++;
      p = p.down();
    }
    return below % rows;
  }

  /**
   * The TV's first block (column 0, row 0) for its block at {@code pos}.
   */
  private BlockPos origin(IBlockAccess world, BlockPos pos, IBlockState state) {
    int col = state.getValue(cell) % cols;
    return pos.offset(along(state), -col).down(rowAt(world, pos, state));
  }

  /**
   * Every block of the TV whose first block is {@code origin}, column by column, row by row.
   */
  private List<BlockPos> cellsFrom(BlockPos origin, EnumFacing along) {
    List<BlockPos> out = new ArrayList<>(cols * rows);
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        out.add(origin.offset(along, c).up(r));
      }
    }
    return out;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
    int col = state.getValue(cell) % cols;
    int row = rowAt(world, pos, state);
    IBlockState s = super.getActualState(state, world, pos)
        .withProperty(cell, row * cols + col)
        .withProperty(BlockTelevision.CHANNEL, BlockTelevision.channelAt(world, pos));
    if (wall) {
      return s;
    }
    // The pedestal's column's bottom block decides, for every block, so the picture stays whole.
    BlockPos foot = pos.offset(along(state), (cols - 1) / 2 - col).down(row);
    SurfaceRest rest = SurfaceRest.under(world, foot);
    return s.withProperty(REST, rest == SurfaceRest.TV_STAND ? rest : SurfaceRest.FLOOR);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    AxisAlignedBB box = boxes[state.getValue(cell) < boxes.length ? state.getValue(cell) : 0];
    if (!wall) {
      return box.offset(0, -state.getValue(REST).getDrop() / 16.0, 0);
    }
    return box;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      IBlockState stored = world.getBlockState(pos);
      TvChannel next = BlockTelevision.channelAt(world, pos).next();
      for (BlockPos p : cellsFrom(origin(world, pos, stored), along(stored))) {
        if (world.getBlockState(p).getBlock() == this) {
          TileEntity te = world.getTileEntity(p);
          if (te instanceof TileEntityTelevision) {
            ((TileEntityTelevision) te).setChannel(next);
          }
        }
      }
      world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
          0.3F, next == TvChannel.OFF ? 0.6F : 0.8F);
    }
    return true;
  }

  // --- placed and broken as one ----------------------------------------------------------

  @Override
  protected ItemBlock createItemBlock() {
    return new ItemLargeTelevision(this);
  }

  /**
   * The first block of a TV placed at {@code pos} facing {@code facing}: the TV is centred on
   * the block clicked (the left of the two middle columns, for an even width) and rises from it.
   *
   * @param pos    the block clicked into
   * @param facing the way the TV faces
   *
   * @return its column 0, row 0
   */
  BlockPos placedOrigin(BlockPos pos, EnumFacing facing) {
    return pos.offset(facing.rotateY(), -((cols - 1) / 2));
  }

  /**
   * Every block a TV placed at {@code pos} facing {@code facing} would take.
   *
   * @param pos    the block clicked into
   * @param facing the way the TV faces
   *
   * @return its blocks
   */
  List<BlockPos> placedCells(BlockPos pos, EnumFacing facing) {
    return cellsFrom(placedOrigin(pos, facing), facing.rotateY());
  }

  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    if (!CLEARING.get()) {
      // The rest of the TV, found while its blocks under this one are all still there.
      BlockPos origin = origin(world, pos, state);
      EnumFacing along = along(state);
      CLEARING.set(true);
      try {
        int i = 0;
        for (BlockPos p : cellsFrom(origin, along)) {
          if (!p.equals(pos) && sameColumn(world.getBlockState(p), state.getValue(FACING),
              i % cols)) {
            world.setBlockToAir(p);
          }
          i++;
        }
      } finally {
        CLEARING.set(false);
      }
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(@Nonnull IBlockState state) {
    return EnumPushReaction.BLOCK;
  }

  // --- the channel -------------------------------------------------------------------------

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityTelevision.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitytelevision";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityTelevision();
  }

  /**
   * The item of a big TV: places all its blocks, and only where every one is free.
   */
  public static class ItemLargeTelevision extends ItemBlock {

    /**
     * Constructs the item.
     *
     * @param block the TV
     */
    public ItemLargeTelevision(Block block) {
      super(block);
    }

    @Override
    public boolean placeBlockAt(@Nonnull ItemStack stack, @Nonnull EntityPlayer player,
        World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY,
        float hitZ, @Nonnull IBlockState newState) {
      BlockLargeTelevision tv = (BlockLargeTelevision) block;
      EnumFacing facing = newState.getValue(FACING);
      List<BlockPos> cells = tv.placedCells(pos, facing);
      for (BlockPos p : cells) {
        if (!p.equals(pos) && (world.isOutsideBuildHeight(p)
            || !world.getBlockState(p).getBlock().isReplaceable(world, p)
            || !player.canPlayerEdit(p, side, stack))) {
          return false;
        }
      }
      // Every block in place before any is told of its neighbours, so none sees a part-built TV.
      int i = 0;
      for (BlockPos p : cells) {
        world.setBlockState(p, newState.withProperty(tv.cell, i % tv.cols), 2);
        i++;
      }
      for (BlockPos p : cells) {
        world.notifyNeighborsRespectDebug(p, tv, false);
      }
      if (world.getBlockState(pos).getBlock() == tv) {
        tv.onBlockPlacedBy(world, pos, world.getBlockState(pos), player, stack);
      }
      return true;
    }
  }
}
