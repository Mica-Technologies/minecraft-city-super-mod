package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.codeutils.packets.TileEntityPortableMessageSignUpdatePacket;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;

public class BlockOverheadMessageSignGui extends GuiScreen {

  private static final int BTN_SAVE = 0;
  private static final int BTN_CANCEL = 1;
  private static final int BTN_PREV_PAGE = 2;
  private static final int BTN_NEXT_PAGE = 3;
  private static final int BTN_ADD_PAGE = 4;
  private static final int BTN_REMOVE_PAGE = 5;
  private static final int BTN_SPEED_DOWN = 7;
  private static final int BTN_SPEED_UP = 8;
  private static final int BTN_HOUSING_COLOR = 9;

  private static final int FIELD_WIDTH = 200;
  private static final int FIELD_HEIGHT = 20;
  private static final int MAX_LINE_LENGTH = 16;
  private static final int MAX_PAGES = 8;

  private final TileEntityPortableMessageSign tileEntity;
  private final List<String[]> pages = new ArrayList<>();
  private int currentPage = 0;
  private int cycleSpeed;
  private TrafficSignalBodyColor housingColor;

  private GuiTextField line1Field;
  private GuiTextField line2Field;
  private GuiTextField line3Field;

  private GuiButton prevPageBtn;
  private GuiButton nextPageBtn;
  private GuiButton addPageBtn;
  private GuiButton removePageBtn;

  public BlockOverheadMessageSignGui(TileEntityPortableMessageSign tileEntity) {
    this.tileEntity = tileEntity;
    for (int i = 0; i < tileEntity.getPageCount(); i++) {
      String[] src = tileEntity.getPage(i);
      pages.add(new String[]{src[0], src[1], src[2]});
    }
    this.cycleSpeed = tileEntity.getCycleSpeed();
    this.housingColor = tileEntity.getHousingColor();
  }

  @Override
  public void initGui() {
    Keyboard.enableRepeatEvents(true);
    super.initGui();
    buttonList.clear();

    ScaledResolution sr = new ScaledResolution(this.mc);
    int centerX = sr.getScaledWidth() / 2;
    int fieldLeft = centerX - FIELD_WIDTH / 2;
    int halfWidth = (FIELD_WIDTH - 4) / 2;
    int startY = sr.getScaledHeight() / 2 - 75;

    int row = startY;

    prevPageBtn = new GuiButton(BTN_PREV_PAGE, fieldLeft, row, 40, 20, "< Prev");
    nextPageBtn = new GuiButton(BTN_NEXT_PAGE, fieldLeft + FIELD_WIDTH - 40, row, 40, 20,
        "Next >");
    buttonList.add(prevPageBtn);
    buttonList.add(nextPageBtn);
    row += 22;

    line1Field = new GuiTextField(10, fontRenderer, fieldLeft, row, FIELD_WIDTH, FIELD_HEIGHT);
    line1Field.setMaxStringLength(MAX_LINE_LENGTH);
    line1Field.setFocused(true);
    row += 22;

    line2Field = new GuiTextField(11, fontRenderer, fieldLeft, row, FIELD_WIDTH, FIELD_HEIGHT);
    line2Field.setMaxStringLength(MAX_LINE_LENGTH);
    row += 22;

    line3Field = new GuiTextField(12, fontRenderer, fieldLeft, row, FIELD_WIDTH, FIELD_HEIGHT);
    line3Field.setMaxStringLength(MAX_LINE_LENGTH);
    row += 22;

    addPageBtn = new GuiButton(BTN_ADD_PAGE, fieldLeft, row, halfWidth, 20, "+ Add Page");
    removePageBtn = new GuiButton(BTN_REMOVE_PAGE, fieldLeft + halfWidth + 4, row, halfWidth, 20,
        "- Remove Page");
    buttonList.add(addPageBtn);
    buttonList.add(removePageBtn);
    row += 22;

    // Speed and housing color on same row
    GuiButton speedDownBtn = new GuiButton(BTN_SPEED_DOWN, fieldLeft, row, 20, 20, "-");
    GuiButton speedUpBtn = new GuiButton(BTN_SPEED_UP, fieldLeft + halfWidth - 20, row, 20, 20,
        "+");
    buttonList.add(speedDownBtn);
    buttonList.add(speedUpBtn);
    buttonList.add(
        new GuiButton(BTN_HOUSING_COLOR, fieldLeft + halfWidth + 4, row, halfWidth, 20, ""));
    row += 22;

    buttonList.add(new GuiButton(BTN_SAVE, fieldLeft, row, halfWidth, 20, "Save"));
    buttonList.add(
        new GuiButton(BTN_CANCEL, fieldLeft + halfWidth + 4, row, halfWidth, 20, "Cancel"));

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
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    ScaledResolution sr = new ScaledResolution(this.mc);
    int centerX = sr.getScaledWidth() / 2;
    int fieldLeft = centerX - FIELD_WIDTH / 2;
    int halfWidth = (FIELD_WIDTH - 4) / 2;
    int startY = sr.getScaledHeight() / 2 - 75;

    drawCenteredString(fontRenderer, "Overhead Message Sign", centerX, startY - 15, 0xFFAA00);

    String pageLabel = "Page " + (currentPage + 1) + " of " + pages.size();
    drawCenteredString(fontRenderer, pageLabel, centerX, startY + 6, 0xFFFFFF);

    int speedRowY = startY + 5 * 22;
    int speedCenterX = fieldLeft + halfWidth / 2;
    drawCenteredString(fontRenderer, "Speed: " + cycleSpeed + "s", speedCenterX,
        speedRowY + 6, 0xFFFFFF);

    for (GuiButton btn : buttonList) {
      if (btn.id == BTN_HOUSING_COLOR) {
        btn.displayString = "Housing: " + housingColor.getFriendlyName();
      }
    }

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
            tileEntity.getPos(), toSend,
            TileEntityPortableMessageSign.FLASHER_NONE, cycleSpeed, 0, 0,
            housingColor.toNBT()));
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

      case BTN_HOUSING_COLOR:
        housingColor = housingColor.getNextColor();
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
