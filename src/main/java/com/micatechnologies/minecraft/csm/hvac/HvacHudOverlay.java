package com.micatechnologies.minecraft.csm.hvac;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-side HUD overlay that renders a temperature readout when the player is near HVAC
 * equipment (active or not). Position is configurable via anchor (top-left, top-right,
 * bottom-left, bottom-right) and offset. The HUD shows whenever any HVAC unit is nearby,
 * even if unpowered, so players can see the ambient temperature in HVAC-equipped areas.
 *
 * @author Mica Technologies
 * @see HvacTemperatureManager
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

  /** Detection range in blocks for nearby HVAC equipment. */
  private static final int HVAC_DETECTION_RANGE = 16;

  private static final long RECHECK_INTERVAL_MS = 500L;
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

  private boolean cachedNearHvac = false;
  private float cachedTemperature = 0.0f;
  private long cachedChunkKey = Long.MIN_VALUE;
  private long lastCheckTime = 0L;

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

    World world = player.world;
    BlockPos playerPos = player.getPosition();

    // Force recheck when player moves to a different chunk
    long currentChunkKey = ((long) (playerPos.getX() >> 4)) ^ (((long) (playerPos.getZ() >> 4)) << 32);
    boolean chunkChanged = currentChunkKey != cachedChunkKey;
    if (chunkChanged) {
      cachedChunkKey = currentChunkKey;
    }

    // Recheck HVAC proximity and temperature periodically or on chunk change
    long now = System.currentTimeMillis();
    if (chunkChanged || (now - lastCheckTime > RECHECK_INTERVAL_MS)) {
      lastCheckTime = now;
      cachedNearHvac = HvacTemperatureManager.isNearAnyHvac(world, playerPos, HVAC_DETECTION_RANGE);
      cachedTemperature = HvacTemperatureManager.getTemperatureAt(world, playerPos);
    }

    if (!cachedNearHvac) {
      return;
    }

    float temperature = cachedTemperature;
    int indicatorColor = getIndicatorColor(temperature);
    String altitudeIndicator = playerPos.getY() > ALTITUDE_THRESHOLD ? " \u2191" : "";
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
