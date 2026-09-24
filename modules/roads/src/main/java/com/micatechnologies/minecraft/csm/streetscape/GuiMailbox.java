package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.roads.CsmRoads;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/**
 * A mailbox compartment's screen: its name and owner, then its slots (the owner), one slot to
 * post through (anyone else) or a Claim button (a free compartment), then the player's
 * inventory. The placer and operators get one more row under the title: a name field with
 * Assign and Free, which hands the compartment to that player or frees it.
 *
 * <p>Every button only asks: the server checks and answers by reopening the screen in whatever
 * mode now applies ({@link MailboxActionPacketHandler}).</p>
 *
 * @version 1.0
 */
@SideOnly(Side.CLIENT)
public class GuiMailbox extends GuiContainer {

  private static final int BTN_CLAIM = 0;
  private static final int BTN_ASSIGN = 1;
  private static final int BTN_FREE = 2;

  /** How far down the admin row pushes everything below the title. */
  private static final int ADMIN_H = 22;

  private final ContainerMailbox container;
  private final String title;
  private final boolean admin;
  private GuiTextField name;

  public GuiMailbox(InventoryPlayer playerInventory, ContainerMailbox container, String title) {
    super(container);
    this.container = container;
    this.title = title;
    EntityPlayer me = playerInventory.player;
    this.admin = BlockMailbox.mayManage(me, container.getBox());
    this.xSize = 176;
    this.ySize = container.getInventoryY() + 84;
    if (admin) {
      this.ySize += ADMIN_H;
      for (Slot s : container.inventorySlots) {
        s.yPos += ADMIN_H;
      }
    }
  }

  @Override
  public void initGui() {
    super.initGui();
    Keyboard.enableRepeatEvents(true);
    buttonList.clear();
    int y = guiTop + ContainerMailbox.TOP_SLOTS_Y + (admin ? ADMIN_H : 0);
    if (container.getMode() == BlockMailbox.MODE_FREE) {
      buttonList.add(new GuiButton(BTN_CLAIM, guiLeft + 48, y - 2, 80, 20, "Claim"));
    }
    if (admin) {
      int ay = guiTop + 26;
      name = new GuiTextField(10, fontRenderer, guiLeft + 8, ay + 1, 88, 16);
      name.setMaxStringLength(16);
      name.setText(container.getBox().getOwnerName(container.getCompartment()));
      buttonList.add(new GuiButton(BTN_ASSIGN, guiLeft + 99, ay, 38, 18, "Assign"));
      buttonList.add(new GuiButton(BTN_FREE, guiLeft + 139, ay, 30, 18, "Free"));
    }
  }

  @Override
  public void onGuiClosed() {
    super.onGuiClosed();
    Keyboard.enableRepeatEvents(false);
  }

  @Override
  protected void actionPerformed(GuiButton button) {
    int action;
    String who = "";
    switch (button.id) {
      case BTN_CLAIM:
        action = MailboxActionPacket.CLAIM;
        break;
      case BTN_ASSIGN:
        action = MailboxActionPacket.ASSIGN;
        who = name.getText().trim();
        if (who.isEmpty()) {
          return;
        }
        break;
      case BTN_FREE:
        action = MailboxActionPacket.FREE;
        break;
      default:
        return;
    }
    CsmRoads.NETWORK.sendToServer(new MailboxActionPacket(container.getBox().getPos(),
        container.getCompartment(), action, who));
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if (name != null && name.isFocused()) {
      if (keyCode == Keyboard.KEY_ESCAPE) {
        super.keyTyped(typedChar, keyCode);
      } else {
        name.textboxKeyTyped(typedChar, keyCode);
      }
      return;
    }
    super.keyTyped(typedChar, keyCode);
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    if (name != null) {
      name.mouseClicked(mouseX, mouseY, mouseButton);
    }
  }

  @Override
  public void updateScreen() {
    super.updateScreen();
    if (name != null) {
      name.updateCursorCounter();
    }
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    super.drawScreen(mouseX, mouseY, partialTicks);
    renderHoveredToolTip(mouseX, mouseY);
  }

  @Override
  protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
    GlStateManager.color(1f, 1f, 1f, 1f);
    // A plain panel in vanilla's greys: a light face, a dark rim, sunken slot wells.
    drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF373737);
    drawRect(guiLeft + 1, guiTop + 1, guiLeft + xSize - 1, guiTop + ySize - 1, 0xFFC6C6C6);
    for (Slot s : container.inventorySlots) {
      int x = guiLeft + s.xPos;
      int y = guiTop + s.yPos;
      drawRect(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
      drawRect(x, y, x + 17, y + 17, 0xFFFFFFFF);
      drawRect(x, y, x + 16, y + 16, 0xFF8B8B8B);
    }
    if (name != null) {
      name.drawTextBox();
    }
  }

  @Override
  protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
    TileEntityMailbox box = container.getBox();
    int i = container.getCompartment();
    fontRenderer.drawString(title, 8, 6, 0x404040);
    String owner = box.getOwnerName(i);
    String line;
    switch (container.getMode()) {
      case BlockMailbox.MODE_OPEN:
        line = owner.isEmpty() ? "Free" : "Owner: " + owner;
        break;
      case BlockMailbox.MODE_POST:
        line = "Post to " + owner;
        break;
      default:
        line = "Free -- claim it to receive mail";
        break;
    }
    fontRenderer.drawString(fontRenderer.trimStringToWidth(line, xSize - 16), 8, 16, 0x404040);
    fontRenderer.drawString(Minecraft.getMinecraft().player.inventory.getDisplayName()
        .getUnformattedText(), 8, container.getInventoryY() + (admin ? ADMIN_H : 0) - 11,
        0x404040);
  }
}
