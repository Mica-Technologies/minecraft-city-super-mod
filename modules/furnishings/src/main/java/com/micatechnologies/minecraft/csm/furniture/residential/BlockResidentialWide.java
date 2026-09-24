package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.furniture.residential.WidePieces.IWidePiece;
import com.micatechnologies.minecraft.csm.furniture.residential.WidePieces.ItemWidePiece;
import java.util.Random;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A piece of Residential furniture two blocks wide, facing whoever places it: the upright piano,
 * the fireplace, a wide painting. Placed and broken as one piece ({@link WidePieces}); the model
 * is drawn whole and cut into the two blocks, which {@link WidePieces#PART} picks between.
 *
 * <p>{@link WidePieces#PART} is stored in the bit above the facing, leaving the top bit for a
 * subclass.</p>
 *
 * @since 2026.9
 */
public class BlockResidentialWide extends BlockResidentialFurniture implements IWidePiece {

  private final AxisAlignedBB[] boxes;

  /**
   * Constructs a wooden two-block piece.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north across both blocks, in sixteenths, x 0 to 32
   */
  public BlockResidentialWide(String registryName, int[] box) {
    super(registryName, box, false);
    this.boxes = new AxisAlignedBB[]{WidePieces.cellBox(box, 0), WidePieces.cellBox(box, 1)};
    setDefaultState(getDefaultState().withProperty(WidePieces.PART, 0));
  }

  /**
   * Constructs a two-block piece of another material.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north across both blocks, in sixteenths, x 0 to 32
   * @param material     its material
   * @param sound        its block sound
   * @param hardness     how long it takes to break
   */
  public BlockResidentialWide(String registryName, int[] box, Material material, SoundType sound,
      float hardness) {
    super(registryName, box, material, sound, hardness);
    this.boxes = new AxisAlignedBB[]{WidePieces.cellBox(box, 0), WidePieces.cellBox(box, 1)};
    setDefaultState(getDefaultState().withProperty(WidePieces.PART, 0));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, WidePieces.PART);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(WidePieces.PART, (meta >> 2) & 1);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(WidePieces.PART) << 2);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return boxes[state.getValue(WidePieces.PART)];
  }

  // --- placing and breaking as one piece ---------------------------------------------------

  @Override
  protected ItemBlock createItemBlock() {
    return new ItemWidePiece(this);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(WidePieces.PART, 0);
  }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    WidePieces.placeOther(world, pos, state);
  }

  @Override
  public void onBlockHarvested(World world, BlockPos pos, IBlockState state,
      EntityPlayer player) {
    WidePieces.harvested(world, pos, state, player);
    super.onBlockHarvested(world, pos, state, player);
  }

  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    WidePieces.broken(world, pos, state);
    super.breakBlock(world, pos, state);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    WidePieces.checkOther(world, pos, state);
    super.neighborChanged(state, world, pos, block, fromPos);
  }

  @Override
  @Nonnull
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return state.getValue(WidePieces.PART) == 1 ? Items.AIR
        : super.getItemDropped(state, rand, fortune);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(@Nonnull IBlockState state) {
    return EnumPushReaction.BLOCK;
  }
}
