package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalSectionInfo;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Configuration GUI for traffic signal heads. Supports two pages:
 * <ul>
 *   <li><b>All Sections</b> — the original simple page. Buttons cycle properties uniformly
 *   across every section at once; best for standard signals.</li>
 *   <li><b>Per Section</b> — lets users customize individual sections for "frankenstein"
 *   signals (e.g. one section with LED bulbs, another with incandescent; a red bulb forced
 *   to "failed" while others stay healthy). Arrow buttons page between sections.</li>
 * </ul>
 * Both pages read live from the client-side tile entity every frame, so changes sync
 * automatically after the server processes each packet.
 */
@SideOnly(Side.CLIENT)
public class SignalHeadConfigGui extends GuiScreen {

  private static final int BUTTON_WIDTH = 160;
  private static final int BUTTON_HEIGHT = 20;
  private static final int ROW_SPACING = 22;
  private static final int COLUMN_GAP = 6;

  // Non-action button IDs — kept well clear of the action ordinal ranges below.
  private static final int CLOSE_BUTTON_ID = 1000;
  private static final int MODE_TOGGLE_ID = 1001;
  private static final int SECTION_PREV_ID = 1002;
  private static final int SECTION_NEXT_ID = 1003;

  // Per-section action button IDs are offset so they can't collide with whole-head ordinals.
  private static final int PER_SECTION_ID_OFFSET = 200;

  private static final String[] ALL_LABELS = {
      "Body Color",
      "Door Color",
      "Visor Color",
      "Visor Type",
      "Body Tilt",
      "Bulb Style",
      "Bulb Type",
      "Alternate Flash",
      "Bulb Aging"
  };

  private static final String[] SECTION_LABELS = {
      "Body Color",
      "Door Color",
      "Visor Color",
      "Visor Type",
      "Bulb Style",
      "Bulb Type",
      "Bulb State"
  };

  private enum Mode {
    ALL_SECTIONS("All Sections"),
    PER_SECTION("Per Section");

    final String label;

    Mode(String label) {
      this.label = label;
    }
  }

  private final TileEntityTrafficSignalHead tileEntity;
  private final BlockPos blockPos;

  private Mode mode = Mode.ALL_SECTIONS;
  private int selectedSection = 0;

  public SignalHeadConfigGui(TileEntityTrafficSignalHead tileEntity) {
    this.tileEntity = tileEntity;
    this.blockPos = tileEntity.getPos();
  }

  @Override
  public void initGui() {
    buttonList.clear();

    int sectionCount = Math.max(1, tileEntity.getSectionCount());
    if (selectedSection >= sectionCount) {
      selectedSection = 0;
    }

    String[] labels = activeLabels();
    int totalWidth = BUTTON_WIDTH * 2 + COLUMN_GAP;
    int leftX = width / 2 - totalWidth / 2;
    int rightX = leftX + BUTTON_WIDTH + COLUMN_GAP;
    int rows = (labels.length + 1) / 2;
    // Reserve one row above the property buttons for the mode toggle and (in per-section mode)
    // the section selector, plus one below for the close button.
    int headerRows = mode == Mode.PER_SECTION ? 2 : 1;
    int topY = height / 2 - (rows * ROW_SPACING + ROW_SPACING * (headerRows + 1)) / 2;

    // Header row 1: mode toggle (always visible), centered.
    buttonList.add(new GuiButton(MODE_TOGGLE_ID, width / 2 - BUTTON_WIDTH / 2, topY,
        BUTTON_WIDTH, BUTTON_HEIGHT, ""));

    // Header row 2: section selector (per-section mode only) — ◀ "Section N of M" ▶.
    int propertyStartY = topY + ROW_SPACING;
    if (mode == Mode.PER_SECTION) {
      int arrowWidth = 24;
      int selectorWidth = BUTTON_WIDTH;
      int selectorX = width / 2 - selectorWidth / 2;
      buttonList.add(new GuiButton(SECTION_PREV_ID, selectorX - arrowWidth - 4, propertyStartY,
          arrowWidth, BUTTON_HEIGHT, "<"));
      // The "label" in the middle is rendered by drawScreen; a disabled button keeps the
      // centered layout without adding click behavior.
      GuiButton sectionLabel = new GuiButton(-1, selectorX, propertyStartY, selectorWidth,
          BUTTON_HEIGHT, sectionSelectorLabel(sectionCount));
      sectionLabel.enabled = false;
      buttonList.add(sectionLabel);
      buttonList.add(new GuiButton(SECTION_NEXT_ID, selectorX + selectorWidth + 4,
          propertyStartY, arrowWidth, BUTTON_HEIGHT, ">"));
      propertyStartY += ROW_SPACING;
    }

    // Property grid.
    for (int i = 0; i < labels.length; i++) {
      int col = i % 2;
      int row = i / 2;
      int x = col == 0 ? leftX : rightX;
      int y = propertyStartY + row * ROW_SPACING;
      int id = mode == Mode.ALL_SECTIONS ? i : PER_SECTION_ID_OFFSET + i;
      buttonList.add(new GuiButton(id, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, ""));
    }

    buttonList.add(new GuiButton(CLOSE_BUTTON_ID, width / 2 - BUTTON_WIDTH / 2,
        propertyStartY + rows * ROW_SPACING + 4, BUTTON_WIDTH, BUTTON_HEIGHT, "Close"));
  }

  private String[] activeLabels() {
    return mode == Mode.ALL_SECTIONS ? ALL_LABELS : SECTION_LABELS;
  }

  private String sectionSelectorLabel(int sectionCount) {
    return "Section " + (selectedSection + 1) + " of " + sectionCount;
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    int sectionCount = Math.max(1, tileEntity.getSectionCount());
    TrafficSignalSectionInfo[] infos = tileEntity.getSectionInfos();

    // Refresh live-text buttons from the TE. Iterate over the button list rather than assuming
    // indexes: the button order depends on mode, and rebuilding that lookup here would just
    // duplicate initGui's layout.
    for (GuiButton button : buttonList) {
      if (button.id == MODE_TOGGLE_ID) {
        button.displayString = "Mode: " + mode.label;
      } else if (button.id == CLOSE_BUTTON_ID || button.id == SECTION_PREV_ID
          || button.id == SECTION_NEXT_ID || button.id < 0) {
        // Static labels — leave them alone.
        continue;
      } else if (mode == Mode.ALL_SECTIONS && button.id < ALL_LABELS.length) {
        button.displayString =
            ALL_LABELS[button.id] + ": " + getAllSectionsValue(button.id, infos);
      } else if (mode == Mode.PER_SECTION && button.id >= PER_SECTION_ID_OFFSET) {
        int localOrdinal = button.id - PER_SECTION_ID_OFFSET;
        if (localOrdinal < SECTION_LABELS.length) {
          button.displayString = SECTION_LABELS[localOrdinal] + ": "
              + getPerSectionValue(localOrdinal, infos);
        }
      }
    }

    // Keep the section-selector label fresh (section count may change if the block is somehow
    // swapped while the GUI is open — defensive).
    for (GuiButton button : buttonList) {
      if (button.id == -1) {
        button.displayString = sectionSelectorLabel(sectionCount);
      }
    }

    String[] labels = activeLabels();
    int rows = (labels.length + 1) / 2;
    int headerRows = mode == Mode.PER_SECTION ? 2 : 1;
    int topY = height / 2 - (rows * ROW_SPACING + ROW_SPACING * (headerRows + 1)) / 2;
    drawCenteredString(fontRenderer, "Signal Head Configuration",
        width / 2, topY - 14, 0xFFFFFF);

    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  private String getAllSectionsValue(int actionOrdinal, TrafficSignalSectionInfo[] infos) {
    if (actionOrdinal >= SignalHeadConfigAction.values().length) return "N/A";
    if (infos == null || infos.length == 0) return "N/A";

    switch (SignalHeadConfigAction.values()[actionOrdinal]) {
      case CYCLE_BODY_COLOR:
        return infos[0].getBodyColor().getFriendlyName();
      case CYCLE_DOOR_COLOR:
        return infos[0].getDoorColor().getFriendlyName();
      case CYCLE_VISOR_COLOR:
        return infos[0].getVisorColor().getFriendlyName();
      case CYCLE_VISOR_TYPE:
        return infos[0].getVisorType().getFriendlyName();
      case CYCLE_BODY_TILT:
        return tileEntity.getBodyTilt().getFriendlyName();
      case CYCLE_BULB_STYLE:
        return infos[0].getBulbStyle().getFriendlyName();
      case CYCLE_BULB_TYPE:
        return infos[0].getBulbType().getFriendlyName();
      case TOGGLE_ALTERNATE_FLASH:
        return tileEntity.isAlternateFlash() ? "ON (wig-wag B)" : "OFF (normal)";
      case TOGGLE_AGING:
        return tileEntity.isAgingEnabled() ? "ON" : "OFF";
      default:
        return "N/A";
    }
  }

  private String getPerSectionValue(int actionOrdinal, TrafficSignalSectionInfo[] infos) {
    if (actionOrdinal >= SignalHeadSectionConfigAction.values().length) return "N/A";
    if (infos == null || selectedSection >= infos.length) return "N/A";

    TrafficSignalSectionInfo info = infos[selectedSection];
    switch (SignalHeadSectionConfigAction.values()[actionOrdinal]) {
      case CYCLE_BODY_COLOR:
        return info.getBodyColor().getFriendlyName();
      case CYCLE_DOOR_COLOR:
        return info.getDoorColor().getFriendlyName();
      case CYCLE_VISOR_COLOR:
        return info.getVisorColor().getFriendlyName();
      case CYCLE_VISOR_TYPE:
        return info.getVisorType().getFriendlyName();
      case CYCLE_BULB_STYLE:
        return info.getBulbStyle().getFriendlyName();
      case CYCLE_BULB_TYPE:
        return info.getBulbType().getFriendlyName();
      case CYCLE_BULB_AGING_STATE:
        return formatAgingState(tileEntity.getBulbAgingState(selectedSection));
      default:
        return "N/A";
    }
  }

  private static String formatAgingState(int state) {
    switch (state) {
      case TileEntityTrafficSignalHead.AGING_FAILING:
        return "Failing";
      case TileEntityTrafficSignalHead.AGING_DEAD:
        return "Failed";
      case TileEntityTrafficSignalHead.AGING_HEALTHY:
      default:
        return "Normal";
    }
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    if (button.id == CLOSE_BUTTON_ID) {
      mc.displayGuiScreen(null);
      return;
    }
    if (button.id == MODE_TOGGLE_ID) {
      mode = mode == Mode.ALL_SECTIONS ? Mode.PER_SECTION : Mode.ALL_SECTIONS;
      initGui();
      return;
    }
    if (button.id == SECTION_PREV_ID) {
      int count = Math.max(1, tileEntity.getSectionCount());
      selectedSection = (selectedSection - 1 + count) % count;
      return;
    }
    if (button.id == SECTION_NEXT_ID) {
      int count = Math.max(1, tileEntity.getSectionCount());
      selectedSection = (selectedSection + 1) % count;
      return;
    }

    if (mode == Mode.ALL_SECTIONS && button.id >= 0
        && button.id < SignalHeadConfigAction.values().length) {
      CsmNetwork.sendToServer(new SignalHeadConfigPacket(blockPos, button.id));
      return;
    }

    if (mode == Mode.PER_SECTION && button.id >= PER_SECTION_ID_OFFSET) {
      int localOrdinal = button.id - PER_SECTION_ID_OFFSET;
      if (localOrdinal < SignalHeadSectionConfigAction.values().length) {
        CsmNetwork.sendToServer(new SignalHeadSectionConfigPacket(blockPos, selectedSection,
            localOrdinal));
      }
    }
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}
