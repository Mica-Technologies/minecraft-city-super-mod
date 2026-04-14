package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Configuration GUI for the HVAC thermostat. Shows a visual temperature gauge with the
 * comfort range highlighted, and explains the behavior clearly.
 */
@SideOnly(Side.CLIENT)
public class HvacThermostatGui extends GuiScreen {

  private static final int BTN_LOW_MINUS = 0;
  private static final int BTN_LOW_PLUS = 1;
  private static final int BTN_HIGH_MINUS = 2;
  private static final int BTN_HIGH_PLUS = 3;
  private static final int BTN_CLOSE = 4;
  private static final int TEMP_STEP = 5;

  private static final int GUI_WIDTH = 240;
  private static final int GUI_HEIGHT = 200;

  private static final int COLOR_COLD = 0xFF3399FF;
  private static final int COLOR_COMFORT = 0xFF33CC33;
  private static final int COLOR_HOT = 0xFFFF5555;
  private static final int COLOR_GAUGE_BG = 0xFF222222;
  private static final int COLOR_NEEDLE = 0xFFFFFFFF;

  private final TileEntityHvacThermostat thermostat;
  private final BlockPos blockPos;
  private int guiLeft;
  private int guiTop;

  public HvacThermostatGui(TileEntityHvacThermostat thermostat) {
    this.thermostat = thermostat;
    this.blockPos = thermostat.getPos();
  }

  @Override
  public void initGui() {
    buttonList.clear();
    guiLeft = (width - GUI_WIDTH) / 2;
    guiTop = (height - GUI_HEIGHT) / 2;

    int btnW = 20;
    int btnH = 16;
    int col1Btn = guiLeft + 100;
    int col2Btn = col1Btn + 68;

    // Min temp controls
    int minY = guiTop + 90;
    buttonList.add(new GuiButton(BTN_LOW_MINUS, col1Btn, minY, btnW, btnH, "-"));
    buttonList.add(new GuiButton(BTN_LOW_PLUS, col2Btn, minY, btnW, btnH, "+"));

    // Max temp controls
    int maxY = guiTop + 112;
    buttonList.add(new GuiButton(BTN_HIGH_MINUS, col1Btn, maxY, btnW, btnH, "-"));
    buttonList.add(new GuiButton(BTN_HIGH_PLUS, col2Btn, maxY, btnW, btnH, "+"));

    // Close button
    buttonList.add(new GuiButton(BTN_CLOSE, guiLeft + GUI_WIDTH / 2 - 40,
        guiTop + GUI_HEIGHT - 20, 80, 16, "Close"));
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    // Panel background
    drawRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xCC000000);
    drawRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + 1, 0xFF444444);
    drawRect(guiLeft, guiTop + GUI_HEIGHT - 1, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xFF444444);
    drawRect(guiLeft, guiTop, guiLeft + 1, guiTop + GUI_HEIGHT, 0xFF444444);
    drawRect(guiLeft + GUI_WIDTH - 1, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xFF444444);

    int cx = guiLeft + GUI_WIDTH / 2;

    // Title
    drawCenteredString(fontRenderer, "HVAC Thermostat", cx, guiTop + 6, 0xFFFFFF);

    // Current temperature — large display
    float currentTemp = thermostat.getCurrentTemperature();
    int lowTarget = thermostat.getTargetTempLow();
    int highTarget = thermostat.getTargetTempHigh();
    int tempColor = currentTemp < lowTarget ? COLOR_COLD
        : currentTemp > highTarget ? COLOR_HOT : COLOR_COMFORT;
    String tempStr = Math.round(currentTemp) + "\u00B0F";
    drawCenteredString(fontRenderer, "Current: " + tempStr, cx, guiTop + 20, tempColor);

    // === Temperature Gauge Bar ===
    int gaugeLeft = guiLeft + 14;
    int gaugeRight = guiLeft + GUI_WIDTH - 14;
    int gaugeWidth = gaugeRight - gaugeLeft;
    int gaugeY = guiTop + 36;
    int gaugeH = 16;
    float gaugeMin = 0.0f;
    float gaugeMax = 120.0f;
    float gaugeRange = gaugeMax - gaugeMin;

    // Background
    drawRect(gaugeLeft, gaugeY, gaugeRight, gaugeY + gaugeH, COLOR_GAUGE_BG);

    // Colored zones
    int lowX = gaugeLeft + (int) ((Math.max(gaugeMin, Math.min(gaugeMax, lowTarget)) - gaugeMin)
        / gaugeRange * gaugeWidth);
    int highX = gaugeLeft + (int) ((Math.max(gaugeMin, Math.min(gaugeMax, highTarget)) - gaugeMin)
        / gaugeRange * gaugeWidth);

    drawRect(gaugeLeft, gaugeY, lowX, gaugeY + gaugeH, 0x553399FF);
    drawRect(lowX, gaugeY, highX, gaugeY + gaugeH, 0x5533CC33);
    drawRect(highX, gaugeY, gaugeRight, gaugeY + gaugeH, 0x55FF5555);

    // Range boundary lines
    drawRect(lowX - 1, gaugeY, lowX + 1, gaugeY + gaugeH, COLOR_COMFORT);
    drawRect(highX - 1, gaugeY, highX + 1, gaugeY + gaugeH, COLOR_COMFORT);

    // Current temperature needle
    float clampedTemp = Math.max(gaugeMin, Math.min(gaugeMax, currentTemp));
    int needleX = gaugeLeft + (int) ((clampedTemp - gaugeMin) / gaugeRange * gaugeWidth);
    drawRect(needleX - 1, gaugeY - 2, needleX + 2, gaugeY + gaugeH + 2, COLOR_NEEDLE);

    // Scale labels
    drawString(fontRenderer, "0\u00B0", gaugeLeft, gaugeY + gaugeH + 2, 0xFF555555);
    drawCenteredString(fontRenderer, "60\u00B0", gaugeLeft + gaugeWidth / 2,
        gaugeY + gaugeH + 2, 0xFF555555);
    drawString(fontRenderer, "120\u00B0", gaugeRight - 20, gaugeY + gaugeH + 2, 0xFF555555);

    // Legend below gauge
    int legendY = gaugeY + gaugeH + 14;
    drawCenteredString(fontRenderer, "\u00A79\u25A0\u00A7r Too Cold   " +
        "\u00A7a\u25A0\u00A7r Comfort   \u00A7c\u25A0\u00A7r Too Hot", cx, legendY, 0xFFAAAAAA);

    // === Comfort Range Controls ===
    int minY = guiTop + 90;
    int maxY = guiTop + 112;

    int valueCenterX = guiLeft + 150; // centered between [-] and [+] buttons, nudged right for °F suffix
    drawString(fontRenderer, "Min temp:", guiLeft + 14, minY + 4, 0xFFAAAAAA);
    drawCenteredString(fontRenderer, lowTarget + "\u00B0F", valueCenterX, minY + 4, COLOR_COLD);

    drawString(fontRenderer, "Max temp:", guiLeft + 14, maxY + 4, 0xFFAAAAAA);
    drawCenteredString(fontRenderer, highTarget + "\u00B0F", valueCenterX, maxY + 4, COLOR_HOT);

    // === Status ===
    int statusY = guiTop + 136;
    boolean calling = thermostat.isCalling();
    boolean hasPower = thermostat.hasSystemPower();
    int unitCount = thermostat.getLinkedUnitCount();
    int ventCount = thermostat.getLinkedVentCount();

    if (unitCount == 0) {
      drawCenteredString(fontRenderer, "\u00A7e\u26A0 No units linked", cx, statusY, 0xFFFFAA00);
    } else if (!hasPower) {
      drawCenteredString(fontRenderer, "\u00A7c\u26A0 NO POWER", cx, statusY, 0xFFFF4444);
    } else if (calling) {
      int efficiency = thermostat.getSystemEfficiencyPercent();
      int mode = thermostat.getCallingMode();
      if (mode == 1) {
        drawCenteredString(fontRenderer, "\u25CF Heating (" + efficiency + "%)", cx, statusY, COLOR_HOT);
      } else {
        drawCenteredString(fontRenderer, "\u25CF Cooling (" + efficiency + "%)", cx, statusY, COLOR_COLD);
      }
    } else {
      drawCenteredString(fontRenderer, "\u25CF Comfortable", cx, statusY, COLOR_COMFORT);
    }

    // System info line
    int infoY = statusY + 12;
    int poweredCount = thermostat.getPoweredUnitCount();
    String info = poweredCount + "/" + unitCount + " units powered | " + ventCount + " vents";
    drawCenteredString(fontRenderer, info, cx, infoY, 0xFF777777);

    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    int low = thermostat.getTargetTempLow();
    int high = thermostat.getTargetTempHigh();

    switch (button.id) {
      case BTN_LOW_MINUS:
        low = Math.max(0, low - TEMP_STEP);
        break;
      case BTN_LOW_PLUS:
        low = Math.min(high - TEMP_STEP, low + TEMP_STEP);
        break;
      case BTN_HIGH_MINUS:
        high = Math.max(low + TEMP_STEP, high - TEMP_STEP);
        break;
      case BTN_HIGH_PLUS:
        high = Math.min(120, high + TEMP_STEP);
        break;
      case BTN_CLOSE:
        mc.displayGuiScreen(null);
        return;
      default:
        return;
    }

    CsmNetwork.sendToServer(new HvacThermostatConfigPacket(blockPos, low, high));
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}
