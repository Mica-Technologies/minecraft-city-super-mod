package com.micatechnologies.minecraft.csm.technology.school;

import com.micatechnologies.minecraft.csm.technology.CsmTechnology;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/**
 * The bell schedule controller's screen: its bells, six a page over two pages, each a 24-hour
 * time of the game day, a tone, on or off and, for an announcement, the words to say; a line
 * showing the time now and the next bell; and buttons to add a bell, ring a tone now, save and
 * cancel.
 *
 * <p>Nothing is sent until Save (the whole schedule, sorted by time on the server) or Ring now
 * (one tone, which leaves the schedule alone). A time that does not read as one is shown in red
 * and Save refuses until it is fixed.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class BellControllerGui extends GuiScreen {

  private static final int WIDTH = 316;
  private static final int HEIGHT = 238;
  private static final int ROWS = 6;
  private static final int ROW_H = 22;

  private static final int ID_PREV = 1;
  private static final int ID_NEXT = 2;
  private static final int ID_ADD = 3;
  private static final int ID_RING_TONE = 4;
  private static final int ID_RING = 5;
  private static final int ID_SAVE = 6;
  private static final int ID_CANCEL = 7;
  private static final int ID_ROW_TONE = 100;
  private static final int ID_ROW_ON = 200;
  private static final int ID_ROW_REMOVE = 300;

  private static final int COLOUR_BG = 0xF0202428;
  private static final int COLOUR_FRAME = 0xFF8A9096;
  private static final int COLOUR_TEXT = 0xFFE8E8E8;
  private static final int COLOUR_DIM = 0xFF9AA0A6;
  private static final int COLOUR_BAD = 0xFFFF6060;

  /** One bell being edited. */
  private static final class Row {

    String time;
    BellTone tone;
    boolean enabled;
    String text;

    Row(String time, BellTone tone, boolean enabled, String text) {
      this.time = time;
      this.tone = tone;
      this.enabled = enabled;
      this.text = text;
    }
  }

  private final TileEntityBellController controller;
  private final List<Row> rows = new ArrayList<>();
  private final List<GuiTextField> timeFields = new ArrayList<>();
  private final List<GuiTextField> textFields = new ArrayList<>();
  private int page;
  private BellTone ringTone = BellTone.BELL;
  private String status = "";
  private int left;
  private int top;

  public BellControllerGui(TileEntityBellController controller) {
    this.controller = controller;
    for (BellSchedule.Entry e : controller.getSchedule().getEntries()) {
      rows.add(new Row(ClockTime.format(e.getMinute()), e.getTone(), e.isEnabled(),
          e.getText()));
    }
  }

  @Override
  public void initGui() {
    Keyboard.enableRepeatEvents(true);
    left = (width - WIDTH) / 2;
    top = (height - HEIGHT) / 2;
    build();
  }

  @Override
  public void onGuiClosed() {
    Keyboard.enableRepeatEvents(false);
  }

  private int pages() {
    return Math.max(1, (rows.size() + ROWS - 1) / ROWS);
  }

  /** Lays out the page's rows and the buttons, after copying back what was typed. */
  private void build() {
    store();
    page = Math.max(0, Math.min(page, pages() - 1));
    buttonList.clear();
    timeFields.clear();
    textFields.clear();
    int y = top + 38;
    for (int i = 0; i < ROWS; i++) {
      int index = page * ROWS + i;
      if (index >= rows.size()) {
        break;
      }
      Row row = rows.get(index);
      GuiTextField time = new GuiTextField(i, fontRenderer, left + 10, y + 2, 38, 16);
      time.setMaxStringLength(5);
      time.setText(row.time);
      timeFields.add(time);
      buttonList.add(new GuiButton(ID_ROW_TONE + i, left + 54, y, 62, 20, row.tone.getLabel()));
      buttonList.add(new GuiButton(ID_ROW_ON + i, left + 120, y, 30, 20,
          row.enabled ? "On" : "Off"));
      GuiTextField text = new GuiTextField(10 + i, fontRenderer, left + 156, y + 2, 126, 16);
      text.setMaxStringLength(BellSchedule.MAX_TEXT);
      text.setText(row.text);
      text.setEnabled(row.tone.speaks());
      textFields.add(text);
      buttonList.add(new GuiButton(ID_ROW_REMOVE + i, left + 288, y, 18, 20, "x"));
      y += ROW_H;
    }
    int by = top + 184;
    GuiButton prev = new GuiButton(ID_PREV, left + 10, by, 20, 20, "<");
    GuiButton next = new GuiButton(ID_NEXT, left + 74, by, 20, 20, ">");
    prev.enabled = page > 0;
    next.enabled = page < pages() - 1;
    buttonList.add(prev);
    buttonList.add(next);
    GuiButton add = new GuiButton(ID_ADD, left + 100, by, 70, 20, "+ Add bell");
    add.enabled = rows.size() < BellSchedule.MAX_ENTRIES;
    buttonList.add(add);
    buttonList.add(new GuiButton(ID_RING_TONE, left + 176, by, 64, 20, ringTone.getLabel()));
    buttonList.add(new GuiButton(ID_RING, left + 242, by, 64, 20, "Ring now"));
    int cy = top + 210;
    buttonList.add(new GuiButton(ID_SAVE, left + WIDTH / 2 - 104, cy, 100, 20, "Save"));
    buttonList.add(new GuiButton(ID_CANCEL, left + WIDTH / 2 + 4, cy, 100, 20, "Cancel"));
  }

  /** Copies the visible fields back into their rows. */
  private void store() {
    for (int i = 0; i < timeFields.size(); i++) {
      int index = page * ROWS + i;
      if (index < rows.size()) {
        rows.get(index).time = timeFields.get(i).getText();
        rows.get(index).text = textFields.get(i).getText();
      }
    }
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    int id = button.id;
    if (id >= ID_ROW_REMOVE) {
      store();
      int index = page * ROWS + (id - ID_ROW_REMOVE);
      if (index < rows.size()) {
        rows.remove(index);
      }
      timeFields.clear();
      textFields.clear();
      build();
      return;
    }
    if (id >= ID_ROW_ON) {
      Row row = rows.get(page * ROWS + (id - ID_ROW_ON));
      row.enabled = !row.enabled;
      build();
      return;
    }
    if (id >= ID_ROW_TONE) {
      Row row = rows.get(page * ROWS + (id - ID_ROW_TONE));
      row.tone = row.tone.next();
      build();
      return;
    }
    switch (id) {
      case ID_PREV:
        store();
        timeFields.clear();
        textFields.clear();
        page--;
        build();
        break;
      case ID_NEXT:
        store();
        timeFields.clear();
        textFields.clear();
        page++;
        build();
        break;
      case ID_ADD:
        store();
        timeFields.clear();
        textFields.clear();
        rows.add(new Row(suggestTime(), BellTone.BELL, true, ""));
        page = pages() - 1;
        build();
        break;
      case ID_RING_TONE:
        ringTone = ringTone == BellTone.CHIME ? BellTone.BELL : ringTone.next();
        build();
        break;
      case ID_RING:
        CsmTechnology.NETWORK.sendToServer(
            BellScheduleUpdatePacket.ring(controller.getPos(), ringTone));
        status = "Rang " + ringTone.getLabel().toLowerCase() + ".";
        break;
      case ID_SAVE:
        save();
        break;
      case ID_CANCEL:
        mc.displayGuiScreen(null);
        break;
      default:
        break;
    }
  }

  /** A new bell's time: fifty minutes after the last one, as a class period runs. */
  private String suggestTime() {
    int last = -1;
    for (Row r : rows) {
      last = Math.max(last, BellSchedule.parseTime(r.time));
    }
    return ClockTime.format(last < 0 ? 8 * 60 : last + 50);
  }

  private void save() {
    store();
    List<BellSchedule.Entry> entries = new ArrayList<>();
    for (Row r : rows) {
      int minute = BellSchedule.parseTime(r.time);
      if (minute < 0) {
        status = "Fix the times shown in red (24-hour, e.g. 8:50 or 13:05).";
        return;
      }
      entries.add(new BellSchedule.Entry(minute, r.tone, r.text, r.enabled));
    }
    CsmTechnology.NETWORK.sendToServer(
        BellScheduleUpdatePacket.save(controller.getPos(), new BellSchedule(entries)));
    mc.displayGuiScreen(null);
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    for (GuiTextField f : timeFields) {
      if (f.textboxKeyTyped(typedChar, keyCode)) {
        return;
      }
    }
    for (GuiTextField f : textFields) {
      if (f.textboxKeyTyped(typedChar, keyCode)) {
        return;
      }
    }
    super.keyTyped(typedChar, keyCode);
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    for (GuiTextField f : timeFields) {
      f.mouseClicked(mouseX, mouseY, mouseButton);
    }
    for (GuiTextField f : textFields) {
      f.mouseClicked(mouseX, mouseY, mouseButton);
    }
  }

  @Override
  public void updateScreen() {
    for (GuiTextField f : timeFields) {
      f.updateCursorCounter();
    }
    for (GuiTextField f : textFields) {
      f.updateCursorCounter();
    }
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    drawRect(left - 1, top - 1, left + WIDTH + 1, top + HEIGHT + 1, COLOUR_FRAME);
    drawRect(left, top, left + WIDTH, top + HEIGHT, COLOUR_BG);
    fontRenderer.drawString("Micaplex Bell Schedule", left + 10, top + 8, COLOUR_TEXT);
    World world = controller.getWorld();
    if (world != null) {
      int now = ClockTime.minuteOfDay(world.getWorldTime());
      store();
      BellSchedule.Entry next = preview().next(now);
      String clock = "Now " + ClockTime.format(now) + (next == null ? "   No bells on"
          : "   Next " + ClockTime.format(next.getMinute()) + " "
              + next.getTone().getLabel().toLowerCase());
      fontRenderer.drawString(clock, left + WIDTH - 10 - fontRenderer.getStringWidth(clock),
          top + 8, COLOUR_DIM);
    }
    fontRenderer.drawString("Time", left + 10, top + 26, COLOUR_DIM);
    fontRenderer.drawString("Tone", left + 56, top + 26, COLOUR_DIM);
    fontRenderer.drawString("Ring", left + 122, top + 26, COLOUR_DIM);
    fontRenderer.drawString("Announcement (spoken)", left + 158, top + 26, COLOUR_DIM);
    for (GuiTextField f : timeFields) {
      f.setTextColor(BellSchedule.parseTime(f.getText()) < 0 ? COLOUR_BAD : 0xE0E0E0);
      f.drawTextBox();
    }
    for (GuiTextField f : textFields) {
      f.drawTextBox();
    }
    if (rows.isEmpty()) {
      fontRenderer.drawString("No bells yet: add one, then save.", left + 10, top + 44,
          COLOUR_DIM);
    }
    fontRenderer.drawString((page + 1) + "/" + pages(), left + 46, top + 190,
        COLOUR_DIM);
    if (!status.isEmpty()) {
      fontRenderer.drawString(status, left + 10, top + 172, COLOUR_DIM);
    }
    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  /** The schedule as typed so far, ignoring times that do not read, for the next-bell line. */
  private BellSchedule preview() {
    List<BellSchedule.Entry> entries = new ArrayList<>();
    for (Row r : rows) {
      int minute = BellSchedule.parseTime(r.time);
      if (minute >= 0) {
        entries.add(new BellSchedule.Entry(minute, r.tone, "", r.enabled));
      }
    }
    return new BellSchedule(entries);
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}
