package com.micatechnologies.minecraft.csm.transit.airport;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.transit.TransitSounds;
import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformFixture;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * The self check-in kiosk. A click prints a {@link ItemBoardingPass} for one of the next few
 * departures that are not yet boarding (at least {@link #LEAD_MINUTES} away, and not cancelled),
 * with a seat, and the action bar says which flight and gate; the pass drops at the player's feet
 * if their inventory is full. It asks for nothing in return: a kiosk prints what a traveller has
 * already bought. Its screen and header give a little light.
 *
 * @since 2026.9
 */
public class BlockSelfCheckinKiosk extends BlockPlatformFixture {

  /** How soon a flight may leave and still be checked in for, in minutes. */
  private static final int LEAD_MINUTES = 45;
  /** How many of the next departures a pass is picked from. */
  private static final int CHOICES = 6;

  private static final String SEAT_LETTERS = "ABCDEF";

  /**
   * Constructs a kiosk.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockSelfCheckinKiosk(String registryName, double[] box) {
    super(registryName, box, false, 4);
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
    Item item = CsmRegistry.getItem("boarding_pass");
    if (!(item instanceof ItemBoardingPass)) {
      return true;
    }
    long now = FlightSchedule.minuteOf(world.getWorldTime());
    long[] slots = new long[CHOICES * 2];
    FlightSchedule.list(false, now + LEAD_MINUTES, 0, slots);
    long pick = Long.MIN_VALUE;
    int offered = 0;
    for (long slot : slots) {
      if (FlightSchedule.cancelled(slot, false)
          || FlightSchedule.scheduled(slot, false) < now + LEAD_MINUTES) {
        continue;
      }
      if (world.rand.nextInt(++offered) == 0) {
        pick = slot;
      }
      if (offered == CHOICES) {
        break;
      }
    }
    if (pick == Long.MIN_VALUE) {
      return true;
    }
    String seat = (1 + world.rand.nextInt(32)) + ""
        + SEAT_LETTERS.charAt(world.rand.nextInt(SEAT_LETTERS.length()));
    ItemStack pass = ((ItemBoardingPass) item).create(pick, seat);
    if (!player.inventory.addItemStackToInventory(pass)) {
      player.dropItem(pass, false);
    }
    SoundEvent sound = TransitSounds.KIOSK_PRINT.getSoundEvent();
    if (sound != null) {
      world.playSound(null, pos, sound, SoundCategory.BLOCKS, 0.8F, 1.0F);
    }
    player.sendStatusMessage(new TextComponentTranslation("csm.transit.kiosk.printed",
        FlightSchedule.flight(pick, false), FlightSchedule.city(pick, false),
        FlightSchedule.gateName(FlightSchedule.gateValue(pick, false))), true);
    return true;
  }
}
