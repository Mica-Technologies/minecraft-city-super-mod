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
 * The route marker sign's editor: which marker it wears, and what route number is on it.
 *
 * <p>Two settings, so the screen is two controls and a preview line. The marker steps forward on
 * a click and backward on a shift-click, as every stepped setting in the mod's GUIs does, and
 * there are jump buttons either side for walking sixty-seven markers without sixty-seven clicks.
 * Everything is sent as one packet whenever either changes, so the sign in the world updates
 * while the screen is still open and the result can be judged without closing it.</p>
 *
 * @version 1.0
 * @since 2026.9.20
 */
@SideOnly(Side.CLIENT)
public class DynamicRouteMarkerSignGui extends GuiScreen {

  private static final int BUTTON_WIDTH = 180;
  private static final int BUTTON_HEIGHT = 20;
  private static final int JUMP_WIDTH = 24;
  private static final int GAP = 4;
  private static final int ROW_SPACING = 24;
  private static final int FIELD_WIDTH = 100;

  private static final int ID_SHIELD = 0;
  private static final int ID_PREV_JUMP = 1;
  private static final int ID_NEXT_JUMP = 2;
  private static final int ID_CLOSE = 100;

  /** How far the jump buttons step through the marker list. */
  private static final int JUMP = 8;

  private final TileEntityDynamicRouteMarkerSign tileEntity;
  private final BlockPos blockPos;

  private GuideSignShieldType shield;
  private GuiTextField routeField;

  public DynamicRouteMarkerSignGui(TileEntityDynamicRouteMarkerSign tileEntity) {
    this.tileEntity = tileEntity;
    this.blockPos = tileEntity.getPos();
    this.shield = tileEntity.getShield();
  }

  @Override
  public void initGui() {
    buttonList.clear();
    Keyboard.enableRepeatEvents(true);

    final int topY = topRowY();
    final int totalWidth = BUTTON_WIDTH + 2 * (JUMP_WIDTH + GAP);
    final int leftX = width / 2 - totalWidth / 2;

    buttonList.add(new GuiButton(ID_PREV_JUMP, leftX, topY, JUMP_WIDTH, BUTTON_HEIGHT, "<<"));
    buttonList.add(new GuiButton(ID_SHIELD, leftX + JUMP_WIDTH + GAP, topY,
        BUTTON_WIDTH, BUTTON_HEIGHT, ""));
    buttonList.add(new GuiButton(ID_NEXT_JUMP, leftX + JUMP_WIDTH + GAP + BUTTON_WIDTH + GAP,
        topY, JUMP_WIDTH, BUTTON_HEIGHT, ">>"));

    routeField = new GuiTextField(0, fontRenderer, width / 2 - FIELD_WIDTH / 2,
        topY + ROW_SPACING + 10, FIELD_WIDTH, BUTTON_HEIGHT);
    routeField.setMaxStringLength(TileEntityDynamicRouteMarkerSign.MAX_ROUTE_LENGTH);
    routeField.setText(tileEntity.getRouteNumber());
    routeField.setFocused(true);

    buttonList.add(new GuiButton(ID_CLOSE, width / 2 - BUTTON_WIDTH / 2,
        topY + 2 * ROW_SPACING + 16, BUTTON_WIDTH, BUTTON_HEIGHT, "Done"));
  }

  private int topRowY() {
    return height / 2 - (3 * ROW_SPACING) / 2;
  }

  @Override
  public void onGuiClosed() {
    Keyboard.enableRepeatEvents(false);
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    switch (button.id) {
      case ID_SHIELD:
        shield = step(isShiftKeyDown() ? -1 : 1);
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
    int next = ((shield.ordinal() + by) % values.length + values.length) % values.length;
    return values[next];
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if (routeField.textboxKeyTyped(typedChar, keyCode)) {
      // The tile entity decides what a route number may contain; reflecting its answer back
      // into the field means the player sees exactly what the sign will print.
      String cleaned = TileEntityDynamicRouteMarkerSign.clampRoute(routeField.getText());
      if (!cleaned.equals(routeField.getText())) {
        routeField.setText(cleaned);
      }
      send();
      return;
    }
    super.keyTyped(typedChar, keyCode);
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    routeField.mouseClicked(mouseX, mouseY, mouseButton);
  }

  @Override
  public void updateScreen() {
    super.updateScreen();
    routeField.updateCursorCounter();
  }

  /** Sends the whole configuration. The server clamps both halves of it. */
  private void send() {
    CsmRoads.NETWORK.sendToServer(
        new RouteMarkerConfigPacket(blockPos, shield.ordinal(), routeField.getText()));
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    for (GuiButton button : buttonList) {
      if (button.id == ID_SHIELD) {
        button.displayString = shield.getFriendlyName();
      }
    }

    final int topY = topRowY();
    drawCenteredString(fontRenderer, "Route Marker Sign", width / 2, topY - 26, 0xFFFFFF);
    drawCenteredString(fontRenderer,
        "Marker " + (shield.ordinal() + 1) + " of " + GuideSignShieldType.values().length,
        width / 2, topY - 14, 0xA0A0A0);

    super.drawScreen(mouseX, mouseY, partialTicks);

    drawCenteredString(fontRenderer, "Route number", width / 2, topY + ROW_SPACING, 0xFFD070);
    routeField.drawTextBox();

    drawCenteredString(fontRenderer,
        "Click the marker to step forward, shift-click to step back",
        width / 2, topY + 3 * ROW_SPACING + 12, 0xA0A0A0);
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}
