package com.micatechnologies.minecraft.csm.powergrid.water;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
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
 * An invisible cell of a tank or of a pedestal column section: what makes it solid.
 *
 * <p>Its one stored property, {@link #KIND}, is its collision: 0 a whole cell, 1 to 15 the
 * balcony's walkway (a floor a sixteenth thick, and the railing along the sides in its mask,
 * {@code KIND - 1}: north 1, east 2, south 4, west 8), both written by the placement from the
 * tank's cell map. Everything else it takes from the root that covers it, found among the cells
 * next to it ({@link TankUnits#findRoot}): its item when picked, its clicks, and the whole unit
 * when it is broken. The root draws the unit, so this draws nothing.</p>
 *
 * @since 2026.9
 */
public class BlockTankPart extends AbstractBlock implements ICsmNoSnowAccumulation {

  /** The one registry name every part shares. */
  public static final String REGISTRY_NAME = "water_tank_part";

  /** 0 a solid cell, else a walkway whose railing mask is {@code KIND - 1}. */
  public static final PropertyInteger KIND = PropertyInteger.create("kind", 0, 15);

  public BlockTankPart() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 3F, 12F, 0F, 0);
  }

  /**
   * {@link #REGISTRY_NAME}, spelled out: the source-reading tools find a block's registry name
   * from a literal returned here.
   */
  @Override
  public String getBlockRegistryName() {
    return "water_tank_part";
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, KIND);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(KIND, meta & 15);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(KIND);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return TankUnits.selectionBox(state.getValue(KIND));
  }

  @Nullable
  @Override
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return TankUnits.selectionBox(state.getValue(KIND));
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
      AxisAlignedBB entityBox, List<AxisAlignedBB> boxes, @Nullable Entity entity,
      boolean isActualState) {
    TankUnits.addCollision(state.getValue(KIND), world, pos, entityBox, boxes);
  }

  @Override
  public boolean removedByPlayer(@Nonnull IBlockState state, World world, @Nonnull BlockPos pos,
      @Nonnull EntityPlayer player, boolean willHarvest) {
    BlockPos root = TankUnits.findRoot(world, pos);
    if (root != null && !world.isRemote) {
      ((ITankRoot) world.getBlockState(root).getBlock()).demolishUnit(world, root, player);
    }
    return super.removedByPlayer(state, world, pos, player, willHarvest);
  }

  /** Removed some other way (an explosion, a command): the unit goes too, without a drop. */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
    if (!world.isRemote && !TankUnits.DEMOLISHING.get()) {
      BlockPos root = TankUnits.findRoot(world, pos);
      if (root != null) {
        ((ITankRoot) world.getBlockState(root).getBlock()).demolishUnit(world, root, null);
      }
    }
    super.breakBlock(world, pos, state);
  }

  /** A click anywhere on the unit is a click on the unit, so it goes to the root. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    BlockPos root = TankUnits.findRoot(world, pos);
    if (root == null) {
      return false;
    }
    IBlockState rootState = world.getBlockState(root);
    return rootState.getBlock().onBlockActivated(world, root, rootState, player, hand, facing,
        hitX, hitY, hitZ);
  }

  @Override
  @SuppressWarnings("deprecation")
  public float getPlayerRelativeBlockHardness(@Nonnull IBlockState state,
      @Nonnull EntityPlayer player, @Nonnull World world, @Nonnull BlockPos pos) {
    BlockPos root = TankUnits.findRoot(world, pos);
    if (root == null) {
      return super.getPlayerRelativeBlockHardness(state, player, world, pos);
    }
    return world.getBlockState(root).getPlayerRelativeBlockHardness(player, world, root);
  }

  @Override
  public float getExplosionResistance(@Nonnull World world, @Nonnull BlockPos pos,
      @Nullable Entity exploder, @Nonnull Explosion explosion) {
    BlockPos root = TankUnits.findRoot(world, pos);
    if (root == null) {
      return super.getExplosionResistance(world, pos, exploder, explosion);
    }
    return world.getBlockState(root).getBlock().getExplosionResistance(world, root, exploder,
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
    BlockPos root = TankUnits.findRoot(world, pos);
    if (root == null) {
      return ItemStack.EMPTY;
    }
    return new ItemStack(((ITankRoot) world.getBlockState(root).getBlock()).getUnitBlock());
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

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
