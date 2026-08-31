package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The wire mount's configuration screen.
 *
 * <p>Follows the same shape as the other CSM config screens: a button per setting showing its
 * current value, cycled by clicking, with the change applied on the server and read back from the
 * tile entity. Nothing is held in the screen itself, so it cannot disagree with the block.
 *
 * <p>The status line at the bottom is the part worth having. Whether a mount is buildable at all
 * depends on how far it sits below the cable and which style it is set to, and those interact —
 * switching from flush to mast is exactly what fixes a mount the tool has been complaining about.
 * Showing the drop and the style's limit together makes that visible instead of a matter of
 * re-linking and reading chat.
 */
@SideOnly(Side.CLIENT)
public class SpanWireMountConfigGui extends GuiScreen {

  private static final int BUTTON_WIDTH = 200;
  private static final int BUTTON_HEIGHT = 20;
  private static final int ROW_SPACING = 24;
  private static final int CLOSE_BUTTON_ID = 100;

  private static final String[] LABELS =
      {"Mount Style", "Conductor Coil", "Signal Side", "Lower Tether", "Cluster Width"};

  private final TileEntitySpanWireHanger tileEntity;
  private final BlockPos blockPos;

  /** A single mount has no width to set, so its screen is one row shorter. */
  private final int rowCount;

  public SpanWireMountConfigGui(TileEntitySpanWireHanger tileEntity) {
    this.tileEntity = tileEntity;
    this.blockPos = tileEntity.getPos();
    // Cluster width is the last row and only a cluster has one.
    this.rowCount = tileEntity instanceof TileEntitySpanWireClusterMount
        ? LABELS.length
        : LABELS.length - 1;
  }

  @Override
  public void initGui() {
    buttonList.clear();

    final int left = width / 2 - BUTTON_WIDTH / 2;
    final int top = height / 2 - (rowCount * ROW_SPACING) / 2;

    for (int i = 0; i < rowCount; i++) {
      buttonList.add(new GuiButton(i, left, top + i * ROW_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT,
          ""));
    }
    buttonList.add(new GuiButton(CLOSE_BUTTON_ID, left,
        top + rowCount * ROW_SPACING + 28, BUTTON_WIDTH, BUTTON_HEIGHT, "Close"));
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    for (int i = 0; i < rowCount && i < buttonList.size(); i++) {
      buttonList.get(i).displayString = LABELS[i] + ": " + currentValue(i);
    }

    final int top = height / 2 - (rowCount * ROW_SPACING) / 2;
    drawCenteredString(fontRenderer, title(), width / 2, top - 28, 0xFFFFFF);
    drawCenteredString(fontRenderer, status(), width / 2,
        top + rowCount * ROW_SPACING + 8, 0xA0A0A0);

    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  private String currentValue(int index) {
    switch (SpanWireMountConfigAction.values()[index]) {
      case CYCLE_MOUNT_STYLE:
        return tileEntity.getMountStyle().getFriendlyName();
      case CYCLE_COIL_STYLE:
        return tileEntity.getCoilStyle().getFriendlyName();
      case CYCLE_SIGNAL_SIDE:
        return signalSideValue();
      case TOGGLE_BOX_SPAN:
        return tileEntity.getSpan() == null
            ? "N/A"
            : (tileEntity.getSpan().isBoxSpan() ? "Box span" : "None");
      case CYCLE_CLUSTER_WIDTH:
        return tileEntity instanceof TileEntitySpanWireClusterMount
            ? ((TileEntitySpanWireClusterMount) tileEntity).getClusterWidth() + " signals"
            : "N/A";
      default:
        return "N/A";
    }
  }

  /**
   * The chosen side, with the compass direction it actually points on this span.
   *
   * <p>The setting is stored relative to the span because a diagonal span has no compass side --
   * but "left" is not something a builder can act on while standing under a wire, so the screen
   * resolves it. A span with nothing strung has no direction to resolve against and says so.
   */
  private String signalSideValue() {
    final SpanWireDefinition span = tileEntity.getSpan();
    final SpanWireSignalSide side = span == null
        ? SpanWireSignalSide.CENTRED
        : span.getSignalSide();
    if (span == null || side == SpanWireSignalSide.CENTRED) {
      return side.getFriendlyName();
    }
    // Resolved from the span's own displacement rather than from the side alone, because an
    // automatic span's amount is measured off its payloads and is not knowable from the enum.
    final EnumFacing compass = SpanWireSignalSide.compassFor(
        span.horizontalDirection(), span.spanOffset());
    return compass == null
        ? side.getFriendlyName()
        : side.getFriendlyName() + " (" + compass.getName() + ")";
  }

  private String title() {
    return tileEntity instanceof TileEntitySpanWireClusterMount
        ? "Wire Mount (Cluster)"
        : "Wire Mount";
  }

  /** One line saying whether this mount actually reaches its cable, and by how much. */
  private String status() {
    if (tileEntity.getSpan() == null) {
      return "Not on a span. Link two anchors either side of this mount.";
    }
    final double drop = tileEntity.getCableDrop();
    if (drop < 0.0) {
      return String.format("Cable passes %.2f blocks BELOW this mount - lower it.", -drop);
    }
    final double limit = tileEntity.getMountStyle().getMaximumDrop();
    if (drop > limit) {
      return String.format("%.2f blocks below the cable, past this style's %.1f limit.",
          drop, limit);
    }
    return String.format("%.2f blocks below the cable, within the %.1f limit.", drop, limit);
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    if (button.id == CLOSE_BUTTON_ID) {
      mc.displayGuiScreen(null);
    } else if (button.id >= 0 && button.id < rowCount) {
      CsmNetwork.sendToServer(new SpanWireMountConfigPacket(blockPos, button.id));
    }
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}
