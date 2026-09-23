package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTrafficPoleIgnored;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
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
 * An invisible cell of a utility box more than one block in size.
 *
 * <p>It stores nothing. It finds the root that covers it by looking at the few cells a root could
 * be in ({@link BlockUtilityBox#findRoot}), and takes everything from there: its share of the
 * unit's box for collision, the whole unit's box for the outline, the unit's item when picked,
 * and the unit itself when broken. The root draws the whole unit, so this draws nothing.</p>
 *
 * @version 1.0
 */
public class BlockUtilityBoxPart extends AbstractBlock
    implements ICsmNoSnowAccumulation, ICsmTrafficPoleIgnored {

  /** The one registry name every part shares. */
  public static final String REGISTRY_NAME = "utility_box_part";

  private static final AxisAlignedBB ORPHAN_BOX = new AxisAlignedBB(0, 0, 0, 1, 0.0625, 1);

  public BlockUtilityBoxPart() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 3F, 12F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return REGISTRY_NAME;
  }

  /**
   * This cell's share of the unit's box. A part whose root has gone (which only an external
   * edit can cause) keeps a thin box so it can still be found and broken.
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    AxisAlignedBB unit = unitBox(source, pos);
    if (unit == null) {
      return ORPHAN_BOX;
    }
    AxisAlignedBB local = unit.offset(-pos.getX(), -pos.getY(), -pos.getZ());
    return new AxisAlignedBB(Math.max(0, local.minX), Math.max(0, local.minY),
        Math.max(0, local.minZ), Math.min(1, local.maxX), Math.min(1, local.maxY),
        Math.min(1, local.maxZ));
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
    AxisAlignedBB unit = unitBox(world, pos);
    return unit != null ? unit : ORPHAN_BOX.offset(pos);
  }

  @Nullable
  private static AxisAlignedBB unitBox(IBlockAccess world, BlockPos pos) {
    BlockPos root = BlockUtilityBox.findRoot(world, pos);
    if (root == null) {
      return null;
    }
    IBlockState rootState = world.getBlockState(root);
    BlockUtilityBox box = (BlockUtilityBox) rootState.getBlock();
    return box.unitBoxInWorld(world, root, rootState.getValue(BlockUtilityBox.FACING));
  }

  /**
   * Breaking a part breaks its unit, from the root, so the unit's one item drops as if the root
   * had been broken.
   */
  @Override
  public boolean removedByPlayer(@Nonnull IBlockState state, World world, @Nonnull BlockPos pos,
      @Nonnull EntityPlayer player, boolean willHarvest) {
    if (!world.isRemote) {
      BlockPos root = BlockUtilityBox.findRoot(world, pos);
      if (root != null) {
        world.destroyBlock(root, !player.capabilities.isCreativeMode);
      }
    }
    return super.removedByPlayer(state, world, pos, player, willHarvest);
  }

  /**
   * Removed some other way (an explosion, a command): the unit goes too, without a drop, since
   * nothing here knows whether a player earned one.
   */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
    if (!world.isRemote && !BlockUtilityBox.DEMOLISHING.get()) {
      BlockPos root = BlockUtilityBox.findRoot(world, pos);
      if (root != null) {
        world.setBlockToAir(root);
      }
    }
    super.breakBlock(world, pos, state);
  }

  /** A click anywhere on the unit is a click on the unit, so it goes to the root. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    BlockPos root = BlockUtilityBox.findRoot(world, pos);
    if (root == null) {
      return false;
    }
    IBlockState rootState = world.getBlockState(root);
    return rootState.getBlock().onBlockActivated(world, root, rootState, player, hand, facing,
        hitX, hitY, hitZ);
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
    BlockPos root = BlockUtilityBox.findRoot(world, pos);
    if (root == null) {
      return ItemStack.EMPTY;
    }
    return new ItemStack(world.getBlockState(root).getBlock());
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumBlockRenderType getRenderType(IBlockState state) {
    return EnumBlockRenderType.INVISIBLE;
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

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
