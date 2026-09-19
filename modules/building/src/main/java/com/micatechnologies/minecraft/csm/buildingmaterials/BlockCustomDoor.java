package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.buildingmaterials.CustomDoorSettings.Movement;
import com.micatechnologies.minecraft.csm.buildingmaterials.CustomDoorSettings.Redstone;
import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * A custom door: made in the Door Workshop from any three blocks -- a frame, an upper and a lower
 * material -- with its own opening movement, sound, speed, auto-close, redstone mode and proximity
 * sensor. A {@link BlockBuildingDoor} in everything else: two halves, hinges, pairs, keypad locks.
 *
 * <p>The settings are in a data-only {@link TileEntityCustomDoor} on the lower half; the door is
 * drawn by {@code CustomDoorBakedModel}, which reads them through this block's extended state and
 * bakes the door into the chunk like any block -- no renderer at rest. A moving door is drawn from
 * {@link CustomDoorMotion} by the moving-door renderer, and hidden from its baked model meanwhile.
 * The shapes come from {@link CustomDoorGeometry}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockCustomDoor extends BlockBuildingDoor {

  public static final IUnlistedProperty<CustomDoorSettings> SETTINGS =
      new Unlisted<>("settings", CustomDoorSettings.class);
  public static final IUnlistedProperty<Boolean> PAIRED = new Unlisted<>("paired", Boolean.class);
  public static final IUnlistedProperty<Boolean> HIDDEN = new Unlisted<>("hidden", Boolean.class);

  /** The block event that tells clients a door has started to move. */
  private static final int EVENT_MOVE = 1;

  /**
   * An unlisted property: a value the model reads, which is not part of the block's state.
   *
   * @since 1.0
   */
  private static final class Unlisted<T> implements IUnlistedProperty<T> {

    private final String name;
    private final Class<T> type;

    Unlisted(String name, Class<T> type) {
      this.name = name;
      this.type = type;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public boolean isValid(T value) {
      return value != null;
    }

    @Override
    public Class<T> getType() {
      return type;
    }

    @Override
    public String valueToString(T value) {
      return String.valueOf(value);
    }
  }

  /**
   * Constructs the {@link BlockCustomDoor}.
   *
   * @since 1.0
   */
  public BlockCustomDoor() {
    super("custom_door");
    instance = this;
  }

  private static BlockCustomDoor instance;

  /**
   * The registered block, for the client to hook its model and renderer to.
   *
   * @return the block
   *
   * @since 1.0
   */
  public static BlockCustomDoor instance() {
    return instance;
  }

  @Override
  public String getBlockRegistryName() {
    return "custom_door";
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new ExtendedBlockState(this,
        new IProperty[]{HALF, FACING, OPEN, HINGE, CLOSER, SWING},
        new IUnlistedProperty[]{SETTINGS, PAIRED, HIDDEN});
  }

  @Override
  protected ItemBlock createItemBlock() {
    return new ItemCustomDoor(this);
  }

  /**
   * The settings of the door {@code pos} is a half of.
   *
   * @param world the world
   * @param pos   either half
   *
   * @return its settings
   *
   * @since 1.0
   */
  public static CustomDoorSettings settings(IBlockAccess world, BlockPos pos) {
    IBlockState state = world.getBlockState(pos);
    BlockPos lowerPos = state.getBlock() instanceof BlockCustomDoor ? lower(state, pos) : pos;
    TileEntity te = world.getTileEntity(lowerPos);
    return te instanceof TileEntityCustomDoor ? ((TileEntityCustomDoor) te).getSettings()
        : CustomDoorSettings.DEFAULT;
  }

  /** Whether the door at {@code lowerPos} is one of a pair (a partner on its latch side). */
  private boolean paired(IBlockAccess world, BlockPos lowerPos, IBlockState door) {
    EnumFacing f = door.getValue(FACING);
    EnumFacing latch = door.getValue(HINGE) == Hinge.LEFT ? f.rotateY() : f.rotateYCCW();
    BlockPos p = lowerPos.offset(latch);
    IBlockState other = world.getBlockState(p);
    if (other.getBlock() != this || other.getValue(HALF) != Half.LOWER) {
      return false;
    }
    IBlockState whole = whole(world, p);
    return whole.getValue(FACING) == f && whole.getValue(HINGE) != door.getValue(HINGE);
  }

  @Override
  @Nonnull
  public IBlockState getExtendedState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    if (!(state instanceof IExtendedBlockState)) {
      return state;
    }
    BlockPos lowerPos = lower(state, pos);
    IBlockState door = whole(world, lowerPos);
    return ((IExtendedBlockState) state).withProperty(SETTINGS, settings(world, pos))
        .withProperty(PAIRED, paired(world, lowerPos, door))
        .withProperty(HIDDEN, CustomDoorMotion.isMoving(lowerPos));
  }

  // --- tile entity: the settings, on the lower half --------------------------------------------

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return state.getValue(HALF) == Half.LOWER;
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World worldIn, int meta) {
    return (meta & 8) == 0 ? new TileEntityCustomDoor() : null;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityCustomDoor.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitycustomdoor";
  }

  // --- the item, with the settings, back out -------------------------------------------------------

  /**
   * A door item carrying these settings.
   *
   * @since 1.0
   */
  public ItemStack stack(CustomDoorSettings settings) {
    ItemStack stack = new ItemStack(this);
    ItemCustomDoor.setSettings(stack, settings);
    return stack;
  }

  @Override
  @Nonnull
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return Items.AIR;
  }

  @Override
  public void getDrops(@Nonnull NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos,
      @Nonnull IBlockState state, int fortune) {
    // Explosions and the like, while the tile entity is still there.
    if (state.getValue(HALF) == Half.LOWER && world.getTileEntity(pos) != null) {
      drops.add(stack(settings(world, pos)));
    }
  }

  /**
   * A player breaking either half gets the door with its settings. By now the half they broke is
   * gone; the lower half's settings come with it as {@code te}, or are still on the lower half if
   * they broke the upper one.
   *
   * @since 1.0
   */
  @Override
  public void harvestBlock(@Nonnull World worldIn, EntityPlayer player, @Nonnull BlockPos pos,
      @Nonnull IBlockState state, @Nullable TileEntity te, @Nonnull ItemStack stack) {
    CustomDoorSettings settings = null;
    if (state.getValue(HALF) == Half.LOWER && te instanceof TileEntityCustomDoor) {
      settings = ((TileEntityCustomDoor) te).getSettings();
    } else if (state.getValue(HALF) == Half.UPPER) {
      TileEntity below = worldIn.getTileEntity(pos.down());
      if (below instanceof TileEntityCustomDoor) {
        settings = ((TileEntityCustomDoor) below).getSettings();
      }
    }
    if (settings != null && !worldIn.isRemote) {
      spawnAsEntity(worldIn, pos, stack(settings));
    }
  }

  @Override
  @Nonnull
  public ItemStack getPickBlock(@Nonnull IBlockState state, RayTraceResult target,
      @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player) {
    return stack(settings(world, pos));
  }

  /** A custom door's auto-close is one of its settings; it takes no closer. */
  @Override
  boolean fitCloser(World world, BlockPos pos) {
    return false;
  }

  // --- moving -------------------------------------------------------------------------------------

  /**
   * Opens or shuts this half's door: the state is set at once (so collision is right at once), a
   * block event tells every client to draw the move, and a timed door schedules its closing.
   *
   * @since 1.0
   */
  @Override
  protected void swing(World world, BlockPos lowerPos, boolean open, boolean byHand) {
    IBlockState lower = world.getBlockState(lowerPos);
    if (lower.getBlock() != this) {
      return;
    }
    CustomDoorSettings s = settings(world, lowerPos);
    if (lower.getValue(OPEN) != open) {
      world.addBlockEvent(lowerPos, this, EVENT_MOVE, open ? 1 : 0);
      world.setBlockState(lowerPos, lower.withProperty(OPEN, open), 3);
      SoundEvent sound = s.sound().event(open);
      if (sound != null) {
        world.playSound(null, lowerPos, sound, SoundCategory.BLOCKS, 1.0F,
            s.sound().pitch() * (world.rand.nextFloat() * 0.1F + 0.95F));
      }
    }
    if (open && s.autoCloseTicks() > 0 && byHand) {
      world.scheduleUpdate(lowerPos, this, s.autoCloseTicks());
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean eventReceived(IBlockState state, World worldIn, BlockPos pos, int id,
      int param) {
    if (id != EVENT_MOVE) {
      return false;
    }
    if (worldIn.isRemote) {
      CustomDoorMotion.start(pos, param == 1, worldIn.getTotalWorldTime(),
          settings(worldIn, pos).openTicks());
      worldIn.markBlockRangeForRenderUpdate(pos, pos.up());
    }
    return true;
  }

  /**
   * Auto-close: shut the door when its time is up, unless redstone is holding it open.
   *
   * @since 1.0
   */
  @Override
  public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
    if (state.getValue(HALF) != Half.LOWER) {
      return;
    }
    IBlockState door = whole(worldIn, pos);
    CustomDoorSettings s = settings(worldIn, pos);
    boolean powered = worldIn.isBlockPowered(pos) || worldIn.isBlockPowered(pos.up());
    boolean held = powered && s.redstone() != Redstone.HAND_ONLY
        && s.redstone() != Redstone.REDSTONE_LOCK;
    if (door.getValue(OPEN) && s.autoCloseTicks() > 0 && !held) {
      setOpen(worldIn, pos, false, false);
    }
  }

  /**
   * Redstone, by the door's mode: it opens and shuts a normal or redstone-only door, does nothing
   * to a hand-only one, and locks a redstone-lock door shut (closing it if it was open).
   *
   * @since 1.0
   */
  @Override
  protected void onPower(World world, BlockPos lowerPos, boolean powered) {
    switch (settings(world, lowerPos).redstone()) {
      case HAND_ONLY:
        return;
      case REDSTONE_LOCK:
        if (powered) {
          setOpen(world, lowerPos, false, false);
        }
        return;
      default:
        setOpen(world, lowerPos, powered, false);
    }
  }

  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    if (hand == EnumHand.MAIN_HAND && !playerIn.isSneaking()) {
      BlockPos lowerPos = lower(state, pos);
      Redstone mode = settings(worldIn, lowerPos).redstone();
      boolean powered = worldIn.isBlockPowered(lowerPos) || worldIn.isBlockPowered(lowerPos.up());
      if (mode == Redstone.REDSTONE_ONLY || (mode == Redstone.REDSTONE_LOCK && powered)) {
        if (!worldIn.isRemote) {
          playerIn.sendStatusMessage(new TextComponentTranslation(
              mode == Redstone.REDSTONE_ONLY ? "gui.csm.door.redstone_only"
                  : "gui.csm.door.locked_redstone"), true);
        }
        return true;
      }
    }
    return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
  }

  // --- shape ------------------------------------------------------------------------------------

  /** Whether an open door has gone somewhere else -- slid or split -- leaving its cell clear. */
  private static boolean cleared(IBlockState door, CustomDoorSettings s) {
    return door.getValue(OPEN) && s.movement() != Movement.SWING;
  }

  /** A sliding, lifting or splitting door stands in the middle of the wall (CustomDoorGeometry). */
  private static final AxisAlignedBB CENTRED =
      new AxisAlignedBB(0, 0, 7.125 / 16, 1, 1, 8.875 / 16);
  private static final AxisAlignedBB STRIP_LEFT =
      new AxisAlignedBB(0, 0, 7.125 / 16, 2 / 16.0, 1, 8.875 / 16);
  private static final AxisAlignedBB STRIP_RIGHT =
      new AxisAlignedBB(14 / 16.0, 0, 7.125 / 16, 1, 1, 8.875 / 16);

  /** The closed or swung leaf's box, for this door's movement. */
  private static AxisAlignedBB box(IBlockState door, CustomDoorSettings s) {
    if (s.movement() != Movement.SWING && !door.getValue(OPEN)) {
      return BlockGarageDoor.turn(CENTRED, door.getValue(FACING));
    }
    return leafBox(door);
  }

  /**
   * A door that has slid or split away can still be clicked -- to shut it -- on a strip at the edge
   * of the opening it went to.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    IBlockState door = getActualState(state, source, pos);
    CustomDoorSettings s = settings(source, pos);
    if (cleared(door, s)) {
      boolean left = door.getValue(HINGE) == Hinge.LEFT
          || s.movement() == Movement.SLIDE_TOGETHER;
      return BlockGarageDoor.turn(left ? STRIP_LEFT : STRIP_RIGHT, door.getValue(FACING));
    }
    return box(door, s);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, @Nonnull World worldIn,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn,
      boolean isActualState) {
    IBlockState door = getActualState(state, worldIn, pos);
    CustomDoorSettings s = settings(worldIn, pos);
    if (!cleared(door, s)) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes, box(door, s));
    }
  }

  /**
   * Drawn in the cutout pass, and in the translucent pass for a translucent material (glass): the
   * model sorts each material into the pass its own block draws in.
   *
   * @since 1.0
   */
  @Override
  public boolean canRenderInLayer(@Nonnull IBlockState state, @Nonnull BlockRenderLayer layer) {
    return layer == BlockRenderLayer.CUTOUT_MIPPED || layer == BlockRenderLayer.TRANSLUCENT;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }
}
