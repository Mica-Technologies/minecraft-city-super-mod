package com.micatechnologies.minecraft.csm.technology.network;

import com.micatechnologies.minecraft.csm.codeutils.DirectionEight;
import javax.annotation.Nonnull;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Places a {@link BlockMicafiDevice.PoleFitted} device round a pole at the angle the placer
 * looks from. Clicking a side of a pole while looking at it from a diagonal (from the south-east,
 * say) puts the device in the block diagonally beside the pole, on that side of it, turned to
 * face the placer: a block placed against a face always lands straight out from it, so without
 * this the diagonal block, in the air beside a pole, could not be reached. Every other click is
 * the ordinary one.
 *
 * @since 2026.10
 */
public class ItemBlockMicafiPoleDevice extends ItemBlock {

  public ItemBlockMicafiPoleDevice(BlockMicafiDevice.PoleFitted block) {
    super(block);
  }

  @Nonnull
  @Override
  public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos,
      EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    BlockPos target = diagonalTarget(worldIn, pos, facing, player.rotationYaw);
    return super.onItemUse(player, worldIn, target != null ? target : pos, hand, facing, hitX,
        hitY, hitZ);
  }

  /** The client asks this before it sends the click: answer for the diagonal block too. */
  @Override
  @SideOnly(Side.CLIENT)
  public boolean canPlaceBlockOnSide(World worldIn, BlockPos pos, EnumFacing side,
      EntityPlayer player, ItemStack stack) {
    return diagonalTarget(worldIn, pos, side, player.rotationYaw) != null
        || super.canPlaceBlockOnSide(worldIn, pos, side, player, stack);
  }

  /**
   * The block diagonally beside a pole that a click on its side puts a device in, or null for an
   * ordinary placement: the clicked block must be a pole, the face a side, the placer must look
   * from a diagonal that includes that side, and the diagonal block must be free.
   *
   * @param world  the world
   * @param pos    the clicked block
   * @param facing the clicked face
   * @param yaw    the placer's yaw
   *
   * @return the block to place in, or null
   */
  static BlockPos diagonalTarget(World world, BlockPos pos, EnumFacing facing, float yaw) {
    if (!facing.getAxis().isHorizontal()
        || BlockMicafiDevice.PoleFitted.poleAt(world, pos) == null) {
      return null;
    }
    DirectionEight aim = BlockMicafiDevice.PoleFitted.aimFromYaw(yaw);
    if (!aim.isDiagonal()) {
      return null;
    }
    boolean sameSide = facing.getAxis() == EnumFacing.Axis.X
        ? aim.getOffsetX() == facing.getXOffset() : aim.getOffsetZ() == facing.getZOffset();
    if (!sameSide) {
      return null;
    }
    BlockPos target = pos.add(aim.getOffsetX(), 0, aim.getOffsetZ());
    return world.getBlockState(target).getBlock().isReplaceable(world, target) ? target : null;
  }
}
