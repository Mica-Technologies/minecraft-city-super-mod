package com.micatechnologies.minecraft.csm.hvac;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-side HUD overlay showing the temperature where the player stands, whenever they are near
 * HVAC equipment or inside a room the simulation knows. Position is configurable via anchor
 * (top-left, top-right, bottom-left, bottom-right) and offset.
 *
 * <p>The number is the server's: {@link HvacHudPacket} brings the simulation's temperature for the
 * player's position about once a second, and this overlay draws it as it came. It computes and
 * smooths nothing of its own, so it shows what a thermostat in the same spot shows.</p>
 *
 * @author Mica Technologies
 * @see HvacThermalWorld
 * @since 2026.4
 */
@SideOnly(Side.CLIENT)
public class HvacHudOverlay {

  /** Width of the background rectangle in pixels. */
  private static final int BG_WIDTH = 80;

  /** Height of the background rectangle in pixels. */
  private static final int BG_HEIGHT = 16;

  /** Size of the color indicator square in pixels. */
  private static final int INDICATOR_SIZE = 8;

  /** Semi-transparent black background color (ARGB). */
  private static final int COLOR_BG = 0xAA000000;

  /** White text color. */
  private static final int COLOR_TEXT = 0xFFFFFFFF;

  private static final int COLOR_COLD = 0xFF3399FF;
  private static final int COLOR_COMFORTABLE = 0xFF33CC33;
  private static final int COLOR_WARM = 0xFFFFCC00;
  private static final int COLOR_HOT = 0xFFFF3333;

  private static final int ALTITUDE_THRESHOLD = 64;
  private static final float THRESHOLD_COLD = 60.0f;
  private static final float THRESHOLD_WARM = 80.0f;
  private static final float THRESHOLD_HOT = 95.0f;

  /**
   * HUD anchor position. Configurable — defaults to top-left.
   * 0 = top-left, 1 = top-right, 2 = bottom-left, 3 = bottom-right
   */
  private static int hudAnchor = 0;

  /** X offset from the anchor edge in pixels. */
  private static int hudOffsetX = 4;

  /** Y offset from the anchor edge in pixels. */
  private static int hudOffsetY = 4;

  /**
   * Sets the HUD position anchor and offset. Can be called from a config system or command.
   *
   * @param anchor  0=top-left, 1=top-right, 2=bottom-left, 3=bottom-right
   * @param offsetX pixels from the anchor edge horizontally
   * @param offsetY pixels from the anchor edge vertically
   */
  public static void setHudPosition(int anchor, int offsetX, int offsetY) {
    hudAnchor = Math.max(0, Math.min(3, anchor));
    hudOffsetX = offsetX;
    hudOffsetY = offsetY;
  }

  public static void register() {
    MinecraftForge.EVENT_BUS.register(new HvacHudOverlay());
  }

  @SubscribeEvent
  public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
    if (event.getType() != RenderGameOverlayEvent.ElementType.TEXT) {
      return;
    }

    Minecraft mc = Minecraft.getMinecraft();
    EntityPlayer player = mc.player;
    if (player == null) {
      return;
    }

    if (!HvacHudPacket.clientVisible
        || System.currentTimeMillis() - HvacHudPacket.clientReceivedMs
        > HvacHudPacket.CLIENT_STALE_MS) {
      return;
    }
    int playerBlockY = MathHelper.floor(player.posY);

    float temperature = HvacHudPacket.clientTemperature;
    int indicatorColor = getIndicatorColor(temperature);
    String altitudeIndicator = playerBlockY > ALTITUDE_THRESHOLD ? " \u2191" : "";
    String tempText = Math.round(temperature) + "\u00B0F" + altitudeIndicator;

    ScaledResolution resolution = event.getResolution();
    int screenW = resolution.getScaledWidth();
    int screenH = resolution.getScaledHeight();

    // Calculate position based on anchor
    int x, y;
    switch (hudAnchor) {
      case 1: // top-right
        x = screenW - BG_WIDTH - hudOffsetX;
        y = hudOffsetY;
        break;
      case 2: // bottom-left
        x = hudOffsetX;
        y = screenH - BG_HEIGHT - hudOffsetY;
        break;
      case 3: // bottom-right
        x = screenW - BG_WIDTH - hudOffsetX;
        y = screenH - BG_HEIGHT - hudOffsetY;
        break;
      default: // 0 = top-left
        x = hudOffsetX;
        y = hudOffsetY;
        break;
    }

    // Draw semi-transparent background
    Gui.drawRect(x, y, x + BG_WIDTH, y + BG_HEIGHT, COLOR_BG);

    // Draw color indicator square (vertically centered)
    int indicatorX = x + 3;
    int indicatorY = y + (BG_HEIGHT - INDICATOR_SIZE) / 2;
    Gui.drawRect(indicatorX, indicatorY, indicatorX + INDICATOR_SIZE,
        indicatorY + INDICATOR_SIZE, indicatorColor);

    // Draw temperature text
    int textX = indicatorX + INDICATOR_SIZE + 4;
    int textY = y + (BG_HEIGHT - mc.fontRenderer.FONT_HEIGHT) / 2 + 1;
    mc.fontRenderer.drawString(tempText, textX, textY, COLOR_TEXT);
  }

  private static int getIndicatorColor(float temperature) {
    if (temperature < THRESHOLD_COLD) {
      return COLOR_COLD;
    } else if (temperature <= THRESHOLD_WARM) {
      return COLOR_COMFORTABLE;
    } else if (temperature <= THRESHOLD_HOT) {
      return COLOR_WARM;
    } else {
      return COLOR_HOT;
    }
  }
}
