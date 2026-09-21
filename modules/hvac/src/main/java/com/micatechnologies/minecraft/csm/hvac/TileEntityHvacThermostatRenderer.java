package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderUtils;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * TESR that renders dynamic text on the thermostat's front face: in-world time,
 * room temperature, and outside (biome) temperature. Styled to look like an
 * LCD thermostat display with consistent coloring. Enabled via CsmConfig.
 *
 * <p>Supports both {@link TileEntityHvacThermostat} and {@link TileEntityHvacZoneThermostat}
 * via the {@link IHvacThermostatDisplay} interface.</p>
 *
 * <p><b>Performance:</b> String formatting and biome lookups are cached and only recomputed
 * when the underlying values change. GL state changes are minimized. The glow quad color
 * components are precomputed as static constants.</p>
 *
 * @author Mica Technologies
 * @since 2026.4
 */
@SideOnly(Side.CLIENT)
public class TileEntityHvacThermostatRenderer
    extends TileEntitySpecialRenderer<TileEntity> {

  /** Scale factor for text rendered on the thermostat face. */
  private static final float TEXT_SCALE = 0.006f;

  /** LCD-style green color for primary display text. */
  private static final int COLOR_LCD = 0x40C040;
  /** LCD green for secondary/label text. */
  private static final int COLOR_LCD_SEC = 0x35A835;

  /** Precomputed glow color components (avoids per-frame bit shifting). */
  private static final float GLOW_R = 0x1A / 255.0f;
  private static final float GLOW_G = 0x40 / 255.0f;
  private static final float GLOW_B = 0x2A / 255.0f;

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");
  private static final int LIGHTMAP_FULLBRIGHT_SKY = 240;
  private static final int LIGHTMAP_FULLBRIGHT_BLOCK = 240;

  /**
   * One thermostat's display strings, their widths and its outside temperature, each rebuilt only
   * when its inputs change.
   *
   * <p>These used to be single fields on the renderer, which Minecraft creates once for every
   * thermostat, so with two thermostats in view each overwrote the other's strings every frame
   * and every frame reformatted all four. Keyed weakly on the tile entity, so an entry goes when
   * its thermostat does.</p>
   */
  private static final class DisplayCache {
    private long lastMinuteOfDay = -1;
    private String timeStr = "";
    private int timeWidth;
    private int lastRoundedRoom = Integer.MIN_VALUE;
    private int lastCallingMode = -1;
    private boolean lastCalling = false;
    private String roomTempStr = "";
    private int roomTempWidth;
    private int lastTargetLow = -1;
    private int lastTargetHigh = -1;
    private String setpointLine = "";
    private int setpointWidth;
    /** Zero, not Long.MIN_VALUE: the elapsed-time test would overflow and never read. */
    private long lastBiomeReadMillis = 0L;
    private int lastOutsideTemp = Integer.MIN_VALUE;
    private String outsideLine = "";
    private int outsideWidth;
  }

  private final Map<TileEntity, DisplayCache> displayCaches = new WeakHashMap<>();

  @Override
  public void render(TileEntity tileEntity, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (!CsmConfig.isThermostatDisplayEnabled()) {
      return;
    }
    if (!(tileEntity instanceof IHvacThermostatDisplay)) {
      return;
    }
    if (tileEntity.getWorld() == null) {
      return;
    }

    IHvacThermostatDisplay te = (IHvacThermostatDisplay) tileEntity;
    World world = tileEntity.getWorld();
    IBlockState state = world.getBlockState(tileEntity.getPos());

    if (!(state.getBlock() instanceof BlockHvacThermostat)
        && !(state.getBlock() instanceof BlockHvacZoneThermostat)) {
      return;
    }

    EnumFacing facing = state.getValue(AbstractBlockRotatableNSEWUD.FACING);

    // Get data to display
    float roomTemp = te.getCurrentTemperature();
    int targetLow = te.getTargetTempLow();
    int targetHigh = te.getTargetTempHigh();
    boolean calling = te.isCalling();
    int callingMode = te.getCallingMode();
    int roundedRoom = Math.round(roomTemp);

    DisplayCache cache = displayCaches.get(tileEntity);
    if (cache == null) {
      cache = new DisplayCache();
      displayCaches.put(tileEntity, cache);
    }
    FontRenderer fr = Minecraft.getMinecraft().fontRenderer;

    // The world clock moves every tick but the display shows minutes (1,000 ticks an hour), so key
    // on the minute; comparing raw ticks reformatted the string twenty times a second.
    long worldTime = world.getWorldTime() % 24000;
    long minuteOfDay = worldTime * 60 / 1000;
    if (minuteOfDay != cache.lastMinuteOfDay) {
      cache.lastMinuteOfDay = minuteOfDay;
      int hours = (int) ((worldTime / 1000 + 6) % 24);
      int minutes = (int) ((worldTime % 1000) * 60 / 1000);
      boolean pm = hours >= 12;
      int displayHour = hours % 12;
      if (displayHour == 0) displayHour = 12;
      cache.timeStr = String.format("%d:%02d %s", displayHour, minutes, pm ? "PM" : "AM");
      cache.timeWidth = fr.getStringWidth(cache.timeStr);
    }

    // The outside temperature is the biome's, which changes only if the biome is edited: read it
    // once a second (wall clock, so it still updates with the day cycle frozen), not every frame.
    long nowMillis = CsmRenderUtils.gameMillis(world);
    if (nowMillis - cache.lastBiomeReadMillis >= 1000L) {
      cache.lastBiomeReadMillis = nowMillis;
      float biomeTemp = world.getBiome(tileEntity.getPos()).getTemperature(tileEntity.getPos());
      int roundedOutside = Math.round(biomeTemp * 90.0f - 4.0f);
      if (roundedOutside != cache.lastOutsideTemp) {
        cache.lastOutsideTemp = roundedOutside;
        cache.outsideLine = "Out: " + roundedOutside + "\u00B0F";
        cache.outsideWidth = fr.getStringWidth(cache.outsideLine);
      }
    }

    // Cache room temperature string — only rebuild when rounded temp or mode changes
    if (roundedRoom != cache.lastRoundedRoom || callingMode != cache.lastCallingMode
        || calling != cache.lastCalling) {
      cache.lastRoundedRoom = roundedRoom;
      cache.lastCallingMode = callingMode;
      cache.lastCalling = calling;
      String statusSymbol = "";
      if (calling) {
        if (callingMode == 1) {
          statusSymbol = "\u25B2 ";
        } else if (callingMode == 2) {
          statusSymbol = "\u25BC ";
        }
      }
      cache.roomTempStr = statusSymbol + roundedRoom + "\u00B0F";
      cache.roomTempWidth = fr.getStringWidth(cache.roomTempStr);
    }

    // Cache setpoint string — only rebuild when setpoints change
    if (targetLow != cache.lastTargetLow || targetHigh != cache.lastTargetHigh) {
      cache.lastTargetLow = targetLow;
      cache.lastTargetHigh = targetHigh;
      cache.setpointLine = "Set: " + targetLow + "-" + targetHigh + "\u00B0F";
      cache.setpointWidth = fr.getStringWidth(cache.setpointLine);
    }

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

    // Flip 180 so text faces outward (toward the player) instead of into the wall
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
    // Bind a 1x1 white pixel texture instead of disableTexture2D — shaders ignore
    // disableTexture2D and sample whatever was last bound. Fullbright lightmap is baked
    // per-vertex via the BLOCK vertex format. FontRenderer rebinds its own texture below.
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    net.minecraft.client.renderer.Tessellator tess = net.minecraft.client.renderer.Tessellator.getInstance();
    net.minecraft.client.renderer.BufferBuilder buf = tess.getBuffer();
    buf.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.BLOCK);
    buf.pos(-33, -29, -0.1).color(GLOW_R, GLOW_G, GLOW_B, 0.45f)
        .tex(0.5f, 0.5f).lightmap(LIGHTMAP_FULLBRIGHT_SKY, LIGHTMAP_FULLBRIGHT_BLOCK).endVertex();
    buf.pos(-33, 31, -0.1).color(GLOW_R, GLOW_G, GLOW_B, 0.45f)
        .tex(0.5f, 0.5f).lightmap(LIGHTMAP_FULLBRIGHT_SKY, LIGHTMAP_FULLBRIGHT_BLOCK).endVertex();
    buf.pos(32, 31, -0.1).color(GLOW_R, GLOW_G, GLOW_B, 0.45f)
        .tex(0.5f, 0.5f).lightmap(LIGHTMAP_FULLBRIGHT_SKY, LIGHTMAP_FULLBRIGHT_BLOCK).endVertex();
    buf.pos(32, -29, -0.1).color(GLOW_R, GLOW_G, GLOW_B, 0.45f)
        .tex(0.5f, 0.5f).lightmap(LIGHTMAP_FULLBRIGHT_SKY, LIGHTMAP_FULLBRIGHT_BLOCK).endVertex();
    tess.draw();
    GlStateManager.disableBlend();

    // Draw time (top center)
    fr.drawString(cache.timeStr, -cache.timeWidth / 2, -25, COLOR_LCD_SEC);

    // Draw room temperature (larger, center)
    GlStateManager.pushMatrix();
    GlStateManager.scale(1.5f, 1.5f, 1.0f);
    fr.drawString(cache.roomTempStr, -cache.roomTempWidth / 2, -7, COLOR_LCD);
    GlStateManager.popMatrix();

    // Draw setpoint and outside temp (strings pre-built, no per-frame concatenation)
    fr.drawString(cache.setpointLine, -cache.setpointWidth / 2, 10, COLOR_LCD_SEC);
    fr.drawString(cache.outsideLine, -cache.outsideWidth / 2, 20, COLOR_LCD_SEC);

    GlStateManager.depthMask(true);
    GlStateManager.enableLighting();
    GlStateManager.popMatrix();
  }
}
