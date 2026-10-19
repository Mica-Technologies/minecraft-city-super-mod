package com.micatechnologies.minecraft.csm.healthcare.codeblue;

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
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The code blue button ({@code code_blue_button}): the blue wall station a nurse presses to call
 * the resuscitation team. A press activates the code: the station's lamp lights, it gives a
 * redstone signal (15, and strongly into the wall it is on, as a vanilla button does, so a lamp or
 * an annunciator on the other side of the wall answers too), and every annunciator it is linked to
 * strobes and chimes. It stays active until it is reset: a sneak-click on it or on a linked
 * annunciator, or {@code /csmhealthcare codeblue reset}.
 *
 * <p><b>Metadata</b>: the facing out of the wall in bits 0-2 (N 2, S 3, W 4, E 5) and bit 3 set
 * while active; a {@code /setblock} with bit 3 activates the code. Links are made with
 * {@code /csmhealthcare codeblue link}.</p>
 *
 * @since 2026.10
 */
public class BlockCodeBlueButton extends BlockHealthcare {

  /** The code is active. */
  public static final PropertyBool ACTIVE = PropertyBool.create("active");

  private static final PropertyBool[] FLAGS = {ACTIVE};

  public BlockCodeBlueButton(String registryName, int[] box) {
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
      int n = CodeBlueNetwork.get(world).reset(world, pos);
      player.sendStatusMessage(new TextComponentTranslation("csm.healthcare.codeblue.reset", n),
          true);
      return true;
    }
    if (!state.getValue(ACTIVE)) {
      setActive(world, pos, state, true);
      CodeBlueNetwork.get(world).setActive(world, pos, true);
      player.sendStatusMessage(new TextComponentTranslation("csm.healthcare.codeblue.activated",
          CodeBlueNetwork.describe(pos)), true);
    }
    return true;
  }

  /**
   * Sets a button's lamp and signal, and tells the redstone around it and behind its wall.
   */
  static void setActive(World world, BlockPos pos, IBlockState state, boolean on) {
    world.setBlockState(pos, state.withProperty(ACTIVE, on), 3);
    world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
        0.5F, on ? 0.9F : 0.6F);
    world.notifyNeighborsOfStateChange(pos.offset(state.getValue(FACING).getOpposite()),
        state.getBlock(), false);
  }

  /** A button that arrives active (a /setblock with bit 3) is recorded as active. */
  @Override
  public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
    super.onBlockAdded(world, pos, state);
    if (!world.isRemote && state.getValue(ACTIVE)) {
      CodeBlueNetwork.get(world).setActive(world, pos, true);
      world.notifyNeighborsOfStateChange(pos.offset(state.getValue(FACING).getOpposite()), this,
          false);
    }
  }

  /** A button taken away while active ends its code. */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
    if (!world.isRemote && state.getValue(ACTIVE)) {
      CodeBlueNetwork.get(world).setActive(world, pos, false);
      world.notifyNeighborsOfStateChange(pos.offset(state.getValue(FACING).getOpposite()), this,
          false);
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
    return state.getValue(ACTIVE) ? 15 : 0;
  }

  /** Strongly into the wall behind, as a vanilla button powers the block it is on. */
  @Override
  @SuppressWarnings("deprecation")
  public int getStrongPower(IBlockState state, IBlockAccess world, BlockPos pos,
      EnumFacing side) {
    return state.getValue(ACTIVE) && side == state.getValue(FACING) ? 15 : 0;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return true;
  }
}
