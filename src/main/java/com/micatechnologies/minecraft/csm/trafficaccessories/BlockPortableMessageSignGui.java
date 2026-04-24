package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.codeutils.packets.TileEntityPortableMessageSignUpdatePacket;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;

public class BlockPortableMessageSignGui extends GuiScreen {

  private static final int BTN_SAVE = 0;
  private static final int BTN_CANCEL = 1;
  private static final int BTN_PREV_PAGE = 2;
  private static final int BTN_NEXT_PAGE = 3;
  private static final int BTN_ADD_PAGE = 4;
  private static final int BTN_REMOVE_PAGE = 5;
  private static final int BTN_FLASHERS = 6;
  private static final int BTN_SPEED_DOWN = 7;
  private static final int BTN_SPEED_UP = 8;
  private static final int BTN_COLOR = 9;
  private static final int BTN_ANGLE = 10;

  private static final int FIELD_WIDTH = 200;
  private static final int FIELD_HEIGHT = 20;
  private static final int MAX_LINE_LENGTH = 10;
  private static final int MAX_PAGES = 8;

  private final TileEntityPortableMessageSign tileEntity;
  private final List<String[]> pages = new ArrayList<>();
  private int currentPage = 0;
  private int flasherMode;
  private int cycleSpeed;
  private int trailerColor;
  private int signAngle;

  private GuiTextField line1Field;
  private GuiTextField line2Field;
  private GuiTextField line3Field;

  private GuiButton prevPageBtn;
  private GuiButton nextPageBtn;
  private GuiButton addPageBtn;
  private GuiButton removePageBtn;
  private GuiButton flashersBtn;
  private GuiButton speedDownBtn;
  private GuiButton speedUpBtn;
  private GuiButton colorBtn;
  private GuiButton angleBtn;

  public BlockPortableMessageSignGui(TileEntityPortableMessageSign tileEntity) {
    this.tileEntity = tileEntity;
    for (int i = 0; i < tileEntity.getPageCount(); i++) {
      String[] src = tileEntity.getPage(i);
      pages.add(new String[]{src[0], src[1], src[2]});
    }
    this.flasherMode = tileEntity.getFlasherMode();
    this.cycleSpeed = tileEntity.getCycleSpeed();
    this.trailerColor = tileEntity.getTrailerColor();
    this.signAngle = tileEntity.getSignAngle();
  }

  @Override
  public void initGui() {
    Keyboard.enableRepeatEvents(true);
    super.initGui();
    buttonList.clear();

    ScaledResolution sr = new ScaledResolution(this.mc);
    int centerX = sr.getScaledWidth() / 2;
    int fieldLeft = centerX - FIELD_WIDTH / 2;
    int startY = sr.getScaledHeight() / 2 - 105;

    int row = startY;

    // Page navigation
    prevPageBtn = new GuiButton(BTN_PREV_PAGE, fieldLeft, row, 40, 20, "< Prev");
    nextPageBtn = new GuiButton(BTN_NEXT_PAGE, fieldLeft + FIELD_WIDTH - 40, row, 40, 20,
        "Next >");
    buttonList.add(prevPageBtn);
    buttonList.add(nextPageBtn);
    row += 25;

    // Text fields
    line1Field = new GuiTextField(10, fontRenderer, fieldLeft, row, FIELD_WIDTH, FIELD_HEIGHT);
    line1Field.setMaxStringLength(MAX_LINE_LENGTH);
    line1Field.setFocused(true);
    row += 25;

    line2Field = new GuiTextField(11, fontRenderer, fieldLeft, row, FIELD_WIDTH, FIELD_HEIGHT);
    line2Field.setMaxStringLength(MAX_LINE_LENGTH);
    row += 25;

    line3Field = new GuiTextField(12, fontRenderer, fieldLeft, row, FIELD_WIDTH, FIELD_HEIGHT);
    line3Field.setMaxStringLength(MAX_LINE_LENGTH);
    row += 25;

    // Add/remove page
    addPageBtn = new GuiButton(BTN_ADD_PAGE, fieldLeft, row, 97, 20, "+ Add Page");
    removePageBtn = new GuiButton(BTN_REMOVE_PAGE, fieldLeft + 103, row, 97, 20, "- Remove Page");
    buttonList.add(addPageBtn);
    buttonList.add(removePageBtn);
    row += 25;

    // Flashers toggle + speed controls
    flashersBtn = new GuiButton(BTN_FLASHERS, fieldLeft, row, 97, 20, "");
    speedDownBtn = new GuiButton(BTN_SPEED_DOWN, fieldLeft + 103, row, 20, 20, "-");
    speedUpBtn = new GuiButton(BTN_SPEED_UP, fieldLeft + FIELD_WIDTH - 20, row, 20, 20, "+");
    buttonList.add(flashersBtn);
    buttonList.add(speedDownBtn);
    buttonList.add(speedUpBtn);
    row += 25;

    // Color + angle selectors
    colorBtn = new GuiButton(BTN_COLOR, fieldLeft, row, 97, 20, "");
    angleBtn = new GuiButton(BTN_ANGLE, fieldLeft + 103, row, 97, 20, "");
    buttonList.add(colorBtn);
    buttonList.add(angleBtn);
    row += 25;

    // Save / Cancel
    buttonList.add(new GuiButton(BTN_SAVE, fieldLeft, row, FIELD_WIDTH, 20, "Save"));
    row += 25;
    buttonList.add(new GuiButton(BTN_CANCEL, fieldLeft, row, FIELD_WIDTH, 20, "Cancel"));

    loadPageFields();
    updateButtonStates();
  }

  private void loadPageFields() {
    String[] page = pages.get(currentPage);
    line1Field.setText(page[0]);
    line2Field.setText(page[1]);
    line3Field.setText(page[2]);
  }

  private void savePageFields() {
    pages.get(currentPage)[0] = line1Field.getText();
    pages.get(currentPage)[1] = line2Field.getText();
    pages.get(currentPage)[2] = line3Field.getText();
  }

  private void updateButtonStates() {
    prevPageBtn.enabled = currentPage > 0;
    nextPageBtn.enabled = currentPage < pages.size() - 1;
    addPageBtn.enabled = pages.size() < MAX_PAGES;
    removePageBtn.enabled = pages.size() > 1;
    flashersBtn.displayString = "Flashers: " + TileEntityPortableMessageSign.FLASHER_MODE_NAMES[flasherMode];
    colorBtn.displayString = TileEntityPortableMessageSign.COLOR_NAMES[trailerColor];
    angleBtn.displayString = TileEntityPortableMessageSign.ANGLE_NAMES[signAngle];
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    ScaledResolution sr = new ScaledResolution(this.mc);
    int centerX = sr.getScaledWidth() / 2;
    int startY = sr.getScaledHeight() / 2 - 105;

    drawCenteredString(fontRenderer, "Portable Message Sign", centerX, startY - 15, 0xFFAA00);

    // Page indicator
    String pageLabel = "Page " + (currentPage + 1) + " of " + pages.size();
    drawCenteredString(fontRenderer, pageLabel, centerX, startY + 6, 0xFFFFFF);

    // Speed label
    int speedLabelY = startY + 125 + 6;
    int speedLabelX = centerX + FIELD_WIDTH / 2 - 50;
    drawCenteredString(fontRenderer, "Speed: " + cycleSpeed + "s", speedLabelX, speedLabelY,
        0xFFFFFF);

    line1Field.drawTextBox();
    line2Field.drawTextBox();
    line3Field.drawTextBox();
    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if (keyCode == Keyboard.KEY_TAB) {
      if (line1Field.isFocused()) {
        line1Field.setFocused(false);
        line2Field.setFocused(true);
        line3Field.setFocused(false);
      } else if (line2Field.isFocused()) {
        line1Field.setFocused(false);
        line2Field.setFocused(false);
        line3Field.setFocused(true);
      } else {
        line1Field.setFocused(true);
        line2Field.setFocused(false);
        line3Field.setFocused(false);
      }
      return;
    }
    line1Field.textboxKeyTyped(typedChar, keyCode);
    line2Field.textboxKeyTyped(typedChar, keyCode);
    line3Field.textboxKeyTyped(typedChar, keyCode);
    super.keyTyped(typedChar, keyCode);
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    line1Field.mouseClicked(mouseX, mouseY, mouseButton);
    line2Field.mouseClicked(mouseX, mouseY, mouseButton);
    line3Field.mouseClicked(mouseX, mouseY, mouseButton);
    super.mouseClicked(mouseX, mouseY, mouseButton);
  }

  @Override
  public void updateScreen() {
    line1Field.updateCursorCounter();
    line2Field.updateCursorCounter();
    line3Field.updateCursorCounter();
  }

  @Override
  @ParametersAreNonnullByDefault
  protected void actionPerformed(GuiButton button) {
    switch (button.id) {
      case BTN_SAVE:
        savePageFields();
        List<String[]> toSend = new ArrayList<>();
        for (String[] p : pages) {
          toSend.add(new String[]{
              p[0].toUpperCase(), p[1].toUpperCase(), p[2].toUpperCase()
          });
        }
        CsmNetwork.sendToServer(new TileEntityPortableMessageSignUpdatePacket(
            tileEntity.getPos(), toSend, flasherMode, cycleSpeed,
            trailerColor, signAngle));
        this.mc.displayGuiScreen(null);
        break;

      case BTN_CANCEL:
        this.mc.displayGuiScreen(null);
        break;

      case BTN_PREV_PAGE:
        if (currentPage > 0) {
          savePageFields();
          currentPage--;
          loadPageFields();
          updateButtonStates();
        }
        break;

      case BTN_NEXT_PAGE:
        if (currentPage < pages.size() - 1) {
          savePageFields();
          currentPage++;
          loadPageFields();
          updateButtonStates();
        }
        break;

      case BTN_ADD_PAGE:
        if (pages.size() < MAX_PAGES) {
          savePageFields();
          pages.add(new String[]{"", "", ""});
          currentPage = pages.size() - 1;
          loadPageFields();
          updateButtonStates();
        }
        break;

      case BTN_REMOVE_PAGE:
        if (pages.size() > 1) {
          savePageFields();
          pages.remove(currentPage);
          if (currentPage >= pages.size()) {
            currentPage = pages.size() - 1;
          }
          loadPageFields();
          updateButtonStates();
        }
        break;

      case BTN_FLASHERS:
        flasherMode = (flasherMode + 1) % TileEntityPortableMessageSign.FLASHER_MODE_COUNT;
        updateButtonStates();
        break;

      case BTN_SPEED_DOWN:
        if (cycleSpeed > 1) {
          cycleSpeed--;
        }
        break;

      case BTN_SPEED_UP:
        if (cycleSpeed < 10) {
          cycleSpeed++;
        }
        break;

      case BTN_COLOR:
        trailerColor = (trailerColor + 1) % TileEntityPortableMessageSign.COLOR_COUNT;
        updateButtonStates();
        break;

      case BTN_ANGLE:
        signAngle = (signAngle + 1) % TileEntityPortableMessageSign.ANGLE_COUNT;
        updateButtonStates();
        break;
    }
  }

  @Override
  public boolean doesGuiPauseGame() {
    return true;
  }

  @Override
  public void onGuiClosed() {
    Keyboard.enableRepeatEvents(false);
  }
}
