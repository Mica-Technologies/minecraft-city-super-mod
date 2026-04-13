package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalControllerCircuit;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalControllerCircuits;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Circuit management page for the visual signal controller GUI. Shows a summary of each
 * circuit's linked devices by type, with options to clear individual circuits.
 */
@SideOnly(Side.CLIENT)
public class SignalControllerCircuitsGui extends GuiScreen {

  private static final int BTN_BACK = 200;
  private static final int BTN_CLEAR_BASE = 300;

  private static final int GUI_WIDTH = 370;
  private static final int GUI_HEIGHT = 250;
  private static final int ROW_HEIGHT = 12;
  private static final int HEADER_HEIGHT = 22;

  private static final int COLOR_HEADER = 0xFFFFFFFF;
  private static final int COLOR_LABEL = 0xFFCCCCCC;
  private static final int COLOR_COUNT = 0xFF88CC88;
  private static final int COLOR_ZERO = 0xFF666666;
  private static final int COLOR_CIRCUIT_HDR = 0xFFFFCC00;

  private final TileEntityTrafficSignalController controller;
  private final BlockPos blockPos;

  private int guiLeft;
  private int guiTop;
  private int scrollOffset = 0;

  public SignalControllerCircuitsGui(TileEntityTrafficSignalController controller) {
    this.controller = controller;
    this.blockPos = controller.getPos();
  }

  @Override
  public void initGui() {
    buttonList.clear();
    guiLeft = (width - GUI_WIDTH) / 2;
    guiTop = (height - GUI_HEIGHT) / 2;

    // Back button
    buttonList.add(new GuiButton(BTN_BACK, guiLeft + 6, guiTop + 6, 50, 14, "< Back"));

    // Clear buttons for each circuit (will be positioned in drawScreen)
    TrafficSignalControllerCircuits circuits = controller.getCircuits();
    for (int i = 0; i < circuits.getCircuitCount(); i++) {
      GuiButton clearBtn = new GuiButton(BTN_CLEAR_BASE + i, 0, 0, 36, 12, "Clear");
      clearBtn.visible = false;
      buttonList.add(clearBtn);
    }
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    // Background panel
    drawRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xCC000000);
    drawRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + 1, 0xFF444444);
    drawRect(guiLeft, guiTop + GUI_HEIGHT - 1, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xFF444444);
    drawRect(guiLeft, guiTop, guiLeft + 1, guiTop + GUI_HEIGHT, 0xFF444444);
    drawRect(guiLeft + GUI_WIDTH - 1, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xFF444444);

    // Title
    drawCenteredString(fontRenderer, "Circuit Management", guiLeft + GUI_WIDTH / 2, guiTop + 9, COLOR_HEADER);

    TrafficSignalControllerCircuits circuits = controller.getCircuits();
    int circuitCount = circuits.getCircuitCount();

    if (circuitCount == 0) {
      drawCenteredString(fontRenderer, "No circuits linked",
          guiLeft + GUI_WIDTH / 2, guiTop + GUI_HEIGHT / 2 - 4, COLOR_ZERO);
      super.drawScreen(mouseX, mouseY, partialTicks);
      return;
    }

    // Content area
    int contentTop = guiTop + HEADER_HEIGHT + 4;
    int contentBottom = guiTop + GUI_HEIGHT - 20;
    int contentLeft = guiLeft + 8;
    int contentRight = guiLeft + GUI_WIDTH - 8;

    // Draw circuits
    int y = contentTop - scrollOffset;

    for (int i = 0; i < circuitCount; i++) {
      TrafficSignalControllerCircuit circuit = circuits.getCircuit(i);
      int circuitTop = y;

      // Circuit header
      if (y >= contentTop - ROW_HEIGHT && y < contentBottom) {
        drawString(fontRenderer, "Circuit " + (i + 1) + " (" + circuit.getSize() + " devices)",
            contentLeft, y + 1, COLOR_CIRCUIT_HDR);

        // Position clear button
        GuiButton clearBtn = findButton(BTN_CLEAR_BASE + i);
        if (clearBtn != null) {
          clearBtn.x = contentRight - 38;
          clearBtn.y = y;
          clearBtn.visible = circuit.getSize() > 0 && y >= contentTop && y + 12 <= contentBottom;
        }
      }
      y += ROW_HEIGHT + 2;

      // Device type rows (two columns)
      y = drawDeviceRow(y, contentLeft, contentTop, contentBottom,
          "Through", circuit.getThroughSignals().size(),
          "Left", circuit.getLeftSignals().size());
      y = drawDeviceRow(y, contentLeft, contentTop, contentBottom,
          "Right", circuit.getRightSignals().size(),
          "Protected", circuit.getProtectedSignals().size());
      y = drawDeviceRow(y, contentLeft, contentTop, contentBottom,
          "Flsh Left", circuit.getFlashingLeftSignals().size(),
          "Flsh Right", circuit.getFlashingRightSignals().size());
      y = drawDeviceRow(y, contentLeft, contentTop, contentBottom,
          "Ped Signals", circuit.getPedestrianSignals().size(),
          "Ped Beacons", circuit.getPedestrianBeaconSignals().size());
      y = drawDeviceRow(y, contentLeft, contentTop, contentBottom,
          "Ped Access", circuit.getPedestrianAccessorySignals().size(),
          "Beacons", circuit.getBeaconSignals().size());
      y = drawDeviceRow(y, contentLeft, contentTop, contentBottom,
          "Sensors", circuit.getSensors().size(),
          null, 0);

      // Separator line
      if (y >= contentTop && y < contentBottom && i < circuitCount - 1) {
        drawRect(contentLeft, y + 1, contentRight, y + 2, 0xFF333333);
      }
      y += 6;
    }

    // Hide clear buttons for circuits that no longer exist
    for (GuiButton btn : buttonList) {
      if (btn.id >= BTN_CLEAR_BASE + circuitCount) {
        btn.visible = false;
      }
    }

    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  private int drawDeviceRow(int y, int left, int contentTop, int contentBottom,
      String label1, int count1, String label2, int count2) {
    if (y >= contentTop - ROW_HEIGHT && y < contentBottom) {
      // Column 1
      drawString(fontRenderer, label1 + ":", left + 8, y + 1, COLOR_LABEL);
      drawString(fontRenderer, String.valueOf(count1), left + 80, y + 1,
          count1 > 0 ? COLOR_COUNT : COLOR_ZERO);
      // Column 2
      if (label2 != null) {
        drawString(fontRenderer, label2 + ":", left + 170, y + 1, COLOR_LABEL);
        drawString(fontRenderer, String.valueOf(count2), left + 250, y + 1,
            count2 > 0 ? COLOR_COUNT : COLOR_ZERO);
      }
    }
    return y + ROW_HEIGHT;
  }

  private GuiButton findButton(int id) {
    for (GuiButton btn : buttonList) {
      if (btn.id == id) return btn;
    }
    return null;
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    if (button.id == BTN_BACK) {
      mc.displayGuiScreen(new SignalControllerVisualGui(controller));
    } else if (button.id >= BTN_CLEAR_BASE) {
      int circuitIndex = button.id - BTN_CLEAR_BASE;
      CsmNetwork.sendToServer(new SignalControllerSetValuePacket(blockPos, "clearCircuit", circuitIndex));
    }
  }

  @Override
  public void handleMouseInput() throws IOException {
    super.handleMouseInput();
    int scroll = org.lwjgl.input.Mouse.getEventDWheel();
    if (scroll != 0) {
      scrollOffset -= Integer.signum(scroll) * ROW_HEIGHT * 3;
      if (scrollOffset < 0) scrollOffset = 0;
    }
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}
