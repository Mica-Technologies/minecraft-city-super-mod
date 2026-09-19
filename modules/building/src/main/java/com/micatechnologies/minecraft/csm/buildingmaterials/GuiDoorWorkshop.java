package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.buildingmaterials.BlockBuildingDoor.Hinge;
import com.micatechnologies.minecraft.csm.buildingmaterials.CustomDoorSettings.Movement;
import com.micatechnologies.minecraft.csm.buildingmaterials.CustomDoorSettings.Redstone;
import com.micatechnologies.minecraft.csm.buildingmaterials.CustomDoorSettings.Sound;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/**
 * The Door Workshop's screen. Down the left, the three material slots (frame, upper, lower); an
 * empty one shows, faded, the block the design still needs. In the middle, the door's behaviour:
 * movement, sound, redstone, proximity sensor, speed and auto-close. Beside it, the door itself,
 * made of the chosen materials, turning slowly and opening and closing the way it will in the
 * world. Down the right, the output and Make (shift-click for a stack), and the edit slot, with Set
 * (give its doors this behaviour) and Copy (take its door's whole design). Along the bottom, the
 * designs saved on this workshop, by name.
 *
 * <p>The design is the workshop's, not the screen's: the screen shows it as the server last sent
 * it, changes it at once for itself when a button is pressed, and sends the change. Make, Set,
 * Copy and the saved designs are done on the server, from what the slots actually hold.</p>
 *
 * <p>The preview is drawn with {@link CustomDoorRenderer#drawDoor}, from the same baked quads as a
 * door in the world, and only while the screen is open.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class GuiDoorWorkshop extends GuiContainer {

  private static final int BTN_MOVEMENT = 0;
  private static final int BTN_SOUND = 1;
  private static final int BTN_REDSTONE = 2;
  private static final int BTN_PROXIMITY = 3;
  private static final int BTN_SLOWER = 4;
  private static final int BTN_FASTER = 5;
  private static final int BTN_CLOSE_LESS = 6;
  private static final int BTN_CLOSE_MORE = 7;
  private static final int BTN_MAKE = 8;
  private static final int BTN_APPLY = 9;
  private static final int BTN_COPY = 10;
  private static final int BTN_PREV = 11;
  private static final int BTN_NEXT = 12;
  private static final int BTN_SAVE = 13;
  private static final int BTN_LOAD = 14;
  private static final int BTN_DELETE = 15;

  private static final int MID_X = 30;
  private static final int MID_W = 112;
  private static final int[] ROW_Y = {14, 33, 52, 71, 90, 109};
  /** Buttons are 18 high, not vanilla's 20, so the screen fits a 240-high window. */
  private static final int H = 18;
  private static final int RIGHT_BTN_X = 214;
  private static final int RIGHT_BTN_W = 30;
  private static final int DESIGN_Y = 131;

  /** The preview's window. */
  private static final int PV_X0 = 146;
  private static final int PV_X1 = 206;
  private static final int PV_Y0 = 14;
  private static final int PV_Y1 = 127;

  /** How long the preview rests shut, and open, between moves: a second each. */
  private static final double PV_REST = 20;

  private static final String[] SLOT_KEYS = {"frame", "upper", "lower", "edit", "output"};

  private final TileEntityDoorWorkshop workshop;

  private GuiButton movement;
  private GuiButton sound;
  private GuiButton redstone;
  private GuiButton proximity;
  private GuiButton load;
  private GuiButton delete;
  private GuiButton save;
  private GuiTextField name;
  /** The saved design picked with the arrows, or -1. */
  private int selected = -1;

  /**
   * Constructs a {@link GuiDoorWorkshop}.
   *
   * @param playerInventory the player's inventory
   * @param workshop        the workshop
   *
   * @since 1.0
   */
  public GuiDoorWorkshop(InventoryPlayer playerInventory, TileEntityDoorWorkshop workshop) {
    super(new ContainerDoorWorkshop(playerInventory, workshop));
    this.workshop = workshop;
    this.xSize = 248;
    this.ySize = 236;
  }

  private CustomDoorSettings design() {
    return workshop.getDesign();
  }

  @Override
  public void initGui() {
    super.initGui();
    Keyboard.enableRepeatEvents(true);
    buttonList.clear();
    int x = guiLeft + MID_X;
    movement = new ShortButton(BTN_MOVEMENT, x, guiTop + ROW_Y[0], MID_W, H, "");
    sound = new ShortButton(BTN_SOUND, x, guiTop + ROW_Y[1], MID_W, H, "");
    redstone = new ShortButton(BTN_REDSTONE, x, guiTop + ROW_Y[2], MID_W, H, "");
    proximity = new ShortButton(BTN_PROXIMITY, x, guiTop + ROW_Y[3], MID_W, H, "");
    buttonList.add(movement);
    buttonList.add(sound);
    buttonList.add(redstone);
    buttonList.add(proximity);
    buttonList.add(new ShortButton(BTN_SLOWER, x, guiTop + ROW_Y[4], 20, H, "-"));
    buttonList.add(new ShortButton(BTN_FASTER, x + MID_W - 20, guiTop + ROW_Y[4], 20, H, "+"));
    buttonList.add(new ShortButton(BTN_CLOSE_LESS, x, guiTop + ROW_Y[5], 20, H, "-"));
    buttonList.add(new ShortButton(BTN_CLOSE_MORE, x + MID_W - 20, guiTop + ROW_Y[5], 20, H, "+"));
    int rx = guiLeft + RIGHT_BTN_X;
    buttonList.add(new ShortButton(BTN_MAKE, rx, guiTop + 37, RIGHT_BTN_W, H,
        I18n.format("gui.csm.workshop.make")));
    buttonList.add(new ShortButton(BTN_APPLY, rx, guiTop + 91, RIGHT_BTN_W, H,
        I18n.format("gui.csm.workshop.apply")));
    buttonList.add(new ShortButton(BTN_COPY, rx, guiTop + 110, RIGHT_BTN_W, H,
        I18n.format("gui.csm.workshop.copy")));
    int dy = guiTop + DESIGN_Y;
    buttonList.add(new ShortButton(BTN_PREV, guiLeft + 8, dy, 18, H, "<"));
    buttonList.add(new ShortButton(BTN_NEXT, guiLeft + 140, dy, 18, H, ">"));
    save = new ShortButton(BTN_SAVE, guiLeft + 160, dy, 28, H,
        I18n.format("gui.csm.workshop.save"));
    load = new ShortButton(BTN_LOAD, guiLeft + 190, dy, 28, H,
        I18n.format("gui.csm.workshop.load"));
    delete = new ShortButton(BTN_DELETE, guiLeft + 220, dy, 24, H,
        I18n.format("gui.csm.workshop.delete"));
    buttonList.add(save);
    buttonList.add(load);
    buttonList.add(delete);
    name = new GuiTextField(100, fontRenderer, guiLeft + 29, dy + 1, 108, 16);
    name.setMaxStringLength(TileEntityDoorWorkshop.MAX_NAME);
    refresh();
  }

  @Override
  public void onGuiClosed() {
    super.onGuiClosed();
    Keyboard.enableRepeatEvents(false);
  }

  private void refresh() {
    CustomDoorSettings s = design();
    movement.displayString = I18n.format("gui.csm.door.tip.movement",
        I18n.format("gui.csm.door.movement." + s.movement().key()));
    sound.displayString = I18n.format("gui.csm.door.tip.sound",
        I18n.format("gui.csm.door.sound." + s.sound().key()));
    redstone.displayString = I18n.format("gui.csm.door.tip.redstone",
        I18n.format("gui.csm.door.redstone." + s.redstone().key()));
    proximity.displayString = I18n.format(s.proximity() ? "gui.csm.workshop.sensor_on"
        : "gui.csm.workshop.sensor_off");
    // The design picked is the one whose name is in the field, saved just now or typed.
    selected = -1;
    List<TileEntityDoorWorkshop.Design> designs = workshop.getDesigns();
    String typed = TileEntityDoorWorkshop.clean(name.getText());
    for (int i = 0; i < designs.size(); i++) {
      if (designs.get(i).name.equals(typed)) {
        selected = i;
      }
    }
    load.enabled = selected >= 0;
    delete.enabled = selected >= 0;
    save.enabled = !TileEntityDoorWorkshop.clean(name.getText()).isEmpty();
  }

  private static <E extends Enum<E>> E next(E value, E[] all) {
    return all[(value.ordinal() + 1) % all.length];
  }

  private void send(int action, CustomDoorSettings s) {
    CsmBuilding.NETWORK.sendToServer(new DoorWorkshopPacket(workshop.getPos(), action, s,
        selected, name.getText()));
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    CustomDoorSettings s = design();
    Movement mv = s.movement();
    Sound snd = s.sound();
    Redstone rs = s.redstone();
    int open = s.openTicks();
    int close = s.autoCloseTicks();
    boolean prox = s.proximity();
    int action = DoorWorkshopPacket.SET;
    int count = workshop.getDesigns().size();
    switch (button.id) {
      case BTN_MOVEMENT:
        mv = next(mv, Movement.values());
        break;
      case BTN_SOUND:
        snd = next(snd, Sound.values());
        break;
      case BTN_REDSTONE:
        rs = next(rs, Redstone.values());
        break;
      case BTN_PROXIMITY:
        prox = !prox;
        break;
      case BTN_SLOWER:
        open += 2;
        break;
      case BTN_FASTER:
        open -= 2;
        break;
      case BTN_CLOSE_LESS:
        close = close <= 20 ? 0 : close - 20;
        break;
      case BTN_CLOSE_MORE:
        close += 20;
        break;
      case BTN_MAKE:
        action = isShiftKeyDown() ? DoorWorkshopPacket.MAKE_ALL : DoorWorkshopPacket.MAKE;
        break;
      case BTN_APPLY:
        action = DoorWorkshopPacket.APPLY;
        break;
      case BTN_COPY:
        send(DoorWorkshopPacket.COPY, s);
        return;
      case BTN_PREV:
      case BTN_NEXT:
        if (count > 0) {
          int step = button.id == BTN_NEXT ? 1 : count - 1;
          selected = selected < 0 ? (button.id == BTN_NEXT ? 0 : count - 1)
              : (selected + step) % count;
          name.setText(workshop.getDesigns().get(selected).name);
        }
        refresh();
        return;
      case BTN_SAVE:
        action = DoorWorkshopPacket.SAVE;
        break;
      case BTN_LOAD:
        send(DoorWorkshopPacket.LOAD, s);
        return;
      case BTN_DELETE:
        send(DoorWorkshopPacket.DELETE, s);
        selected = -1;
        name.setText("");
        refresh();
        return;
      default:
        return;
    }
    CustomDoorSettings changed = s.withBehaviour(mv, snd, open, close, rs, prox);
    workshop.setDesign(changed);
    send(action, changed);
    refresh();
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if (name.isFocused() && keyCode != Keyboard.KEY_ESCAPE) {
      name.textboxKeyTyped(typedChar, keyCode);
      refresh();
      return;
    }
    super.keyTyped(typedChar, keyCode);
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    name.mouseClicked(mouseX, mouseY, mouseButton);
  }

  @Override
  public void updateScreen() {
    super.updateScreen();
    name.updateCursorCounter();
    refresh();
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    super.drawScreen(mouseX, mouseY, partialTicks);
    name.drawTextBox();
    Slot hovered = getSlotUnderMouse();
    if (hovered != null && !hovered.getHasStack() && hovered.slotNumber < SLOT_KEYS.length) {
      // Say what an empty workshop slot is for, and, for a material, what the design has in it.
      List<String> lines = new ArrayList<>();
      lines.add(I18n.format("gui.csm.workshop.slot." + SLOT_KEYS[hovered.slotNumber]));
      if (hovered.slotNumber <= TileEntityDoorWorkshop.LOWER) {
        ItemStack ghost = ghost(hovered.slotNumber);
        if (!ghost.isEmpty()) {
          lines.add(I18n.format("gui.csm.workshop.design_has", ghost.getDisplayName()));
        }
      }
      drawHoveringText(lines, mouseX, mouseY);
    } else {
      renderHoveredToolTip(mouseX, mouseY);
    }
  }

  /** The design's block for an empty material slot, as an item to show there. */
  private ItemStack ghost(int slot) {
    IBlockState m = TileEntityDoorWorkshop.designMaterial(design(), slot);
    return new ItemStack(m.getBlock(), 1, m.getBlock().damageDropped(m));
  }

  @Override
  protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
    drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF2B2B2B);
    drawRect(guiLeft + 2, guiTop + 2, guiLeft + xSize - 2, guiTop + ySize - 2, 0xFFC6C6C6);
    for (Slot slot : inventorySlots.inventorySlots) {
      int x = guiLeft + slot.xPos;
      int y = guiTop + slot.yPos;
      drawRect(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
      drawRect(x, y, x + 17, y + 17, 0xFFFFFFFF);
      drawRect(x, y, x + 16, y + 16, 0xFF8B8B8B);
    }
    drawGhosts();
    drawRect(guiLeft + PV_X0 - 1, guiTop + PV_Y0 - 1, guiLeft + PV_X1 + 1, guiTop + PV_Y1 + 1,
        0xFF373737);
    drawRect(guiLeft + PV_X0, guiTop + PV_Y0, guiLeft + PV_X1, guiTop + PV_Y1, 0xFF6F7478);
    drawPreview();
  }

  /** Each empty material slot shows the design's block there, faded. */
  private void drawGhosts() {
    RenderHelper.enableGUIStandardItemLighting();
    for (int i = TileEntityDoorWorkshop.FRAME; i <= TileEntityDoorWorkshop.LOWER; i++) {
      Slot slot = inventorySlots.getSlot(i);
      if (slot.getHasStack()) {
        continue;
      }
      ItemStack ghost = ghost(i);
      if (ghost.isEmpty()) {
        continue;
      }
      int x = guiLeft + slot.xPos;
      int y = guiTop + slot.yPos;
      itemRender.renderItemAndEffectIntoGUI(ghost, x, y);
      GlStateManager.disableLighting();
      GlStateManager.disableDepth();
      drawRect(x, y, x + 16, y + 16, 0xA08B8B8B);
      GlStateManager.enableDepth();
    }
    RenderHelper.disableStandardItemLighting();
  }

  /**
   * The door as it will be made, turning slowly and opening and closing at its own speed: shut a
   * second, opening, open a second, closing. A pair for the movements that need one.
   */
  private void drawPreview() {
    CustomDoorSettings s = workshop.effective();
    boolean paired = s.movement() == Movement.SLIDE_TOGETHER || s.movement() == Movement.SPLIT;
    double ticks = Minecraft.getSystemTime() / 50.0;
    double move = s.openTicks();
    double t = ticks % (2 * (PV_REST + move));
    double p;
    if (t < PV_REST) {
      p = 0;
    } else if (t < PV_REST + move) {
      p = (t - PV_REST) / move;
    } else if (t < 2 * PV_REST + move) {
      p = 1;
    } else {
      p = 1 - (t - 2 * PV_REST - move) / move;
    }
    double openness = p * p * (3 - 2 * p);
    float scale = paired ? 24F : 34F;
    // Turned a little from face on, swaying either side of that.
    float yaw = (float) (25.0 + Math.sin(ticks / 30.0) * 20.0);

    // Kept inside its window: a sliding panel goes out of sight as into a pocket in the wall.
    int sf = new ScaledResolution(mc).getScaleFactor();
    GL11.glEnable(GL11.GL_SCISSOR_TEST);
    GL11.glScissor((guiLeft + PV_X0) * sf, mc.displayHeight - (guiTop + PV_Y1) * sf,
        (PV_X1 - PV_X0) * sf, (PV_Y1 - PV_Y0) * sf);
    mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    GlStateManager.pushMatrix();
    GlStateManager.translate(guiLeft + (PV_X0 + PV_X1) / 2F, guiTop + PV_Y1 - 18F, 150F);
    GlStateManager.scale(scale, -scale, scale);
    GlStateManager.rotate(-15F, 1F, 0F, 0F);
    GlStateManager.rotate(yaw, 0F, 1F, 0F);
    GlStateManager.enableDepth();
    GlStateManager.disableCull();
    GlStateManager.enableRescaleNormal();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    GlStateManager.enableAlpha();
    RenderHelper.enableStandardItemLighting();
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
    float width = paired ? 2F : 1F;
    GlStateManager.translate(-width / 2F, 0F, -0.5F);
    if (paired) {
      CustomDoorRenderer.drawDoor(s, Hinge.LEFT, true, openness);
      GlStateManager.translate(1F, 0F, 0F);
      CustomDoorRenderer.drawDoor(s, Hinge.RIGHT, true, openness);
    } else {
      CustomDoorRenderer.drawDoor(s, Hinge.LEFT, false, openness);
    }
    RenderHelper.disableStandardItemLighting();
    GlStateManager.disableRescaleNormal();
    GlStateManager.enableCull();
    GlStateManager.disableBlend();
    GlStateManager.popMatrix();
    GL11.glDisable(GL11.GL_SCISSOR_TEST);
  }

  @Override
  protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
    fontRenderer.drawString(I18n.format("tile.door_workshop.name"), 8, 4, 0x404040);
    CustomDoorSettings s = design();
    String speed = I18n.format("gui.csm.workshop.speed", s.openTicks() / 20.0);
    String close = s.autoCloseTicks() == 0 ? I18n.format("gui.csm.workshop.stays_open")
        : I18n.format("gui.csm.workshop.closes", s.autoCloseTicks() / 20.0);
    int cx = MID_X + MID_W / 2;
    fontRenderer.drawString(speed, cx - fontRenderer.getStringWidth(speed) / 2, ROW_Y[4] + 5,
        0x404040);
    fontRenderer.drawString(close, cx - fontRenderer.getStringWidth(close) / 2, ROW_Y[5] + 5,
        0x404040);
  }

  /**
   * A vanilla button drawn 18 high: the texture's top rows, then its last two, so the bottom edge
   * and shadow are kept rather than cut off.
   */
  private static final class ShortButton extends GuiButton {

    ShortButton(int id, int x, int y, int width, int height, String text) {
      super(id, x, y, width, height, text);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
      if (!visible) {
        return;
      }
      mc.getTextureManager().bindTexture(BUTTON_TEXTURES);
      GlStateManager.color(1F, 1F, 1F, 1F);
      hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
      int v = 46 + getHoverState(hovered) * 20;
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
          GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
          GlStateManager.DestFactor.ZERO);
      int half = width / 2;
      drawTexturedModalRect(x, y, 0, v, half, height - 2);
      drawTexturedModalRect(x + half, y, 200 - (width - half), v, width - half, height - 2);
      drawTexturedModalRect(x, y + height - 2, 0, v + 18, half, 2);
      drawTexturedModalRect(x + half, y + height - 2, 200 - (width - half), v + 18,
          width - half, 2);
      int colour = !enabled ? 0xA0A0A0 : hovered ? 0xFFFFA0 : 0xE0E0E0;
      drawCenteredString(mc.fontRenderer, displayString, x + width / 2, y + (height - 8) / 2,
          colour);
    }
  }
}
