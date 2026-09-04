package com.micatechnologies.minecraft.csm.lifesafety;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

/**
 * "CSM 4100" -- a fire alarm control panel front-panel GUI. A light enclosure with a red header,
 * an amber-on-black alphanumeric display, a column of status lamps and a row of membrane keys,
 * modelled on a commercial addressable FACP and styled as a sibling of the ASC-3 signal
 * controller programmer.
 *
 * <p>The display reports the system state and a live census of the linked notification
 * appliances; the keys perform ACKNOWLEDGE, SIGNAL SILENCE / RESOUND, SYSTEM RESET, DRILL and
 * LAMP TEST; and the list picks the voice evacuation message directly instead of cycling through
 * seventeen of them one press at a time. Every key press is sent to the server as a
 * {@link FireAlarmPanelConfigPacket}; the panel state is read back off the (client-synced) tile
 * entity.
 *
 * @author Mica Technologies
 * @since 2026.8
 */
@SideOnly(Side.CLIENT)
public class FireAlarmControlPanelGui extends GuiScreen {

  // Enclosure.
  private static final int COLOR_BODY = 0xFFD5D2CA;
  private static final int COLOR_BODY_EDGE = 0xFF6E6C65;
  private static final int COLOR_BODY_SHADE = 0xFFBFBCB4;
  private static final int COLOR_HEADER = 0xFFA81F1F;
  private static final int COLOR_HEADER_EDGE = 0xFF751515;
  private static final int COLOR_HEADER_TEXT = 0xFFF6ECEC;
  private static final int COLOR_LABEL = 0xFF2B2A26;
  private static final int COLOR_LABEL_DIM = 0xFF6A675F;

  // Display.
  private static final int COLOR_LCD_BG = 0xFF0D0A03;
  private static final int COLOR_LCD_EDGE = 0xFF44403A;
  private static final int COLOR_AMBER = 0xFFFFB000;
  private static final int COLOR_AMBER_HEAD = 0xFFFFD27A;
  private static final int COLOR_AMBER_DIM = 0xFF8A6210;
  private static final int COLOR_LCD_ALARM = 0xFFFF5B4A;
  private static final int COLOR_ROW_SEL = 0xFF5A3A00;

  // Lamps.
  private static final int COLOR_LAMP_BEZEL = 0xFF3A3833;
  private static final int COLOR_LAMP_RED = 0xFFFF3B30;
  private static final int COLOR_LAMP_RED_OFF = 0xFF5E3A36;
  private static final int COLOR_LAMP_AMBER = 0xFFFFB000;
  private static final int COLOR_LAMP_AMBER_OFF = 0xFF5E4C2C;
  private static final int COLOR_LAMP_GREEN = 0xFF3CE03C;
  private static final int COLOR_LAMP_GREEN_OFF = 0xFF2F4A2F;

  // Membrane keys.
  private static final int COLOR_KEY_FACE = 0xFFEDEAE3;
  private static final int COLOR_KEY_HOVER = 0xFFFFFDF6;
  private static final int COLOR_KEY_DISABLED = 0xFFC6C3BC;
  private static final int COLOR_KEY_EDGE = 0xFF56544E;
  private static final int COLOR_KEY_HILIGHT = 0xFFFFFFFF;
  private static final int COLOR_KEY_LEGEND = 0xFF23221F;
  private static final int COLOR_KEY_LEGEND_RED = 0xFF9E1B1B;
  private static final int COLOR_KEY_LEGEND_OFF = 0xFF8E8B85;

  private static final int BTN_ACK = 0;
  private static final int BTN_SILENCE = 1;
  private static final int BTN_RESET = 2;
  private static final int BTN_DRILL = 3;
  private static final int BTN_LAMP_TEST = 4;
  private static final int BTN_GLITCH = 5;
  private static final int BTN_CLOSE = 6;

  /** Fixed design space; the whole panel is scaled about the screen centre to fit. */
  private static final int W = 372;
  private static final int H = 238;
  private static final float PANEL_SCALE = 0.9f;

  private static final int VISIBLE_ROWS = 6;
  private static final int ROW_HEIGHT = 11;
  private static final int LAMP_SPACING = 13;
  /** Ticks a lamp test holds every indicator lit -- long enough to see, short enough to not nag. */
  private static final int LAMP_TEST_TICKS = 60;
  /** Ticks an optimistic message selection is displayed before deferring to the synced state. */
  private static final int PENDING_TICKS = 40;

  private final TileEntityFireAlarmControlPanel panel;
  private final BlockPos blockPos;
  private final List<Hover> hovers = new ArrayList<>();

  private int left;
  private int top;
  private int lcdX;
  private int lcdY;
  private int lcdW;
  private int lcdH;
  private int lampX;
  private int lampY;
  private int listX;
  private int listY;
  private int listW;
  private int listH;

  private int ticks;
  private int scroll;
  private int lampTestTicks;
  private int pendingSoundIndex = -1;
  private int pendingTicks;

  // Appliance census, recomputed on a slow cadence rather than every frame.
  private int deviceTotal;
  private int deviceSpeakers;
  private int deviceHorns;
  private int deviceStrobes;
  private int deviceUnloaded;
  private int deviceMissing;

  public FireAlarmControlPanelGui(TileEntityFireAlarmControlPanel panel) {
    this.panel = panel;
    this.blockPos = panel.getPos();
  }

  @Override
  public void initGui() {
    buttonList.clear();
    left = (width - W) / 2;
    top = (height - H) / 2;

    lcdX = left + 10;
    lcdY = top + 26;
    lcdW = W - 20 - 116;
    lcdH = 62;
    lampX = left + W - 112;
    lampY = top + 30;
    listX = left + 10;
    listY = top + 108;
    listW = W - 20;
    listH = VISIBLE_ROWS * ROW_HEIGHT + 6;

    int keyY = listY + listH + 10;
    int keyH = 18;
    int keyGap = 5;
    int keyW = (W - 20 - keyGap * 4) / 5;
    int keyX = left + 10;
    buttonList.add(new PanelKey(BTN_ACK, keyX, keyY, keyW, keyH, "ACK", COLOR_KEY_LEGEND));
    buttonList.add(new PanelKey(BTN_SILENCE, keyX + (keyW + keyGap), keyY, keyW, keyH,
        "SILENCE", COLOR_KEY_LEGEND));
    buttonList.add(new PanelKey(BTN_RESET, keyX + (keyW + keyGap) * 2, keyY, keyW, keyH,
        "RESET", COLOR_KEY_LEGEND_RED));
    buttonList.add(new PanelKey(BTN_DRILL, keyX + (keyW + keyGap) * 3, keyY, keyW, keyH,
        "DRILL", COLOR_KEY_LEGEND_RED));
    buttonList.add(new PanelKey(BTN_LAMP_TEST, keyX + (keyW + keyGap) * 4, keyY, keyW, keyH,
        "LAMP TEST", COLOR_KEY_LEGEND));

    int stripY = keyY + keyH + 6;
    buttonList.add(new PanelKey(BTN_GLITCH, left + 10, stripY, 150, 16, "", COLOR_KEY_LEGEND));
    buttonList.add(new PanelKey(BTN_CLOSE, left + W - 10 - 70, stripY, 70, 16, "CLOSE",
        COLOR_KEY_LEGEND));

    scrollToSelection();
    recomputeCensus();
  }

  @Override
  public void updateScreen() {
    super.updateScreen();
    ticks++;
    if (lampTestTicks > 0) {
      lampTestTicks--;
    }
    if (pendingSoundIndex >= 0) {
      // Drop the optimistic selection once the server's state agrees with it, or if the panel
      // never came back with it (a rejected index, or a lost packet) so the display cannot keep
      // claiming a message the panel is not actually playing.
      if (panel.getSoundIndex() == pendingSoundIndex || --pendingTicks <= 0) {
        pendingSoundIndex = -1;
      }
    }
    if (panel.isInvalid()) {
      mc.displayGuiScreen(null);
      return;
    }
    if (ticks % 20 == 0) {
      recomputeCensus();
    }
  }

  /**
   * Counts the linked appliances by kind, using the same classification the panel's tile entity
   * uses when it dispatches sound. Positions in unloaded chunks are counted separately rather than
   * reported as missing -- a client cannot tell an unloaded appliance from a removed one, and
   * calling a far-away speaker a fault would light TROUBLE on a healthy system.
   */
  private void recomputeCensus() {
    deviceTotal = 0;
    deviceSpeakers = 0;
    deviceHorns = 0;
    deviceStrobes = 0;
    deviceUnloaded = 0;
    deviceMissing = 0;

    World world = panel.getWorld();
    if (world == null) {
      return;
    }
    for (BlockPos pos : panel.getConnectedAppliances()) {
      deviceTotal++;
      if (!world.isBlockLoaded(pos)) {
        deviceUnloaded++;
        continue;
      }
      IBlockState state = world.getBlockState(pos);
      Block block = state.getBlock();
      if (block instanceof IStrobeBlock) {
        deviceStrobes++;
      }
      if (block instanceof AbstractBlockFireAlarmSounderVoiceEvac) {
        deviceSpeakers++;
      } else if (block instanceof AbstractBlockFireAlarmSounder) {
        if (((AbstractBlockFireAlarmSounder) block).getSoundResourceName(state) != null) {
          deviceHorns++;
        }
      } else {
        deviceMissing++;
      }
    }
  }

  private boolean hasTrouble() {
    return deviceMissing > 0 || deviceTotal == 0;
  }

  private int displayedSoundIndex() {
    return pendingSoundIndex >= 0 ? pendingSoundIndex : panel.getSoundIndex();
  }

  private void scrollToSelection() {
    int selected = displayedSoundIndex();
    if (selected < scroll) {
      scroll = selected;
    } else if (selected >= scroll + VISIBLE_ROWS) {
      scroll = selected - VISIBLE_ROWS + 1;
    }
    scroll = Math.max(0, Math.min(maxScroll(), scroll));
  }

  private int maxScroll() {
    return Math.max(0, TileEntityFireAlarmControlPanel.getSoundNames().length - VISIBLE_ROWS);
  }

  private void send(FireAlarmPanelConfigAction action) {
    CsmLifeSafety.NETWORK.sendToServer(new FireAlarmPanelConfigPacket(blockPos, action.ordinal()));
  }

  private void send(FireAlarmPanelConfigAction action, int value) {
    CsmLifeSafety.NETWORK.sendToServer(new FireAlarmPanelConfigPacket(blockPos, action.ordinal(), value));
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    switch (button.id) {
      case BTN_ACK:
        send(FireAlarmPanelConfigAction.ACKNOWLEDGE);
        break;
      case BTN_SILENCE:
        send(panel.getAudibleSilence() ? FireAlarmPanelConfigAction.RESOUND
            : FireAlarmPanelConfigAction.AUDIBLE_SILENCE);
        break;
      case BTN_RESET:
        send(FireAlarmPanelConfigAction.RESET_PANEL);
        break;
      case BTN_DRILL:
        send(FireAlarmPanelConfigAction.DRILL);
        break;
      case BTN_LAMP_TEST:
        lampTestTicks = LAMP_TEST_TICKS;
        break;
      case BTN_GLITCH:
        send(FireAlarmPanelConfigAction.TOGGLE_GLITCHY);
        break;
      case BTN_CLOSE:
        mc.displayGuiScreen(null);
        break;
      default:
        break;
    }
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    int mx = toDesignX(mouseX);
    int my = toDesignY(mouseY);
    super.mouseClicked(mx, my, mouseButton);
    if (mouseButton != 0) {
      return;
    }
    String[] names = TileEntityFireAlarmControlPanel.getSoundNames();
    for (int row = 0; row < VISIBLE_ROWS; row++) {
      int index = scroll + row;
      if (index >= names.length) {
        break;
      }
      int rowY = listY + 3 + row * ROW_HEIGHT;
      if (mx >= listX + 3 && mx <= listX + listW - 3 && my >= rowY && my < rowY + ROW_HEIGHT) {
        if (index != displayedSoundIndex()) {
          pendingSoundIndex = index;
          pendingTicks = PENDING_TICKS;
          send(FireAlarmPanelConfigAction.SET_VOICE_EVAC_SOUND, index);
        }
        return;
      }
    }
  }

  @Override
  public void handleMouseInput() throws IOException {
    super.handleMouseInput();
    int wheel = Mouse.getEventDWheel();
    if (wheel != 0) {
      scroll = Math.max(0, Math.min(maxScroll(), scroll + (wheel > 0 ? -1 : 1)));
    }
  }

  private int toDesignX(int screenX) {
    return Math.round((screenX - width / 2f) / PANEL_SCALE + width / 2f);
  }

  private int toDesignY(int screenY) {
    return Math.round((screenY - height / 2f) / PANEL_SCALE + height / 2f);
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    hovers.clear();

    // The dimmed background stays unscaled; the panel itself is drawn (and mouse-picked) in the
    // fixed W x H design space so it fits at any GUI scale.
    final float cx = width / 2f;
    final float cy = height / 2f;
    GlStateManager.pushMatrix();
    GlStateManager.translate(cx, cy, 0f);
    GlStateManager.scale(PANEL_SCALE, PANEL_SCALE, 1f);
    GlStateManager.translate(-cx, -cy, 0f);

    drawEnclosure();
    drawDisplay();
    drawLamps();
    drawMessageList();
    updateKeys();

    int dmx = toDesignX(mouseX);
    int dmy = toDesignY(mouseY);
    super.drawScreen(dmx, dmy, partialTicks);
    drawHovers(dmx, dmy);

    GlStateManager.popMatrix();
  }

  private void drawEnclosure() {
    drawRect(left - 2, top - 2, left + W + 2, top + H + 2, COLOR_BODY_EDGE);
    drawRect(left, top, left + W, top + H, COLOR_BODY);
    // Red header strip carrying the model name, as on the real cabinet's door label.
    drawRect(left, top, left + W, top + 18, COLOR_HEADER);
    drawRect(left, top + 18, left + W, top + 19, COLOR_HEADER_EDGE);
    fontRenderer.drawString("CSM 4100", left + 8, top + 5, COLOR_HEADER_TEXT);
    String subtitle = "FIRE ALARM CONTROL PANEL";
    fontRenderer.drawString(subtitle, left + W - 8 - fontRenderer.getStringWidth(subtitle),
        top + 5, COLOR_HEADER_TEXT);
    // Silk-screened label above the message list.
    drawRect(left + 6, listY - 14, left + W - 6, listY - 13, COLOR_BODY_SHADE);
    fontRenderer.drawString("VOICE EVACUATION MESSAGE", left + 10, listY - 11, COLOR_LABEL);
    String position = "PANEL " + blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ();
    fontRenderer.drawString(position, left + W - 10 - fontRenderer.getStringWidth(position),
        listY - 11, COLOR_LABEL_DIM);
  }

  private void drawDisplay() {
    drawRect(lcdX - 3, lcdY - 3, lcdX + lcdW + 3, lcdY + lcdH + 3, COLOR_LCD_EDGE);
    drawRect(lcdX - 1, lcdY - 1, lcdX + lcdW + 1, lcdY + lcdH + 1, COLOR_LCD_BG);

    boolean alarm = panel.getAlarmState();
    boolean silenced = panel.getAudibleSilence();
    boolean flash = (ticks / 6) % 2 == 0;
    int y = lcdY + 2;

    String headline;
    int headlineColor;
    if (alarm && panel.getDrill()) {
      headline = "*** EVACUATION DRILL ***";
      headlineColor = COLOR_AMBER_HEAD;
    } else if (alarm && silenced) {
      headline = "*** ALARM - SIGNALS SILENCED ***";
      headlineColor = COLOR_AMBER;
    } else if (alarm) {
      headline = "*** FIRE ALARM ***";
      // An unacknowledged alarm blinks its headline, the way an unacknowledged point does on a
      // real panel; acknowledging settles it to steady without touching the appliances.
      headlineColor = (panel.getAcknowledged() || flash) ? COLOR_LCD_ALARM : COLOR_AMBER_DIM;
    } else if (panel.getAlarmStormState()) {
      headline = "** SUPERVISORY - STORM WARNING **";
      headlineColor = COLOR_AMBER;
    } else {
      headline = ">>> SYSTEM IS NORMAL <<<";
      headlineColor = COLOR_AMBER_HEAD;
    }
    fontRenderer.drawString(headline, lcdX + 3, y, headlineColor);
    y += 13;

    fontRenderer.drawString("DEVICES " + deviceTotal + "   SPKR " + deviceSpeakers
        + "   HORN " + deviceHorns + "   STRB " + deviceStrobes, lcdX + 3, y, COLOR_AMBER);
    y += 11;

    int soundIndex = displayedSoundIndex();
    String[] names = TileEntityFireAlarmControlPanel.getSoundNames();
    int safeIndex = Math.max(0, Math.min(names.length - 1, soundIndex));
    String messageLine = "MSG " + (safeIndex + 1) + "/" + names.length + "  " + names[safeIndex];
    fontRenderer.drawString(trim(messageLine, lcdW - 6), lcdX + 3, y, COLOR_AMBER);
    y += 11;

    // Where the alarm came from, when a device reported it. Coordinates come before the device
    // name so that trimming a long name to the display width never costs the location, which is
    // the part worth walking to.
    String origin = originLine();
    if (origin != null) {
      fontRenderer.drawString(trim(origin, lcdW - 6), lcdX + 3, y, COLOR_AMBER_HEAD);
      y += 11;
    }

    String detail;
    int detailColor = COLOR_AMBER_DIM;
    if (deviceTotal == 0) {
      detail = "TROUBLE: NO APPLIANCES LINKED";
      detailColor = COLOR_AMBER;
    } else if (deviceMissing > 0) {
      detail = "TROUBLE: " + deviceMissing + " LINKED DEVICE"
          + (deviceMissing == 1 ? "" : "S") + " MISSING";
      detailColor = COLOR_AMBER;
    } else if (alarm && panel.getAcknowledged()) {
      detail = "ALARM ACKNOWLEDGED";
    } else if (deviceUnloaded > 0) {
      detail = deviceUnloaded + " DEVICE" + (deviceUnloaded == 1 ? "" : "S") + " NOT LOADED";
    } else {
      detail = "ALL POINTS NORMAL";
    }
    fontRenderer.drawString(trim(detail, lcdW - 6), lcdX + 3, y, detailColor);

    addHover(lcdX - 3, lcdY - 3, lcdW + 6, lcdH + 6, "System Display",
        "Live panel state, the linked appliance census,",
        "and the selected voice evacuation message.");
  }

  /**
   * The annunciation line naming the device that raised the alarm, or {@code null} when there is
   * nothing to annunciate -- no alarm, or an alarm with no reporting device (a drill, redstone,
   * or an activation from outside the mod).
   */
  private String originLine() {
    BlockPos origin = panel.getAlarmOriginPos();
    if (!panel.getAlarmState() || origin == null) {
      return null;
    }
    String line = "FROM " + origin.getX() + ", " + origin.getY() + ", " + origin.getZ();
    String registryName = panel.getAlarmOriginName();
    if (registryName == null || registryName.isEmpty()) {
      return line;
    }
    Block block = Block.getBlockFromName(registryName);
    if (block == null) {
      return line;
    }
    // Device names run longer than the display, so the name is elided to whatever room is left
    // after the coordinates rather than letting the whole line be cut mid-word.
    int room = lcdW - 6 - fontRenderer.getStringWidth(line + "  ");
    String name = elide(block.getLocalizedName(), room);
    return name.isEmpty() ? line : line + "  " + name;
  }

  /**
   * Shortens text to fit a pixel width, marking it with an ellipsis when anything was dropped.
   * Returns an empty string when there is not even room for the ellipsis.
   */
  private String elide(String text, int maxPx) {
    if (fontRenderer.getStringWidth(text) <= maxPx) {
      return text;
    }
    String ellipsis = "...";
    int budget = maxPx - fontRenderer.getStringWidth(ellipsis);
    if (budget <= 0) {
      return "";
    }
    return trim(text, budget) + ellipsis;
  }

  private void drawLamps() {
    boolean test = lampTestTicks > 0;
    boolean flash = (ticks / 6) % 2 == 0;
    boolean alarm = panel.getAlarmState();
    // Unacknowledged alarms flash; acknowledging holds the lamp steady.
    boolean alarmLit = alarm && (panel.getAcknowledged() || flash);

    int y = lampY;
    lamp(lampX, y, test || alarmLit, COLOR_LAMP_RED, COLOR_LAMP_RED_OFF, "FIRE ALARM",
        "FIRE ALARM", "Lit red while the panel is in alarm. Flashes",
        "until the alarm is acknowledged, then holds steady.");
    y += LAMP_SPACING;
    lamp(lampX, y, test || panel.getAlarmStormState(), COLOR_LAMP_AMBER, COLOR_LAMP_AMBER_OFF,
        "SUPERVISORY", "SUPERVISORY",
        "Lit while the panel has redstone power, which drives",
        "the storm warning announcement over the speakers.");
    y += LAMP_SPACING;
    lamp(lampX, y, test || hasTrouble(), COLOR_LAMP_AMBER, COLOR_LAMP_AMBER_OFF, "TROUBLE",
        "TROUBLE", "Lit when a linked appliance is no longer there,",
        "or when nothing is linked to this panel at all.");
    y += LAMP_SPACING;
    lamp(lampX, y, test || panel.getAudibleSilence(), COLOR_LAMP_AMBER, COLOR_LAMP_AMBER_OFF,
        "SIGNALS SILENCED", "SIGNALS SILENCED",
        "Lit while horns and speakers are silenced but the",
        "strobes keep flashing. Press RESOUND to start them again.");
    y += LAMP_SPACING;
    lamp(lampX, y, true, COLOR_LAMP_GREEN, COLOR_LAMP_GREEN_OFF, "AC POWER",
        "AC POWER", "Lit whenever the panel is powered.");
  }

  private void lamp(int x, int y, boolean lit, int onColor, int offColor, String label,
      String... help) {
    drawRect(x - 1, y - 1, x + 9, y + 9, COLOR_LAMP_BEZEL);
    drawRect(x, y, x + 8, y + 8, lit ? onColor : offColor);
    if (lit) {
      // A single bright corner reads as a lens catching the light.
      drawRect(x + 1, y + 1, x + 3, y + 3, 0x88FFFFFF);
    }
    fontRenderer.drawString(label, x + 13, y, lit ? COLOR_LABEL : COLOR_LABEL_DIM);
    addHover(x - 1, y - 1, 108, 10, help);
  }

  private void drawMessageList() {
    drawRect(listX - 2, listY - 2, listX + listW + 2, listY + listH + 2, COLOR_LCD_EDGE);
    drawRect(listX, listY, listX + listW, listY + listH, COLOR_LCD_BG);

    String[] names = TileEntityFireAlarmControlPanel.getSoundNames();
    int selected = displayedSoundIndex();
    for (int row = 0; row < VISIBLE_ROWS; row++) {
      int index = scroll + row;
      if (index >= names.length) {
        break;
      }
      int rowY = listY + 3 + row * ROW_HEIGHT;
      boolean isSelected = index == selected;
      if (isSelected) {
        drawRect(listX + 3, rowY - 1, listX + listW - 9, rowY + ROW_HEIGHT - 2, COLOR_ROW_SEL);
      }
      String number = (index + 1 < 10 ? "0" : "") + (index + 1);
      fontRenderer.drawString(number, listX + 6, rowY,
          isSelected ? COLOR_AMBER_HEAD : COLOR_AMBER_DIM);
      fontRenderer.drawString(trim(names[index], listW - 40), listX + 24, rowY,
          isSelected ? COLOR_AMBER_HEAD : COLOR_AMBER);
      if (isSelected) {
        fontRenderer.drawString("<", listX + listW - 16, rowY, COLOR_AMBER_HEAD);
      }
    }

    // Scroll track and thumb.
    int trackX = listX + listW - 6;
    drawRect(trackX, listY + 3, trackX + 3, listY + listH - 3, 0xFF2A2620);
    int span = listH - 6;
    int thumbH = Math.max(8, span * VISIBLE_ROWS / names.length);
    int thumbY = listY + 3 + (maxScroll() == 0 ? 0 : (span - thumbH) * scroll / maxScroll());
    drawRect(trackX, thumbY, trackX + 3, thumbY + thumbH, COLOR_AMBER_DIM);

    addHover(listX, listY, listW, listH, "Voice Evacuation Message",
        "Click a message to select it. Scroll with the mouse wheel.",
        "The change takes effect immediately, mid-alarm included.");
  }

  /** Refreshes the key legends and enablement against the current panel state. */
  private void updateKeys() {
    boolean alarm = panel.getAlarmState();
    boolean silenced = panel.getAudibleSilence();
    for (GuiButton button : buttonList) {
      switch (button.id) {
        case BTN_ACK:
          button.enabled = alarm && !panel.getAcknowledged();
          addKeyHover(button, "ACKNOWLEDGE",
              "Acknowledges an active alarm so the FIRE ALARM lamp",
              "stops flashing. Does not silence anything.");
          break;
        case BTN_SILENCE:
          button.enabled = alarm;
          button.displayString = silenced ? "RESOUND" : "SILENCE";
          if (silenced) {
            addKeyHover(button, "RESOUND",
                "Lifts the silence and starts the horns and speakers again.");
          } else {
            addKeyHover(button, "SIGNAL SILENCE",
                "Silences the horns and speakers while leaving the",
                "strobes flashing, as a real panel does.");
          }
          break;
        case BTN_RESET:
          button.enabled = alarm;
          addKeyHover(button, "SYSTEM RESET",
              "Clears the alarm and stops every appliance. A pull station",
              "still in its pulled state will simply re-alarm the panel.");
          break;
        case BTN_DRILL:
          button.enabled = !alarm;
          addKeyHover(button, "DRILL",
              "Starts an evacuation drill: every linked appliance sounds",
              "and flashes exactly as it would in a real alarm.");
          break;
        case BTN_LAMP_TEST:
          addKeyHover(button, "LAMP TEST",
              "Lights every panel lamp for three seconds so a dead",
              "indicator can be spotted. Affects nothing outside the panel.");
          break;
        case BTN_GLITCH:
          button.displayString = "AUDIO GLITCH: " + (panel.getGlitchy() ? "ON" : "OFF");
          addKeyHover(button, "AUDIO GLITCH",
              "Makes the voice evacuation message stutter and drop out,",
              "like a failing amplifier. Cosmetic.");
          break;
        case BTN_CLOSE:
          addKeyHover(button, "CLOSE", "Close the panel.");
          break;
        default:
          break;
      }
    }
  }

  private void addKeyHover(GuiButton button, String... lines) {
    addHover(button.x, button.y, button.width, button.height, lines);
  }

  private void addHover(int x, int y, int w, int h, String... lines) {
    hovers.add(new Hover(x, y, w, h, lines));
  }

  /** Draws the help box for the first region under the (design-space) cursor, if any. */
  private void drawHovers(int mx, int my) {
    for (Hover hover : hovers) {
      if (mx >= hover.x && mx <= hover.x + hover.w && my >= hover.y && my <= hover.y + hover.h) {
        drawTip(mx, my, hover.lines);
        return;
      }
    }
  }

  private void drawTip(int mx, int my, String[] lines) {
    int textW = 0;
    int count = 0;
    for (String line : lines) {
      if (line == null || line.isEmpty()) {
        continue;
      }
      textW = Math.max(textW, fontRenderer.getStringWidth(line));
      count++;
    }
    if (count == 0) {
      return;
    }
    int boxW = textW + 8;
    int boxH = count * 10 + 4;
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
    drawRect(x - 1, y - 1, x + boxW + 1, y + boxH + 1, 0xF00D0A03);
    drawRect(x - 1, y - 1, x + boxW + 1, y, COLOR_AMBER_DIM);
    drawRect(x - 1, y + boxH, x + boxW + 1, y + boxH + 1, COLOR_AMBER_DIM);
    drawRect(x - 1, y - 1, x, y + boxH + 1, COLOR_AMBER_DIM);
    drawRect(x + boxW, y - 1, x + boxW + 1, y + boxH + 1, COLOR_AMBER_DIM);
    int line = 0;
    for (String text : lines) {
      if (text == null || text.isEmpty()) {
        continue;
      }
      fontRenderer.drawString(text, x + 4, y + 3 + line * 10,
          line == 0 ? COLOR_AMBER_HEAD : COLOR_AMBER);
      line++;
    }
  }

  private String trim(String text, int maxPx) {
    if (text == null) {
      return "";
    }
    while (text.length() > 0 && fontRenderer.getStringWidth(text) > maxPx) {
      text = text.substring(0, text.length() - 1);
    }
    return text;
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }

  /** A rectangle of the panel that shows a help box while the cursor is over it. */
  private static final class Hover {

    private final int x;
    private final int y;
    private final int w;
    private final int h;
    private final String[] lines;

    private Hover(int x, int y, int w, int h, String[] lines) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      this.lines = lines;
    }
  }

  /**
   * A flat membrane key drawn to match the enclosure. Vanilla's textured button would read as a
   * stone button glued to a fire panel, so the face is drawn directly instead.
   */
  private static final class PanelKey extends GuiButton {

    private final int legendColor;

    private PanelKey(int id, int x, int y, int width, int height, String text, int legendColor) {
      super(id, x, y, width, height, text);
      this.legendColor = legendColor;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
      if (!visible) {
        return;
      }
      hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
      int face = !enabled ? COLOR_KEY_DISABLED : (hovered ? COLOR_KEY_HOVER : COLOR_KEY_FACE);
      drawRect(x, y, x + width, y + height, COLOR_KEY_EDGE);
      drawRect(x + 1, y + 1, x + width - 1, y + height - 1, face);
      drawRect(x + 1, y + 1, x + width - 1, y + 2, COLOR_KEY_HILIGHT);
      drawCenteredString(mc.fontRenderer, displayString, x + width / 2,
          y + (height - 8) / 2, enabled ? legendColor : COLOR_KEY_LEGEND_OFF);
    }
  }
}
