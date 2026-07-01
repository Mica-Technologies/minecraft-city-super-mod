package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalCoordinationMode;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalPhaseMovement;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalOverlapType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalPreempt;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalProgrammedOverlap;
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
import net.minecraft.client.renderer.GlStateManager;
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

  private enum Screen { STATUS, TIMING, MAP, COORD, PREEMPT, ACT, OVL, HELP }

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
  // Screen-select tabs occupy BTN_SCREEN_BASE .. BTN_SCREEN_BASE + Screen.values().length - 1.
  // Reserve a generous block (220..239) so adding a screen never collides with the buttons below.
  private static final int BTN_SCREEN_BASE = 220;
  private static final int BTN_TEMPLATE = 240;
  private static final int BTN_CLOSE = 241;
  private static final int BTN_PE_ADD = 242;
  private static final int BTN_PE_REMOVE = 243;
  private static final int BTN_PE_PREV = 244;
  private static final int BTN_PE_NEXT = 245;

  private static final int W = 384;
  private static final int H = 296;

  /**
   * Uniform render scale applied to the whole panel. The panel is laid out in a fixed
   * {@code W}&times;{@code H} design space, then drawn (and mouse-picked) at this scale so it fits
   * comfortably inside smaller GUI resolutions / higher GUI-scale settings instead of overflowing
   * the screen's right and bottom edges.
   */
  private static final float PANEL_SCALE = 0.8f;

  /**
   * Vertical offset (from the LCD top {@code lcdY}) at which a table's body rows begin. Leaves a
   * clear row for the screen title ({@code lcdY}) and the column-header row ({@code lcdY + 12}) so
   * the first phase row does not render on top of the headers.
   */
  private static final int TABLE_BODY_DY = 24;

  private static final String[] MOVEMENT_ABBR = {"THRU", "LEFT", "PLFT", "RGHT", "PED"};
  private static final String[] RECALL_ABBR = {"NONE", "MIN", "MAX", "PED", "SOFT"};
  // Combined ped-recall (bit 0) + rest-in-walk (bit 1) state labels for the MAP "PED" column.
  private static final String[] PED_OPT_LABELS = {"no", "PedR", "Walk", "P+W"};
  // Combined dual-entry (bit 0) + conditional-service (bit 1) labels for the MAP "OPT" column.
  private static final String[] PHASE_OPT_LABELS = {"-", "DE", "CS", "D+C"};
  private static final String[] TIMING_HEADERS =
      {"MnG", "Pas", "MxG", "Yel", "Red", "Wlk", "PCl", "DGn"};
  private static final String[] TIMING_ACTIONS = {"ph.minGreen", "ph.passage", "ph.maxGreen",
      "ph.yellow", "ph.redClear", "ph.walk", "ph.pedClear", "ph.dlyGreen"};
  // ACT (actuation / volume-density) screen: Max 2, Bike Min Green, Added Initial, Max Initial,
  // Min Gap, Time-Before-Reduce, Time-To-Reduce.
  private static final String[] ACT_HEADERS =
      {"Mx2", "BkG", "AdI", "MxI", "Gap", "TB4", "TTR"};
  private static final String[] ACT_ACTIONS = {"ph.max2", "ph.bikeMinGreen", "ph.addedInitial",
      "ph.maxInitial", "ph.minGap", "ph.timeB4Reduce", "ph.timeToReduce"};

  // HELP screen copy. Paragraphs are word-wrapped to the LCD width at draw time; "" is a blank line
  // and a line ending in ":" (or the title) renders as a heading.
  private static final String[] HELP_PARAGRAPHS = {
      "CSM ASC-3 — Quick Help",
      "",
      "TABS:",
      "STATUS: live mode + ring/barrier view.  TIMING: per-phase intervals.  MAP: assign phases to "
          + "circuits & movements.  COORD: cycle/offset/splits.  PREEMPT: priority/rail preempts.  "
          + "ACT: volume-density timing.  OVL: right-turn & ped overlaps.",
      "",
      "BASIC SETUP:",
      "1. Link each approach's signal heads to a circuit with the Signal Link Tool (one circuit "
          + "per approach).",
      "2. On MAP, press 'Load Std 8-Phase' to auto-assign the standard NEMA phases from the head "
          + "facings, or set each phase's circuit and movement by hand.",
      "3. On TIMING, set min/max green, passage, yellow, red clear, walk and ped clear (seconds).",
      "4. Set a phase's RCL to MIN/MAX/PED to recall it every cycle, or SOFT to rest on it.",
      "",
      "EDITING VALUES:",
      "Click a field (or use the arrows) to select it, type a number and press ENTER to set it in "
          + "seconds. +/- nudge a value or cycle a list. Closing the panel also saves a typed value.",
      "",
      "FYA LEFT TURNS:",
      "An FYA head is TWO linked blocks: the 3-section flashing-left lens (linked as the "
          + "flashing-left signal) and the 1-section green-arrow add-on (linked as the left signal) "
          + "mounted below it.",
      "To program: on MAP set the left phase's movement to PROT LEFT, assign its circuit, and set "
          + "the FYA column to the OPPOSING through phase (e.g. left 1 → P6). 0 = protected "
          + "only, no flash.",
      "It then shows: green arrow during the protected phase, solid yellow during clearance, "
          + "flashing yellow while the opposing through is green, and red otherwise.",
      "",
      "PEDESTRIANS:",
      "Rest-in-Walk holds WALK while the controller rests on a phase; it always runs a full "
          + "flashing-don't-walk clearance before the phase yields.",
      "",
      "CONCURRENT PHASES (DUAL RING):",
      "Two compatible phases — one from each ring on the same barrier — run green together. So two "
          + "phases that are both called (e.g. the two opposing throughs 2 & 6, or the two lefts "
          + "1 & 5) are served at the same time automatically; no setting needed.",
      "To also bring up a companion phase that has NO call of its own (e.g. give 6 a green while 2 "
          + "holds the street, or 2 while 5 turns), set that phase's OPT column to DE (Dual Entry). "
          + "When one ring is serving and the other ring has no call, the idle ring companions with "
          + "its first active DE phase on that barrier. A real call always wins over dual entry.",
      "Note: DE serves the FIRST active DE phase in the ring, so flag only the phase you want as the "
          + "companion (e.g. DE on 6, not 5, to pair 6 with 2).",
      "",
      "TIP: hover any on-screen label for a one-line explanation.",
  };

  private final TileEntityTrafficSignalController controller;
  private final BlockPos blockPos;

  private Screen screen = Screen.STATUS;
  /** First visible line on the HELP screen (scroll offset). */
  private int helpScroll = 0;
  private final List<Cell> cells = new ArrayList<>();
  /** Hover-help regions, rebuilt each frame as the current screen is drawn. */
  private final List<Help> helps = new ArrayList<>();
  private int selected = 0;
  private String entry = "";
  private int selectedPreempt = 0;
  private int selectedOverlap = 0;

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

  /** A hover-help region: design-space bounds plus the lines to show. First line is the title. */
  private static final class Help {
    final int x;
    final int y;
    final int w;
    final int h;
    final String[] lines;

    Help(int x, int y, int w, int h, String[] lines) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      this.lines = lines;
    }
  }

  private void addHelp(int x, int y, int w, int h, String... lines) {
    helps.add(new Help(x, y, w, h, lines));
  }

  /** Registers a help region over the on-screen bounds of the button with the given id. */
  private void addButtonHelp(int id, String... lines) {
    for (GuiButton b : buttonList) {
      if (b.id == id) {
        addHelp(b.x, b.y, b.width, b.height, lines);
        return;
      }
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
    // Flush a value the operator typed but didn't ENTER before closing (Close button or ESC), so
    // "type a number, click Close" saves it instead of silently discarding it.
    commitPendingEntry();
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
    int sy = top + H - 94;
    String[] names = {"STATUS", "TIMING", "MAP", "COORD", "PREEMPT", "ACT", "OVL", "HELP"};
    // Size the tab row to the panel so all tabs fit regardless of count.
    int tabW = (W - 24) / names.length;
    for (int i = 0; i < names.length; i++) {
      buttonList.add(new GuiButton(BTN_SCREEN_BASE + i, sx + i * tabW, sy, tabW - 3, 14, names[i]));
    }

    // Numeric keypad (left cluster).
    int kx = left + 12;
    int ky = top + H - 76;
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
      case ACT:
        buildActCells();
        break;
      case OVL:
        buildOverlapCells();
        break;
      case HELP:
        helpScroll = 0; // no editable cells; the arrow/plus-minus keys scroll instead
        break;
      case STATUS:
      default:
        break;
    }
  }

  private void buildTimingCells() {
    int rowH = 11;
    int colW = (lcdW - 24) / TIMING_HEADERS.length;
    int y0 = lcdY + TABLE_BODY_DY;
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

  private void buildActCells() {
    int rowH = 11;
    int colW = (lcdW - 24) / ACT_HEADERS.length;
    int y0 = lcdY + TABLE_BODY_DY;
    for (int pn = 1; pn <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; pn++) {
      final int n = pn;
      int y = y0 + (pn - 1) * rowH;
      for (int ci = 0; ci < ACT_ACTIONS.length; ci++) {
        final int fieldIdx = ci;
        final String action = ACT_ACTIONS[ci];
        int x = lcdX + 24 + ci * colW;
        cells.add(new Cell(x, y, colW - 2,
            () -> secs(actValue(plan().getPhase(n), fieldIdx)),
            dir -> send(action, n, actValue(plan().getPhase(n), fieldIdx) + dir * 10L),
            sec -> send(action, n, Math.round(sec * 20))));
      }
    }
  }

  private long actValue(TrafficSignalProgrammedPhase p, int fieldIdx) {
    if (p == null) {
      return 0;
    }
    switch (fieldIdx) {
      case 0: return p.getMax2();
      case 1: return p.getBikeMinGreen();
      case 2: return p.getAddedInitial();
      case 3: return p.getMaxInitial();
      case 4: return p.getMinGap();
      case 5: return p.getTimeBeforeReduce();
      case 6: return p.getTimeToReduce();
      default: return 0;
    }
  }

  private void buildMapCells() {
    int rowH = 11;
    int y0 = lcdY + TABLE_BODY_DY;
    int circuitCount = controller.getSignalCircuitCount();
    int[] colX = {lcdX + 26, lcdX + 70, lcdX + 126, lcdX + 174, lcdX + 222};
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
      cells.add(new Cell(colX[2], y, 44,
          () -> MOVEMENT_ABBR[plan().getPhase(n).getMovement().ordinal()],
          dir -> send("ph.movement", n,
              cyc(plan().getPhase(n).getMovement().ordinal(), dir,
                  TrafficSignalPhaseMovement.values().length)), null));
      // Recall
      cells.add(new Cell(colX[3], y, 44,
          () -> RECALL_ABBR[plan().getPhase(n).getRecallMode().ordinal()],
          dir -> send("ph.recall", n,
              cyc(plan().getPhase(n).getRecallMode().ordinal(), dir,
                  TrafficSignalRecallMode.values().length)), null));
      // Ped options: combined ped recall + rest-in-walk as a 4-state cycle.
      cells.add(new Cell(colX[4], y, 34,
          () -> PED_OPT_LABELS[pedOptState(plan().getPhase(n))],
          dir -> {
            int st = (pedOptState(plan().getPhase(n)) + (dir >= 0 ? 1 : 3)) & 3;
            send("ph.pedOpts", n, st);
          }, null));
      // Phase options: combined dual entry + conditional service as a 4-state cycle.
      cells.add(new Cell(lcdX + 304, y, 48,
          () -> PHASE_OPT_LABELS[phaseOptState(plan().getPhase(n))],
          dir -> {
            int st = (phaseOptState(plan().getPhase(n)) + (dir >= 0 ? 1 : 3)) & 3;
            send("ph.phaseOpts", n, st);
          }, null));
      // FYA permissive phase (opposing through; 0 = protected-only)
      cells.add(new Cell(lcdX + 260, y, 40,
          () -> {
            int pp = plan().getPhase(n).getPermissivePhase();
            return pp <= 0 ? "--" : ("P" + pp);
          },
          dir -> {
            int max = TrafficSignalProgrammedPhasePlan.PHASE_COUNT;
            int pp = plan().getPhase(n).getPermissivePhase() + dir;
            if (pp < 0) {
              pp = max;
            }
            if (pp > max) {
              pp = 0;
            }
            send("ph.permPhase", n, pp);
          }, null));
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
    y += 24;
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

  private void buildOverlapCells() {
    List<TrafficSignalProgrammedOverlap> ovs = plan().getVehicleOverlaps();
    if (ovs.isEmpty()) {
      return;
    }
    if (selectedOverlap >= ovs.size()) {
      selectedOverlap = ovs.size() - 1;
    }
    final int oi = selectedOverlap;
    int circuitCount = controller.getSignalCircuitCount();
    int y = lcdY + 26;
    cells.add(new Cell(lcdX + 80, y, 40,
        () -> ovs.get(oi).isEnabled() ? "On" : "off",
        dir -> send("ov.enabled", oi, ovs.get(oi).isEnabled() ? 0 : 1), null));
    y += 12;
    cells.add(new Cell(lcdX + 80, y, 60,
        () -> ovs.get(oi).getType().getName(),
        dir -> send("ov.type", oi,
            cyc(ovs.get(oi).getType().ordinal(), dir,
                TrafficSignalOverlapType.values().length)), null));
    y += 12;
    cells.add(new Cell(lcdX + 80, y, 50,
        () -> ovs.get(oi).getOutputCircuitIndex() < 0 ? "--"
            : ("C" + (ovs.get(oi).getOutputCircuitIndex() + 1)),
        dir -> {
          int c = ovs.get(oi).getOutputCircuitIndex() + dir;
          if (c < -1) {
            c = circuitCount - 1;
          }
          if (c >= circuitCount) {
            c = -1;
          }
          send("ov.outCircuit", oi, c);
        }, null));
    y += 12;
    cells.add(new Cell(lcdX + 80, y, 60,
        () -> MOVEMENT_ABBR[ovs.get(oi).getOutputMovement().ordinal()],
        dir -> send("ov.outMovement", oi,
            cyc(ovs.get(oi).getOutputMovement().ordinal(), dir,
                TrafficSignalPhaseMovement.values().length)), null));
    y += 12;
    cells.add(new Cell(lcdX + 80, y, 50,
        () -> secs(ovs.get(oi).getTrailGreen()),
        dir -> send("ov.trailGreen", oi, ovs.get(oi).getTrailGreen() + dir * 10L),
        sec -> send("ov.trailGreen", oi, Math.round(sec * 20))));
    y += 12;
    cells.add(new Cell(lcdX + 80, y, 50,
        () -> secs(ovs.get(oi).getLeadGreen()),
        dir -> send("ov.leadGreen", oi, ovs.get(oi).getLeadGreen() + dir * 10L),
        sec -> send("ov.leadGreen", oi, Math.round(sec * 20))));
    y += 16;
    for (int pn = 1; pn <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; pn++) {
      final int n = pn;
      cells.add(new Cell(lcdX + 70 + (pn - 1) * 22, y, 18,
          () -> contains(ovs.get(oi).getIncludedPhases())[n] ? ("[" + n + "]") : (" " + n + " "),
          dir -> send("ov.includedToggle", oi, n), null));
    }
    y += 14;
    for (int pn = 1; pn <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; pn++) {
      final int n = pn;
      cells.add(new Cell(lcdX + 70 + (pn - 1) * 22, y, 18,
          () -> contains(ovs.get(oi).getModifierPhases())[n] ? ("[" + n + "]") : (" " + n + " "),
          dir -> send("ov.modifierToggle", oi, n), null));
    }
  }

  /** Combined ped-recall (bit 0) + rest-in-walk (bit 1) state for the MAP "PED" column. */
  private static int pedOptState(TrafficSignalProgrammedPhase p) {
    return (p.isPedRecall() ? 1 : 0) | (p.isRestInWalk() ? 2 : 0);
  }

  /** Combined dual-entry (bit 0) + conditional-service (bit 1) state for the MAP "OPT" column. */
  private static int phaseOptState(TrafficSignalProgrammedPhase p) {
    return (p.isDualEntry() ? 1 : 0) | (p.isConditionalService() ? 2 : 0);
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
      case 7: return p.getDelayedGreen();
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
      if (screen == Screen.HELP) {
        helpScroll += 3;
      } else {
        adjustSelected(1);
      }
    } else if (id == BTN_MINUS) {
      if (screen == Screen.HELP) {
        helpScroll -= 3;
      } else {
        adjustSelected(-1);
      }
    } else if (id == BTN_UP || id == BTN_LEFT) {
      if (screen == Screen.HELP) {
        helpScroll--;
      } else {
        moveSelection(-1);
      }
    } else if (id == BTN_DOWN || id == BTN_RIGHT) {
      if (screen == Screen.HELP) {
        helpScroll++;
      } else {
        moveSelection(1);
      }
    } else if (id >= BTN_SCREEN_BASE && id < BTN_SCREEN_BASE + Screen.values().length) {
      screen = Screen.values()[id - BTN_SCREEN_BASE];
      rebuildCells();
    } else if (id == BTN_TEMPLATE) {
      send("loadTemplate", 0, 0);
    } else if (id == BTN_PE_ADD) {
      // The P+/P-/Prev/Next cluster edits overlaps on the OVL screen, preempts elsewhere.
      send(screen == Screen.OVL ? "ov.add" : "pe.add", 0, 0);
    } else if (id == BTN_PE_REMOVE) {
      if (screen == Screen.OVL) {
        send("ov.remove", selectedOverlap, 0);
      } else {
        send("pe.remove", selectedPreempt, 0);
      }
    } else if (id == BTN_PE_PREV) {
      if (screen == Screen.OVL) {
        selectedOverlap = Math.max(0, selectedOverlap - 1);
      } else {
        selectedPreempt = Math.max(0, selectedPreempt - 1);
      }
      rebuildCells();
    } else if (id == BTN_PE_NEXT) {
      if (screen == Screen.OVL) {
        selectedOverlap++;
      } else {
        selectedPreempt++;
      }
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
        if (screen == Screen.HELP) {
          helpScroll--;
        } else {
          moveSelection(-1);
        }
        return;
      case Keyboard.KEY_DOWN:
      case Keyboard.KEY_RIGHT:
        if (screen == Screen.HELP) {
          helpScroll++;
        } else {
          moveSelection(1);
        }
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

  /**
   * Commits a pending typed numeric entry to the selected cell without the enum-cycle fallback of
   * {@link #commitOrCycle()}. Used on close so a typed-but-not-ENTERed value is still saved.
   */
  private void commitPendingEntry() {
    Cell cell = selectedCell();
    if (cell != null && cell.commitSeconds != null && !entry.isEmpty()) {
      try {
        cell.commitSeconds.accept(Double.parseDouble(entry));
      } catch (NumberFormatException ignored) {
        // ignore malformed entry
      }
    }
    entry = "";
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
    // Map screen-space click to design space (inverse of the PANEL_SCALE applied in drawScreen).
    final float cx = width / 2f;
    final float cy = height / 2f;
    int mx = Math.round((mouseX - cx) / PANEL_SCALE + cx);
    int my = Math.round((mouseY - cy) / PANEL_SCALE + cy);
    super.mouseClicked(mx, my, mouseButton);
    // Click a cell to select it.
    for (int i = 0; i < cells.size(); i++) {
      Cell c = cells.get(i);
      if (mx >= c.x - 1 && mx <= c.x + c.w && my >= c.y - 1 && my <= c.y + 9) {
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
    helps.clear();
    // The full-screen dimming above stays unscaled; everything from here down is rendered in the
    // fixed W x H design space, scaled about the screen centre (== panel centre) so it fits.
    final float cx = width / 2f;
    final float cy = height / 2f;
    GlStateManager.pushMatrix();
    GlStateManager.translate(cx, cy, 0f);
    GlStateManager.scale(PANEL_SCALE, PANEL_SCALE, 1f);
    GlStateManager.translate(-cx, -cy, 0f);
    // Bezel.
    drawRect(left - 2, top - 2, left + W + 2, top + H + 2, COLOR_BODY_EDGE);
    drawRect(left, top, left + W, top + H, COLOR_BODY);
    // Header.
    fontRenderer.drawString("CSM ASC-3  ·  Advanced Signal Controller", left + 10, top + 8,
        COLOR_AMBER_HEAD);
    drawLeds();
    // LCD.
    int lcdBottom = top + H - 110;
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
      case ACT:
        drawAct();
        break;
      case OVL:
        drawOverlap();
        break;
      case HELP:
        drawHelp();
        break;
      default:
        break;
    }

    drawCells();
    drawHint();
    registerButtonHelps();
    // Buttons are drawn by super in the same design space; feed it design-space mouse coords so
    // hover highlighting lines up with the scaled rendering.
    int dmx = Math.round((mouseX - cx) / PANEL_SCALE + cx);
    int dmy = Math.round((mouseY - cy) / PANEL_SCALE + cy);
    super.drawScreen(dmx, dmy, partialTicks);
    // Tooltips render last so they sit on top of everything, still in the scaled design space.
    drawTooltips(dmx, dmy);
    GlStateManager.popMatrix();
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
    addHelp(x, y, 40, 9, "RUN", "Lit green when the controller is in",
        "ADVANCED mode and running with no fault.");
    addHelp(x + 46, y, 48, 9, "COORD", "Lit green when running a coordinated",
        "(fixed-cycle) plan — set on the COORD screen.");
    addHelp(x + 100, y, 46, 9, "FAULT", "Lit red when a fault is latched. Clear it",
        "from the main controller GUI (Clear Faults).");
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
    int ringTop = y;
    fontRenderer.drawString("Ring/Barrier:", lcdX, y, COLOR_AMBER_DIM);
    y += 11;
    drawRingRow(plan, 1, lcdX, y);
    y += 11;
    drawRingRow(plan, 2, lcdX, y);
    addHelp(lcdX, ringTop, lcdW - 6, (y - ringTop) + 9, "Ring & Barrier diagram",
        "The two rings (R1/R2) time concurrently; a barrier keeps conflicting",
        "movements apart. Each box is phase:movement — bright = main-street",
        "barrier, dim = side-street barrier. Configure on MAP and TIMING.");
    y += 14;
    int coordTop = y;
    String coord = plan.getCoordination().isCoordinated()
        ? ("COORD cycle " + secs(plan.getCoordination().getCycleLength()) + "s offset "
            + secs(plan.getCoordination().getOffset()) + "s")
        : "FREE (fully actuated)";
    fontRenderer.drawString(coord, lcdX, y, COLOR_AMBER);
    addHelp(lcdX, coordTop, lcdW - 6, 9, "Coordination",
        "FREE = fully actuated, no fixed cycle. COORD shows the running cycle",
        "length and offset. Change it on the COORD screen.");
    y += 11;
    fontRenderer.drawString("Preempts: " + plan.getPreempts().size(), lcdX, y, COLOR_AMBER);
    addHelp(lcdX, y, lcdW - 6, 9, "Preempts",
        "Number of preemption sequences (railroad / emergency / transit)",
        "configured. Add and edit them on the PREEMPT screen.");
    y += 14;
    for (String line : fontRenderer.listFormattedStringToWidth(
        "Use the keys below to program. Load Std 8-Phase auto-assigns by approach.", lcdW - 6)) {
      fontRenderer.drawString(line, lcdX, y, COLOR_AMBER_DIM);
      y += 10;
    }
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
      int y = lcdY + TABLE_BODY_DY + (pn - 1) * rowH;
      TrafficSignalProgrammedPhase p = plan().getPhase(pn);
      int color = p != null && p.isActive() ? COLOR_AMBER : COLOR_AMBER_DIM;
      fontRenderer.drawString("φ" + pn, lcdX, y, color);
    }
    // Hover help: each column (header + its 8 cells) explains the timing interval it sets.
    int colHelpH = (TABLE_BODY_DY - 12) + TrafficSignalProgrammedPhasePlan.PHASE_COUNT * rowH;
    String[][] colHelp = {
        {"Minimum Green (MnG)", "Shortest time the phase stays green once it starts,",
            "regardless of demand."},
        {"Passage / Gap (Pas)", "Green extension added by each vehicle call, up to",
            "Max Green. Larger = holds green through bigger gaps."},
        {"Maximum Green (MxG)", "Longest green allowed while a conflicting phase is",
            "calling; the phase force-offs when this is reached."},
        {"Yellow Change (Yel)", "Yellow clearance interval shown before the phase",
            "goes red."},
        {"Red Clearance (Red)", "All-red time after yellow, before the next phase",
            "is given green."},
        {"Walk (Wlk)", "Pedestrian WALK interval for this phase (steady",
            "WALK before the clearance countdown)."},
        {"Ped Clearance (PCl)", "Flashing DON'T WALK countdown after WALK ends,",
            "sized so pedestrians can finish crossing."},
        {"Delayed Green (DGn)", "Leading ped interval: holds the vehicle green this long",
            "after WALK starts (peds go first). 0 = off; ignored with no ped call."},
    };
    for (int i = 0; i < TIMING_HEADERS.length; i++) {
      addHelp(lcdX + 24 + i * colW, lcdY + 12, colW, colHelpH, colHelp[i]);
    }
    addHelp(lcdX, lcdY + TABLE_BODY_DY, 22,
        TrafficSignalProgrammedPhasePlan.PHASE_COUNT * rowH, "Phase φ1–φ8",
        "NEMA phase number. See the STATUS screen for how these",
        "phases are arranged into rings and barriers.");
  }

  private void drawAct() {
    fontRenderer.drawString("ACTUATION / VOLUME-DENSITY (seconds)", lcdX, lcdY, COLOR_AMBER_HEAD);
    int colW = (lcdW - 24) / ACT_HEADERS.length;
    for (int i = 0; i < ACT_HEADERS.length; i++) {
      fontRenderer.drawString(ACT_HEADERS[i], lcdX + 24 + i * colW, lcdY + 12, COLOR_AMBER_DIM);
    }
    int rowH = 11;
    for (int pn = 1; pn <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; pn++) {
      int y = lcdY + TABLE_BODY_DY + (pn - 1) * rowH;
      TrafficSignalProgrammedPhase p = plan().getPhase(pn);
      int color = p != null && p.isActive() ? COLOR_AMBER : COLOR_AMBER_DIM;
      fontRenderer.drawString("φ" + pn, lcdX, y, color);
    }
    int colHelpH = (TABLE_BODY_DY - 12) + TrafficSignalProgrammedPhasePlan.PHASE_COUNT * rowH;
    String[][] colHelp = {
        {"Max Green 2 (Mx2)", "Secondary max green, used in coordinated operation",
            "when set. 0 = always use Max Green (the TIMING screen)."},
        {"Bike Min Green (BkG)", "Minimum green guaranteed when a bicycle is detected",
            "at green start. 0 = off."},
        {"Added Initial (AdI)", "Extra guaranteed initial green per vehicle waiting at",
            "green start (volume-density). 0 = off."},
        {"Max Initial (MxI)", "Cap on the added-initial green. 0 = uncapped."},
        {"Min Gap (Gap)", "Floor the passage/gap shrinks toward as the green runs",
            "on (gap reduction). 0 = no reduction."},
        {"Time Before Reduce (TB4)", "Green time before gap reduction begins."},
        {"Time To Reduce (TTR)", "Ramp time from the full passage down to the min gap.",
            "0 = gap reduction disabled."},
    };
    for (int i = 0; i < ACT_HEADERS.length; i++) {
      addHelp(lcdX + 24 + i * colW, lcdY + 12, colW, colHelpH, colHelp[i]);
    }
    addHelp(lcdX, lcdY + TABLE_BODY_DY, 22,
        TrafficSignalProgrammedPhasePlan.PHASE_COUNT * rowH, "Phase φ1–φ8",
        "NEMA phase number. Volume-density and Max-2 timing per phase.");
  }

  private void drawMap() {
    fontRenderer.drawString("PHASE MAP", lcdX, lcdY, COLOR_AMBER_HEAD);
    fontRenderer.drawString("EN", lcdX + 26, lcdY + 12, COLOR_AMBER_DIM);
    fontRenderer.drawString("CKT", lcdX + 70, lcdY + 12, COLOR_AMBER_DIM);
    fontRenderer.drawString("MOVE", lcdX + 126, lcdY + 12, COLOR_AMBER_DIM);
    fontRenderer.drawString("RCL", lcdX + 174, lcdY + 12, COLOR_AMBER_DIM);
    fontRenderer.drawString("PED", lcdX + 222, lcdY + 12, COLOR_AMBER_DIM);
    fontRenderer.drawString("FYA", lcdX + 260, lcdY + 12, COLOR_AMBER_DIM);
    fontRenderer.drawString("OPT", lcdX + 304, lcdY + 12, COLOR_AMBER_DIM);
    int rowH = 11;
    for (int pn = 1; pn <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; pn++) {
      int y = lcdY + TABLE_BODY_DY + (pn - 1) * rowH;
      fontRenderer.drawString("φ" + pn, lcdX, y, COLOR_AMBER);
    }
    int colHelpH = (TABLE_BODY_DY - 12) + TrafficSignalProgrammedPhasePlan.PHASE_COUNT * rowH;
    int[] hx = {lcdX + 26, lcdX + 70, lcdX + 126, lcdX + 174, lcdX + 222, lcdX + 260, lcdX + 304};
    int[] hw = {40, 50, 46, 46, 36, 42, 50};
    String[][] colHelp = {
        {"Enable (EN)", "On = this phase runs. off = the phase is skipped",
            "entirely and never served."},
        {"Circuit (CKT)", "Which signal circuit (C1, C2, …) this phase drives.",
            "'--' = unassigned. Manage circuits in the main GUI."},
        {"Movement (MOVE)", "THRU through · LEFT left · PLFT protected left ·",
            "RGHT right · PED walk. Selects both the signals the",
            "phase drives and the detector zone that calls it."},
        {"Recall (RCL)", "NONE actuated (served only on a call) · MIN recall",
            "to min green · MAX hold to max green · PED recall a",
            "walk each cycle · SOFT rest here when nothing else calls."},
        {"Ped Options (PED)", "PedR = recall a Walk every cycle (no button needed).",
            "Walk = rest in walk (hold WALK while resting here).",
            "P+W = both. 'no' = button-only, don't-walk at rest."},
        {"Flashing Yellow Arrow (FYA)", "For a left phase: the opposing-through phase whose",
            "green makes this left flash yellow (permissive). PnN =",
            "that phase; '--' = protected-only (no FYA)."},
        {"Phase Options (OPT)", "DE = dual entry (serve to companion the other ring",
            "when it has no call). CS = conditional service (re-serve",
            "once per barrier on a fresh call). D+C = both. - = neither."},
    };
    for (int i = 0; i < hx.length; i++) {
      addHelp(hx[i], lcdY + 12, hw[i], colHelpH, colHelp[i]);
    }
  }

  private void drawCoord() {
    fontRenderer.drawString("COORDINATION", lcdX, lcdY, COLOR_AMBER_HEAD);
    fontRenderer.drawString("Mode:", lcdX, lcdY + 14, COLOR_AMBER_DIM);
    fontRenderer.drawString("Cycle:", lcdX, lcdY + 26, COLOR_AMBER_DIM);
    fontRenderer.drawString("Offset:", lcdX, lcdY + 38, COLOR_AMBER_DIM);
    fontRenderer.drawString("Splits / coordinated phases:", lcdX, lcdY + 50, COLOR_AMBER_DIM);
    addHelp(lcdX, lcdY + 14, 64, 9, "Coordination Mode",
        "FREE = fully actuated (no fixed cycle). COORDINATED = run a",
        "fixed background cycle with per-phase splits and force-offs.");
    addHelp(lcdX, lcdY + 26, 64, 9, "Cycle Length",
        "Total length of one full cycle through all phases.",
        "Only used in COORDINATED mode.");
    addHelp(lcdX, lcdY + 38, 64, 9, "Offset",
        "Cycle-start offset relative to the system master clock —",
        "stagger offsets along a corridor to build green waves.");
    addHelp(lcdX, lcdY + 50, lcdW - 6, 9, "Splits & coordinated phases",
        "Split = the slice of the cycle budgeted to each phase. Toggle",
        "COORD to mark a phase as a coordinated (synced) phase that",
        "rests in green between its permissive windows.");
    int rowH = 11;
    for (int pn = 1; pn <= TrafficSignalProgrammedPhasePlan.PHASE_COUNT; pn++) {
      int y = lcdY + 62 + (pn - 1) * rowH;
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
    addHelp(lcdX, y, 70, 9, "Enabled", "Turn this preemption sequence on or off.");
    y += 12;
    fontRenderer.drawString("Type:", lcdX, y, COLOR_AMBER_DIM);
    addHelp(lcdX, y, 70, 9, "Preempt Type",
        "RAILROAD > EMERGENCY VEHICLE > TRANSIT PRIORITY.",
        "If several fire at once, the higher type wins.");
    y += 12;
    fontRenderer.drawString("Trig CKT:", lcdX, y, COLOR_AMBER_DIM);
    addHelp(lcdX, y, 70, 9, "Trigger Circuit",
        "Which signal circuit's detector input arms this preempt.");
    y += 12;
    fontRenderer.drawString("Trig MOV:", lcdX, y, COLOR_AMBER_DIM);
    addHelp(lcdX, y, 70, 9, "Trigger Movement",
        "Which movement on the trigger circuit (e.g. THRU, LEFT)",
        "is watched as the preempt input.");
    y += 12;
    fontRenderer.drawString("Min Dwell:", lcdX, y, COLOR_AMBER_DIM);
    addHelp(lcdX, y, 70, 9, "Minimum Dwell",
        "Shortest time the controller stays in the preempt's dwell",
        "(hold) state before it is allowed to end.");
    y += 16;
    fontRenderer.drawString("TRACK", lcdX, y, COLOR_AMBER_DIM);
    addHelp(lcdX, y, 60, 9, "Track Clearance Phases",
        "Phases served first to clear the conflicting path (e.g. a grade",
        "crossing) before dwell. [n] = phase included in the set.");
    y += 12;
    fontRenderer.drawString("DWELL", lcdX, y, COLOR_AMBER_DIM);
    addHelp(lcdX, y, 60, 9, "Dwell Phases",
        "Phases held green throughout the preempt (e.g. the emergency",
        "or transit approach). [n] = phase included in the set.");
    y += 12;
    fontRenderer.drawString("EXIT", lcdX, y, COLOR_AMBER_DIM);
    addHelp(lcdX, y, 60, 9, "Exit Phases",
        "Phases served on the way out of the preempt before the",
        "controller returns to normal operation.");
  }

  private void drawOverlap() {
    List<TrafficSignalProgrammedOverlap> ovs = plan().getVehicleOverlaps();
    fontRenderer.drawString("VEHICLE OVERLAPS  (" + ovs.size() + " configured)", lcdX, lcdY,
        COLOR_AMBER_HEAD);
    if (ovs.isEmpty()) {
      fontRenderer.drawString("No overlaps. Press P+ to add one.", lcdX, lcdY + 16, COLOR_AMBER);
      return;
    }
    fontRenderer.drawString("Selected: #" + (selectedOverlap + 1) + " of " + ovs.size()
        + "  (Prev/Next)", lcdX, lcdY + 12, COLOR_AMBER_DIM);
    int y = lcdY + 26;
    fontRenderer.drawString("Enabled:", lcdX, y, COLOR_AMBER_DIM);
    addHelp(lcdX, y, 76, 9, "Enabled", "Turn this vehicle overlap on or off.");
    y += 12;
    fontRenderer.drawString("Type:", lcdX, y, COLOR_AMBER_DIM);
    addHelp(lcdX, y, 76, 9, "Overlap Type",
        "Normal = green with the included phases. -Grn/Yel = also",
        "forced red while a MOD (modifier) phase is green/yellow.");
    y += 12;
    fontRenderer.drawString("Out CKT:", lcdX, y, COLOR_AMBER_DIM);
    addHelp(lcdX, y, 76, 9, "Output Circuit",
        "Which signal circuit's heads this overlap drives.");
    y += 12;
    fontRenderer.drawString("Out MOV:", lcdX, y, COLOR_AMBER_DIM);
    addHelp(lcdX, y, 76, 9, "Output Movement",
        "Which heads on the output circuit the overlap drives.",
        "RGHT = the common right-turn overlap.");
    y += 12;
    fontRenderer.drawString("Trail:", lcdX, y, COLOR_AMBER_DIM);
    addHelp(lcdX, y, 76, 9, "Lag (Trailing) Green",
        "Seconds the overlap holds green after its included phases",
        "leave green, to clear turning vehicles. 0 = off.");
    y += 12;
    fontRenderer.drawString("Lead:", lcdX, y, COLOR_AMBER_DIM);
    addHelp(lcdX, y, 76, 9, "Lead (Advance) Green",
        "Seconds the overlap greens BEFORE an included phase greens,",
        "during the preceding red clearance (within-barrier). 0 = off.");
    y += 16;
    fontRenderer.drawString("INCL", lcdX, y, COLOR_AMBER_DIM);
    addHelp(lcdX, y, 60, 9, "Included Phases",
        "The overlap runs GREEN while any included phase is green,",
        "YELLOW during their clearance, else red. [n] = included.");
    y += 14;
    fontRenderer.drawString("MOD", lcdX, y, COLOR_AMBER_DIM);
    addHelp(lcdX, y, 60, 9, "Modifier Phases (-Grn/Yel)",
        "Only for the -Grn/Yel type: the overlap is forced red while",
        "any [n] modifier phase is green or yellow. [n] = modifier.");
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
    // The full hint is too wide for one line, so it's split in two. The first half stays in its
    // spot just above the keypad's screen-select row (preserving the LCD's full height); the
    // second half is tucked into the empty band at the bottom of the panel, below the ENTER /
    // Prev / Next buttons (right of the numeric keypad's bottom row).
    fontRenderer.drawString(
        "Hover any label for help   ·   Arrows: select   ·   +/-: adjust",
        left + 12, top + H - 106, COLOR_AMBER_DIM);
    fontRenderer.drawString(
        "digits+ENTER: set time   ·   ENTER: cycle",
        left + 100, top + H - 14, COLOR_AMBER_DIM);
  }

  /** Scrollable, word-wrapped help copy shown on the HELP screen. */
  private void drawHelp() {
    int lineH = 10;
    int areaTop = lcdY;
    int areaBottom = top + H - 116; // leave the scroll indicator + hint band below
    int maxLines = Math.max(1, (areaBottom - areaTop) / lineH);
    java.util.List<String> lines = wrappedHelpLines();
    int maxScroll = Math.max(0, lines.size() - maxLines);
    helpScroll = Math.max(0, Math.min(helpScroll, maxScroll));
    int y = areaTop;
    for (int i = helpScroll; i < lines.size() && i < helpScroll + maxLines; i++) {
      String line = lines.get(i);
      boolean heading = line.endsWith(":") || line.startsWith("CSM ASC-3");
      fontRenderer.drawString(line, lcdX, y, heading ? COLOR_AMBER_HEAD : COLOR_AMBER);
      y += lineH;
    }
    if (lines.size() > maxLines) {
      fontRenderer.drawString(
          "▲▼ / +- scroll   (" + (helpScroll + 1) + "-"
              + Math.min(lines.size(), helpScroll + maxLines) + " of " + lines.size() + ")",
          lcdX, areaBottom + 2, COLOR_AMBER_DIM);
    }
  }

  /** The help paragraphs word-wrapped to the current LCD width (blank entries stay blank lines). */
  private java.util.List<String> wrappedHelpLines() {
    java.util.List<String> out = new java.util.ArrayList<>();
    for (String para : HELP_PARAGRAPHS) {
      if (para.isEmpty()) {
        out.add("");
      } else {
        out.addAll(fontRenderer.listFormattedStringToWidth(para, lcdW));
      }
    }
    return out;
  }

  /** Registers hover help over the screen-select and keypad buttons (static positions). */
  private void registerButtonHelps() {
    addButtonHelp(BTN_SCREEN_BASE, "STATUS",
        "Live overview: mode, ring/barrier diagram, coordination",
        "summary and preempt count. Start here.");
    addButtonHelp(BTN_SCREEN_BASE + 1, "TIMING",
        "Per-phase intervals: min/max green, passage, yellow,",
        "red clearance, walk and ped clearance (in seconds).");
    addButtonHelp(BTN_SCREEN_BASE + 2, "MAP",
        "Map each phase to a circuit and movement, and set its",
        "recall / ped-recall behavior.");
    addButtonHelp(BTN_SCREEN_BASE + 3, "COORD",
        "Coordination: free vs. fixed cycle, cycle length, offset",
        "and per-phase splits for green-wave timing.");
    addButtonHelp(BTN_SCREEN_BASE + 4, "PREEMPT",
        "Railroad / emergency / transit preemption sequences",
        "and the phases they clear, hold and exit through.");
    addButtonHelp(BTN_SCREEN_BASE + 5, "ACT",
        "Actuation / volume-density timing: Max 2, bike min green,",
        "added/max initial, and gap reduction (min gap, TB4, TTR).");
    addButtonHelp(BTN_SCREEN_BASE + 6, "OVL",
        "Phase-based vehicle overlaps (e.g. right-turn overlaps): a",
        "circuit+movement that greens with its included phases.");
    addButtonHelp(BTN_SCREEN_BASE + 7, "HELP",
        "Quick reference: common setups, editing, and how to link",
        "and program FYA left turns. Arrows / +- scroll.");
    addButtonHelp(BTN_TEMPLATE, "Load Std 8-Phase",
        "Overwrites the plan with a standard NEMA 8-phase dual-ring",
        "layout and auto-assigns phases to your circuits by approach.",
        "The quickest way to get a working ADVANCED program.");
    addButtonHelp(BTN_PE_ADD, "Add Preempt", "Add a new preemption sequence to the list.");
    addButtonHelp(BTN_PE_REMOVE, "Remove Preempt", "Delete the currently selected preempt.");
    addButtonHelp(BTN_PE_PREV, "Previous Preempt", "Select the previous preempt in the list.");
    addButtonHelp(BTN_PE_NEXT, "Next Preempt", "Select the next preempt in the list.");
    addButtonHelp(BTN_ENT, "ENTER",
        "On a time field: set the typed value. On a list/toggle field:",
        "advance to the next option.");
    addButtonHelp(BTN_PLUS, "Increment", "Increase the selected value, or cycle a list forward.");
    addButtonHelp(BTN_MINUS, "Decrement", "Decrease the selected value, or cycle a list backward.");
    addButtonHelp(BTN_CLR, "Clear", "Discard the number currently being typed.");
    addButtonHelp(BTN_UP, "Move Up", "Move the selection to the previous field.");
    addButtonHelp(BTN_DOWN, "Move Down", "Move the selection to the next field.");
    addButtonHelp(BTN_LEFT, "Move Left", "Move the selection to the previous field.");
    addButtonHelp(BTN_RIGHT, "Move Right", "Move the selection to the next field.");
    addButtonHelp(BTN_CLOSE, "Close", "Close the ASC-3 programmer.");
  }

  /** Draws the help box for the first region under the (design-space) cursor, if any. */
  private void drawTooltips(int mx, int my) {
    for (Help h : helps) {
      if (mx >= h.x && mx <= h.x + h.w && my >= h.y && my <= h.y + h.h) {
        drawTip(mx, my, h.lines);
        return;
      }
    }
  }

  private void drawTip(int mx, int my, String[] lines) {
    int textW = 0;
    for (String line : lines) {
      textW = Math.max(textW, fontRenderer.getStringWidth(line));
    }
    int boxW = textW + 8;
    int boxH = lines.length * 10 + 4;
    int x = mx + 10;
    int y = my + 8;
    if (x + boxW > left + W - 2) {
      x = mx - 10 - boxW;
    }
    if (x < left + 2) {
      x = left + 2;
    }
    if (y + boxH > top + H - 2) {
      y = (top + H - 2) - boxH;
    }
    if (y < top + 2) {
      y = top + 2;
    }
    drawRect(x - 1, y - 1, x + boxW + 1, y + boxH + 1, 0xF00A0F0A);
    drawRect(x - 1, y - 1, x + boxW + 1, y, COLOR_AMBER_DIM);
    drawRect(x - 1, y + boxH, x + boxW + 1, y + boxH + 1, COLOR_AMBER_DIM);
    drawRect(x - 1, y - 1, x, y + boxH + 1, COLOR_AMBER_DIM);
    drawRect(x + boxW, y - 1, x + boxW + 1, y + boxH + 1, COLOR_AMBER_DIM);
    for (int i = 0; i < lines.length; i++) {
      fontRenderer.drawString(lines[i], x + 4, y + 3 + i * 10,
          i == 0 ? COLOR_AMBER_HEAD : COLOR_AMBER);
    }
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
