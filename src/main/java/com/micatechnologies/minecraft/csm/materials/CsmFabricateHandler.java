package com.micatechnologies.minecraft.csm.materials;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link CsmFabricatePacket}.
 *
 * <p>Everything the client sent is treated as untrusted and re-derived or re-checked here:</p>
 * <ul>
 *   <li>the requested quantity is clamped to a sane range;</li>
 *   <li>the player must still be within ~6 blocks of the Fabricator, which blocks replaying the
 *       packet from across the world;</li>
 *   <li>the block at that position must still actually be a {@link BlockCsmFabricator};</li>
 *   <li>the named block must resolve in the registry and must be fabricable — the cost is looked
 *       up server-side from {@link CsmFabricatorCosts}, never taken from the packet;</li>
 *   <li>the player must hold every required part in the required quantity.</li>
 * </ul>
 *
 * <p>Parts are only consumed once all checks pass, so a failed attempt can never destroy
 * materials.</p>
 *
 * @author Mica Technologies
 * @since 2026.7
 */
public class CsmFabricateHandler implements IMessageHandler<CsmFabricatePacket, IMessage> {

  /** Maximum squared distance the player may be from the Fabricator when fabricating. */
  private static final double MAX_INTERACT_DIST_SQ = 36.0; // 6 blocks

  /** Largest batch that may be fabricated in a single request. */
  private static final int MAX_QUANTITY = 64;

  @Override
  public IMessage onMessage(CsmFabricatePacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> handle(message, player));
    return null;
  }

  private static void handle(CsmFabricatePacket message, EntityPlayerMP player) {
    BlockPos pos = message.getFabricatorPos();
    if (pos == null || message.getBlockRegistryName() == null) {
      return;
    }

    int quantity = Math.max(1, Math.min(MAX_QUANTITY, message.getQuantity()));

    World world = player.world;
    if (player.getDistanceSq(pos) > MAX_INTERACT_DIST_SQ) {
      return;
    }
    if (!(world.getBlockState(pos).getBlock() instanceof BlockCsmFabricator)) {
      return;
    }

    Block target = CsmRegistry.getBlock(message.getBlockRegistryName());
    if (target == null) {
      return;
    }
    Map<String, Integer> unitCost = CsmFabricatorCosts.getCost(target);
    if (unitCost == null) {
      // Not a fabricable block. A well-behaved client never offers these, so no message.
      return;
    }

    // Item.getItemFromBlock returns Items.AIR for a block with no item form (e.g. the
    // double-slab counterparts), which must never be handed to the player.
    Item targetItem = Item.getItemFromBlock(target);
    if (targetItem == Items.AIR) {
      return;
    }

    // Scale the unit cost by the batch size, then verify affordability before taking anything.
    InventoryPlayer inv = player.inventory;
    Map<String, Integer> totalCost = new HashMap<>();
    for (Map.Entry<String, Integer> entry : unitCost.entrySet()) {
      totalCost.put(entry.getKey(), entry.getValue() * quantity);
    }

    for (Map.Entry<String, Integer> entry : totalCost.entrySet()) {
      Item part = CsmRegistry.getItem(entry.getKey());
      if (part == null) {
        return;
      }
      if (countItem(inv, part) < entry.getValue()) {
        player.sendMessage(new TextComponentString(
            "§cNot enough parts — need " + describeCost(totalCost) + "."));
        return;
      }
    }

    for (Map.Entry<String, Integer> entry : totalCost.entrySet()) {
      consumeItem(inv, CsmRegistry.getItem(entry.getKey()), entry.getValue());
    }

    ItemStack result = new ItemStack(targetItem, quantity);
    if (!inv.addItemStackToInventory(result)) {
      player.dropItem(result, false);
    }

    SoundEvent sound = SoundEvents.BLOCK_ANVIL_USE;
    world.playSound(null, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
        sound, SoundCategory.BLOCKS, 0.4F, 1.4F);

    player.inventoryContainer.detectAndSendChanges();
  }

  /** Renders a cost map as "2x Sheet Metal, 1x Fastener Kit" for player-facing messages. */
  private static String describeCost(Map<String, Integer> cost) {
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<String, Integer> entry : cost.entrySet()) {
      if (builder.length() > 0) {
        builder.append(", ");
      }
      Item part = CsmRegistry.getItem(entry.getKey());
      String name = part == null
          ? entry.getKey()
          : new ItemStack(part).getDisplayName();
      builder.append(entry.getValue()).append("x ").append(name);
    }
    return builder.toString();
  }

  /** Counts an item across the player's main inventory and hotbar. */
  private static int countItem(InventoryPlayer inv, Item item) {
    int total = 0;
    for (int i = 0; i < inv.mainInventory.size(); i++) {
      ItemStack stack = inv.mainInventory.get(i);
      if (stack.getItem() == item) {
        total += stack.getCount();
      }
    }
    return total;
  }

  /**
   * Removes {@code amount} of an item from the player's main inventory. Callers must have
   * verified availability with {@link #countItem} first.
   */
  private static void consumeItem(InventoryPlayer inv, Item item, int amount) {
    int remaining = amount;
    for (int i = 0; i < inv.mainInventory.size() && remaining > 0; i++) {
      ItemStack stack = inv.mainInventory.get(i);
      if (stack.getItem() == item) {
        int take = Math.min(stack.getCount(), remaining);
        stack.shrink(take);
        remaining -= take;
        if (stack.isEmpty()) {
          inv.mainInventory.set(i, ItemStack.EMPTY);
        }
      }
    }
  }
}
