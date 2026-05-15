package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.codeutils.GuiMultiLineTextField;
import java.io.IOException;
import java.util.Random;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Gag desktop GUI shown when a player right-clicks a powered iMac, iMac Pro, or MacBook Pro.
 * Faux operating-system look: top menu bar with the in-game time and day count, a notepad text
 * area whose contents persist on the {@link TileEntityComputer}, a "fortune" button that
 * cycles a small palette of jokey one-liners, and shutdown / save / cancel controls.
 *
 * <p>The clock matches the thermostat's formatting (12-hour, +6 hour Minecraft offset so 0
 * ticks reads as 6:00 AM) so all CSM-OS clocks across the world stay in agreement.</p>
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class ComputerGui extends GuiScreen {

  private static final int BUTTON_ID_SAVE = 0;
  private static final int BUTTON_ID_CANCEL = 1;
  private static final int BUTTON_ID_FORTUNE = 2;
  private static final int BUTTON_ID_SHUTDOWN = 3;

  private static final int COLOR_DESKTOP_BG = 0xFF1B2233;
  private static final int COLOR_MENU_BAR = 0xFFE8ECF2;
  private static final int COLOR_MENU_BAR_TEXT = 0xFF1A1A1A;
  private static final int COLOR_WINDOW_FRAME = 0xFFD8DCE6;
  private static final int COLOR_WINDOW_BG = 0xFFF5F6F8;
  private static final int COLOR_WINDOW_TITLE = 0xFF263043;
  private static final int COLOR_WINDOW_TITLE_TEXT = 0xFFFFFFFF;
  private static final int COLOR_INFO_TEXT = 0xFF2A2F3A;

  private static final String[] FORTUNES = new String[] {
      "Today is a good day to refactor.",
      "You will find a creeper in your basement.",
      "An emerald block is in your future.",
      "All your TPS are belong to us.",
      "Have you tried turning it off and on again?",
      "Beware of falling anvils on Tuesdays.",
      "Your build will be reviewed by a wandering trader.",
      "There is no spoon. There is, however, a sponge.",
      "Patch notes will be longer than expected.",
      "It is pitch dark. You are likely to be eaten by an enderman.",
  };

  private final BlockPos tilePos;
  private final String computerName;
  private final String initialNotepadText;

  private GuiMultiLineTextField notepad;

  private String fortuneText = "Welcome to CSM-OS. Click 'Fortune' for a tip.";
  private final Random fortuneRandom = new Random();
  private int lastFortuneIndex = -1;

  public ComputerGui(TileEntityComputer tile) {
    this.tilePos = tile.getPos();
    this.initialNotepadText = tile.getNotepadText() == null ? "" : tile.getNotepadText();
    this.computerName = computerNameFromBlock(tile);
  }

  private static String computerNameFromBlock(TileEntityComputer tile) {
    if (tile.getWorld() == null) {
      return "CSM-OS";
    }
    String path = "";
    if (tile.getWorld().getBlockState(tile.getPos()).getBlock().getRegistryName() != null) {
      path = tile.getWorld().getBlockState(tile.getPos()).getBlock().getRegistryName().getPath();
    }
    switch (path) {
      case "imac":
        return "iMac — CSM-OS";
      case "imacpro":
        return "iMac Pro — CSM-OS";
      case "mbp":
        return "MacBook Pro — CSM-OS";
      default:
        return "CSM-OS";
    }
  }

  @Override
  public void initGui() {
    Keyboard.enableRepeatEvents(true);
    super.initGui();
    ScaledResolution sr = new ScaledResolution(this.mc);
    int screenW = sr.getScaledWidth();
    int screenH = sr.getScaledHeight();

    int windowW = Math.min(320, screenW - 40);
    int windowH = Math.min(180, screenH - 80);
    int windowX = (screenW - windowW) / 2;
    int windowY = (screenH - windowH) / 2;

    int padding = 8;
    int notepadX = windowX + padding;
    int notepadY = windowY + 28;
    int notepadW = windowW - padding * 2;
    int notepadH = windowH - 28 - padding - 28;

    notepad = new GuiMultiLineTextField(0, fontRenderer, notepadX, notepadY, notepadW, notepadH);
    notepad.setText(initialNotepadText);
    notepad.setFocused(true);

    int btnY = windowY + windowH - 22;
    int btnW = 62;
    int btnGap = 4;
    int btnTotalW = btnW * 4 + btnGap * 3;
    int btnStartX = windowX + (windowW - btnTotalW) / 2;

    this.buttonList.add(new GuiButton(BUTTON_ID_SAVE, btnStartX, btnY, btnW, 20, "Save"));
    this.buttonList.add(new GuiButton(BUTTON_ID_CANCEL,
        btnStartX + (btnW + btnGap), btnY, btnW, 20, "Cancel"));
    this.buttonList.add(new GuiButton(BUTTON_ID_FORTUNE,
        btnStartX + (btnW + btnGap) * 2, btnY, btnW, 20, "Fortune"));
    this.buttonList.add(new GuiButton(BUTTON_ID_SHUTDOWN,
        btnStartX + (btnW + btnGap) * 3, btnY, btnW, 20, "Shutdown"));
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    ScaledResolution sr = new ScaledResolution(this.mc);
    int screenW = sr.getScaledWidth();
    int screenH = sr.getScaledHeight();

    drawRect(0, 0, screenW, screenH, COLOR_DESKTOP_BG);

    int menuBarH = 14;
    drawRect(0, 0, screenW, menuBarH, COLOR_MENU_BAR);
    String leftLabel = "❖ " + computerName;
    fontRenderer.drawString(leftLabel, 6, 3, COLOR_MENU_BAR_TEXT);
    String rightLabel = formatRightMenu();
    int rightWidth = fontRenderer.getStringWidth(rightLabel);
    fontRenderer.drawString(rightLabel, screenW - rightWidth - 6, 3, COLOR_MENU_BAR_TEXT);

    int windowW = Math.min(320, screenW - 40);
    int windowH = Math.min(180, screenH - 80);
    int windowX = (screenW - windowW) / 2;
    int windowY = (screenH - windowH) / 2;
    int titleBarH = 14;
    drawRect(windowX - 1, windowY - 1, windowX + windowW + 1, windowY + windowH + 1,
        COLOR_WINDOW_FRAME);
    drawRect(windowX, windowY, windowX + windowW, windowY + titleBarH, COLOR_WINDOW_TITLE);
    drawRect(windowX, windowY + titleBarH, windowX + windowW, windowY + windowH,
        COLOR_WINDOW_BG);

    fontRenderer.drawString("Notepad", windowX + 6, windowY + 3, COLOR_WINDOW_TITLE_TEXT);
    String fortuneShort = trimToWidth(fortuneText, windowW - 12);
    fontRenderer.drawString(fortuneShort, windowX + 6, windowY + windowH - 36,
        COLOR_INFO_TEXT);

    notepad.drawTextBox();

    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  private String trimToWidth(String s, int maxPx) {
    if (s == null) return "";
    if (fontRenderer.getStringWidth(s) <= maxPx) return s;
    String suffix = "...";
    int suffixW = fontRenderer.getStringWidth(suffix);
    while (s.length() > 0 && fontRenderer.getStringWidth(s) + suffixW > maxPx) {
      s = s.substring(0, s.length() - 1);
    }
    return s + suffix;
  }

  /** Builds the right-aligned menu label: "Day N  •  H:MM AM/PM". */
  private String formatRightMenu() {
    World world = this.mc.world;
    if (world == null) {
      return "";
    }
    long worldTime = world.getWorldTime() % 24000L;
    long totalDays = world.getWorldTime() / 24000L + 1L;
    int hours = (int) ((worldTime / 1000L + 6L) % 24L);
    int minutes = (int) ((worldTime % 1000L) * 60L / 1000L);
    boolean pm = hours >= 12;
    int displayHour = hours % 12;
    if (displayHour == 0) {
      displayHour = 12;
    }
    return String.format("Day %d  •  %d:%02d %s", totalDays, displayHour, minutes,
        pm ? "PM" : "AM");
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    notepad.setFocused(true);
    super.keyTyped(typedChar, keyCode);
    notepad.textboxKeyTyped(typedChar, keyCode);
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    notepad.mouseClicked(mouseX, mouseY, mouseButton);
  }

  @Override
  public void handleMouseInput() throws IOException {
    super.handleMouseInput();
    int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
    int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
    int scrollDelta = Mouse.getEventDWheel();
    notepad.handleMouseInput(mouseX, mouseY, scrollDelta);
  }

  @Override
  @ParametersAreNonnullByDefault
  protected void actionPerformed(GuiButton button) {
    switch (button.id) {
      case BUTTON_ID_SAVE:
        sendNotepad(false);
        this.mc.displayGuiScreen(null);
        break;
      case BUTTON_ID_CANCEL:
        this.mc.displayGuiScreen(null);
        break;
      case BUTTON_ID_FORTUNE:
        cycleFortune();
        break;
      case BUTTON_ID_SHUTDOWN:
        sendNotepad(true);
        this.mc.displayGuiScreen(null);
        break;
      default:
        break;
    }
  }

  private void cycleFortune() {
    int idx;
    do {
      idx = fortuneRandom.nextInt(FORTUNES.length);
    } while (FORTUNES.length > 1 && idx == lastFortuneIndex);
    lastFortuneIndex = idx;
    fortuneText = FORTUNES[idx];
  }

  private void sendNotepad(boolean shutdown) {
    String text = notepad.getText();
    CsmNetwork.sendToServer(new ComputerNotepadPacket(tilePos, text, shutdown));
  }

  @Override
  public void onGuiClosed() {
    Keyboard.enableRepeatEvents(false);
    super.onGuiClosed();
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}
