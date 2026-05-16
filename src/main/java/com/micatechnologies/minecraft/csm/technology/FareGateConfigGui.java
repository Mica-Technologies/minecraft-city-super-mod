package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.math.BlockPos;

/**
 * Operator-only GUI for selecting a {@link FareGateOpMode} on a fare gate.
 * Opened by sneak+right-clicking the gate with an empty hand. Sends the selected mode
 * back to the server via {@link FareGateOpModePacket}.
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class FareGateConfigGui extends GuiScreen {

  private static final int BUTTON_ID_BASE = 100;
  private static final int BUTTON_ID_CLOSE = 0;

  private static final int COLOR_PANEL_BG = 0xFF263043;
  private static final int COLOR_HEADER_BG = 0xFF1F2A3D;
  private static final int COLOR_HEADER_TEXT = 0xFFE2EBFF;
  private static final int COLOR_INFO_DIM = 0xFFB8C0D0;

  private final BlockPos gatePos;
  private final FareGateOpMode currentMode;

  public FareGateConfigGui(TileEntityFareGate tile) {
    this.gatePos = tile.getPos();
    this.currentMode = tile.getOpMode();
  }

  @Override
  public void initGui() {
    super.initGui();
    ScaledResolution sr = new ScaledResolution(this.mc);
    int screenW = sr.getScaledWidth();
    int screenH = sr.getScaledHeight();

    int panelW = 240;
    int panelH = 160;
    int panelX = (screenW - panelW) / 2;
    int panelY = (screenH - panelH) / 2;

    this.buttonList.clear();

    FareGateOpMode[] modes = FareGateOpMode.values();
    int btnY = panelY + 44;
    int btnH = 20;
    int btnGap = 4;
    for (int i = 0; i < modes.length; i++) {
      String label = modes[i].label;
      if (modes[i] == currentMode) {
        label = "§a✓ " + label;
      }
      this.buttonList.add(new GuiButton(BUTTON_ID_BASE + i,
          panelX + 16, btnY, panelW - 32, btnH, label));
      btnY += btnH + btnGap;
    }

    this.buttonList.add(new GuiButton(BUTTON_ID_CLOSE,
        panelX + (panelW - 80) / 2, panelY + panelH - 26, 80, 20, "Close"));
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    ScaledResolution sr = new ScaledResolution(this.mc);
    int screenW = sr.getScaledWidth();
    int screenH = sr.getScaledHeight();
    int panelW = 240;
    int panelH = 160;
    int panelX = (screenW - panelW) / 2;
    int panelY = (screenH - panelH) / 2;

    drawRect(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_PANEL_BG);
    drawRect(panelX, panelY, panelX + panelW, panelY + 32, COLOR_HEADER_BG);

    fontRenderer.drawString("Fare Gate — Operator Config",
        panelX + 10, panelY + 6, COLOR_HEADER_TEXT);
    fontRenderer.drawString("Select override mode:",
        panelX + 10, panelY + 20, COLOR_INFO_DIM);

    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  @Override
  @ParametersAreNonnullByDefault
  protected void actionPerformed(GuiButton button) {
    if (button.id == BUTTON_ID_CLOSE) {
      this.mc.displayGuiScreen(null);
      return;
    }
    int idx = button.id - BUTTON_ID_BASE;
    FareGateOpMode mode = FareGateOpMode.fromOrdinal(idx);
    CsmNetwork.sendToServer(new FareGateOpModePacket(gatePos, mode));
    this.mc.displayGuiScreen(null);
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}
