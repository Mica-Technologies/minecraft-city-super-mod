package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import java.util.Locale;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A cell of a {@link BlockHvacPackagedRtu} other than its master ({@code hvac_rtu_large_part},
 * hidden).
 *
 * <p>Metadata: bits 0-1 the unit's facing (S 0, W 1, N 2, E 3), bits 2-3 its finish
 * ({@link Finish}), both written by the master when it fills its footprint. Which cell of the
 * unit this is ({@code cell}) is worked out from the world, not stored: the master is found at
 * one of the footprint's offsets from here, turned by the facing, and the cell is that offset.
 * Each cell draws its own slice of the unit; a part with no master draws nothing. Everything
 * else comes from the master: this cell's share of the unit's boxes for collision, the unit's
 * outline, its item when picked, the unit itself when broken, and a click (so the linker links
 * the unit from any cell).</p>
 *
 * @author Mica Technologies
 * @since 2026.11
 */
public class BlockHvacPackagedRtuPart extends AbstractBlock {

  /** The cabinet finish, as the master's registry name gives it. */
  public enum Finish implements IStringSerializable {
    SANDSTONE, SILVER, BLACK;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  public static final PropertyDirection FACING = BlockHorizontal.FACING;
  public static final PropertyEnum<Finish> FINISH = PropertyEnum.create("finish", Finish.class);

  private static final AxisAlignedBB ORPHAN_BOX = new AxisAlignedBB(0, 0, 0, 1, 0.0625, 1);

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final BlockHvacPackagedRtu.Size size;

  public BlockHvacPackagedRtuPart(String registryName) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 3F, 15F, 0F, 0);
    this.registryName = registryName;
    this.size = BlockHvacPackagedRtu.sizeOf(registryName);
    PENDING.remove();
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(FINISH, Finish.SANDSTONE).withProperty(size.cell, size.none()));
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  public BlockHvacPackagedRtu.Size getSize() {
    return size != null ? size : BlockHvacPackagedRtu.sizeOf(getBlockRegistryName());
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, FINISH, getSize().cell);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    int finish = Math.min((meta >> 2) & 3, Finish.values().length - 1);
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(FINISH, Finish.values()[finish]);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | state.getValue(FINISH).ordinal() << 2;
  }

  /** The cell this part is, from where its master is; none for an orphan. */
  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    int i = BlockHvacPackagedRtu.findCell(world, pos, getSize(), state.getValue(FACING),
        state.getValue(FINISH).ordinal());
    return state.withProperty(getSize().cell, i < 0 ? getSize().none() : i);
  }

  // --- shape ---------------------------------------------------------------------------------

  /** This cell's share of the unit's boxes, or a thin slab for an orphan. */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    BlockPos master = BlockHvacPackagedRtu.findMaster(source, pos);
    if (master == null) {
      return ORPHAN_BOX;
    }
    AxisAlignedBB share = BlockHvacPackagedRtu.share(getSize(), master,
        source.getBlockState(master).getValue(BlockHvacPackagedRtu.FACING), pos);
    return share == null ? ORPHAN_BOX : share;
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
    BlockPos master = BlockHvacPackagedRtu.findMaster(world, pos);
    return master == null ? ORPHAN_BOX.offset(pos)
        : BlockHvacPackagedRtu.worldOutline(getSize(), master,
            world.getBlockState(master).getValue(BlockHvacPackagedRtu.FACING));
  }

  // --- breaking ------------------------------------------------------------------------------

  /** Breaking a part breaks the unit from its master, so the unit's one item drops. */
  @Override
  public boolean removedByPlayer(@Nonnull IBlockState state, World world, @Nonnull BlockPos pos,
      @Nonnull EntityPlayer player, boolean willHarvest) {
    BlockPos master = BlockHvacPackagedRtu.findMaster(world, pos);
    if (master != null && !world.isRemote) {
      world.destroyBlock(master, !player.capabilities.isCreativeMode);
    }
    return super.removedByPlayer(state, world, pos, player, willHarvest);
  }

  /**
   * Removed some other way (a command, an explosion): the unit goes too, with no drop. The cell
   * is already gone from the world, so the master is looked for at every place the footprint
   * allows.
   */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
    if (!world.isRemote && !BlockHvacPackagedRtu.DEMOLISHING.get()) {
      EnumFacing facing = state.getValue(FACING);
      for (int[] c : getSize().cells) {
        BlockPos m = pos.subtract(BlockHvacPackagedRtu.turn(c[0], c[1], c[2], facing));
        IBlockState s = world.getBlockState(m);
        if (s.getBlock() instanceof BlockHvacPackagedRtu
            && ((BlockHvacPackagedRtu) s.getBlock()).getSize() == getSize()
            && s.getValue(BlockHvacPackagedRtu.FACING) == facing) {
          world.setBlockToAir(m);
          break;
        }
      }
    }
    super.breakBlock(world, pos, state);
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
    BlockPos master = BlockHvacPackagedRtu.findMaster(world, pos);
    return master == null ? ItemStack.EMPTY
        : new ItemStack(world.getBlockState(master).getBlock());
  }

  /** A click anywhere on the unit is a click on the master. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    BlockPos master = BlockHvacPackagedRtu.findMaster(world, pos);
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

  /**
   * Always a model, never {@code INVISIBLE}: the chunk builder asks the block's default state
   * (cell none), so an invisible default would hide every cell (see the lab fan's part).
   */
  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumBlockRenderType getRenderType(IBlockState state) {
    return EnumBlockRenderType.MODEL;
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
    return BlockRenderLayer.CUTOUT_MIPPED;
  }
}
