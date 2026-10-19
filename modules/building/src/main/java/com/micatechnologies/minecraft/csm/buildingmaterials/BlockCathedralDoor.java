package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.BlockUtils;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A cathedral door leaf: two blocks wide and as tall as its opening, with strap hinges, a ring pull
 * and studded panels, in dark oak, weathered oak or bronze ({@code cathedral_door_<variant>}). Its
 * pointed head is a second block, {@link BlockCathedralDoorArch}, three courses of leaf above it.
 *
 * <p><b>One block per cell</b>, as a garage door is built to its opening. Every cell of a leaf stores
 * the same four bits -- the facing (the way to the inside), whether the leaf is open, and which side
 * it is hinged on -- and reads the rest from its neighbours as actual state: which {@link Column} it
 * is in (counted along the run of like cells from the hinge side, so leaves stood side by side are
 * told apart), which {@link Row}, and whether the leaf is swinging. So each cell draws its own piece
 * of the leaf from baked models, culled and lit as its own block; a leaf has no tile entity at rest
 * and needs no master to refill it; and a leaf is placed by command by giving all its cells one
 * metadata value. This was chosen over a master cell with invisible part cells, which would have
 * needed its height and head kept in a tile entity (a master's four bits are spent on the same
 * facing, open and hinge), drawn the whole leaf from one cell (lit by that cell, culled with its
 * chunk section, and past the reach of a JSON model), and a command to put back its parts.</p>
 *
 * <ul>
 *   <li><b>The master</b> is the leaf's bottom cell on its hinge side. Only it holds the swing's
 *   tile entity -- {@link TileEntityDoorSwing}, on the clients only, for the length of the swing --
 *   and its renderer turns every cell's shut model about the hinge. A lock is keyed by it.</li>
 *   <li><b>Pairs.</b> Two leaves latch to latch, hinged on opposite sides, are a pair: they open and
 *   shut together, and their heads make one arch across both.</li>
 *   <li><b>Redstone</b> at any cell holds the leaf (and its pair) open while it is powered.</li>
 *   <li><b>A keypad</b> linked to the leaf locks it from outside, as on any door.</li>
 *   <li><b>Breaking</b> any cell takes the whole leaf, its head with it; breaking a head cell takes
 *   only the head.</li>
 * </ul>
 *
 * <p>The models come from {@code dev-env-utils/scripts/gen_cathedral_doors.py}, by way of
 * {@code gen_doors.py}.</p>
 *
 * @version 1.0
 * @since 2026.10
 */
public class BlockCathedralDoor extends AbstractBlock implements ICsmTileEntityProvider {

  /**
   * Which of a leaf's two columns a cell is in.
   *
   * @since 1.0
   */
  public enum Column implements IStringSerializable {
    HINGE, LATCH;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /**
   * Which row of the leaf below its head a cell is in: the strap hinges are on the bottom and top
   * rows, the ring pull on the bottom.
   *
   * @since 1.0
   */
  public enum Row implements IStringSerializable {
    BOTTOM, MIDDLE, TOP;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  public static final PropertyDirection FACING = BlockHorizontal.FACING;
  public static final PropertyBool OPEN = BlockBuildingDoor.OPEN;
  public static final PropertyEnum<BlockBuildingDoor.Hinge> HINGE = BlockBuildingDoor.HINGE;
  /** Actual state only: whether the leaf's master has a {@link TileEntityDoorSwing}. */
  public static final PropertyBool SWING = BlockBuildingDoor.SWING;
  /** Actual state only. */
  public static final PropertyEnum<Column> COLUMN = PropertyEnum.create("column", Column.class);
  /** Actual state only. */
  public static final PropertyEnum<Row> ROW = PropertyEnum.create("row", Row.class);

  /** SHARED with gen_cathedral_doors.T: the leaf's thickness, in blocks. */
  static final double LEAF = 2.5 / 16;
  /** How long a swing takes, in ticks: a heavy leaf, twice an ordinary door's. */
  static final int SWING_TICKS = 16;
  /** How far up a leaf, and along a run of leaves, the cells are followed. */
  static final int MAX_ROWS = 12;
  private static final int MAX_RUN = 16;
  /** The block event that tells every client a leaf has started to swing (param 1 opening). */
  private static final int EVENT_SWING = 2;

  private static final AxisAlignedBB CLOSED_NORTH = new AxisAlignedBB(0, 0, 1 - LEAF, 1, 1, 1);

  /** Which leaves were last seen powered, per world, by master, to act only when it changes. */
  private static final Map<World, Set<BlockPos>> POWERED =
      Collections.synchronizedMap(new WeakHashMap<>());
  /** Set while a leaf's other cells are being taken away, so their breaking does not recurse. */
  private static final ThreadLocal<Boolean> REMOVING = ThreadLocal.withInitial(() -> false);
  /** Set from onBlockHarvested: a survival player broke the cell, so a removed head drops. */
  private static final ThreadLocal<Boolean> HARVESTED = ThreadLocal.withInitial(() -> false);

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a {@link BlockCathedralDoor}.
   *
   * @param registryName {@code cathedral_door_<variant>}
   *
   * @since 1.0
   */
  public BlockCathedralDoor(String registryName) {
    super(pendingMaterial(registryName), bronze(registryName) ? SoundType.METAL : SoundType.WOOD,
        bronze(registryName) ? "pickaxe" : "axe", 0, 4F, 20F, 0F, 0);
    this.registryName = registryName;
    setDefaultState(initialState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(OPEN, false).withProperty(HINGE, BlockBuildingDoor.Hinge.LEFT)
        .withProperty(SWING, false).withProperty(COLUMN, Column.HINGE)));
    PENDING_REGISTRY_NAME.remove();
  }

  private static boolean bronze(String registryName) {
    return registryName.contains("bronze");
  }

  private static Material pendingMaterial(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return bronze(registryName) ? Material.IRON : Material.WOOD;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  /** The default state's properties that only this kind of cell has. */
  protected IBlockState initialState(IBlockState state) {
    return state.withProperty(ROW, Row.BOTTOM);
  }

  /**
   * The variant, {@code dark_oak}, {@code weathered_oak} or {@code bronze}: a leaf and its head
   * are the same variant.
   *
   * @return the variant
   *
   * @since 1.0
   */
  public String variant() {
    String n = getBlockRegistryName().substring("cathedral_door_".length());
    return n.endsWith("_arch") ? n.substring(0, n.length() - "_arch".length()) : n;
  }

  @Override
  protected ItemBlock createItemBlock() {
    return new ItemCathedralDoor(this);
  }

  // --- state --------------------------------------------------------------------------------------

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, OPEN, HINGE, SWING, COLUMN, ROW);
  }

  /**
   * Bits 0-1 the facing (horizontal index: S 0, W 1, N 2, E 3), bit 2 open, bit 3 hinged on the
   * right. Every cell of a leaf, head included, has the same value.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(OPEN, (meta & 4) != 0)
        .withProperty(HINGE, (meta & 8) != 0 ? BlockBuildingDoor.Hinge.RIGHT
            : BlockBuildingDoor.Hinge.LEFT);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(OPEN) ? 4 : 0)
        | (state.getValue(HINGE) == BlockBuildingDoor.Hinge.RIGHT ? 8 : 0);
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    boolean below = sameKind(worldIn.getBlockState(pos.down()), state);
    boolean above = sameKind(worldIn.getBlockState(pos.up()), state);
    return withSwing(state.withProperty(COLUMN, column(worldIn::getBlockState, pos, state))
        .withProperty(ROW, !below ? Row.BOTTOM : !above ? Row.TOP : Row.MIDDLE), worldIn, pos);
  }

  /** {@code state} with {@link #SWING} read off its leaf's master. */
  protected IBlockState withSwing(IBlockState state, IBlockAccess world, BlockPos pos) {
    BlockPos master = master(world::getBlockState, pos, state);
    // A chunk being built reads through a ChunkCache, whose getTileEntity only looks.
    return state.withProperty(SWING,
        BlockUtils.getTileEntitySafe(world, master) instanceof TileEntityDoorSwing);
  }

  /** Whether {@code other} is a cell of this same block hung the same way as {@code mine}. */
  protected boolean sameKind(IBlockState other, IBlockState mine) {
    return other.getBlock() == this && other.getValue(FACING) == mine.getValue(FACING)
        && other.getValue(HINGE) == mine.getValue(HINGE);
  }

  /** Whether {@code other} is a cell of a leaf of this variant, body or head, hung as {@code mine}. */
  protected boolean sameLeaf(IBlockState other, IBlockState mine) {
    return other.getBlock() instanceof BlockCathedralDoor
        && ((BlockCathedralDoor) other.getBlock()).variant().equals(variant())
        && other.getValue(FACING) == mine.getValue(FACING)
        && other.getValue(HINGE) == mine.getValue(HINGE);
  }

  /** The way to a leaf's hinge side from its latch side, seen from outside looking in. */
  static EnumFacing hingeSide(IBlockState state) {
    EnumFacing f = state.getValue(FACING);
    return state.getValue(HINGE) == BlockBuildingDoor.Hinge.LEFT ? f.rotateYCCW() : f.rotateY();
  }

  /**
   * Which column a cell is in: count the like cells beside it toward the hinge side. An even count
   * is a hinge column, an odd one a latch column, so two leaves hinged the same way side by side
   * are still two leaves.
   */
  Column column(Function<BlockPos, IBlockState> at, BlockPos pos, IBlockState state) {
    EnumFacing h = hingeSide(state);
    int k = 0;
    BlockPos p = pos;
    while (k < MAX_RUN) {
      p = p.offset(h);
      if (!sameKind(at.apply(p), state)) {
        break;
      }
      k++;
    }
    return k % 2 == 0 ? Column.HINGE : Column.LATCH;
  }

  /**
   * The leaf's master: its bottom cell on the hinge side, found from any of its cells.
   *
   * @param at    the world as it is, or as it was before a cell went
   * @param pos   a cell of the leaf
   * @param state its stored state
   *
   * @return the master's position
   */
  BlockPos master(Function<BlockPos, IBlockState> at, BlockPos pos, IBlockState state) {
    BlockPos p = pos;
    for (int i = 0; i < MAX_ROWS; i++) {
      if (!sameLeaf(at.apply(p.down()), state)) {
        break;
      }
      p = p.down();
    }
    IBlockState bottom = at.apply(p);
    BlockCathedralDoor block = bottom.getBlock() instanceof BlockCathedralDoor
        ? (BlockCathedralDoor) bottom.getBlock() : this;
    return block.column(at, p, state) == Column.LATCH ? p.offset(hingeSide(state)) : p;
  }

  /**
   * Every cell of the leaf whose master is {@code master}: up its hinge column, and beside each the
   * latch column's cell, while they are cells of this leaf.
   */
  List<BlockPos> leafCells(Function<BlockPos, IBlockState> at, BlockPos master,
      IBlockState state) {
    EnumFacing latch = hingeSide(state).getOpposite();
    List<BlockPos> cells = new ArrayList<>();
    for (int y = 0; y < MAX_ROWS; y++) {
      BlockPos h = master.up(y);
      if (!sameLeaf(at.apply(h), state)) {
        break;
      }
      cells.add(h);
      BlockPos l = h.offset(latch);
      if (sameLeaf(at.apply(l), state)) {
        cells.add(l);
      }
    }
    return cells;
  }

  /** The other leaf of a pair: latch to latch with this one, hinged the other way. */
  @Nullable
  BlockPos partnerMaster(World world, BlockPos master, IBlockState state) {
    EnumFacing latch = hingeSide(state).getOpposite();
    BlockPos theirLatch = master.offset(latch, 2);
    IBlockState s = world.getBlockState(theirLatch);
    if (!(s.getBlock() instanceof BlockCathedralDoor)
        || !((BlockCathedralDoor) s.getBlock()).variant().equals(variant())
        || s.getValue(FACING) != state.getValue(FACING)
        || s.getValue(HINGE) == state.getValue(HINGE)) {
      return null;
    }
    BlockPos theirs = theirLatch.offset(latch);
    return sameLeaf(world.getBlockState(theirs), s) ? theirs : null;
  }

  // --- placing and breaking -------------------------------------------------------------------------

  /**
   * Places a whole leaf from its item: the cell clicked into is the leaf's hinge column, and the
   * leaf grows to the right (sneak: hinged on the right, growing left) -- unless a leaf beside it,
   * latch side on, is waiting for its partner, when the new leaf is that one's pair. The leaf fills
   * the opening upward: three blocks, or four where the fourth is clear.
   *
   * @return whether it was placed
   */
  boolean placeFromItem(World world, BlockPos pos, EntityPlayer player) {
    EnumFacing f = player.getHorizontalFacing();
    EnumFacing left = f.rotateYCCW();
    EnumFacing right = f.rotateY();
    BlockBuildingDoor.Hinge hinge;
    BlockPos hingeCell;
    if (waitingLatch(world, pos.offset(left), f, BlockBuildingDoor.Hinge.LEFT)) {
      hinge = BlockBuildingDoor.Hinge.RIGHT;
      hingeCell = pos.offset(right);
    } else if (waitingLatch(world, pos.offset(right), f, BlockBuildingDoor.Hinge.RIGHT)) {
      hinge = BlockBuildingDoor.Hinge.LEFT;
      hingeCell = pos.offset(left);
    } else {
      hinge = player.isSneaking() ? BlockBuildingDoor.Hinge.RIGHT : BlockBuildingDoor.Hinge.LEFT;
      hingeCell = pos;
    }
    BlockPos latchCell = hingeCell.offset(hinge == BlockBuildingDoor.Hinge.LEFT ? right : left);
    int rows = 0;
    while (rows < 4 && clear(world, hingeCell.up(rows)) && clear(world, latchCell.up(rows))) {
      rows++;
    }
    if (rows < 3) {
      return refuse(world, player, "gui.csm.door.cathedral.no_room");
    }
    IBlockState state = getDefaultState().withProperty(FACING, f).withProperty(HINGE, hinge);
    for (int y = 0; y < rows; y++) {
      world.setBlockState(hingeCell.up(y), state, 3);
      world.setBlockState(latchCell.up(y), state, 3);
    }
    return true;
  }

  /** Whether the cell at {@code p} is the bottom latch cell of a leaf with no partner yet. */
  private boolean waitingLatch(World world, BlockPos p, EnumFacing f,
      BlockBuildingDoor.Hinge hinge) {
    IBlockState s = world.getBlockState(p);
    if (s.getBlock() != this || s.getValue(FACING) != f || s.getValue(HINGE) != hinge
        || sameKind(world.getBlockState(p.down()), s)) {
      return false;
    }
    return column(world::getBlockState, p, s) == Column.LATCH
        && partnerMaster(world, master(world::getBlockState, p, s), s) == null;
  }

  static boolean clear(World world, BlockPos p) {
    return p.getY() < world.getHeight() && world.getBlockState(p).getBlock()
        .isReplaceable(world, p);
  }

  static boolean refuse(World world, EntityPlayer player, String key) {
    if (!world.isRemote) {
      player.sendStatusMessage(new TextComponentTranslation(key), true);
    }
    return false;
  }

  @Override
  public void onBlockHarvested(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer player) {
    HARVESTED.set(!player.capabilities.isCreativeMode);
    super.onBlockHarvested(worldIn, pos, state, player);
  }

  /**
   * Any cell going takes its leaf with it: a body cell the whole leaf and its head, a head cell the
   * head. Worked out from the cells still there and the state of the one that went, since by now
   * the world holds whatever replaced it.
   *
   * @since 1.0
   */
  @Override
  public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
    boolean harvested = HARVESTED.get();
    HARVESTED.set(false);
    if (!worldIn.isRemote && !REMOVING.get()) {
      REMOVING.set(true);
      try {
        Function<BlockPos, IBlockState> was = p -> p.equals(pos) ? state
            : worldIn.getBlockState(p);
        BlockPos master = master(was, pos, state);
        boolean head = this instanceof BlockCathedralDoorArch;
        if (!head) {
          DoorLocks.get(worldIn).unlock(master);
        }
        Block headBlock = null;
        for (BlockPos cell : leafCells(was, master, state)) {
          if (cell.equals(pos)) {
            continue;
          }
          IBlockState s = worldIn.getBlockState(cell);
          boolean isHead = s.getBlock() instanceof BlockCathedralDoorArch;
          if (head && !isHead) {
            continue;
          }
          if (isHead && !head) {
            headBlock = s.getBlock();
          }
          worldIn.setBlockState(cell, Blocks.AIR.getDefaultState(), 3);
        }
        if (headBlock != null && harvested) {
          spawnAsEntity(worldIn, pos, new ItemStack(Item.getItemFromBlock(headBlock)));
        }
      } finally {
        REMOVING.set(false);
      }
    }
    super.breakBlock(worldIn, pos, state);
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public EnumPushReaction getPushReaction(IBlockState state) {
    return EnumPushReaction.DESTROY;
  }

  // --- opening and shutting -------------------------------------------------------------------------

  private SoundEvent sound(boolean open) {
    if (bronze(getBlockRegistryName())) {
      return open ? SoundEvents.BLOCK_IRON_DOOR_OPEN : SoundEvents.BLOCK_IRON_DOOR_CLOSE;
    }
    return open ? SoundEvents.BLOCK_WOODEN_DOOR_OPEN : SoundEvents.BLOCK_WOODEN_DOOR_CLOSE;
  }

  /**
   * Opens or shuts a leaf and its pair.
   *
   * @param world  the world
   * @param pos    any cell of the leaf
   * @param open   whether to open it
   *
   * @since 1.0
   */
  void setOpen(World world, BlockPos pos, boolean open) {
    IBlockState state = world.getBlockState(pos);
    if (!(state.getBlock() instanceof BlockCathedralDoor)) {
      return;
    }
    BlockPos master = master(world::getBlockState, pos, state);
    swingLeaf(world, master, open);
    BlockPos other = partnerMaster(world, master, state);
    if (other != null) {
      swingLeaf(world, other, open);
    }
  }

  private static void swingLeaf(World world, BlockPos master, boolean open) {
    IBlockState ms = world.getBlockState(master);
    if (!(ms.getBlock() instanceof BlockCathedralDoor)) {
      return;
    }
    BlockCathedralDoor block = (BlockCathedralDoor) ms.getBlock();
    boolean changed = false;
    for (BlockPos cell : block.leafCells(world::getBlockState, master, ms)) {
      IBlockState s = world.getBlockState(cell);
      if (s.getBlock() instanceof BlockCathedralDoor && s.getValue(OPEN) != open) {
        world.setBlockState(cell, s.withProperty(OPEN, open), 10);
        changed = true;
      }
    }
    if (changed) {
      // Every client near enough makes the swing's tile entity on the master itself.
      world.addBlockEvent(master, block, EVENT_SWING, open ? 1 : 0);
      world.playSound(null, master, block.sound(open), SoundCategory.BLOCKS, 1.0F,
          world.rand.nextFloat() * 0.1F + 0.6F);
    }
  }

  /**
   * A leaf has started to swing: on a client its master gets a {@link TileEntityDoorSwing} for the
   * length of the swing, which draws the leaf turning and then takes itself away.
   *
   * @since 1.0
   */
  @Override
  @SuppressWarnings("deprecation")
  public boolean eventReceived(IBlockState state, World worldIn, BlockPos pos, int id,
      int param) {
    if (id != EVENT_SWING) {
      return false;
    }
    if (worldIn.isRemote) {
      TileEntityDoorSwing te = new TileEntityDoorSwing();
      te.begin(param == 1, worldIn.getTotalWorldTime(), SWING_TICKS);
      worldIn.setTileEntity(pos, te);
      redrawLeaf(worldIn, pos);
    }
    return true;
  }

  /**
   * A client's leaf has started or stopped swinging: rebuild every cell now, on the main thread,
   * so the baked models and the renderer hand over in the same frame.
   *
   * @param world  the client's world
   * @param master the leaf's master
   *
   * @since 1.0
   */
  static void redrawLeaf(World world, BlockPos master) {
    IBlockState ms = world.getBlockState(master);
    if (!(ms.getBlock() instanceof BlockCathedralDoor)) {
      return;
    }
    for (BlockPos cell : ((BlockCathedralDoor) ms.getBlock()).leafCells(world::getBlockState,
        master, ms)) {
      IBlockState s = world.getBlockState(cell);
      world.notifyBlockUpdate(cell, s, s, 8);
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    if (worldIn.isRemote || REMOVING.get() || blockIn instanceof BlockCathedralDoor) {
      return;
    }
    BlockPos master = master(worldIn::getBlockState, pos, state);
    boolean powered = false;
    for (BlockPos cell : leafCells(worldIn::getBlockState, master, state)) {
      if (worldIn.isBlockPowered(cell)) {
        powered = true;
        break;
      }
    }
    Set<BlockPos> seen = POWERED.computeIfAbsent(worldIn, w -> new HashSet<>());
    if (powered != seen.contains(master)) {
      if (powered) {
        seen.add(master.toImmutable());
      } else {
        seen.remove(master);
      }
      setOpen(worldIn, master, powered);
    }
  }

  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return false;
    }
    Item held = playerIn.getHeldItem(hand).getItem();
    if (facing == EnumFacing.UP && (held instanceof ItemCathedralDoor
        || held instanceof BlockCathedralTympanum.ItemTympanum)) {
      // An arch head or tympanum put on top of the leaf: the item places it.
      return false;
    }
    BlockPos master = master(worldIn::getBlockState, pos, state);
    if (playerIn.isSneaking()) {
      // Linking a keypad or control to this leaf.
      return GarageDoorLinks.finish(playerIn, worldIn, master);
    }
    if (!worldIn.isRemote) {
      boolean open = state.getValue(OPEN);
      if (!open && !BlockBuildingDoor.inside(playerIn, master, state.getValue(FACING))
          && locked(worldIn, master, state)) {
        playerIn.sendStatusMessage(new TextComponentTranslation("gui.csm.door.locked"), true);
        worldIn.playSound(null, master, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE,
            SoundCategory.BLOCKS, 0.4F, 1.6F);
        return true;
      }
      setOpen(worldIn, master, !open);
    }
    return true;
  }

  /** Whether the leaf or its pair is locked by a keypad. */
  private boolean locked(World world, BlockPos master, IBlockState state) {
    if (DoorLocks.holds(world, master)) {
      return true;
    }
    BlockPos other = partnerMaster(world, master, state);
    return other != null && DoorLocks.holds(world, other);
  }

  /**
   * A command from a linked control: a keypad's right code opens the leaf, a button toggles it, a
   * station opens or shuts it.
   *
   * @since 1.0
   */
  void command(World world, BlockPos pos, BlockGarageDoor.Command command) {
    IBlockState state = world.getBlockState(pos);
    if (!(state.getBlock() instanceof BlockCathedralDoor)) {
      return;
    }
    boolean open = state.getValue(OPEN);
    switch (command) {
      case OPEN:
        setOpen(world, pos, true);
        break;
      case CLOSE:
        setOpen(world, pos, false);
        break;
      case TOGGLE:
        setOpen(world, pos, !open);
        break;
      default:
        break;
    }
  }

  // --- the swing's tile entity ----------------------------------------------------------------------

  /**
   * Every cell may hold a {@link TileEntityDoorSwing} (only the master ever does, and only on a
   * client, for a swing), and so must say so for every state; it never makes one itself.
   *
   * @since 1.0
   */
  @Override
  public boolean hasTileEntity(IBlockState state) {
    return true;
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World worldIn, int meta) {
    return null;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityDoorSwing.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitydoorswing";
  }

  // --- shape ------------------------------------------------------------------------------------

  /**
   * Shut, the leaf's plane on the outside face of the cell. Open, the hinge column's cell has the
   * whole leaf along the jamb, its own half and the latch column's half a block inside; the latch
   * column's cell has its own half, out where its model draws it. A look ray that meets that half
   * crosses only the cell it lies in, which is not the door's, so {@link DoorClickThrough} gives
   * such a click to the door.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    if (!state.getValue(OPEN)) {
      return BlockGarageDoor.turn(CLOSED_NORTH, state.getValue(FACING));
    }
    boolean left = state.getValue(HINGE) == BlockBuildingDoor.Hinge.LEFT;
    AxisAlignedBB north;
    if (column(source::getBlockState, pos, state) == Column.HINGE) {
      // The whole open leaf, the latch column's half too: that half lies in the cell inside this
      // one, diagonal to its own cell, and an entity's collision skips the cells diagonal to the
      // ones it is in, so only this cell, beside it, can make it solid.
      north = left ? new AxisAlignedBB(0, 0, -1, LEAF, 1, 1)
          : new AxisAlignedBB(1 - LEAF, 0, -1, 1, 1, 1);
    } else {
      north = left ? new AxisAlignedBB(-1, 0, -1, -1 + LEAF, 1, 0)
          : new AxisAlignedBB(2 - LEAF, 0, -1, 2, 1, 0);
    }
    return BlockGarageDoor.turn(north, state.getValue(FACING));
  }

  @Override
  @Nonnull
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
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

  /**
   * The item that places a whole leaf (or, for a head, a whole head) rather than one cell.
   *
   * @since 1.0
   */
  static class ItemCathedralDoor extends ItemBlock {

    ItemCathedralDoor(Block block) {
      super(block);
    }

    @Override
    public boolean placeBlockAt(@Nonnull ItemStack stack, @Nonnull EntityPlayer player,
        @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EnumFacing side, float hitX,
        float hitY, float hitZ, @Nonnull IBlockState newState) {
      return ((BlockCathedralDoor) block).placeFromItem(world, pos, player);
    }
  }
}
