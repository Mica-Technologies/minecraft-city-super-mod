package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A parking garage ticket (#302): issued by a ticket dispenser with the time of entry, taken
 * back by an exit pay kiosk, which charges for the time since. Real time, as the parking meters
 * keep, so a garage charges for the hours a player was actually away. NBT: {@code t} the entry
 * time in epoch milliseconds.
 *
 * @since 2026.10
 */
public class ItemParkingTicket extends AbstractItem {

  public ItemParkingTicket() {
    super(0, 1);
  }

  @Override
  public String getItemRegistryName() {
    return "parking_ticket";
  }

  /** A ticket stamped with the time now. */
  public ItemStack issue() {
    ItemStack stack = new ItemStack(this);
    NBTTagCompound tag = new NBTTagCompound();
    tag.setLong("t", System.currentTimeMillis());
    stack.setTagCompound(tag);
    return stack;
  }

  /** The time a ticket was issued, or -1 for one with no stamp. */
  public static long entryTime(ItemStack stack) {
    NBTTagCompound tag = stack.getTagCompound();
    return tag != null && tag.hasKey("t") ? tag.getLong("t") : -1L;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
      ITooltipFlag flag) {
    long t = entryTime(stack);
    if (t > 0) {
      tooltip.add("Entered " + new SimpleDateFormat("MMM d, HH:mm").format(new Date(t)));
    }
  }
}
