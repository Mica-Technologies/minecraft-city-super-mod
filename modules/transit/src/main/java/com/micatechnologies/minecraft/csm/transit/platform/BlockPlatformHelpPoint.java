package com.micatechnologies.minecraft.csm.transit.platform;

import com.micatechnologies.minecraft.csm.transit.TransitSounds;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * A place on the platform to call for help: the help point column, with a green information
 * button over a red emergency button, and the red wall emergency point, which is all emergency.
 * A click plays the connect chime ({@link TransitSounds#HELP_POINT_CHIME}) and says on the
 * action bar who is answering; the emergency button's chime is pitched lower. Nothing is sent
 * anywhere: a help point is a place, not a service.
 *
 * <p>Which button a click on the column's front pressed is read from its height:
 * above {@link #SPLIT_Y} the information button, below it the emergency button
 * ({@code HELP_SPLIT_Y} in {@code gen_transit_platforms.py}). Any other face is information.
 * Both give light 4, for the lit header.</p>
 *
 * @since 2026.9
 */
public class BlockPlatformHelpPoint extends BlockPlatformFixture {

  /** The height on the column's front between its two buttons, in blocks. */
  private static final float SPLIT_Y = 12.6F / 16.0F;

  private final boolean emergencyOnly;

  /**
   * Constructs a help point.
   *
   * @param registryName  its registry name
   * @param box           its box facing north, in sixteenths
   * @param emergencyOnly whether every click is an emergency call (the wall emergency point)
   */
  public BlockPlatformHelpPoint(String registryName, double[] box, boolean emergencyOnly) {
    super(registryName, box, false, 4);
    this.emergencyOnly = emergencyOnly;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    if (world.isRemote) {
      return true;
    }
    boolean emergency = emergencyOnly || (side == state.getValue(FACING) && hitY < SPLIT_Y);
    SoundEvent chime = TransitSounds.HELP_POINT_CHIME.getSoundEvent();
    if (chime != null) {
      world.playSound(null, pos, chime, SoundCategory.BLOCKS, 0.8F, emergency ? 0.84F : 1.0F);
    }
    player.sendStatusMessage(new TextComponentTranslation(
        emergency ? "csm.transit.help.emergency" : "csm.transit.help.info"), true);
    return true;
  }
}
