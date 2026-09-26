package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.roads.CsmRoads;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignShieldType;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/**
 * The mile marker's editor: the mile number, the tenth on an intermediate plate, and on an
 * enhanced plate the direction, route shield and route number.
 *
 * <p>A screen rather than click-to-count: a marker is set once to whatever mile it stands at,
 * which is typing three digits, where clicking would be hundreds of clicks. It follows the route
 * marker sign's editor -- stepped settings click forward and shift-click back, the shield has
 * jump buttons, and every change is sent at once so the plate in the world updates while the
 * screen is open.</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class MileMarkerSignGui extends GuiScreen {

  private static final int BUTTON_WIDTH = 180;
  private static final int BUTTON_HEIGHT = 20;
  private static final int JUMP_WIDTH = 24;
  private static final int GAP = 4;
  private static final int ROW = 26;
  private static final int FIELD_WIDTH = 60;

  private static final int ID_DIRECTION = 0;
  private static final int ID_SHIELD = 1;
  private static final int ID_PREV_JUMP = 2;
  private static final int ID_NEXT_JUMP = 3;
  private static final int ID_CLOSE = 100;
  private static final int JUMP = 8;

  private final TileEntityMileMarkerSign tileEntity;
  private final MileMarkerLayout layout;
  private final BlockPos blockPos;

  private GuideSignShieldType shield;
  private int direction;
  private GuiTextField mileField;
  private GuiTextField tenthField;
  private GuiTextField routeField;

  public MileMarkerSignGui(TileEntityMileMarkerSign tileEntity, MileMarkerLayout layout) {
    this.tileEntity = tileEntity;
    this.layout = layout;
    this.blockPos = tileEntity.getPos();
    this.shield = tileEntity.getShield();
    this.direction = tileEntity.getDirection();
  }

  private int rows() {
    return 2 + (layout.isEnhanced() ? 3 : 0);
  }

  private int topY() {
    return height / 2 - rows() * ROW / 2;
  }

  @Override
  public void initGui() {
    buttonList.clear();
    Keyboard.enableRepeatEvents(true);
    MileMarkerLegend legend = tileEntity.legend(layout);
    int y = topY() + 12;
    int centre = width / 2;
    int mileX = layout.hasTenth() ? centre - FIELD_WIDTH - GAP : centre - FIELD_WIDTH / 2;
    mileField = new GuiTextField(0, fontRenderer, mileX, y, FIELD_WIDTH, BUTTON_HEIGHT);
    mileField.setMaxStringLength(layout.getDigits());
    mileField.setText(legend.getMile());
    mileField.setFocused(true);
    tenthField = null;
    if (layout.hasTenth()) {
      tenthField = new GuiTextField(1, fontRenderer, centre + GAP, y, FIELD_WIDTH,
          BUTTON_HEIGHT);
      tenthField.setMaxStringLength(1);
      tenthField.setText(Integer.toString(tileEntity.getTenth()));
    }
    y += ROW;
    routeField = null;
    if (layout.isEnhanced()) {
      buttonList.add(new GuiButton(ID_DIRECTION, centre - BUTTON_WIDTH / 2, y, BUTTON_WIDTH,
          BUTTON_HEIGHT, ""));
      y += ROW;
      int total = BUTTON_WIDTH + 2 * (JUMP_WIDTH + GAP);
      int left = centre - total / 2;
      buttonList.add(new GuiButton(ID_PREV_JUMP, left, y, JUMP_WIDTH, BUTTON_HEIGHT, "<<"));
      buttonList.add(new GuiButton(ID_SHIELD, left + JUMP_WIDTH + GAP, y, BUTTON_WIDTH,
          BUTTON_HEIGHT, ""));
      buttonList.add(new GuiButton(ID_NEXT_JUMP, left + JUMP_WIDTH + GAP + BUTTON_WIDTH + GAP, y,
          JUMP_WIDTH, BUTTON_HEIGHT, ">>"));
      y += ROW + 10;
      routeField = new GuiTextField(2, fontRenderer, centre - FIELD_WIDTH / 2, y, FIELD_WIDTH,
          BUTTON_HEIGHT);
      routeField.setMaxStringLength(MileMarkerLegend.MAX_ROUTE_LENGTH);
      routeField.setText(tileEntity.getRoute());
      y += ROW;
    }
    buttonList.add(new GuiButton(ID_CLOSE, centre - BUTTON_WIDTH / 2, y + 6, BUTTON_WIDTH,
        BUTTON_HEIGHT, "Done"));
  }

  @Override
  public void onGuiClosed() {
    Keyboard.enableRepeatEvents(false);
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    int back = isShiftKeyDown() ? -1 : 1;
    switch (button.id) {
      case ID_DIRECTION:
        direction = Math.floorMod(direction + back, MileMarkerLegend.DIRECTIONS.length);
        send();
        break;
      case ID_SHIELD:
        shield = step(back);
        send();
        break;
      case ID_PREV_JUMP:
        shield = step(-JUMP);
        send();
        break;
      case ID_NEXT_JUMP:
        shield = step(JUMP);
        send();
        break;
      case ID_CLOSE:
        mc.displayGuiScreen(null);
        break;
      default:
        break;
    }
  }

  private GuideSignShieldType step(int by) {
    GuideSignShieldType[] values = GuideSignShieldType.values();
    return values[Math.floorMod(shield.ordinal() + by, values.length)];
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if (keyCode == Keyboard.KEY_TAB) {
      cycleFocus();
      return;
    }
    // Digits only: every field here is a number, and the glyph sheet holds nothing else.
    boolean digitOrEdit = Character.isDigit(typedChar) || typedChar < ' '
        || keyCode == Keyboard.KEY_LEFT || keyCode == Keyboard.KEY_RIGHT;
    for (GuiTextField field : fields()) {
      if (field.isFocused() && digitOrEdit && field.textboxKeyTyped(typedChar, keyCode)) {
        send();
        return;
      }
    }
    super.keyTyped(typedChar, keyCode);
  }

  private GuiTextField[] fields() {
    if (tenthField != null && routeField != null) {
      return new GuiTextField[]{mileField, tenthField, routeField};
    }
    if (tenthField != null) {
      return new GuiTextField[]{mileField, tenthField};
    }
    if (routeField != null) {
      return new GuiTextField[]{mileField, routeField};
    }
    return new GuiTextField[]{mileField};
  }

  private void cycleFocus() {
    GuiTextField[] all = fields();
    int at = 0;
    for (int i = 0; i < all.length; i++) {
      if (all[i].isFocused()) {
        at = i;
      }
      all[i].setFocused(false);
    }
    all[(at + 1) % all.length].setFocused(true);
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    for (GuiTextField field : fields()) {
      field.mouseClicked(mouseX, mouseY, mouseButton);
    }
  }

  @Override
  public void updateScreen() {
    super.updateScreen();
    for (GuiTextField field : fields()) {
      field.updateCursorCounter();
    }
  }

  private static int parse(GuiTextField field, int fallback) {
    if (field == null || field.getText().isEmpty()) {
      return fallback;
    }
    try {
      return Integer.parseInt(field.getText());
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  /** Sends the whole configuration; the server clamps it to the plate. */
  private void send() {
    CsmRoads.NETWORK.sendToServer(new MileMarkerConfigPacket(blockPos,
        parse(mileField, tileEntity.getMile()), parse(tenthField, tileEntity.getTenth()),
        shield.ordinal(), routeField == null ? tileEntity.getRoute() : routeField.getText(),
        direction));
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    for (GuiButton button : buttonList) {
      if (button.id == ID_SHIELD) {
        button.displayString = shield.getFriendlyName();
      } else if (button.id == ID_DIRECTION) {
        button.displayString = "Direction: " + MileMarkerLegend.DIRECTIONS[direction];
      }
    }
    int top = topY();
    drawCenteredString(fontRenderer, "Mile Marker (" + layout.getCode() + ")", width / 2,
        top - 30, 0xFFFFFF);
    drawCenteredString(fontRenderer, "Mile " + layout.getMinMile() + " to "
            + layout.getMaxMile() + (layout.hasTenth() ? ", and tenths" : ""), width / 2,
        top - 18, 0xA0A0A0);
    drawCenteredString(fontRenderer, layout.hasTenth() ? "Mile          Tenth" : "Mile",
        width / 2, top + 2, 0xFFD070);
    super.drawScreen(mouseX, mouseY, partialTicks);
    for (GuiTextField field : fields()) {
      field.drawTextBox();
    }
    if (routeField != null) {
      drawCenteredString(fontRenderer, "Route number", width / 2, routeField.y - 10, 0xFFD070);
    }
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}
