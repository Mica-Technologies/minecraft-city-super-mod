package com.micatechnologies.minecraft.csm.technology;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Makes the Technology tab's iPhones open SUM's phone when SUM is installed. On their own they are
 * decorative ("does nothing and is only for looks"); SUM has a real phone, with banking, messages and
 * other mods' apps, so with SUM present an iPhone is a way to open it.
 *
 * <p><b>SUM is never required,</b> so nothing here is compiled against it. The phone is opened through
 * SUM's public phone API, {@code com.micatechnologies.minecraft.sum.api.phone.PhoneApps.openPhone()},
 * looked up by reflection once. Without SUM, or with a SUM from before that API, the iPhones stay
 * exactly as decorative as they were.</p>
 *
 * <p>An event handler rather than a new item class so the items, their registrations and the
 * catalogue built from them are unchanged. It touches no client-only class: the phone itself is
 * opened by SUM, on the client side of the right-click only.</p>
 *
 * @since 2026.10
 */
public final class IphoneOpensSumPhone {

  private static final String SUM_MOD_ID = "sum";
  private static final String PHONE_APPS = "com.micatechnologies.minecraft.sum.api.phone.PhoneApps";

  /** The iPhones, by registry path in the {@code csm} namespace. */
  private static final Set<String> IPHONES = new HashSet<>(
      Arrays.asList("appleiphonese2020", "appleiphonexr", "appleiphonexs"));

  private static boolean resolved;
  private static Method openPhone;

  private static Method openPhone() {
    if (!resolved) {
      resolved = true;
      if (Loader.isModLoaded(SUM_MOD_ID)) {
        try {
          openPhone = Class.forName(PHONE_APPS).getMethod("openPhone");
        } catch (ReflectiveOperationException | LinkageError e) {
          openPhone = null; // an older SUM, with no phone API: the iPhones stay decorative
        }
      }
    }
    return openPhone;
  }

  private static boolean isIphone(ItemStack stack) {
    ResourceLocation name = stack.getItem().getRegistryName();
    return name != null && "csm".equals(name.getNamespace()) && IPHONES.contains(name.getPath());
  }

  @SubscribeEvent
  public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
    if (!event.getWorld().isRemote || !isIphone(event.getItemStack())) {
      return;
    }
    Method open = openPhone();
    if (open == null) {
      return;
    }
    try {
      if ((Boolean) open.invoke(null)) {
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
      }
    } catch (ReflectiveOperationException | RuntimeException e) {
      // SUM could not open its phone; the iPhone simply does nothing, as it always did.
    }
  }

  @SubscribeEvent
  public void onTooltip(ItemTooltipEvent event) {
    if (!isIphone(event.getItemStack()) || openPhone() == null) {
      return;
    }
    List<String> lines = event.getToolTip();
    for (int i = 0; i < lines.size(); i++) {
      if (lines.get(i).contains("does nothing")) {
        lines.set(i, "Right-click to open your phone.");
      }
    }
  }
}
