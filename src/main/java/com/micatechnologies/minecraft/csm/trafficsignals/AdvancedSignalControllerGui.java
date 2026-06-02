package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalCoordinationMode;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalPhaseMovement;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalPreempt;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalPreemptType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalProgrammedPhase;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalProgrammedPhasePlan;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalRecallMode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Keyboard;

/**
 * "CSM ASC-3" — a NEMA controller-style front-panel GUI for programming a traffic signal
 * controller's ADVANCED (ring-and-barrier) mode. Amber-on-black LCD with a rubber keypad and
 * status LEDs, navigated like a real cabinet controller. Edits are sent to the server via
 * {@link AdvancedSignalControllerConfigPacket}; the controller (client-synced) is read back for
 * display.
 *
 * <p>Screens: STATUS (program overview + ring diagram), TIMING (per-phase intervals), MAP
 * (phase&rarr;circuit/movement/recall), COORD (cycle/offset/splits), PREEMPT (preempt table).
 *
 * @author Mica Technologies
 * @since 2026.6
 */
public class AdvancedSignalControllerGui extends GuiScreen {

  private enum Screen { STATUS, TIMING, MAP, COORD, PREEMPT }

  // Palette — controller front panel.
  private static final int COLOR_BODY = 0xFF23272B;
  private static final int COLOR_BODY_EDGE = 0xFF3B4046;
  private static final int COLOR_LCD_BG = 0xFF0A0F0A;
  private static final int COLOR_AMBER = 0xFFFFB000;
  private static final int COLOR_AMBER_DIM = 0xFF9A6A12;
  private static final int COLOR_AMBER_HEAD = 0xFFFFD27A;
  private static final int COLOR_SEL = 0xFF5A3A00;
  private static final int COLOR_LED_GREEN = 0xFF3CE03C;
  private static final int COLOR_LED_RED = 0xFFE03C3C;
  private static final int COLOR_LED_OFF = 0xFF333A33;

  // Keypad button ids.
  private static final int BTN_DIGIT_BASE = 200; // 200..209 -> 0..9
  private static final int BTN_DOT = 210;
  private static final int BTN_PLUS = 211;
  private static final int BTN_MINUS = 212;
  private static final int BTN_CLR = 213;
  private static final int BTN_ENT = 214;
  private static final int BTN_UP = 215;
  private static final int BTN_DOWN = 216;
  private static final int BTN_LEFT = 217;
  private static final int BTN_RIGHT = 218;
  private static final int BTN_SCREEN_BASE = 220; // 220..224 -> STATUS..PREEMPT
  private static final int BTN_TEMPLATE = 225;
  private static final int BTN_CLOSE = 226;
  private static final int BTN_PE_ADD = 227;
  private static final int BTN_PE_REMOVE = 228;
  private static final int BTN_PE_PREV = 229;
  private static final int BTN_PE_NEXT = 230;

  private static final int W = 384;
  private static final int H = 244;

  private static final String[] MOVEMENT_ABBR = {"THRU", "LEFT", "PLFT", "RGHT", "PED"};
  private static final String[] RECALL_ABBR = {"NONE", "MIN", "MAX", "PED", "SOFT"};
  private static final String[] TIMING_HEADERS = {"MnG", "Pas", "MxG", "Yel", "Red", "Wlk", "PCl"};
  private static final String[] TIMING_ACTIONS = {"ph.minGreen", "ph.passage", "ph.maxGreen",
      "ph.yellow", "ph.redClear", "ph.walk", "ph.pedClear"};

  private final TileEntityTrafficSignalController controller;
  private final BlockPos blockPos;

  private Screen screen = Screen.STATUS;
  private final List<Cell> cells = new ArrayList<>();
  private int selected = 0;
  private String entry = "";
  private int selectedPreempt = 0;

  private int left;
  private int top;
  private int lcdX;
  private int lcdY;
  private int lcdW;

  public AdvancedSignalControllerGui(TileEntityTrafficSignalController controller) {
    this.controller = controller;
    this.blockPos = controller.getPos();
  }

  /** An editable LCD field: a position, a live value string, and adjust/commit hooks. */
  private static final class Cell {
    final int x;
    final int y;
    final int w;
    final Supplier<String> value;
    final IntConsumer adjust;        // +1 / -1; null if not adjustable
    final DoubleConsumer commitSeconds; // seconds entry; null if non-numeric

    Cell(int x, int y, int w, Supplier<String> value, IntConsumer adjust,
        DoubleConsumer commitSeconds) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.value = value;
      this.adjust = adjust;
      this.commitSeconds = commitSeconds;
    }
  }

  // region: lifecycle

  @Override
  public void initGui() {
    Keyboard.enableRepeatEvents(true);
    left = (width - W) / 2;
    top = (height - H) / 2;
    lcdX = left + 12;
    lcdY = top + 34;
    lcdW = W - 24;
    buildKeypad();
    rebuildCells();
  }

  @Override
  public void onGuiClosed() {
    Keyboard.enableRepeatEvents(false);
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }

  private TrafficSignalProgrammedPhasePlan plan() {
    TrafficSignalProgrammedPhasePlan p = controller.getProgrammedPhasePlan();
    return p != null ? p : TrafficSignalProgrammedPhasePlan.createDefault();
  }

  private void send(String action, int index, long value) {
    CsmNetwork.sendToServer(new AdvancedSignalControllerConfigPacket(blockPos, action, index, value));
  }

  // endregion

  // region: keypad

  private void buildKeypad() {
    buttonList.clear();
    // Screen-select row across the top of the keypad.
    int sx = left + 12;
    int sy = top + H - 78;
    String[] names = {"STATUS", "TIMING", "MAP", "COORD", "PREEMPT"};
    for (int i = 0; i < names.length; i++) {
      buttonList.add(new GuiButton(BTN_SCREEN_BASE + i, sx + i * 62, sy, 58, 14, names[i]));
    }

    // Numeric keypad (left cluster).
    int kx = left + 12;
    int ky = top + H - 58;
    int kw = 24;
    int kh = 16;
    int gap = 2;
    String[][] pad = {{"7", "8", "9"}, {"4", "5", "6"}, {"1", "2", "3"}, {"0", ".", "CLR"}};
    for (int r = 0; r < pad.length; r++) {
      for (int c = 0; c < pad[r].length; c++) {
        String label = pad[r][c];
        int id;
        if (label.equals(".")) {
          id = BTN_DOT;
        } else if (label.equals("CLR")) {
          id = BTN_CLR;
        } else {
          id = BTN_DIGIT_BASE + Integer.parseInt(label);
        }
        buttonList.add(new GuiButton(id, kx + c * (kw + gap), ky + r * (kh + gap), kw, kh, label));
      }
    }

    // Adjust / nav cluster (middle).
    int ax = kx + 3 * (kw + gap) + 10;
    buttonList.add(new GuiButton(BTN_MINUS, ax, ky, kw, kh, "-"));
    buttonList.add(new GuiButton(BTN_UP, ax + (kw + gap), ky, kw, kh, "▲"));
    buttonList.add(new GuiButton(BTN_PLUS, ax + 2 * (kw + gap), ky, kw, kh, "+"));
    buttonList.add(new GuiButton(BTN_LEFT, ax, ky + (kh + gap), kw, kh, "◄"));
    buttonList.add(new GuiButton(BTN_DOWN, ax + (kw + gap), ky + (kh + gap), kw, kh, "▼"));
    buttonList.add(new GuiButton(BTN_RIGHT, ax + 2 * (kw + gap), ky + (kh + gap), kw, kh, "►"));
    buttonList.add(new GuiButton(BTN_ENT, ax, ky + 2 * (kh + gap), kw * 3 + gap * 2, kh, "ENTER"));

    // Right cluster: template, preempt ops, close.
    int rx = ax + 3 * (kw + gap) + 12;
    buttonList.add(new GuiButton(BTN_TEMPLATE, rx, ky, 96, kh, "Load Std 8-Phase"));
    buttonList.add(new GuiButton(BTN_PE_ADD, rx, ky + (kh + gap), 46, kh, "P+"));
    buttonList.add(new GuiButton(BTN_PE_REMOVE, rx + 50, ky + (kh + gap), 46, kh, "P-"));
    buttonList.add(new GuiButton(BTN_PE_PREV, rx, ky + 2 * (kh + gap), 46, kh, "Prev"));
    buttonList.add(new GuiButton(BTN_PE_NEXT, rx + 50, ky + 2 * (kh + gap), 46, kh, "Next"));
    buttonList.add(new GuiButton(BTN_CLOSE, left + W - 52, top + 6, 46, 14, "Close"));
  }

  // endregion

  // region: cells

  private void rebuildCells() {
    cells.clear();
    selected = 0;
    entry = "";
    switch (screen) {
      case TIMING:
        buildTimingCells();
        break;
      case MAP:
        buildMapCells();
        break;
      case COORD:
        buildCoordCells();
        break;
      case PREEMPT:
        buildPreemptCells();
        break;
      case STATUS:
      default:
        break;
    }
  }

  private void buildTimingCells() {
    int rowH = 11;
    int colW = (lcdW - 24) / TIMING_HEADERS.length;
    int y0 = lcdY + 14;
    for (int pn = 1; pn <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; pn++) {
      final int n = pn;
      int y = y0 + (pn - 1) * rowH;
      for (int ci = 0; ci < TIMING_ACTIONS.length; ci++) {
        final int fieldIdx = ci;
        final String action = TIMING_ACTIONS[ci];
        int x = lcdX + 24 + ci * colW;
        cells.add(new Cell(x, y, colW - 2,
            () -> secs(timingValue(plan().getPhase(n), fieldIdx)),
            dir -> send(action, n, timingValue(plan().getPhase(n), fieldIdx) + dir * 10L),
            sec -> send(action, n, Math.round(sec * 20))));
      }
    }
  }

  private void buildMapCells() {
    int rowH = 11;
    int y0 = lcdY + 14;
    int circuitCount = controller.getSignalCircuitCount();
    int[] colX = {lcdX + 26, lcdX + 70, lcdX + 130, lcdX + 200, lcdX + 270};
    for (int pn = 1; pn <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; pn++) {
      final int n = pn;
      int y = y0 + (pn - 1) * rowH;
      // EN
      cells.add(new Cell(colX[0], y, 32,
          () -> plan().getPhase(n).isEnabled() ? "On" : "off",
          dir -> send("ph.enabled", n, plan().getPhase(n).isEnabled() ? 0 : 1), null));
      // Circuit
      cells.add(new Cell(colX[1], y, 50,
          () -> plan().getPhase(n).getCircuitIndex() < 0 ? "--"
              : ("C" + (plan().getPhase(n).getCircuitIndex() + 1)),
          dir -> {
            int c = plan().getPhase(n).getCircuitIndex() + dir;
            if (c < -1) {
              c = circuitCount - 1;
            }
            if (c >= circuitCount) {
              c = -1;
            }
            send("ph.circuit", n, c);
          }, null));
      // Movement
      cells.add(new Cell(colX[2], y, 60,
          () -> MOVEMENT_ABBR[plan().getPhase(n).getMovement().ordinal()],
          dir -> send("ph.movement", n,
              cyc(plan().getPhase(n).getMovement().ordinal(), dir,
                  TrafficSignalPhaseMovement.values().length)), null));
      // Recall
      cells.add(new Cell(colX[3], y, 60,
          () -> RECALL_ABBR[plan().getPhase(n).getRecallMode().ordinal()],
          dir -> send("ph.recall", n,
              cyc(plan().getPhase(n).getRecallMode().ordinal(), dir,
                  TrafficSignalRecallMode.values().length)), null));
      // Ped recall
      cells.add(new Cell(colX[4], y, 40,
          () -> plan().getPhase(n).isPedRecall() ? "PedR" : "no",
          dir -> send("ph.pedRecall", n, plan().getPhase(n).isPedRecall() ? 0 : 1), null));
    }
  }

  private void buildCoordCells() {
    int y = lcdY + 14;
    cells.add(new Cell(lcdX + 70, y, 80,
        () -> plan().getCoordination().getMode().getName(),
        dir -> send("co.mode",
            cyc(plan().getCoordination().getMode().ordinal(), dir,
                TrafficSignalCoordinationMode.values().length), 0), null));
    y += 12;
    cells.add(new Cell(lcdX + 70, y, 50,
        () -> secs(plan().getCoordination().getCycleLength()),
        dir -> send("co.cycle", 0, plan().getCoordination().getCycleLength() + dir * 20L),
        sec -> send("co.cycle", 0, Math.round(sec * 20))));
    y += 12;
    cells.add(new Cell(lcdX + 70, y, 50,
        () -> secs(plan().getCoordination().getOffset()),
        dir -> send("co.offset", 0, plan().getCoordination().getOffset() + dir * 20L),
        sec -> send("co.offset", 0, Math.round(sec * 20))));
    y += 14;
    int rowH = 11;
    for (int pn = 1; pn <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; pn++) {
      final int n = pn;
      int ry = y + (pn - 1) * rowH;
      cells.add(new Cell(lcdX + 70, ry, 50,
          () -> secs(plan().getCoordination().getSplit(n)),
          dir -> send("co.split", n, plan().getCoordination().getSplit(n) + dir * 20L),
          sec -> send("co.split", n, Math.round(sec * 20))));
      cells.add(new Cell(lcdX + 150, ry, 40,
          () -> plan().getCoordination().isCoordinatedPhase(n) ? "COORD" : "-",
          dir -> send("co.coordToggle", 0, n), null));
    }
  }

  private void buildPreemptCells() {
    List<TrafficSignalPreempt> preempts = plan().getPreempts();
    if (preempts.isEmpty()) {
      return;
    }
    if (selectedPreempt >= preempts.size()) {
      selectedPreempt = preempts.size() - 1;
    }
    final int pi = selectedPreempt;
    int circuitCount = controller.getSignalCircuitCount();
    int y = lcdY + 26;
    cells.add(new Cell(lcdX + 80, y, 40,
        () -> preempts.get(pi).isEnabled() ? "On" : "off",
        dir -> send("pe.enabled", pi, preempts.get(pi).isEnabled() ? 0 : 1), null));
    y += 12;
    cells.add(new Cell(lcdX + 80, y, 110,
        () -> preempts.get(pi).getType().getName(),
        dir -> send("pe.type", pi,
            cyc(preempts.get(pi).getType().ordinal(), dir,
                TrafficSignalPreemptType.values().length)), null));
    y += 12;
    cells.add(new Cell(lcdX + 80, y, 50,
        () -> preempts.get(pi).getTriggerCircuitIndex() < 0 ? "--"
            : ("C" + (preempts.get(pi).getTriggerCircuitIndex() + 1)),
        dir -> {
          int c = preempts.get(pi).getTriggerCircuitIndex() + dir;
          if (c < -1) {
            c = circuitCount - 1;
          }
          if (c >= circuitCount) {
            c = -1;
          }
          send("pe.trigCircuit", pi, c);
        }, null));
    y += 12;
    cells.add(new Cell(lcdX + 80, y, 60,
        () -> MOVEMENT_ABBR[preempts.get(pi).getTriggerMovement().ordinal()],
        dir -> send("pe.trigMovement", pi,
            cyc(preempts.get(pi).getTriggerMovement().ordinal(), dir,
                TrafficSignalPhaseMovement.values().length)), null));
    y += 12;
    cells.add(new Cell(lcdX + 80, y, 50,
        () -> secs(preempts.get(pi).getMinDwell()),
        dir -> send("pe.minDwell", pi, preempts.get(pi).getMinDwell() + dir * 20L),
        sec -> send("pe.minDwell", pi, Math.round(sec * 20))));
    // Phase-set toggle rows: TRACK / DWELL / EXIT.
    y += 16;
    addPhaseToggleRow(preempts, pi, y, "pe.trackToggle", p -> contains(p.getTrackClearPhases()));
    y += 12;
    addPhaseToggleRow(preempts, pi, y, "pe.dwellToggle", p -> contains(p.getDwellPhases()));
    y += 12;
    addPhaseToggleRow(preempts, pi, y, "pe.exitToggle", p -> contains(p.getExitPhases()));
  }

  private interface PhaseSet {
    boolean[] of(TrafficSignalPreempt preempt);
  }

  private void addPhaseToggleRow(List<TrafficSignalPreempt> preempts, int pi, int y, String action,
      PhaseSet set) {
    for (int pn = 1; pn <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; pn++) {
      final int n = pn;
      cells.add(new Cell(lcdX + 70 + (pn - 1) * 22, y, 18,
          () -> set.of(preempts.get(pi))[n] ? ("[" + n + "]") : (" " + n + " "),
          dir -> send(action, pi, n), null));
    }
  }

  private static boolean[] contains(int[] arr) {
    boolean[] out = new boolean[TrafficSignalProgrammedPhasePlan.PHASE_COUNT + 1];
    for (int v : arr) {
      if (v >= 0 && v < out.length) {
        out[v] = true;
      }
    }
    return out;
  }

  private static int cyc(int ordinal, int dir, int len) {
    return (ordinal + dir + len) % len;
  }

  private long timingValue(TrafficSignalProgrammedPhase p, int fieldIdx) {
    if (p == null) {
      return 0;
    }
    switch (fieldIdx) {
      case 0: return p.getMinGreen();
      case 1: return p.getPassage();
      case 2: return p.getMaxGreen();
      case 3: return p.getYellow();
      case 4: return p.getRedClear();
      case 5: return p.getWalk();
      case 6: return p.getPedClear();
      default: return 0;
    }
  }

  private static String secs(long ticks) {
    double s = ticks / 20.0;
    return (s == (long) s) ? String.valueOf((long) s) : String.format("%.1f", s);
  }

  // endregion

  // region: input

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    int id = button.id;
    if (id >= BTN_DIGIT_BASE && id <= BTN_DIGIT_BASE + 9) {
      typeChar((char) ('0' + (id - BTN_DIGIT_BASE)));
    } else if (id == BTN_DOT) {
      typeChar('.');
    } else if (id == BTN_CLR) {
      entry = "";
    } else if (id == BTN_ENT) {
      commitOrCycle();
    } else if (id == BTN_PLUS) {
      adjustSelected(1);
    } else if (id == BTN_MINUS) {
      adjustSelected(-1);
    } else if (id == BTN_UP || id == BTN_LEFT) {
      moveSelection(-1);
    } else if (id == BTN_DOWN || id == BTN_RIGHT) {
      moveSelection(1);
    } else if (id >= BTN_SCREEN_BASE && id <= BTN_SCREEN_BASE + 4) {
      screen = Screen.values()[id - BTN_SCREEN_BASE];
      rebuildCells();
    } else if (id == BTN_TEMPLATE) {
      send("loadTemplate", 0, 0);
    } else if (id == BTN_PE_ADD) {
      send("pe.add", 0, 0);
    } else if (id == BTN_PE_REMOVE) {
      send("pe.remove", selectedPreempt, 0);
    } else if (id == BTN_PE_PREV) {
      selectedPreempt = Math.max(0, selectedPreempt - 1);
      rebuildCells();
    } else if (id == BTN_PE_NEXT) {
      selectedPreempt++;
      rebuildCells();
    } else if (id == BTN_CLOSE) {
      mc.displayGuiScreen(null);
    }
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    switch (keyCode) {
      case Keyboard.KEY_ESCAPE:
        mc.displayGuiScreen(null);
        return;
      case Keyboard.KEY_UP:
      case Keyboard.KEY_LEFT:
        moveSelection(-1);
        return;
      case Keyboard.KEY_DOWN:
      case Keyboard.KEY_RIGHT:
        moveSelection(1);
        return;
      case Keyboard.KEY_RETURN:
      case Keyboard.KEY_NUMPADENTER:
        commitOrCycle();
        return;
      case Keyboard.KEY_ADD:
      case Keyboard.KEY_EQUALS:
        adjustSelected(1);
        return;
      case Keyboard.KEY_SUBTRACT:
      case Keyboard.KEY_MINUS:
        adjustSelected(-1);
        return;
      case Keyboard.KEY_BACK:
        if (!entry.isEmpty()) {
          entry = entry.substring(0, entry.length() - 1);
        }
        return;
      default:
        if ((typedChar >= '0' && typedChar <= '9') || typedChar == '.') {
          typeChar(typedChar);
        }
    }
  }

  private void typeChar(char c) {
    Cell cell = selectedCell();
    if (cell != null && cell.commitSeconds != null && entry.length() < 6) {
      entry += c;
    }
  }

  private void commitOrCycle() {
    Cell cell = selectedCell();
    if (cell == null) {
      return;
    }
    if (cell.commitSeconds != null && !entry.isEmpty()) {
      try {
        cell.commitSeconds.accept(Double.parseDouble(entry));
      } catch (NumberFormatException ignored) {
        // ignore malformed entry
      }
      entry = "";
    } else if (cell.adjust != null) {
      cell.adjust.accept(1); // ENTER cycles enums / toggles bools
    }
  }

  private void adjustSelected(int dir) {
    Cell cell = selectedCell();
    if (cell != null && cell.adjust != null) {
      cell.adjust.accept(dir);
    }
  }

  private void moveSelection(int delta) {
    if (cells.isEmpty()) {
      return;
    }
    selected = (selected + delta + cells.size()) % cells.size();
    entry = "";
  }

  private Cell selectedCell() {
    return (selected >= 0 && selected < cells.size()) ? cells.get(selected) : null;
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    // Click a cell to select it.
    for (int i = 0; i < cells.size(); i++) {
      Cell c = cells.get(i);
      if (mouseX >= c.x - 1 && mouseX <= c.x + c.w && mouseY >= c.y - 1 && mouseY <= c.y + 9) {
        selected = i;
        entry = "";
        break;
      }
    }
  }

  // endregion

  // region: rendering

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    // Bezel.
    drawRect(left - 2, top - 2, left + W + 2, top + H + 2, COLOR_BODY_EDGE);
    drawRect(left, top, left + W, top + H, COLOR_BODY);
    // Header.
    fontRenderer.drawString("CSM ASC-3  ·  Advanced Signal Controller", left + 10, top + 8,
        COLOR_AMBER_HEAD);
    drawLeds();
    // LCD.
    int lcdBottom = top + H - 82;
    drawRect(lcdX - 4, lcdY - 6, lcdX + lcdW + 4, lcdBottom, COLOR_LCD_BG);

    switch (screen) {
      case STATUS:
        drawStatus();
        break;
      case TIMING:
        drawTiming();
        break;
      case MAP:
        drawMap();
        break;
      case COORD:
        drawCoord();
        break;
      case PREEMPT:
        drawPreempt();
        break;
      default:
        break;
    }

    drawCells();
    drawHint();
    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  private void drawLeds() {
    int y = top + 8;
    int x = left + W - 150;
    boolean fault = controller.isInFaultState();
    String modeName = controller.getModeName();
    boolean advanced = "Advanced".equals(modeName);
    boolean coord = controller.getProgrammedPhasePlan() != null
        && controller.getProgrammedPhasePlan().getCoordination().isCoordinated();
    led(x, y, advanced && !fault ? COLOR_LED_GREEN : COLOR_LED_OFF, "RUN");
    led(x + 46, y, coord ? COLOR_LED_GREEN : COLOR_LED_OFF, "COORD");
    led(x + 100, y, fault ? COLOR_LED_RED : COLOR_LED_OFF, "FAULT");
  }

  private void led(int x, int y, int color, String label) {
    drawRect(x, y + 1, x + 6, y + 7, color);
    fontRenderer.drawString(label, x + 9, y, COLOR_AMBER_DIM);
  }

  private void drawStatus() {
    TrafficSignalProgrammedPhasePlan plan = plan();
    int y = lcdY;
    String modeLine = controller.getModeName()
        + (controller.getProgrammedPhasePlan() == null ? "  (not configured)" : "");
    fontRenderer.drawString("Mode: " + modeLine, lcdX, y, COLOR_AMBER);
    y += 12;
    if (controller.isInFaultState()) {
      fontRenderer.drawString("FAULT: " + trim(controller.getCurrentFaultMessage(), lcdW),
          lcdX, y, COLOR_LED_RED);
      y += 12;
    }
    // Ring diagram.
    fontRenderer.drawString("Ring/Barrier:", lcdX, y, COLOR_AMBER_DIM);
    y += 11;
    drawRingRow(plan, 1, lcdX, y);
    y += 11;
    drawRingRow(plan, 2, lcdX, y);
    y += 14;
    String coord = plan.getCoordination().isCoordinated()
        ? ("COORD cycle " + secs(plan.getCoordination().getCycleLength()) + "s offset "
            + secs(plan.getCoordination().getOffset()) + "s")
        : "FREE (fully actuated)";
    fontRenderer.drawString(coord, lcdX, y, COLOR_AMBER);
    y += 11;
    fontRenderer.drawString("Preempts: " + plan.getPreempts().size(), lcdX, y, COLOR_AMBER);
    y += 14;
    fontRenderer.drawString("Use the keys below to program. Load Std 8-Phase auto-assigns by approach.",
        lcdX, y, COLOR_AMBER_DIM);
  }

  private void drawRingRow(TrafficSignalProgrammedPhasePlan plan, int ring, int x, int y) {
    fontRenderer.drawString("R" + ring, x, y, COLOR_AMBER_DIM);
    int cx = x + 22;
    for (int n : plan.getRingSequence(ring)) {
      TrafficSignalProgrammedPhase p = plan.getPhase(n);
      String box = p != null && p.isActive()
          ? (n + ":" + MOVEMENT_ABBR[p.getMovement().ordinal()])
          : (n + ":--");
      int color = (p != null && p.getBarrier() == 0) ? COLOR_AMBER : COLOR_AMBER_DIM;
      fontRenderer.drawString(box, cx, y, color);
      cx += 44;
    }
  }

  private void drawTiming() {
    fontRenderer.drawString("PHASE TIMING (seconds)", lcdX, lcdY, COLOR_AMBER_HEAD);
    int colW = (lcdW - 24) / TIMING_HEADERS.length;
    for (int i = 0; i < TIMING_HEADERS.length; i++) {
      fontRenderer.drawString(TIMING_HEADERS[i], lcdX + 24 + i * colW, lcdY + 12, COLOR_AMBER_DIM);
    }
    int rowH = 11;
    for (int pn = 1; pn <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; pn++) {
      int y = lcdY + 14 + (pn - 1) * rowH;
      TrafficSignalProgrammedPhase p = plan().getPhase(pn);
      int color = p != null && p.isActive() ? COLOR_AMBER : COLOR_AMBER_DIM;
      fontRenderer.drawString("φ" + pn, lcdX, y, color);
    }
  }

  private void drawMap() {
    fontRenderer.drawString("PHASE MAP", lcdX, lcdY, COLOR_AMBER_HEAD);
    fontRenderer.drawString("EN", lcdX + 26, lcdY + 12, COLOR_AMBER_DIM);
    fontRenderer.drawString("CKT", lcdX + 70, lcdY + 12, COLOR_AMBER_DIM);
    fontRenderer.drawString("MOVE", lcdX + 130, lcdY + 12, COLOR_AMBER_DIM);
    fontRenderer.drawString("RECALL", lcdX + 200, lcdY + 12, COLOR_AMBER_DIM);
    fontRenderer.drawString("PED", lcdX + 270, lcdY + 12, COLOR_AMBER_DIM);
    int rowH = 11;
    for (int pn = 1; pn <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; pn++) {
      int y = lcdY + 14 + (pn - 1) * rowH;
      fontRenderer.drawString("φ" + pn, lcdX, y, COLOR_AMBER);
    }
  }

  private void drawCoord() {
    fontRenderer.drawString("COORDINATION", lcdX, lcdY, COLOR_AMBER_HEAD);
    fontRenderer.drawString("Mode:", lcdX, lcdY + 14, COLOR_AMBER_DIM);
    fontRenderer.drawString("Cycle:", lcdX, lcdY + 26, COLOR_AMBER_DIM);
    fontRenderer.drawString("Offset:", lcdX, lcdY + 38, COLOR_AMBER_DIM);
    fontRenderer.drawString("Splits / coordinated phases:", lcdX, lcdY + 50, COLOR_AMBER_DIM);
    int rowH = 11;
    for (int pn = 1; pn <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; pn++) {
      int y = lcdY + 52 + (pn - 1) * rowH;
      fontRenderer.drawString("φ" + pn, lcdX + 40, y, COLOR_AMBER);
    }
  }

  private void drawPreempt() {
    List<TrafficSignalPreempt> preempts = plan().getPreempts();
    fontRenderer.drawString("PREEMPT  (" + preempts.size() + " configured)", lcdX, lcdY,
        COLOR_AMBER_HEAD);
    if (preempts.isEmpty()) {
      fontRenderer.drawString("No preempts. Press P+ to add one.", lcdX, lcdY + 16, COLOR_AMBER);
      return;
    }
    fontRenderer.drawString("Selected: #" + (selectedPreempt + 1) + " of " + preempts.size()
        + "  (Prev/Next)", lcdX, lcdY + 12, COLOR_AMBER_DIM);
    int y = lcdY + 26;
    fontRenderer.drawString("Enabled:", lcdX, y, COLOR_AMBER_DIM);
    y += 12;
    fontRenderer.drawString("Type:", lcdX, y, COLOR_AMBER_DIM);
    y += 12;
    fontRenderer.drawString("Trig CKT:", lcdX, y, COLOR_AMBER_DIM);
    y += 12;
    fontRenderer.drawString("Trig MOV:", lcdX, y, COLOR_AMBER_DIM);
    y += 12;
    fontRenderer.drawString("Min Dwell:", lcdX, y, COLOR_AMBER_DIM);
    y += 16;
    fontRenderer.drawString("TRACK", lcdX, y, COLOR_AMBER_DIM);
    y += 12;
    fontRenderer.drawString("DWELL", lcdX, y, COLOR_AMBER_DIM);
    y += 12;
    fontRenderer.drawString("EXIT", lcdX, y, COLOR_AMBER_DIM);
  }

  private void drawCells() {
    for (int i = 0; i < cells.size(); i++) {
      Cell c = cells.get(i);
      boolean sel = i == selected;
      if (sel) {
        drawRect(c.x - 2, c.y - 1, c.x + c.w, c.y + 9, COLOR_SEL);
      }
      String text = (sel && c.commitSeconds != null && !entry.isEmpty()) ? (entry + "_")
          : c.value.get();
      fontRenderer.drawString(text, c.x, c.y, sel ? COLOR_AMBER_HEAD : COLOR_AMBER);
    }
  }

  private void drawHint() {
    String hint = "Arrows: select   +/-: adjust   digits+ENTER: set time   ENTER: cycle";
    fontRenderer.drawString(hint, left + 12, top + H - 12, COLOR_AMBER_DIM);
  }

  private String trim(String s, int maxPx) {
    if (s == null) {
      return "";
    }
    while (s.length() > 0 && fontRenderer.getStringWidth(s) > maxPx) {
      s = s.substring(0, s.length() - 1);
    }
    return s;
  }

  // endregion
}
