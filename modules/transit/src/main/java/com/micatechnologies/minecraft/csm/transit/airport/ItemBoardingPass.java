package com.micatechnologies.minecraft.csm.transit.airport;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A boarding pass, printed by the self check-in kiosk for a flight off the departure boards and
 * read at the gate by the boarding pass scanner, which marks it boarded. The pass carries only
 * its flight's slot, a seat and whether it has been used (NBT {@code s}, {@code seat} and
 * {@code b}); the flight number, city, time and gate are worked out from the slot by
 * {@link FlightSchedule}, so a pass always agrees with the boards. A pass with no flight (one
 * taken from the creative tab) says where passes come from.
 *
 * @since 2026.9
 */
public class ItemBoardingPass extends AbstractItem {

  private static final String KEY_SLOT = "s";
  private static final String KEY_SEAT = "seat";
  private static final String KEY_BOARDED = "b";

  public ItemBoardingPass() {
    super(0, 1);
  }

  @Override
  public String getItemRegistryName() {
    return "boarding_pass";
  }

  /**
   * Makes a pass for a flight.
   *
   * @param slot the departing flight's slot
   * @param seat the seat, "14C"
   *
   * @return the pass
   */
  public ItemStack create(long slot, String seat) {
    ItemStack stack = new ItemStack(this, 1);
    NBTTagCompound tag = new NBTTagCompound();
    tag.setLong(KEY_SLOT, slot);
    tag.setString(KEY_SEAT, seat);
    stack.setTagCompound(tag);
    return stack;
  }

  /** Whether the pass is for a flight at all. */
  public static boolean hasFlight(ItemStack stack) {
    NBTTagCompound tag = stack.getTagCompound();
    return tag != null && tag.hasKey(KEY_SLOT);
  }

  /** The flight's slot; meaningful only when {@link #hasFlight}. */
  public static long getSlot(ItemStack stack) {
    NBTTagCompound tag = stack.getTagCompound();
    return tag == null ? 0L : tag.getLong(KEY_SLOT);
  }

  /** The seat printed on the pass. */
  public static String getSeat(ItemStack stack) {
    NBTTagCompound tag = stack.getTagCompound();
    return tag == null ? "" : tag.getString(KEY_SEAT);
  }

  /** Whether the pass has been scanned at a gate. */
  public static boolean isBoarded(ItemStack stack) {
    NBTTagCompound tag = stack.getTagCompound();
    return tag != null && tag.getBoolean(KEY_BOARDED);
  }

  /** Marks the pass scanned. */
  public static void setBoarded(ItemStack stack) {
    NBTTagCompound tag = stack.getTagCompound();
    if (tag != null) {
      tag.setBoolean(KEY_BOARDED, true);
    }
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
      ITooltipFlag flag) {
    super.addInformation(stack, world, tooltip, flag);
    if (!hasFlight(stack)) {
      tooltip.add("§7" + I18n.format("csm.transit.pass.blank"));
      return;
    }
    long slot = getSlot(stack);
    tooltip.add("§f" + I18n.format("csm.transit.pass.flight",
        FlightSchedule.flight(slot, false), FlightSchedule.city(slot, false)));
    tooltip.add("§7" + I18n.format("csm.transit.pass.departs",
        FlightSchedule.clock(FlightSchedule.scheduled(slot, false)),
        FlightSchedule.gateName(FlightSchedule.gateValue(slot, false))));
    tooltip.add("§7" + I18n.format("csm.transit.pass.seat", getSeat(stack)));
    if (isBoarded(stack)) {
      tooltip.add("§a" + I18n.format("csm.transit.pass.boarded"));
    }
  }

  @Override
  public boolean hasEffect(ItemStack stack) {
    return false;
  }
}
