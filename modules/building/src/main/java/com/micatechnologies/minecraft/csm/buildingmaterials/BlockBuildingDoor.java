package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
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
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
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
 * A door: interior wood, hollow metal, fire, exit, storefront or residential. One class,
 * constructed by registry name ({@code door_<style>_<colour>}).
 *
 * <p>Two blocks, a lower and an upper half, as a vanilla door is, and the state is split between
 * them the same way: the lower half stores the facing (the way to the inside: the way the player
 * faced when placing it, from outside) and whether it is open; the upper half stores the hinge
 * side, whether a {@link ItemDoorCloser door closer} is fitted, and whether it is swinging. Each
 * half reads the rest from the other as actual state. The leaf lies along the outside face of its
 * cell and swings inward.</p>
 *
 * <ul>
 *   <li><b>At rest</b> a door is baked models: no tile entity. Only while it swings does its
 *   upper half have one, {@link TileEntityDoorSwing}, whose renderer turns the closed model about
 *   the hinge; a scheduled tick ends the swing, so nothing ticks. Whether the swing is drawn or the
 *   door snaps is each client's own choice ({@code animateDoors}).</li>
 *   <li><b>Pairs.</b> A door placed beside one hinged on its far side hinges the other way, and a
 *   pair opens and closes together.</li>
 *   <li><b>Redstone</b> holds a door open while it is powered.</li>
 *   <li><b>A door closer</b>, fitted with the item, shuts the door three seconds after it is
 *   opened. Sneak-clicking the door with an empty hand takes it off again.</li>
 *   <li><b>A keypad</b> linked to the door locks it: it then opens from outside only with the
 *   code, and from inside as ever. Locks are kept in {@link DoorLocks}, since no state bit is
 *   left.</li>
 * </ul>
 *
 * <p>The models come from {@code dev-env-utils/scripts/gen_doors.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockBuildingDoor extends AbstractBlock implements ICsmTileEntityProvider {

  /**
   * Which half of the door a block is.
   *
   * @since 1.0
   */
  public enum Half implements IStringSerializable {
    LOWER, UPPER;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /**
   * Which side the hinges are on, seen from outside.
   *
   * @since 1.0
   */
  public enum Hinge implements IStringSerializable {
    LEFT, RIGHT;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  public static final PropertyEnum<Half> HALF = PropertyEnum.create("half", Half.class);
  public static final PropertyDirection FACING = BlockHorizontal.FACING;
  public static final PropertyBool OPEN = PropertyBool.create("open");
  public static final PropertyEnum<Hinge> HINGE = PropertyEnum.create("hinge", Hinge.class);
  public static final PropertyBool CLOSER = PropertyBool.create("closer");
  public static final PropertyBool SWING = PropertyBool.create("swing");

  /** How long a swing takes, in ticks. SHARED with {@link TileEntityDoorSwingRenderer}. */
  static final int SWING_TICKS = 8;
  /** How long a door closer holds the door open, in ticks. */
  private static final int CLOSER_TICKS = 60;

  /** SHARED with gen_doors: the leaf's thickness, and so its plane, in blocks. */
  private static final double LEAF = 1.75 / 16;
  private static final AxisAlignedBB CLOSED_NORTH = new AxisAlignedBB(0, 0, 1 - LEAF, 1, 1, 1);
  private static final AxisAlignedBB OPEN_LEFT_NORTH = new AxisAlignedBB(0, 0, 0, LEAF, 1, 1);
  private static final AxisAlignedBB OPEN_RIGHT_NORTH =
      new AxisAlignedBB(1 - LEAF, 0, 0, 1, 1, 1);

  /** Which doors were last seen powered, per world, to act only when the signal changes. */
  private static final Map<World, Set<BlockPos>> POWERED =
      Collections.synchronizedMap(new WeakHashMap<>());

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();
  private static final ThreadLocal<Hinge> PENDING_HINGE = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a {@link BlockBuildingDoor}.
   *
   * @param registryName {@code door_<style>_<colour>}
   *
   * @since 1.0
   */
  public BlockBuildingDoor(String registryName) {
    super(pendingMaterial(registryName), metal(registryName) ? SoundType.METAL : SoundType.WOOD,
        metal(registryName) ? "pickaxe" : "axe", 0, 3F, 15F, 0F, 0);
    this.registryName = registryName;
    setDefaultState(blockState.getBaseState().withProperty(HALF, Half.LOWER)
        .withProperty(FACING, EnumFacing.NORTH).withProperty(OPEN, false)
        .withProperty(HINGE, Hinge.LEFT).withProperty(CLOSER, false).withProperty(SWING, false));
    PENDING_REGISTRY_NAME.remove();
  }

  private static boolean metal(String registryName) {
    return registryName.contains("metal") || registryName.contains("storefront");
  }

  private static Material pendingMaterial(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return metal(registryName) ? Material.IRON : Material.WOOD;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  private boolean glazed() {
    String n = getBlockRegistryName();
    return n.contains("lite") || n.contains("fire") || n.contains("storefront")
        || n.contains("glass");
  }

  // --- state --------------------------------------------------------------------------------------

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, HALF, FACING, OPEN, HINGE, CLOSER, SWING);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    if ((meta & 8) != 0) {
      return getDefaultState().withProperty(HALF, Half.UPPER)
          .withProperty(HINGE, (meta & 1) != 0 ? Hinge.RIGHT : Hinge.LEFT)
          .withProperty(CLOSER, (meta & 2) != 0).withProperty(SWING, (meta & 4) != 0);
    }
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(OPEN, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    if (state.getValue(HALF) == Half.UPPER) {
      return 8 | (state.getValue(HINGE) == Hinge.RIGHT ? 1 : 0)
          | (state.getValue(CLOSER) ? 2 : 0) | (state.getValue(SWING) ? 4 : 0);
    }
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(OPEN) ? 4 : 0);
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    if (state.getValue(HALF) == Half.LOWER) {
      IBlockState up = worldIn.getBlockState(pos.up());
      if (up.getBlock() == this && up.getValue(HALF) == Half.UPPER) {
        state = state.withProperty(HINGE, up.getValue(HINGE))
            .withProperty(CLOSER, up.getValue(CLOSER)).withProperty(SWING, up.getValue(SWING));
      }
    } else {
      IBlockState down = worldIn.getBlockState(pos.down());
      if (down.getBlock() == this && down.getValue(HALF) == Half.LOWER) {
        state = state.withProperty(FACING, down.getValue(FACING))
            .withProperty(OPEN, down.getValue(OPEN));
      }
    }
    return state;
  }

  /** The whole door's state, read from wherever {@code pos} is in it. */
  private IBlockState whole(IBlockAccess world, BlockPos pos) {
    return getActualState(world.getBlockState(pos), world, pos);
  }

  private static BlockPos lower(IBlockState state, BlockPos pos) {
    return state.getValue(HALF) == Half.LOWER ? pos : pos.down();
  }

  // --- placing and breaking -------------------------------------------------------------------------

  @Override
  public boolean canPlaceBlockAt(World worldIn, @Nonnull BlockPos pos) {
    return pos.getY() < worldIn.getHeight() - 1 && super.canPlaceBlockAt(worldIn, pos)
        && worldIn.getBlockState(pos.up()).getBlock().isReplaceable(worldIn, pos.up());
  }

  /**
   * The lower half, facing the way the player looks -- a door is hung from outside, looking in.
   * The hinge is decided here and handed to {@link #onBlockPlacedBy}, which puts up the upper half
   * that stores it: opposite a door beside it hinged on its far side, so the two make a pair, or
   * else on the side of the opening the player clicked.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing f = placer.getHorizontalFacing();
    EnumFacing left = f.rotateYCCW();
    IBlockState onLeft = worldIn.getBlockState(pos.offset(left));
    IBlockState onRight = worldIn.getBlockState(pos.offset(left.getOpposite()));
    Hinge hinge;
    if (onLeft.getBlock() == this && whole(worldIn, pos.offset(left)).getValue(HINGE) == Hinge.LEFT
        && whole(worldIn, pos.offset(left)).getValue(FACING) == f) {
      hinge = Hinge.RIGHT;
    } else if (onRight.getBlock() == this
        && whole(worldIn, pos.offset(left.getOpposite())).getValue(HINGE) == Hinge.RIGHT
        && whole(worldIn, pos.offset(left.getOpposite())).getValue(FACING) == f) {
      hinge = Hinge.LEFT;
    } else {
      double along = (hitX - 0.5) * left.getXOffset() + (hitZ - 0.5) * left.getZOffset();
      hinge = along > 0 ? Hinge.LEFT : Hinge.RIGHT;
    }
    PENDING_HINGE.set(hinge);
    return getDefaultState().withProperty(FACING, f);
  }

  @Override
  public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    Hinge hinge = PENDING_HINGE.get();
    PENDING_HINGE.remove();
    worldIn.setBlockState(pos.up(), getDefaultState().withProperty(HALF, Half.UPPER)
        .withProperty(HINGE, hinge == null ? Hinge.LEFT : hinge), 2);
  }

  /**
   * Either half going takes the other with it; the item drops once, from the lower half.
   *
   * @since 1.0
   */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    if (state.getValue(HALF) == Half.UPPER) {
      IBlockState down = worldIn.getBlockState(pos.down());
      if (down.getBlock() != this) {
        worldIn.setBlockToAir(pos);
      } else if (blockIn != this) {
        neighborChanged(down, worldIn, pos.down(), blockIn, fromPos);
      }
      return;
    }
    IBlockState up = worldIn.getBlockState(pos.up());
    if (up.getBlock() != this) {
      worldIn.setBlockToAir(pos);
      if (!worldIn.isRemote) {
        dropBlockAsItem(worldIn, pos, state, 0);
      }
      return;
    }
    if (worldIn.isRemote || blockIn == this) {
      return;
    }
    boolean powered = worldIn.isBlockPowered(pos) || worldIn.isBlockPowered(pos.up());
    Set<BlockPos> seen = POWERED.computeIfAbsent(worldIn, w -> new HashSet<>());
    if (powered != seen.contains(pos)) {
      if (powered) {
        seen.add(pos.toImmutable());
      } else {
        seen.remove(pos);
      }
      setOpen(worldIn, pos, powered, false);
    }
  }

  /** Breaking the upper half in creative takes the lower away first, so nothing drops. */
  @Override
  public void onBlockHarvested(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer player) {
    if (state.getValue(HALF) == Half.UPPER && player.capabilities.isCreativeMode
        && worldIn.getBlockState(pos.down()).getBlock() == this) {
      worldIn.setBlockToAir(pos.down());
    }
  }

  @Override
  public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
    if (state.getValue(HALF) == Half.LOWER && !worldIn.isRemote) {
      DoorLocks.get(worldIn).unlock(pos);
    }
    if (state.getValue(HALF) == Half.UPPER && state.getValue(CLOSER) && !worldIn.isRemote) {
      spawnAsEntity(worldIn, pos, new ItemStack(ItemDoorCloser.instance()));
    }
    super.breakBlock(worldIn, pos, state);
  }

  @Override
  @Nonnull
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return state.getValue(HALF) == Half.UPPER ? Items.AIR : super.getItemDropped(state, rand,
        fortune);
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public EnumPushReaction getPushReaction(IBlockState state) {
    return EnumPushReaction.DESTROY;
  }

  // --- opening and closing --------------------------------------------------------------------------

  private SoundEvent sound(boolean open) {
    if (metal(getBlockRegistryName())) {
      return open ? SoundEvents.BLOCK_IRON_DOOR_OPEN : SoundEvents.BLOCK_IRON_DOOR_CLOSE;
    }
    return open ? SoundEvents.BLOCK_WOODEN_DOOR_OPEN : SoundEvents.BLOCK_WOODEN_DOOR_CLOSE;
  }

  /** The other door of a pair: beside this one on its latch side, hinged the other way. */
  @Nullable
  private BlockPos partner(World world, BlockPos lowerPos, IBlockState door) {
    EnumFacing f = door.getValue(FACING);
    EnumFacing latch = door.getValue(HINGE) == Hinge.LEFT ? f.rotateY() : f.rotateYCCW();
    BlockPos p = lowerPos.offset(latch);
    IBlockState other = world.getBlockState(p);
    if (other.getBlock() != this || other.getValue(HALF) != Half.LOWER) {
      return null;
    }
    IBlockState whole = whole(world, p);
    return whole.getValue(FACING) == f && whole.getValue(HINGE) != door.getValue(HINGE) ? p
        : null;
  }

  /**
   * Opens or shuts the door at {@code lowerPos}, and its pair.
   *
   * @param world    the world
   * @param lowerPos its lower half
   * @param open     whether to open it
   * @param byHand   whether a player or a keypad did it, which starts a fitted closer
   *
   * @since 1.0
   */
  void setOpen(World world, BlockPos lowerPos, boolean open, boolean byHand) {
    swing(world, lowerPos, open, byHand);
    BlockPos p = partner(world, lowerPos, whole(world, lowerPos));
    if (p != null) {
      swing(world, p, open, byHand);
    }
  }

  private void swing(World world, BlockPos lowerPos, boolean open, boolean byHand) {
    IBlockState lower = world.getBlockState(lowerPos);
    IBlockState upper = world.getBlockState(lowerPos.up());
    if (lower.getBlock() != this || upper.getBlock() != this) {
      return;
    }
    if (lower.getValue(OPEN) != open) {
      world.setBlockState(lowerPos, lower.withProperty(OPEN, open), 10);
      world.setBlockState(lowerPos.up(), upper.withProperty(SWING, true), 3);
      TileEntity te = world.getTileEntity(lowerPos.up());
      if (te instanceof TileEntityDoorSwing) {
        ((TileEntityDoorSwing) te).begin(open, world.getTotalWorldTime());
      }
      world.scheduleUpdate(lowerPos.up(), this, SWING_TICKS);
      world.playSound(null, lowerPos, sound(open), SoundCategory.BLOCKS, 1.0F,
          world.rand.nextFloat() * 0.1F + 0.9F);
      world.markBlockRangeForRenderUpdate(lowerPos, lowerPos.up());
    }
    if (open && byHand && upper.getValue(CLOSER)) {
      world.scheduleUpdate(lowerPos, this, CLOSER_TICKS);
    }
  }

  /**
   * The upper half: the swing is over. The lower half: a fitted closer shuts the door, unless
   * something is holding it open with redstone.
   *
   * @since 1.0
   */
  @Override
  public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
    if (state.getValue(HALF) == Half.UPPER) {
      if (state.getValue(SWING)) {
        worldIn.setBlockState(pos, state.withProperty(SWING, false), 3);
      }
      return;
    }
    IBlockState door = whole(worldIn, pos);
    if (door.getValue(OPEN) && door.getValue(CLOSER) && !worldIn.isBlockPowered(pos)
        && !worldIn.isBlockPowered(pos.up())) {
      setOpen(worldIn, pos, false, false);
    }
  }

  /**
   * Whether a player is on the inside -- the side the door faces, where it swings to.
   */
  private static boolean inside(EntityPlayer player, BlockPos pos, EnumFacing f) {
    return (player.posX - (pos.getX() + 0.5)) * f.getXOffset()
        + (player.posZ - (pos.getZ() + 0.5)) * f.getZOffset() > 0;
  }

  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return false;
    }
    BlockPos lowerPos = lower(state, pos);
    IBlockState door = whole(worldIn, lowerPos);
    if (playerIn.isSneaking()) {
      // Linking a keypad or control to this door, or else taking off its closer.
      if (GarageDoorLinks.finish(playerIn, worldIn, lowerPos)) {
        return true;
      }
      if (door.getValue(CLOSER)) {
        if (!worldIn.isRemote) {
          IBlockState upper = worldIn.getBlockState(lowerPos.up());
          worldIn.setBlockState(lowerPos.up(), upper.withProperty(CLOSER, false), 3);
          if (!playerIn.capabilities.isCreativeMode) {
            ItemStack item = new ItemStack(ItemDoorCloser.instance());
            if (!playerIn.inventory.addItemStackToInventory(item)) {
              spawnAsEntity(worldIn, pos, item);
            }
          }
        }
        return true;
      }
      return false;
    }
    if (!worldIn.isRemote) {
      if (!door.getValue(OPEN) && DoorLocks.get(worldIn).isLocked(lowerPos)
          && !inside(playerIn, lowerPos, door.getValue(FACING))) {
        playerIn.sendStatusMessage(new TextComponentTranslation("gui.csm.door.locked"), true);
        worldIn.playSound(null, lowerPos, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE,
            SoundCategory.BLOCKS, 0.4F, 1.6F);
        return true;
      }
      setOpen(worldIn, lowerPos, !door.getValue(OPEN), true);
    }
    return true;
  }

  /**
   * Fits a door closer.
   *
   * @return whether one was fitted (there was none)
   *
   * @since 1.0
   */
  boolean fitCloser(World world, BlockPos pos) {
    IBlockState state = world.getBlockState(pos);
    BlockPos upperPos = lower(state, pos).up();
    IBlockState upper = world.getBlockState(upperPos);
    if (upper.getBlock() != this || upper.getValue(CLOSER)) {
      return false;
    }
    world.setBlockState(upperPos, upper.withProperty(CLOSER, true), 3);
    return true;
  }

  /**
   * A command from a linked control: a keypad's right code opens the door (lock or no lock), a
   * button toggles it, a station opens or closes it.
   *
   * @since 1.0
   */
  void command(World world, BlockPos pos, BlockGarageDoor.Command command) {
    BlockPos lowerPos = lower(world.getBlockState(pos), pos);
    boolean open = whole(world, lowerPos).getValue(OPEN);
    switch (command) {
      case OPEN:
        setOpen(world, lowerPos, true, true);
        break;
      case CLOSE:
        setOpen(world, lowerPos, false, false);
        break;
      case TOGGLE:
        setOpen(world, lowerPos, !open, true);
        break;
      default:
        break;
    }
  }

  // --- the swing's tile entity ----------------------------------------------------------------------

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return state.getValue(HALF) == Half.UPPER && state.getValue(SWING);
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World worldIn, int meta) {
    return (meta & 8) != 0 && (meta & 4) != 0 ? new TileEntityDoorSwing() : null;
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

  static AxisAlignedBB leafBox(IBlockState door) {
    AxisAlignedBB north = !door.getValue(OPEN) ? CLOSED_NORTH
        : door.getValue(HINGE) == Hinge.LEFT ? OPEN_LEFT_NORTH : OPEN_RIGHT_NORTH;
    return BlockGarageDoor.turn(north, door.getValue(FACING));
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return leafBox(getActualState(state, source, pos));
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
    return glazed() ? BlockRenderLayer.TRANSLUCENT : BlockRenderLayer.CUTOUT;
  }
}
