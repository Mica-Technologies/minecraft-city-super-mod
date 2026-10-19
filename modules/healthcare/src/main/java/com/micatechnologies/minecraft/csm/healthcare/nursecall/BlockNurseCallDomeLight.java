package com.micatechnologies.minecraft.csm.healthcare.nursecall;

import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcareLight;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * The corridor dome light over a patient room's door ({@code nurse_call_dome_light}): it lights
 * while anyone in the room is calling. It is the room on the master panel, and the room's
 * devices are linked to it.
 *
 * <p>It keeps the Lighting module's metadata ({@code facing * 4 + state}, horizontal index S 0,
 * W 1, N 2, E 3): in states 0 and 1 it is lit by a call in the room or by a redstone signal; a
 * generator can force it with state 2 (dark) or 3 (lit), which calls then leave alone. A click
 * cancels the room's calls, as a nurse does on arriving; a sneak-click picks it to link devices
 * to, or links it to the master panel picked before.</p>
 *
 * <p>It gives no redstone signal: it takes one, and a lamp that powered the dust powering it would
 * never go out. The devices and the master panel give the signal.</p>
 *
 * @since 2026.10
 */
public class BlockNurseCallDomeLight extends BlockHealthcareLight {

  public BlockNurseCallDomeLight(String registryName, int[] box) {
    super(registryName, box, 7);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    if (!world.isRemote) {
      NurseCallNetwork.get(world).refresh(world, pos);
    }
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (world.isRemote) {
      return true;
    }
    if (player.isSneaking()) {
      NurseCallLinking.sneakClick(world, pos, NurseCallNetwork.Kind.DOME, player);
      return true;
    }
    int n = NurseCallNetwork.get(world).cancelRoom(world, pos);
    player.sendStatusMessage(new TextComponentTranslation(n == 0
        ? "csm.healthcare.nursecall.no_call" : "csm.healthcare.nursecall.cancelled", n), true);
    return true;
  }

  @Override
  public void onBlockAdded(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    super.onBlockAdded(world, pos, state);
    if (!world.isRemote) {
      NurseCallNetwork.get(world).refresh(world, pos);
    }
  }
}
