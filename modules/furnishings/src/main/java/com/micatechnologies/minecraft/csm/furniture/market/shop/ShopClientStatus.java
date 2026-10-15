package com.micatechnologies.minecraft.csm.furniture.market.shop;

import javax.annotation.Nullable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The latest {@link ShopStatusPacket} this client was sent, for the shop screen that is open.
 * Only one shop screen is ever open, so one is kept.
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public final class ShopClientStatus {

  @Nullable
  private static ShopStatusPacket latest;

  private ShopClientStatus() {
  }

  static void set(ShopStatusPacket status) {
    latest = status;
  }

  /** The status of the shop at {@code pos}, or null if none has arrived for it. */
  @Nullable
  public static ShopStatusPacket get(BlockPos pos) {
    ShopStatusPacket s = latest;
    return s != null && s.getPos().equals(pos) ? s : null;
  }

  /** Forgets it, when the screen closes. */
  public static void clear() {
    latest = null;
  }
}
