package com.micatechnologies.minecraft.csm.healthcare.systems;

import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcare;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareSounds;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The infant security door panel ({@code infant_security_panel}): the badge reader and keypad
 * beside a maternity unit's doors, its status band SECURE in green. A tag alarm, a redstone
 * signal into the wall it is mounted on, puts it in alarm: the band flashes ALARM in red (an
 * animated texture), it sounds once ({@link HealthcareSounds#INFANT_SECURITY_ALARM}) and it gives
 * a redstone signal (15) out of its front, sides, top and bottom, so it can lock the doors or
 * sound something else. The alarm latches: it stays until staff reset it with a click, which it
 * refuses while the tag alarm is still on.
 *
 * <p>It is linked by redstone only: a signal in through its wall, a signal out on every other
 * side. The input is read from the wall block alone (the block behind it), and its output never
 * goes into that block, so the panel cannot hold itself in alarm through its own wall; wire its
 * output away from that wall.</p>
 *
 * <p><b>Metadata</b>: the facing out of the wall in bits 0-2 (N 2, S 3, W 4, E 5) and bit 3 in
 * alarm (a {@code /setblock} with it places it in alarm).</p>
 *
 * @since 2026.10
 */
public class BlockInfantSecurityPanel extends BlockHealthcare {

  /** In alarm. */
  public static final PropertyBool ALARM = PropertyBool.create("alarm");

  private static final PropertyBool[] FLAGS = {ALARM};

  public BlockInfantSecurityPanel(String registryName, int[] box) {
    super(registryName, box, HealthcareMaterial.PLASTIC, Mount.WALL, false);
  }

  @Override
  protected PropertyBool[] storedFlags() {
    return FLAGS;
  }

  /** Whether the wall behind the panel is powered: the tag alarm's input. */
  private static boolean tagAlarm(World world, BlockPos pos, IBlockState state) {
    EnumFacing out = state.getValue(FACING);
    BlockPos wall = pos.offset(out.getOpposite());
    return world.getRedstonePower(wall, out.getOpposite()) > 0;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    if (world.isRemote || state.getValue(ALARM) || !tagAlarm(world, pos, state)) {
      return;
    }
    world.setBlockState(pos, state.withProperty(ALARM, true), 3);
    SoundEvent alarm = HealthcareSounds.INFANT_SECURITY_ALARM.getSoundEvent();
    if (alarm != null) {
      world.playSound(null, pos, alarm, SoundCategory.BLOCKS, 1.2F, 1.0F);
    }
  }

  /** A click resets a latched alarm, once the tag alarm is off. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (world.isRemote) {
      return true;
    }
    String key;
    if (!state.getValue(ALARM)) {
      key = "csm.healthcare.infant.secure";
    } else if (tagAlarm(world, pos, state)) {
      key = "csm.healthcare.infant.held";
    } else {
      world.setBlockState(pos, state.withProperty(ALARM, false), 3);
      key = "csm.healthcare.infant.reset";
    }
    player.sendStatusMessage(new TextComponentTranslation(key), true);
    return true;
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canProvidePower(IBlockState state) {
    return true;
  }

  /** Out of every side but the wall's. */
  @Override
  @SuppressWarnings("deprecation")
  public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
    return state.getValue(ALARM) && side != state.getValue(FACING) ? 15 : 0;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return true;
  }

  @Override
  public void onBlockAdded(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    super.onBlockAdded(world, pos, state);
    if (!world.isRemote) {
      neighborChanged(state, world, pos, this, pos);
    }
  }
}
