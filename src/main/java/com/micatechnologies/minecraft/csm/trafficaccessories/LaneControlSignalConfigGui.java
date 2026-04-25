package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LaneControlSignalConfigGui extends GuiScreen {

    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_SPACING = 22;
    private static final int COLUMN_GAP = 6;
    private static final int CLOSE_BUTTON_ID = 100;

    private static final String[] LABELS = {
            "Body Color",
            "Visor Color",
            "Visor Type",
            "Mount Type",
            "Body Tilt",
            "Signal Type"
    };

    private final TileEntityLaneControlSignal tileEntity;
    private final BlockPos blockPos;

    public LaneControlSignalConfigGui(TileEntityLaneControlSignal tileEntity) {
        this.tileEntity = tileEntity;
        this.blockPos = tileEntity.getPos();
    }

    @Override
    public void initGui() {
        buttonList.clear();

        int totalWidth = BUTTON_WIDTH * 2 + COLUMN_GAP;
        int leftX = width / 2 - totalWidth / 2;
        int rightX = leftX + BUTTON_WIDTH + COLUMN_GAP;
        int rows = (LABELS.length + 1) / 2;
        int topY = height / 2 - (rows * ROW_SPACING + ROW_SPACING) / 2;

        for (int i = 0; i < LABELS.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int x = col == 0 ? leftX : rightX;
            int y = topY + row * ROW_SPACING;
            buttonList.add(new GuiButton(i, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, ""));
        }

        buttonList.add(new GuiButton(CLOSE_BUTTON_ID, width / 2 - BUTTON_WIDTH / 2,
                topY + rows * ROW_SPACING + 4, BUTTON_WIDTH, BUTTON_HEIGHT, "Close"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        for (int i = 0; i < LABELS.length && i < buttonList.size(); i++) {
            buttonList.get(i).displayString = LABELS[i] + ": " + getCurrentValue(i);
        }

        int rows = (LABELS.length + 1) / 2;
        int topY = height / 2 - (rows * ROW_SPACING + ROW_SPACING) / 2;
        drawCenteredString(fontRenderer, "Lane Control Signal Configuration",
                width / 2, topY - 14, 0xFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private String getCurrentValue(int actionOrdinal) {
        if (actionOrdinal >= LaneControlSignalConfigAction.values().length) return "N/A";

        switch (LaneControlSignalConfigAction.values()[actionOrdinal]) {
            case CYCLE_BODY_COLOR:
                return tileEntity.getBodyColor().getFriendlyName();
            case CYCLE_VISOR_COLOR:
                return tileEntity.getVisorColor().getFriendlyName();
            case CYCLE_VISOR_TYPE:
                return tileEntity.getVisorType().getFriendlyName();
            case CYCLE_MOUNT_TYPE:
                return tileEntity.getMountType().getFriendlyName();
            case CYCLE_BODY_TILT:
                return tileEntity.getBodyTilt().getFriendlyName();
            case CYCLE_SIGNAL_TYPE:
                return tileEntity.getSignalType().getFriendlyName();
            default:
                return "N/A";
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == CLOSE_BUTTON_ID) {
            mc.displayGuiScreen(null);
        } else if (button.id >= 0
                && button.id < LaneControlSignalConfigAction.values().length) {
            CsmNetwork.sendToServer(
                    new LaneControlSignalConfigPacket(blockPos, button.id));
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
