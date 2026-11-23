package com.micatechnologies.minecraft.csm.technology.radio;

import com.micatechnologies.minecraft.csm.technology.CsmTechnology;
import java.io.IOException;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Keyboard;

/**
 * A handheld radio's screen: the radio's own face, drawn in its make's colours, with its display
 * showing the preset it is on and keys for power, channel, volume, squelch and whatever else the
 * make has (a privacy code, scan, a unit ID).
 *
 * <p>The keys change a local copy of the settings. One {@link RadioSettingsPacket} goes to the
 * server when the screen closes, and only if something changed.</p>
 *
 * @since 2026.11
 */
public class RadioGui extends GuiScreen {

  private static final int PANEL_WIDTH = 200;
  private static final int PANEL_HEIGHT = 196;

  private static final int BUTTON_POWER = 0;
  private static final int BUTTON_CHANNEL_DOWN = 1;
  private static final int BUTTON_CHANNEL_UP = 2;
  private static final int BUTTON_VOLUME_DOWN = 3;
  private static final int BUTTON_VOLUME_UP = 4;
  private static final int BUTTON_SQUELCH_DOWN = 5;
  private static final int BUTTON_SQUELCH_UP = 6;
  private static final int BUTTON_CODE_DOWN = 7;
  private static final int BUTTON_CODE_UP = 8;
  private static final int BUTTON_SCAN = 9;

  private final RadioModel model;
  private final RadioSettings original;
  @Nullable
  private final EnumHand hand;
  private final BlockPos pos;

  private RadioSettings settings;
  @Nullable
  private GuiTextField unitIdField;
  private int left;
  private int top;

  /**
   * The screen of a radio held in a hand.
   *
   * @param model    the make of radio
   * @param settings its settings now
   * @param hand     the hand holding it
   */
  public RadioGui(RadioModel model, RadioSettings settings, EnumHand hand) {
    this(model, settings, hand, BlockPos.ORIGIN);
  }

  /**
   * The screen of a radio standing in the world.
   *
   * @param model    the make of radio
   * @param settings its settings now
   * @param pos      where it stands
   */
  public RadioGui(RadioModel model, RadioSettings settings, BlockPos pos) {
    this(model, settings, null, pos);
  }

  private RadioGui(RadioModel model, RadioSettings settings, @Nullable EnumHand hand,
      BlockPos pos) {
    this.model = model;
    this.original = settings.forModel(model);
    this.settings = this.original;
    this.hand = hand;
    this.pos = pos;
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }

  @Override
  public void initGui() {
    Keyboard.enableRepeatEvents(true);
    left = (width - PANEL_WIDTH) / 2;
    top = (height - PANEL_HEIGHT) / 2;
    buttonList.clear();

    int rowA = top + 90;
    buttonList.add(new GuiButton(BUTTON_CHANNEL_DOWN, left + 20, rowA, 40, 20, "CH -"));
    buttonList.add(new GuiButton(BUTTON_POWER, left + 64, rowA, 72, 20, "PWR"));
    buttonList.add(new GuiButton(BUTTON_CHANNEL_UP, left + 140, rowA, 40, 20, "CH +"));

    int rowB = top + 114;
    buttonList.add(new GuiButton(BUTTON_VOLUME_DOWN, left + 20, rowB, 38, 20, "VOL -"));
    buttonList.add(new GuiButton(BUTTON_VOLUME_UP, left + 61, rowB, 38, 20, "VOL +"));
    buttonList.add(new GuiButton(BUTTON_SQUELCH_DOWN, left + 102, rowB, 38, 20, "SQL -"));
    buttonList.add(new GuiButton(BUTTON_SQUELCH_UP, left + 143, rowB, 37, 20, "SQL +"));

    int rowC = top + 138;
    if (model.hasPrivacyCode()) {
      buttonList.add(new GuiButton(BUTTON_CODE_DOWN, left + 20, rowC, 38, 20, "CODE -"));
      buttonList.add(new GuiButton(BUTTON_CODE_UP, left + 61, rowC, 38, 20, "CODE +"));
    }
    if (model.hasScan()) {
      int x = model.hasPrivacyCode() ? left + 102 : left + 20;
      int w = model.hasPrivacyCode() ? 78 : 160;
      buttonList.add(new GuiButton(BUTTON_SCAN, x, rowC, w, 20, "SCAN"));
    }

    if (model.hasUnitId()) {
      String text = unitIdField != null ? unitIdField.getText() : settings.getUnitId();
      unitIdField = new GuiTextField(100, fontRenderer, left + 62, top + 164, 118, 16);
      unitIdField.setMaxStringLength(RadioSettings.MAX_UNIT_ID_LENGTH);
      unitIdField.setText(text);
    }
  }

  @Override
  protected void actionPerformed(GuiButton button) {
    int channels = model.getChannels().size();
    switch (button.id) {
      case BUTTON_POWER:
        settings = settings.withOn(!settings.isOn());
        break;
      case BUTTON_CHANNEL_DOWN:
        settings = settings.withChannel((settings.getChannel() + channels - 1) % channels);
        break;
      case BUTTON_CHANNEL_UP:
        settings = settings.withChannel((settings.getChannel() + 1) % channels);
        break;
      case BUTTON_VOLUME_DOWN:
        settings = settings.withVolume(settings.getVolume() - 1);
        break;
      case BUTTON_VOLUME_UP:
        settings = settings.withVolume(settings.getVolume() + 1);
        break;
      case BUTTON_SQUELCH_DOWN:
        settings = settings.withSquelch(settings.getSquelch() - 1);
        break;
      case BUTTON_SQUELCH_UP:
        settings = settings.withSquelch(settings.getSquelch() + 1);
        break;
      case BUTTON_CODE_DOWN:
        settings = settings.withPrivacyCode(settings.getPrivacyCode() - 1);
        break;
      case BUTTON_CODE_UP:
        settings = settings.withPrivacyCode(settings.getPrivacyCode() + 1);
        break;
      case BUTTON_SCAN:
        settings = settings.withScan(!settings.isScan());
        break;
      default:
        break;
    }
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if (unitIdField != null && unitIdField.isFocused() && keyCode != Keyboard.KEY_ESCAPE) {
      if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
        unitIdField.setFocused(false);
        return;
      }
      unitIdField.textboxKeyTyped(typedChar, keyCode);
      String clean = RadioSettings.cleanUnitId(unitIdField.getText());
      // Keep a space being typed mid-word; only the stored value is trimmed.
      if (!clean.equals(unitIdField.getText().trim())) {
        unitIdField.setText(clean);
      } else if (!unitIdField.getText().equals(unitIdField.getText().toUpperCase())) {
        unitIdField.setText(unitIdField.getText().toUpperCase());
      }
      return;
    }
    if (keyCode == mc.gameSettings.keyBindInventory.getKeyCode()) {
      mc.player.closeScreen();
      return;
    }
    super.keyTyped(typedChar, keyCode);
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    if (unitIdField != null) {
      unitIdField.mouseClicked(mouseX, mouseY, mouseButton);
    }
  }

  @Override
  public void updateScreen() {
    if (unitIdField != null) {
      unitIdField.updateCursorCounter();
    }
  }

  @Override
  public void onGuiClosed() {
    Keyboard.enableRepeatEvents(false);
    RadioSettings result = current();
    if (result.equals(original)) {
      return;
    }
    CsmTechnology.NETWORK.sendToServer(hand != null
        ? new RadioSettingsPacket(hand, result) : new RadioSettingsPacket(pos, result));
  }

  /** The settings as the screen stands, with the unit ID box's text. */
  private RadioSettings current() {
    RadioSettings result = settings;
    if (unitIdField != null) {
      result = result.withUnitId(unitIdField.getText());
    }
    return result.forModel(model);
  }

  // --- drawing -------------------------------------------------------------------------------

  private static int scale(int argb, float k) {
    int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * k));
    int g = Math.min(255, (int) (((argb >> 8) & 0xFF) * k));
    int b = Math.min(255, (int) ((argb & 0xFF) * k));
    return 0xFF000000 | (r << 16) | (g << 8) | b;
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    int body = model.getBodyColor();
    int right = left + PANEL_WIDTH;
    int bottom = top + PANEL_HEIGHT;

    // The body: a slab with a lit top-left edge and a shaded bottom-right one, corners clipped.
    drawRect(left + 2, top, right - 2, bottom, scale(body, 0.6F));
    drawRect(left, top + 2, right, bottom - 2, scale(body, 0.6F));
    drawRect(left + 2, top + 2, right - 2, bottom - 2, body);
    drawRect(left + 2, top + 2, right - 2, top + 3, scale(body, 1.35F));
    drawRect(left + 2, top + 2, left + 3, bottom - 2, scale(body, 1.35F));

    // The maker's name over the display, the accent key's colour as a stripe beside it.
    drawRect(left + 14, top + 8, left + 18, top + 16, model.getAccentColor());
    boolean lightBody = ((body >> 16) & 0xFF) + ((body >> 8) & 0xFF) + (body & 0xFF) > 384;
    fontRenderer.drawString(model.getBrand(), left + 22, top + 8,
        lightBody ? 0xFF16171A : 0xFFE8EAEC);

    // The display in its bezel.
    int sx0 = left + 20;
    int sy0 = top + 22;
    int sx1 = right - 20;
    int sy1 = top + 82;
    drawRect(sx0 - 3, sy0 - 3, sx1 + 3, sy1 + 3, model.getBezelColor());
    drawRect(sx0 - 1, sy0 - 1, sx1 + 1, sy1 + 1, 0xFF050506);
    if (!settings.isOn()) {
      drawRect(sx0, sy0, sx1, sy1, scale(model.getScreenColor(), 0.28F));
    } else {
      drawRect(sx0, sy0, sx1, sy1, model.getScreenColor());
      drawDisplay(sx0, sy0, sx1, sy1);
    }

    if (unitIdField != null) {
      fontRenderer.drawString("UNIT ID", left + 20, top + 168,
          lightBody ? 0xFF16171A : 0xFFE8EAEC);
      unitIdField.drawTextBox();
    }
    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  private void drawDisplay(int x0, int y0, int x1, int y1) {
    int ink = model.getScreenTextColor();
    RadioChannel channel = model.getChannels().get(settings.getChannel());

    fontRenderer.drawString(model.getProduct(), x0 + 4, y0 + 3, ink);
    String unitId = unitIdField != null
        ? RadioSettings.cleanUnitId(unitIdField.getText()) : settings.getUnitId();
    if (!unitId.isEmpty()) {
      fontRenderer.drawString(unitId, x1 - 4 - fontRenderer.getStringWidth(unitId), y0 + 3, ink);
    }

    // The preset's name at twice the size, as the one thing a radio's display is for.
    String name = channel.getName();
    int nameWidth = fontRenderer.getStringWidth(name) * 2;
    GlStateManager.pushMatrix();
    GlStateManager.translate((x0 + x1 - nameWidth) / 2.0F, y0 + 15, 0);
    GlStateManager.scale(2.0F, 2.0F, 1.0F);
    fontRenderer.drawString(name, 0, 0, ink);
    GlStateManager.popMatrix();

    String frequency = channel.getFrequencyText() + " MHz"
        + (channel.isReceiveOnly() ? "  RX" : "");
    fontRenderer.drawString(frequency,
        (x0 + x1 - fontRenderer.getStringWidth(frequency)) / 2, y0 + 35, ink);

    StringBuilder status = new StringBuilder();
    status.append("VOL ").append(settings.getVolume());
    status.append("  SQL ").append(settings.getSquelch());
    if (model.hasPrivacyCode()) {
      status.append("  PL ").append(settings.getPrivacyCode() == 0
          ? "--" : String.valueOf(settings.getPrivacyCode()));
    }
    fontRenderer.drawString(status.toString(), x0 + 4, y1 - 11, ink);
    // Scanning blinks, twice a second.
    if (settings.isScan() && (System.currentTimeMillis() / 500L) % 2L == 0L) {
      fontRenderer.drawString("SCAN", x1 - 4 - fontRenderer.getStringWidth("SCAN"), y1 - 11,
          ink);
    }
  }
}
