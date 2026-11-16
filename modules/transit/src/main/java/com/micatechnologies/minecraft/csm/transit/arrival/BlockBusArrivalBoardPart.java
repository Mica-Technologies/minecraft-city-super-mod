package com.micatechnologies.minecraft.csm.transit.arrival;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * An invisible cell of an arrival board or info totem ({@link BlockBusArrivalBoard}),
 * {@code bus_arrival_board_part}, in Transit's hidden tab. (The pattern of the venue unit's part,
 * which this module may not name.)
 *
 * <p>Its metadata is the {@link EnumFacing} index of the step towards its master, which the master
 * writes when it fills its cells, so a part finds the master in a few lookups. Everything else
 * comes from the master: this cell's box, the whole board's outline, the board's item when picked,
 * a click (the editor), and the board itself when broken. It draws nothing.</p>
 *
 * @since 2026.11
 */
public class BlockBusArrivalBoardPart extends AbstractBlock {

  /** The one registry name every part shares. */
  public static final String REGISTRY_NAME = "bus_arrival_board_part";

  /** The step towards the master. */
  public static final PropertyDirection TOWARD = PropertyDirection.create("toward");

  /** Set while a board removes its own cells, so the removals do not recurse. */
  static final ThreadLocal<Boolean> DEMOLISHING = ThreadLocal.withInitial(() -> false);

  /** The most steps from a part to its master: across half a 5-wide board, up a post. */
  private static final int MAX_STEPS = 16;

  private static final AxisAlignedBB ORPHAN_BOX = new AxisAlignedBB(0, 0, 0, 1, 0.0625, 1);

  /** Constructs the part block. */
  public BlockBusArrivalBoardPart() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2F, 6F, 0F, 0);
    setDefaultState(blockState.getBaseState().withProperty(TOWARD, EnumFacing.DOWN));
  }

  /**
   * {@link #REGISTRY_NAME}, spelled out: the source-reading tools find a block's registry name
   * from a literal returned here.
   */
  @Override
  public String getBlockRegistryName() {
    return "bus_arrival_board_part";
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, TOWARD);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(TOWARD, EnumFacing.byIndex(meta & 7));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(TOWARD).getIndex();
  }

  /**
   * The master of the board covering a part, following the parts' steps.
   *
   * @param world the world
   * @param part  a part's position
   *
   * @return the master's position, or null for an orphan
   */
  @Nullable
  public static BlockPos findMaster(IBlockAccess world, BlockPos part) {
    BlockPos at = part;
    for (int i = 0; i < MAX_STEPS; i++) {
      IBlockState s = world.getBlockState(at);
      if (s.getBlock() instanceof BlockBusArrivalBoardPart) {
        at = at.offset(s.getValue(TOWARD));
        continue;
      }
      if (s.getBlock() instanceof BlockBusArrivalBoard) {
        return ((BlockBusArrivalBoard) s.getBlock()).cellOf(at, s, part) != null ? at : null;
      }
      return null;
    }
    return null;
  }

  // --- shape ----------------------------------------------------------------------------------

  /** This cell's share of the board: its slab, post or totem body, turned with the board. */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    BlockPos master = findMaster(source, pos);
    if (master == null) {
      return ORPHAN_BOX;
    }
    IBlockState s = source.getBlockState(master);
    BlockBusArrivalBoard board = (BlockBusArrivalBoard) s.getBlock();
    int[] c = board.cellOf(master, s, pos);
    return c == null ? ORPHAN_BOX : board.turnedCellBox(s, c[0], c[1]);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return getBlockBoundingBox(state, source, pos);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos) {
    BlockPos master = findMaster(world, pos);
    if (master == null) {
      return ORPHAN_BOX.offset(pos);
    }
    IBlockState s = world.getBlockState(master);
    return ((BlockBusArrivalBoard) s.getBlock()).worldOutline(master, s);
  }

  // --- breaking -------------------------------------------------------------------------------

  /** Breaking a part breaks the board from its master, so the board's one item drops. */
  @Override
  public boolean removedByPlayer(@Nonnull IBlockState state, World world, @Nonnull BlockPos pos,
      @Nonnull EntityPlayer player, boolean willHarvest) {
    BlockPos master = findMaster(world, pos);
    if (master != null && !world.isRemote) {
      world.destroyBlock(master, !player.capabilities.isCreativeMode);
    }
    return super.removedByPlayer(state, world, pos, player, willHarvest);
  }

  /** Removed some other way (a command, an explosion): the board goes too, with no drop. */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
    if (!world.isRemote && !DEMOLISHING.get()) {
      // the cell is already gone from the world, so start one step towards the master
      BlockPos next = pos.offset(state.getValue(TOWARD));
      IBlockState there = world.getBlockState(next);
      BlockPos master = there.getBlock() instanceof BlockBusArrivalBoard ? next
          : findMaster(world, next);
      if (master != null) {
        world.setBlockToAir(master);
      }
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  public float getExplosionResistance(@Nonnull World world, @Nonnull BlockPos pos,
      @Nullable Entity exploder, @Nonnull Explosion explosion) {
    BlockPos master = findMaster(world, pos);
    return master == null ? super.getExplosionResistance(world, pos, exploder, explosion)
        : world.getBlockState(master).getBlock().getExplosionResistance(world, master, exploder,
            explosion);
  }

  @Override
  @Nonnull
  public Item getItemDropped(@Nonnull IBlockState state, @Nonnull Random rand, int fortune) {
    return Items.AIR;
  }

  @Override
  @Nonnull
  public ItemStack getPickBlock(@Nonnull IBlockState state, RayTraceResult target,
      @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player) {
    BlockPos master = findMaster(world, pos);
    return master == null ? ItemStack.EMPTY
        : new ItemStack(world.getBlockState(master).getBlock());
  }

  /** A click anywhere on the board is a click on the master. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    BlockPos master = findMaster(world, pos);
    if (master == null) {
      return false;
    }
    IBlockState s = world.getBlockState(master);
    return s.getBlock().onBlockActivated(world, master, s, player, hand, facing, hitX, hitY,
        hitZ);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(@Nonnull IBlockState state) {
    return EnumPushReaction.BLOCK;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumBlockRenderType getRenderType(IBlockState state) {
    return EnumBlockRenderType.INVISIBLE;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess world, @Nonnull IBlockState state,
      @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
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
}
