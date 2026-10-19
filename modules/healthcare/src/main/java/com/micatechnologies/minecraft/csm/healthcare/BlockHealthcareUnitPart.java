package com.micatechnologies.minecraft.csm.healthcare;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import java.util.List;
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
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * An invisible cell of a {@link BlockHealthcareUnit} ({@code healthcare_unit_part}, hidden).
 *
 * <p>Its metadata is the {@link EnumFacing} index (D 0, U 1, N 2, S 3, W 4, E 5) of the step
 * towards its master; the master writes it when it fills its footprint. Everything else comes
 * from the master: this cell's share of the unit's boxes for collision, the whole unit's outline,
 * the unit's item when picked, and the unit itself when broken. The master draws the whole unit,
 * so this draws nothing.</p>
 *
 * @since 2026.10
 */
public class BlockHealthcareUnitPart extends AbstractBlock {

  /** The one registry name every part shares. */
  public static final String REGISTRY_NAME = "healthcare_unit_part";

  /** The step towards the master. */
  public static final PropertyDirection TOWARD = PropertyDirection.create("toward");

  private static final AxisAlignedBB ORPHAN_BOX = new AxisAlignedBB(0, 0, 0, 1, 0.0625, 1);

  /** Constructs the part block. */
  public BlockHealthcareUnitPart() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 0, 2F, 4F, 0F, 0);
    setDefaultState(blockState.getBaseState().withProperty(TOWARD, EnumFacing.DOWN));
  }

  /**
   * {@link #REGISTRY_NAME}, spelled out: the source-reading tools find a block's registry name
   * from a literal returned here.
   */
  @Override
  public String getBlockRegistryName() {
    return "healthcare_unit_part";
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

  // --- shape ---------------------------------------------------------------------------------

  /** This cell's share of the unit's boxes, or a thin slab for an orphan. */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    BlockPos master = BlockHealthcareUnit.findMaster(source, pos);
    if (master == null) {
      return ORPHAN_BOX;
    }
    AxisAlignedBB cell = new AxisAlignedBB(pos);
    AxisAlignedBB share = null;
    for (AxisAlignedBB b : boxes(source, master)) {
      if (b.intersects(cell)) {
        AxisAlignedBB c = b.intersect(cell);
        share = share == null ? c : share.union(c);
      }
    }
    return share == null ? ORPHAN_BOX : share.offset(-pos.getX(), -pos.getY(), -pos.getZ());
  }

  private static List<AxisAlignedBB> boxes(IBlockAccess world, BlockPos master) {
    IBlockState s = world.getBlockState(master);
    return ((BlockHealthcareUnit) s.getBlock()).worldBoxes(master,
        s.getValue(BlockHealthcare.FACING));
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
    BlockPos master = BlockHealthcareUnit.findMaster(world, pos);
    if (master == null) {
      return ORPHAN_BOX.offset(pos);
    }
    IBlockState s = world.getBlockState(master);
    return ((BlockHealthcareUnit) s.getBlock()).worldOutline(master,
        s.getValue(BlockHealthcare.FACING));
  }

  // --- breaking ------------------------------------------------------------------------------

  /** Breaking a part breaks the unit from its master, so the unit's one item drops. */
  @Override
  public boolean removedByPlayer(@Nonnull IBlockState state, World world, @Nonnull BlockPos pos,
      @Nonnull EntityPlayer player, boolean willHarvest) {
    BlockPos master = BlockHealthcareUnit.findMaster(world, pos);
    if (master != null && !world.isRemote) {
      world.destroyBlock(master, !player.capabilities.isCreativeMode);
    }
    return super.removedByPlayer(state, world, pos, player, willHarvest);
  }

  /** Removed some other way (a command, an explosion): the unit goes too, with no drop. */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
    if (!world.isRemote && !BlockHealthcareUnit.DEMOLISHING.get()) {
      // the cell is already gone from the world, so follow its step from here
      BlockPos master = BlockHealthcareUnit.findMaster(world, pos.offset(state.getValue(TOWARD)));
      if (master == null && world.getBlockState(pos.offset(state.getValue(TOWARD))).getBlock()
          instanceof BlockHealthcareUnit) {
        master = pos.offset(state.getValue(TOWARD));
      }
      if (master != null) {
        world.setBlockToAir(master);
      }
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  public float getExplosionResistance(@Nonnull World world, @Nonnull BlockPos pos,
      @Nullable Entity exploder, @Nonnull net.minecraft.world.Explosion explosion) {
    BlockPos master = BlockHealthcareUnit.findMaster(world, pos);
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
    BlockPos master = BlockHealthcareUnit.findMaster(world, pos);
    return master == null ? ItemStack.EMPTY : new ItemStack(world.getBlockState(master)
        .getBlock());
  }

  /** A click anywhere on the unit is a click on the master. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    BlockPos master = BlockHealthcareUnit.findMaster(world, pos);
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
