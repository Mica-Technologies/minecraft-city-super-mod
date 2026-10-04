package com.micatechnologies.minecraft.csm.transit.airport;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformFixture;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A turn in a jet bridge: a square corner room, a curved quarter turn or a U-turn, level, at the
 * level bridge's size or the large bridge's.
 *
 * <p>A corridor two blocks wide (three and a bit for the large bridge) cannot turn inside one
 * cell: two tunnel lines at right angles next to one cell overlap each other's walls, and across
 * a large turn the far wall is past the block the game looks for another block's collision boxes.
 * So a turn is several real blocks, placed and broken as one, as the mast arm curves are: every
 * cell is this block, its index in a {@link TileEntityJetBridgeTurn} read into {@link #cellProperty
 * cell}, and each draws and collides its own share. The cells, the two open ends and each cell's
 * boxes are {@link JetBridgeTurnShape}, written by gen_transit_airside.py from the same walls and
 * floors as the models.</p>
 *
 * <p>Placing: stand at the end the bridge comes from and look the way it goes. The turn is laid
 * out ahead of the block placed, which is its entry; it turns towards the side of straight ahead
 * the player looks to ({@link #LEFT}). Every cell is placed or none (the status bar says what is
 * in the way). Sneak and use with an empty hand to swing a placed turn to the other side about the
 * same entry. Breaking any cell takes the whole turn and drops one item.</p>
 *
 * <p>Joining: each open end is a cell's face, and {@link #opensAt} answers for it when a tunnel,
 * slope, cab or rotunda asks {@link BlockJetBridge#opensToward}; a turn asks the same of whatever
 * is beyond its own ends and draws the tunnel's end frame where nothing continues ({@link
 * #FRAME}). Large turns join large pieces only.</p>
 *
 * @since 2026.10
 */
public class BlockJetBridgeTurn extends BlockPlatformFixture implements ICsmTileEntityProvider {

  /** A left-hand turn (the right-hand shape turned, entered at its end B). In metadata. */
  public static final PropertyBool LEFT = PropertyBool.create("left");
  /** This cell is an open end that nothing continues from: draw the end frame. Actual state. */
  public static final PropertyBool FRAME = PropertyBool.create("frame");

  /** The light inside the bridge, as the other pieces. */
  private static final int LIGHT = 9;

  /** One cell property per cell count, shared, so the container and lookups agree. */
  private static final Map<Integer, PropertyInteger> CELL_PROPERTIES = new HashMap<>();
  /** The shape, past {@code super()}: {@link #createBlockState} runs before the fields exist. */
  private static final ThreadLocal<JetBridgeTurnShape> PENDING = new ThreadLocal<>();
  /** Set while a turn takes itself down, so a sibling's removal does not start another. */
  private static final ThreadLocal<Boolean> DEMOLISHING = ThreadLocal.withInitial(() -> false);

  private final JetBridgeTurnShape shape;
  private final PropertyInteger cellProperty;
  /** Each cell's collision boxes, facing north, in blocks. */
  private final AxisAlignedBB[][] boxes;
  /** Each cell's floor, facing north: what the player aims at. */
  private final AxisAlignedBB[] floors;

  /**
   * Constructs a jet bridge turn.
   *
   * @param registryName its registry name
   * @param shape        which turn, and its size
   */
  public BlockJetBridgeTurn(String registryName, JetBridgeTurnShape shape) {
    super(registryName, stash(shape), false, LIGHT);
    PENDING.remove();
    this.shape = shape;
    this.cellProperty = cellProperty(shape.getCellCount());
    int n = shape.getCellCount();
    this.boxes = new AxisAlignedBB[n][];
    this.floors = new AxisAlignedBB[n];
    for (int i = 0; i < n; i++) {
      double[][] b = shape.getBoxes(i);
      boxes[i] = new AxisAlignedBB[b.length];
      AxisAlignedBB floor = null;
      for (int j = 0; j < b.length; j++) {
        boxes[i][j] = box(b[j]);
        if (b[j][1] == 0) {
          floor = floor == null ? boxes[i][j] : floor.union(boxes[i][j]);
        }
      }
      floors[i] = floor != null ? floor : new AxisAlignedBB(0, 0, 0, 1, 0.1, 1);
    }
    setDefaultState(getDefaultState().withProperty(LEFT, false).withProperty(cellProperty, 0)
        .withProperty(FRAME, false));
  }

  private static double[] stash(JetBridgeTurnShape shape) {
    PENDING.set(shape);
    return new double[]{0, 0, 0, 16, BlockJetBridge.FLOOR, 16};
  }

  private static synchronized PropertyInteger cellProperty(int count) {
    return CELL_PROPERTIES.computeIfAbsent(count, c -> PropertyInteger.create("cell", 0, c - 1));
  }

  private static AxisAlignedBB box(double[] b) {
    return new AxisAlignedBB(b[0] / 16.0, b[1] / 16.0, b[2] / 16.0, b[3] / 16.0, b[4] / 16.0,
        b[5] / 16.0);
  }

  /** Which turn this is. */
  public JetBridgeTurnShape getShape() {
    return shape;
  }

  /** Whether this is a turn of the large bridge. */
  public boolean isLarge() {
    return shape.isLarge();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    JetBridgeTurnShape s = shape != null ? shape : PENDING.get();
    return new CsmBlockStateContainer(this, FACING, LEFT, cellProperty(s.getCellCount()), FRAME);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(LEFT, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(LEFT) ? 4 : 0);
  }

  // --- the turn's frame ---------------------------------------------------------------------

  /** Quarter turns clockwise from north: north 0, east 1, south 2, west 3. */
  private static int quarters(EnumFacing f) {
    return (f.getHorizontalIndex() + 2) & 3;
  }

  private static EnumFacing turned(EnumFacing f, int quarters) {
    for (int i = 0; i < (quarters & 3); i++) {
      f = f.rotateY();
    }
    return f;
  }

  /** Quarter turns that bring end B's face round to the south, where an entry is. */
  private int leftQuarters() {
    return (quarters(EnumFacing.SOUTH) - quarters(shape.getFaceB())) & 3;
  }

  /** The way the right-hand shape's north is turned to: the facing, and a left turn's offset. */
  private EnumFacing turnFacing(IBlockState state) {
    return state.getValue(LEFT) ? turned(state.getValue(FACING), leftQuarters())
        : state.getValue(FACING);
  }

  /** The entry cell: end A for a right-hand turn, end B for a left-hand one. */
  private int entryCell(IBlockState state) {
    return state.getValue(LEFT) ? shape.getEndB() : 0;
  }

  /**
   * Where a cell of the turn is.
   *
   * @param entry the entry cell's position
   * @param state the turn's state (facing and hand)
   * @param index the cell
   *
   * @return its position
   */
  private BlockPos cellPos(BlockPos entry, IBlockState state, int index) {
    EnumFacing t = turnFacing(state);
    int[] c = shape.getCell(index);
    int[] e = shape.getCell(entryCell(state));
    return entry.offset(t.rotateY(), c[0] - e[0]).offset(t.getOpposite(), c[1] - e[1]);
  }

  /** The entry cell's position, from any cell's. */
  private BlockPos entryPos(BlockPos pos, IBlockState state, int index) {
    return pos.subtract(cellPos(BlockPos.ORIGIN, state, index));
  }

  private int cellAt(IBlockAccess world, BlockPos pos) {
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityJetBridgeTurn) {
      return MathHelper.clamp(((TileEntityJetBridgeTurn) te).getCell(), 0,
          shape.getCellCount() - 1);
    }
    return 0;
  }

  /** The face a cell is open on in the world, or null if it is not an end. */
  @Nullable
  private EnumFacing openFace(IBlockState state, int index) {
    EnumFacing t = turnFacing(state);
    if (index == 0) {
      return turned(EnumFacing.SOUTH, quarters(t));
    }
    if (index == shape.getEndB()) {
      return turned(shape.getFaceB(), quarters(t));
    }
    return null;
  }

  /**
   * Whether the turn's cell at {@code pos} is one of its open ends, open on its {@code face}.
   *
   * @param world the world
   * @param pos   the cell
   * @param state the block state there (facing and hand)
   * @param face  the face asked about
   *
   * @return whether the turn opens that way there
   */
  boolean opensAt(IBlockAccess world, BlockPos pos, IBlockState state, EnumFacing face) {
    return openFace(state, cellAt(world, pos)) == face;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    int index = cellAt(world, pos);
    EnumFacing open = openFace(state, index);
    boolean frame = open != null && !BlockJetBridge.continuesFrom(world, pos, open, isLarge());
    return state.withProperty(cellProperty, index).withProperty(FRAME, frame);
  }

  // --- placing and breaking -----------------------------------------------------------------

  /** Facing is the way the player looks; the hand, which side of straight ahead they look to. */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing look = placer.getHorizontalFacing();
    float off = MathHelper.wrapDegrees(placer.rotationYaw - look.getHorizontalAngle());
    return getDefaultState().withProperty(FACING, look).withProperty(LEFT, off < 0);
  }

  /** Lays the rest of the turn out from its entry, all of it or none. */
  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    if (world.isRemote) {
      return;
    }
    BlockPos blocked = blocked(world, pos, state, Collections.singleton(pos));
    if (blocked != null) {
      DEMOLISHING.set(true);
      try {
        world.setBlockToAir(pos);
      } finally {
        DEMOLISHING.set(false);
      }
      if (placer instanceof EntityPlayer) {
        EntityPlayer player = (EntityPlayer) placer;
        if (!player.capabilities.isCreativeMode) {
          player.inventory.addItemStackToInventory(new ItemStack(this));
        }
        tellBlocked(player, blocked);
      }
      return;
    }
    build(world, pos, state, true);
  }

  /** The first cell of the turn at {@code entry} that something is in the way of, or null. */
  @Nullable
  private BlockPos blocked(World world, BlockPos entry, IBlockState state, Set<BlockPos> ours) {
    for (int i = 0; i < shape.getCellCount(); i++) {
      BlockPos p = cellPos(entry, state, i);
      if (!ours.contains(p) && !world.getBlockState(p).getBlock().isReplaceable(world, p)) {
        return p;
      }
    }
    return null;
  }

  private static void tellBlocked(EntityPlayer player, BlockPos p) {
    player.sendStatusMessage(new TextComponentTranslation("csm.transit.jet_bridge_turn.blocked",
        p.getX(), p.getY(), p.getZ()), true);
  }

  /** Places every cell of the turn at {@code entry} (its entry too, unless already there). */
  private void build(World world, BlockPos entry, IBlockState state, boolean entryPlaced) {
    IBlockState base = getDefaultState().withProperty(FACING, state.getValue(FACING))
        .withProperty(LEFT, state.getValue(LEFT));
    for (int i = 0; i < shape.getCellCount(); i++) {
      BlockPos p = cellPos(entry, state, i);
      if (!(entryPlaced && p.equals(entry))) {
        world.setBlockState(p, base, 3);
      }
      TileEntity te = world.getTileEntity(p);
      if (te instanceof TileEntityJetBridgeTurn) {
        ((TileEntityJetBridgeTurn) te).setCell(i);
        ((TileEntityJetBridgeTurn) te).syncServerToClient(world);
      }
    }
  }

  /** Removes every cell of the turn but the one at {@code except}, without drops. */
  private void demolish(World world, BlockPos entry, IBlockState state, @Nullable BlockPos except) {
    DEMOLISHING.set(true);
    try {
      for (int i = 0; i < shape.getCellCount(); i++) {
        BlockPos p = cellPos(entry, state, i);
        if (!p.equals(except) && world.getBlockState(p).getBlock() == this) {
          world.setBlockToAir(p);
        }
      }
    } finally {
      DEMOLISHING.set(false);
    }
  }

  /**
   * Takes the rest of the turn with whichever cell was broken. The siblings go with
   * {@code setBlockToAir}, which drops nothing, so a turn costs one item however it is taken down.
   */
  @Override
  public void breakBlock(World world, BlockPos pos, IBlockState state) {
    if (!world.isRemote && !DEMOLISHING.get()) {
      int index = cellAt(world, pos);
      demolish(world, entryPos(pos, state, index), state, pos);
    }
    super.breakBlock(world, pos, state);
  }

  /** Sneak and use with an empty hand: swing the turn to the other side about its entry. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!player.isSneaking() || !player.getHeldItem(hand).isEmpty()) {
      return super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
    }
    if (!world.isRemote) {
      BlockPos entry = entryPos(pos, state, cellAt(world, pos));
      IBlockState flipped = state.withProperty(LEFT, !state.getValue(LEFT));
      Set<BlockPos> ours = new HashSet<>();
      for (int i = 0; i < shape.getCellCount(); i++) {
        ours.add(cellPos(entry, state, i));
      }
      BlockPos blocked = blocked(world, entry, flipped, ours);
      if (blocked != null) {
        tellBlocked(player, blocked);
        return true;
      }
      demolish(world, entry, state, null);
      build(world, entry, flipped, false);
      player.sendStatusMessage(new TextComponentTranslation("csm.transit.jet_bridge_turn.hand",
          new TextComponentTranslation("csm.transit.jet_bridge_turn."
              + (flipped.getValue(LEFT) ? "left" : "right"))), true);
    }
    return true;
  }

  // --- what the player aims at and collides with --------------------------------------------

  /**
   * The cell's floor, so the inside can still be clicked. Turned by the hand's offset only: the
   * caller ({@code AbstractBlockRotatableNSEW#getBoundingBox}) turns it by the facing.
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    if (!(state.getBlock() instanceof BlockJetBridgeTurn)) {
      return super.getBlockBoundingBox(state, source, pos);
    }
    AxisAlignedBB floor = floors[cellAt(source, pos)];
    if (state.getValue(LEFT) && leftQuarters() != 0) {
      floor = RotationUtils.rotateBoundingBoxByFacing(floor,
          turned(EnumFacing.NORTH, leftQuarters()));
    }
    return floor;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity,
      boolean isActualState) {
    EnumFacing t = turnFacing(state);
    for (AxisAlignedBB b : boxes[cellAt(world, pos)]) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes,
          RotationUtils.rotateBoundingBoxByFacing(b, t));
    }
  }

  // --- the tile entity ----------------------------------------------------------------------

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityJetBridgeTurn.class;
  }

  @Override
  public String getTileEntityName() {
    return "jet_bridge_turn";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityJetBridgeTurn();
  }
}
