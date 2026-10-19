package com.micatechnologies.minecraft.csm.healthcare.nursecall;

import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcare;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A nurse call device a patient or visitor uses: the pillow speaker, the wall call station and
 * the bathroom pull cord. A click places a call, or cancels the one it placed; the call lights the
 * room's dome light and the master panel's lamp for the room, and the device gives a redstone
 * signal while it calls, so the call reaches anything else (a chime, a light, another mod).
 *
 * <p><b>Metadata</b>: the facing out of the wall in bits 0-2 (N 2, S 3, W 4, E 5), and bit 3
 * set while calling. A generator links devices with {@code /csmhealthcare nursecall link}; a
 * player sneak-clicks a dome light or panel and then the device.</p>
 *
 * @since 2026.10
 */
public class BlockNurseCallDevice extends BlockHealthcare {

  /** A call is placed. */
  public static final PropertyBool CALLING = PropertyBool.create("calling");

  private static final PropertyBool[] FLAGS = {CALLING};

  /**
   * Constructs a device.
   *
   * @param registryName its registry name
   * @param box          its box facing north (on a wall at +Z), in sixteenths
   */
  public BlockNurseCallDevice(String registryName, int[] box) {
    super(registryName, box, HealthcareMaterial.PLASTIC, Mount.WALL, false);
  }

  @Override
  protected PropertyBool[] storedFlags() {
    return FLAGS;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (world.isRemote) {
      return true;
    }
    if (player.isSneaking()) {
      NurseCallLinking.sneakClick(world, pos, NurseCallNetwork.Kind.DEVICE, player);
      return true;
    }
    boolean call = !state.getValue(CALLING);
    world.setBlockState(pos, state.withProperty(CALLING, call), 3);
    world.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK, SoundCategory.BLOCKS, 0.4F,
        call ? 1.0F : 0.8F);
    NurseCallNetwork.get(world).setCalling(world, pos, call);
    return true;
  }

  /** A device that arrives calling (a /setblock with bit 3) is recorded as calling. */
  @Override
  public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
    super.onBlockAdded(world, pos, state);
    if (!world.isRemote && state.getValue(CALLING)) {
      NurseCallNetwork.get(world).setCalling(world, pos, true);
    }
  }

  /** A device taken away while calling ends its call. */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
    if (!world.isRemote && state.getValue(CALLING)) {
      NurseCallNetwork.get(world).setCalling(world, pos, false);
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canProvidePower(IBlockState state) {
    return true;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
    return state.getValue(CALLING) ? 15 : 0;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return true;
  }
}
