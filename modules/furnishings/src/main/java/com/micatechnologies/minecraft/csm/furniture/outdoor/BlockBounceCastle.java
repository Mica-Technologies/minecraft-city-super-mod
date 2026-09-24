package com.micatechnologies.minecraft.csm.furniture.outdoor;

import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialFurniture;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * An inflatable bounce castle, three blocks square and two tall, with a turret at each corner,
 * netted walls and a doorway in the front. It is placed and broken as one piece: this block, the
 * root, stands in the middle of the floor and draws the whole castle
 * ({@code gen_furniture_outdoor.py}; a JSON model reaches a block past its own on every side,
 * which is just three blocks), and sixteen invisible {@link BlockBounceCastlePart}s fill the
 * rest, carrying its floor and walls ({@link BounceCastleLayout}). Its item places the castle
 * with its doorway toward the player and its front row where the player clicked, and only where
 * all eighteen blocks are free and nobody is standing in them.
 *
 * <p>The floor bounces like the trampoline ({@link Bounce}); the walls do not.</p>
 *
 * @since 2026.9
 */
public class BlockBounceCastle extends BlockResidentialFurniture implements IBouncy {

  /**
   * Constructs a bounce castle.
   *
   * @param registryName its registry name, ending in its colours
   * @param box          the root's own box facing north, in sixteenths (the middle of the
   *                     floor)
   */
  public BlockBounceCastle(String registryName, int[] box) {
    super(registryName, box, Material.CLOTH, SoundType.CLOTH, 0.8F);
  }

  @Override
  protected ItemBlock createItemBlock() {
    return new ItemBounceCastle(this);
  }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    Block part = BlockBounceCastlePart.get();
    if (part == null) {
      return;
    }
    for (int i = 0; i < BounceCastleLayout.PARTS; i++) {
      world.setBlockState(BounceCastleLayout.partPos(pos, i),
          part.getDefaultState().withProperty(BlockBounceCastlePart.INDEX, i), 3);
    }
  }

  /** The parts go with the root, dropping nothing: the root drops the castle. */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    for (int i = 0; i < BounceCastleLayout.PARTS; i++) {
      BlockPos at = BounceCastleLayout.partPos(pos, i);
      IBlockState other = world.getBlockState(at);
      if (other.getBlock() instanceof BlockBounceCastlePart
          && other.getValue(BlockBounceCastlePart.INDEX) == i) {
        world.setBlockToAir(at);
      }
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(@Nonnull IBlockState state) {
    return EnumPushReaction.BLOCK;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }

  @Override
  public void onFallenUpon(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull Entity entity,
      float fallDistance) {
    Bounce.fallenUpon(world, pos, entity, fallDistance);
  }

  @Override
  public void onLanded(@Nonnull World world, @Nonnull Entity entity) {
    Bounce.landed(this, world, entity);
  }

  /**
   * The castle's item: places the root one block beyond where the player clicked, so the front
   * row is there and the doorway faces the player, and only where the whole castle fits.
   */
  public static class ItemBounceCastle extends ItemBlock {

    /**
     * Constructs the item.
     *
     * @param block the castle
     */
    public ItemBounceCastle(Block block) {
      super(block);
    }

    @Override
    public boolean placeBlockAt(@Nonnull ItemStack stack, @Nonnull EntityPlayer player,
        World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ,
        @Nonnull IBlockState newState) {
      EnumFacing front = newState.getValue(FACING);
      BlockPos root = pos.offset(front.getOpposite());
      for (int dx = -1; dx <= 1; dx++) {
        for (int dy = 0; dy <= 1; dy++) {
          for (int dz = -1; dz <= 1; dz++) {
            BlockPos at = root.add(dx, dy, dz);
            if (!world.getBlockState(at).getBlock().isReplaceable(world, at)
                || !player.canPlayerEdit(at, side, stack)
                || !world.checkNoEntityCollision(new AxisAlignedBB(at))) {
              return false;
            }
          }
        }
      }
      return super.placeBlockAt(stack, player, world, root, side, hitX, hitY, hitZ, newState);
    }
  }
}
