package com.micatechnologies.minecraft.csm.healthcare.queue;

import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcare;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * A queue controller: the check-in kiosk and the desk's call console. A click calls the next
 * number of its queue ({@link QueueNetwork#advance}), which every now-serving display linked to
 * it shows; {@code /csmhealthcare queue next} does the same from a command. Linking is by command
 * ({@code /csmhealthcare queue link <controller x y z> <display x y z>}).
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), the way its screen faces. Its
 * number lives in the queue network's saved data, not in the block, so a controller placed by
 * {@code /setblock} starts at 0 (dashes) and its first click calls 1. The kiosk's screen is an
 * animated texture; nothing ticks.</p>
 *
 * @since 2026.10
 */
public class BlockQueueController extends BlockHealthcare {

  /**
   * Constructs a controller.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   * @param collides     whether it can be walked into
   */
  public BlockQueueController(String registryName, int[] box, boolean collides) {
    super(registryName, box, HealthcareMaterial.METAL, Mount.FLOOR, collides);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      int n = QueueNetwork.get(world).advance(world, pos);
      player.sendStatusMessage(new TextComponentTranslation("csm.healthcare.queue.serving",
          String.format("%03d", n)), true);
    }
    return true;
  }

  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
    if (!world.isRemote) {
      QueueNetwork.get(world).forgetController(pos.toLong());
    }
    super.breakBlock(world, pos, state);
  }
}
