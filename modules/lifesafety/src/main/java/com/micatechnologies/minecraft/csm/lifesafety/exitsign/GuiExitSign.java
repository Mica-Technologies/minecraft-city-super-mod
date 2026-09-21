package com.micatechnologies.minecraft.csm.lifesafety.exitsign;

import com.micatechnologies.minecraft.csm.lifesafety.CsmLifeSafety;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * An exit sign's screen: one row per option the sign offers, each a button showing the current
 * value. Left-click steps to the next value, right-click to the previous; every change is sent
 * to the server at once, so the sign behind the screen changes as the player clicks. A large
 * icon of the sign's item shows the legend, colours and heads chosen.
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class GuiExitSign extends GuiScreen {

  private static final int PANEL_WIDTH = 280;
  private static final int ROW_HEIGHT = 22;
  private static final int PREVIEW_SIZE = 72;
  private static final int BUTTON_WIDTH = 120;
  private static final int DONE_ID = 100;

  private final AbstractBlockExitSign sign;
  private final TileEntityExitSign tileEntity;
  private final BlockPos pos;
  private final List<Row<?>> rows = new ArrayList<>();
  private ExitSignConfig config;
  private int left;
  private int top;
  private int panelHeight;

  public GuiExitSign(AbstractBlockExitSign sign, TileEntityExitSign tileEntity) {
    this.sign = sign;
    this.tileEntity = tileEntity;
    this.pos = tileEntity.getPos();
    this.config = sign.getSpec().clamp(tileEntity.getConfig());
    ExitSignSpec spec = sign.getSpec();
    addRow("legend", spec.getLegends(), ExitSignConfig::getLegend, ExitSignConfig::withLegend);
    addRow("letters", spec.getLetterColours(), ExitSignConfig::getLetters,
        ExitSignConfig::withLetters);
    addRow("housing", spec.getHousings(), ExitSignConfig::getHousing,
        ExitSignConfig::withHousing);
    addRow("arrow", spec.getArrows(), ExitSignConfig::getArrow, ExitSignConfig::withArrow);
    addRow("mount", spec.getMounts(), ExitSignConfig::getMount, ExitSignConfig::withMount);
    addRow("heads", spec.getHeadTypes(), ExitSignConfig::getHeads, ExitSignConfig::withHeads);
  }

  /** Adds a row for an option, unless the sign offers only one value of it. */
  private <E extends IStringSerializable> void addRow(String option, List<E> values,
      Function<ExitSignConfig, E> get, BiFunction<ExitSignConfig, E, ExitSignConfig> set) {
    if (values.size() > 1) {
      rows.add(new Row<>(option, values, get, set));
    }
  }

  @Override
  public void initGui() {
    buttonList.clear();
    panelHeight = 34 + Math.max(rows.size() * ROW_HEIGHT, PREVIEW_SIZE) + 44;
    left = (width - PANEL_WIDTH) / 2;
    top = (height - panelHeight) / 2;
    int x = left + PANEL_WIDTH - BUTTON_WIDTH - 10;
    for (int i = 0; i < rows.size(); i++) {
      buttonList.add(new GuiButton(i, x, top + 30 + i * ROW_HEIGHT, BUTTON_WIDTH, 20, ""));
    }
    buttonList.add(new GuiButton(DONE_ID, left + (PANEL_WIDTH - 100) / 2,
        top + panelHeight - 28, 100, 20, I18n.format("gui.done")));
    refreshLabels();
  }

  private void refreshLabels() {
    for (GuiButton button : buttonList) {
      if (button.id < rows.size()) {
        button.displayString = rows.get(button.id).valueName(config);
      }
    }
  }

  @Override
  protected void actionPerformed(GuiButton button) {
    if (button.id == DONE_ID) {
      mc.displayGuiScreen(null);
    } else if (button.id < rows.size()) {
      step(rows.get(button.id), 1);
    }
  }

  /** Right-click on an option steps it backwards; a left-click goes through actionPerformed. */
  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    if (mouseButton != 1) {
      return;
    }
    for (GuiButton button : buttonList) {
      if (button.id < rows.size() && button.mousePressed(mc, mouseX, mouseY)) {
        button.playPressSound(mc.getSoundHandler());
        step(rows.get(button.id), -1);
        return;
      }
    }
  }

  private void step(Row<?> row, int direction) {
    config = sign.getSpec().clamp(row.step(config, direction));
    CsmLifeSafety.NETWORK.sendToServer(new ExitSignConfigPacket(pos, config));
    refreshLabels();
  }

  /** Closes if the sign is broken or replaced while the screen is open. */
  @Override
  public void updateScreen() {
    if (mc.world == null || mc.world.getTileEntity(pos) != tileEntity
        || mc.player.getDistanceSq(pos) > 64) {
      mc.displayGuiScreen(null);
    }
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    drawRect(left, top, left + PANEL_WIDTH, top + panelHeight, 0xE0101010);
    drawRect(left, top, left + PANEL_WIDTH, top + 1, 0xFF808080);
    drawCenteredString(fontRenderer, sign.getLocalizedName(), left + PANEL_WIDTH / 2, top + 10,
        0xFFFFFF);
    int labelX = left + PANEL_WIDTH - BUTTON_WIDTH - 16;
    for (int i = 0; i < rows.size(); i++) {
      String label = I18n.format("csm.exitsign." + rows.get(i).option);
      fontRenderer.drawString(label, labelX - fontRenderer.getStringWidth(label),
          top + 36 + i * ROW_HEIGHT, 0xD0D0D0);
    }
    drawCenteredString(fontRenderer, I18n.format("csm.exitsign.gui.hint"),
        left + PANEL_WIDTH / 2, top + panelHeight - 42, 0x909090);
    drawPreview();
    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  /** The sign's item, drawn large: the icon the stack with this setup would show. */
  private void drawPreview() {
    int size = PREVIEW_SIZE;
    int x = left + 12;
    int y = top + 30;
    drawRect(x - 2, y - 2, x + size + 2, y + size + 2, 0xFF303030);
    GlStateManager.pushMatrix();
    GlStateManager.translate(x, y, 0);
    GlStateManager.scale(size / 16f, size / 16f, 1f);
    RenderHelper.enableGUIStandardItemLighting();
    itemRender.renderItemAndEffectIntoGUI(sign.stackFor(config), 0, 0);
    RenderHelper.disableStandardItemLighting();
    GlStateManager.popMatrix();
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }

  /** One option: its lang key, the values the sign offers, and how to read and set it. */
  private static final class Row<E extends IStringSerializable> {

    final String option;
    final List<E> values;
    final Function<ExitSignConfig, E> get;
    final BiFunction<ExitSignConfig, E, ExitSignConfig> set;

    Row(String option, List<E> values, Function<ExitSignConfig, E> get,
        BiFunction<ExitSignConfig, E, ExitSignConfig> set) {
      this.option = option;
      this.values = values;
      this.get = get;
      this.set = set;
    }

    ExitSignConfig step(ExitSignConfig config, int direction) {
      int i = values.indexOf(get.apply(config));
      int next = Math.floorMod((i < 0 ? 0 : i) + direction, values.size());
      return set.apply(config, values.get(next));
    }

    String valueName(ExitSignConfig config) {
      return I18n.format("csm.exitsign." + option + "." + get.apply(config).getName());
    }
  }
}
