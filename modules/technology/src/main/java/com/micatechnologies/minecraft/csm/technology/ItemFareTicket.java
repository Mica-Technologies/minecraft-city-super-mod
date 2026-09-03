package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Single-use fare ticket. Purchased at a {@link BlockFareVendingMachine} for one trip; the
 * fare gate consumes the ticket from the player's inventory when it's used to open the gate.
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class ItemFareTicket extends AbstractItem {

  public ItemFareTicket() {
    super(0, 64);
  }

  @Override
  public String getItemRegistryName() {
    return "fareticket";
  }

  @Override
  public void addInformation(ItemStack stack, World world, List<String> tooltip,
      ITooltipFlag flag) {
    super.addInformation(stack, world, tooltip, flag);
    tooltip.add("§7Single-use fare ticket — 1 trip");
    tooltip.add("§7Use at a fare gate to enter");
  }
}
