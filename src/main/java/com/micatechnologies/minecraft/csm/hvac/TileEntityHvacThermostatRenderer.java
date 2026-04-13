package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * TESR that renders dynamic text on the thermostat's front face: in-world time,
 * room temperature, and outside (biome) temperature. Styled to look like an
 * LCD thermostat display with consistent coloring. Enabled via CsmConfig.
 *
 * @author Mica Technologies
 * @since 2026.4
 */
@SideOnly(Side.CLIENT)
public class TileEntityHvacThermostatRenderer
    extends TileEntitySpecialRenderer<TileEntityHvacThermostat> {

  /** Scale factor for text rendered on the thermostat face. */
  private static final float TEXT_SCALE = 0.006f;

  /** LCD-style green color for primary display text. */
  private static final int COLOR_LCD = 0x40C040;
  /** LCD green for secondary/label text. */
  private static final int COLOR_LCD_SEC = 0x35A835;
  /** Subtle backlight glow color (low alpha green). */
  private static final int COLOR_GLOW = 0x1A402A;

  @Override
  public void render(TileEntityHvacThermostat te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (!CsmConfig.isThermostatDisplayEnabled()) {
      return;
    }
    if (te.getWorld() == null) {
      return;
    }

    World world = te.getWorld();
    IBlockState state = world.getBlockState(te.getPos());

    if (!(state.getBlock() instanceof BlockHvacThermostat)) {
      return;
    }

    EnumFacing facing = state.getValue(AbstractBlockRotatableNSEWUD.FACING);

    // Get data to display
    float roomTemp = te.getCurrentTemperature();
    int targetLow = te.getTargetTempLow();
    int targetHigh = te.getTargetTempHigh();
    boolean calling = te.isCalling();

    float biomeTemp = world.getBiome(te.getPos()).getTemperature(te.getPos());
    float outsideTempF = biomeTemp * 90.0f - 4.0f;

    long worldTime = world.getWorldTime() % 24000;
    int hours = (int) ((worldTime / 1000 + 6) % 24);
    int minutes = (int) ((worldTime % 1000) * 60 / 1000);
    boolean pm = hours >= 12;
    int displayHour = hours % 12;
    if (displayHour == 0) displayHour = 12;
    String timeStr = String.format("%d:%02d %s", displayHour, minutes, pm ? "PM" : "AM");

    // Room temperature with status indicator
    int roundedRoom = Math.round(roomTemp);
    String statusSymbol = "";
    if (calling) {
      if (roomTemp < targetLow) {
        statusSymbol = "\u25B2 "; // ▲ heating
      } else if (roomTemp > targetHigh) {
        statusSymbol = "\u25BC "; // ▼ cooling
      }
    }
    String roomTempStr = statusSymbol + roundedRoom + "\u00B0F";

    String outsideTempStr = Math.round(outsideTempF) + "\u00B0F";
    String setpointStr = targetLow + "-" + targetHigh + "\u00B0F";

    FontRenderer fr = Minecraft.getMinecraft().fontRenderer;

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);

    // Rotate to face the correct direction
    switch (facing) {
      case SOUTH:
        GlStateManager.rotate(180, 0, 1, 0);
        break;
      case WEST:
        GlStateManager.rotate(90, 0, 1, 0);
        break;
      case EAST:
        GlStateManager.rotate(-90, 0, 1, 0);
        break;
      case NORTH:
      default:
        break;
    }

    // Flip 180° so text faces outward (toward the player) instead of into the wall
    GlStateManager.rotate(180, 0, 1, 0);

    // Position on the front face of the thermostat model
    GlStateManager.translate(0, -0.09, -0.405);

    // Scale text and flip Y (MC text Y goes down, block Y goes up)
    GlStateManager.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

    // Disable lighting so text is always readable
    GlStateManager.disableLighting();
    GlStateManager.depthMask(false);

    // Draw subtle LCD backlight glow behind the display area
    GlStateManager.enableBlend();
    GlStateManager.tryBlendFuncSeparate(
        GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    GlStateManager.disableTexture2D();
    net.minecraft.client.renderer.Tessellator tess = net.minecraft.client.renderer.Tessellator.getInstance();
    net.minecraft.client.renderer.BufferBuilder buf = tess.getBuffer();
    float glowR = ((COLOR_GLOW >> 16) & 0xFF) / 255.0f;
    float glowG = ((COLOR_GLOW >> 8) & 0xFF) / 255.0f;
    float glowB = (COLOR_GLOW & 0xFF) / 255.0f;
    buf.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);
    buf.pos(-33, -29, -0.1).color(glowR, glowG, glowB, 0.45f).endVertex();
    buf.pos(-33, 31, -0.1).color(glowR, glowG, glowB, 0.45f).endVertex();
    buf.pos(32, 31, -0.1).color(glowR, glowG, glowB, 0.45f).endVertex();
    buf.pos(32, -29, -0.1).color(glowR, glowG, glowB, 0.45f).endVertex();
    tess.draw();
    GlStateManager.enableTexture2D();
    GlStateManager.disableBlend();

    // Draw time (top center)
    fr.drawString(timeStr, -fr.getStringWidth(timeStr) / 2, -25, COLOR_LCD_SEC);

    // Draw room temperature (larger, center)
    GlStateManager.pushMatrix();
    GlStateManager.scale(1.5f, 1.5f, 1.0f);
    fr.drawString(roomTempStr, -fr.getStringWidth(roomTempStr) / 2, -7, COLOR_LCD);
    GlStateManager.popMatrix();

    // Draw setpoint and outside temp
    fr.drawString("Set: " + setpointStr, -fr.getStringWidth("Set: " + setpointStr) / 2, 10, COLOR_LCD_SEC);
    fr.drawString("Out: " + outsideTempStr, -fr.getStringWidth("Out: " + outsideTempStr) / 2, 20, COLOR_LCD_SEC);

    GlStateManager.depthMask(true);
    GlStateManager.enableLighting();
    GlStateManager.popMatrix();
  }
}
