package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.roads.CsmRoads;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkBulbType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkVisorType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Configuration GUI for crosswalk signals. Two-column layout with cycle buttons for all
 * configurable properties. Reads values from the client-side tile entity every frame so
 * changes sync automatically after the server processes each packet.
 */
@SideOnly( Side.CLIENT )
public class CrosswalkConfigGui extends GuiScreen {

    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_SPACING = 22;
    private static final int COLUMN_GAP = 6;
    private static final int CLOSE_BUTTON_ID = 100;
    private static final int COPY_BUTTON_ID = 101;
    private static final int PASTE_BUTTON_ID = 102;

    /**
     * Client-side appearance clipboard for the Copy/Paste buttons, mirroring the vehicle signal
     * head GUI's. Static so a look copied from one crosswalk signal survives closing the GUI and
     * persists until another copy (or game exit), letting players stamp the same style onto every
     * signal at an intersection. {@code null} until the first copy. Kept separate from the head's
     * clipboard because the two carry different properties and pasting one onto the other would
     * be meaningless.
     */
    private static AppearanceClipboard clipboard = null;

    /**
     * Immutable snapshot of the copy/paste-able appearance of a crosswalk signal. Mount type and
     * body tilt are deliberately absent: they say where this signal sits, not what it looks like,
     * so copying them across an intersection would re-aim every head. The vehicle head clipboard
     * omits its own mount type and tilt for the same reason.
     */
    private static final class AppearanceClipboard {
        final TrafficSignalBodyColor bodyColor;
        final TrafficSignalBodyColor visorColor;
        final CrosswalkVisorType visorType;
        final CrosswalkBulbType bulbType;

        AppearanceClipboard( TrafficSignalBodyColor bodyColor, TrafficSignalBodyColor visorColor,
                CrosswalkVisorType visorType, CrosswalkBulbType bulbType ) {
            this.bodyColor = bodyColor;
            this.visorColor = visorColor;
            this.visorType = visorType;
            this.bulbType = bulbType;
        }
    }

    private static final String[] LABELS = {
            "Body Color",
            "Visor Color",
            "Visor Type",
            "Mount Type",
            "Body Tilt",
            "Bulb Type"
    };

    private final TileEntityCrosswalkSignalNew tileEntity;
    private final BlockPos blockPos;
    private final boolean isDouble;

    public CrosswalkConfigGui( TileEntityCrosswalkSignalNew tileEntity, boolean isDouble ) {
        this.tileEntity = tileEntity;
        this.blockPos = tileEntity.getPos();
        this.isDouble = isDouble;
    }

    @Override
    public void initGui() {
        buttonList.clear();

        int totalWidth = BUTTON_WIDTH * 2 + COLUMN_GAP;
        int leftX = width / 2 - totalWidth / 2;
        int rightX = leftX + BUTTON_WIDTH + COLUMN_GAP;
        int rows = ( LABELS.length + 1 ) / 2;
        // Two rows below the property grid now: the copy/paste row and the close button.
        int topY = height / 2 - ( rows * ROW_SPACING + ROW_SPACING * 2 ) / 2;

        for ( int i = 0; i < LABELS.length; i++ ) {
            int col = i % 2;
            int row = i / 2;
            int x = col == 0 ? leftX : rightX;
            int y = topY + row * ROW_SPACING;
            GuiButton button = new GuiButton( i, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, "" );
            // Disable bulb type button for single (16-inch) signals — fixed display type
            if ( i == CrosswalkConfigAction.CYCLE_BULB_TYPE.ordinal() && !isDouble ) {
                button.enabled = false;
            }
            buttonList.add( button );
        }

        // Copy / Paste row: two columns matching the property grid. Paste stays disabled until
        // something has been copied.
        int copyPasteY = topY + rows * ROW_SPACING + 4;
        buttonList.add( new GuiButton( COPY_BUTTON_ID, leftX, copyPasteY, BUTTON_WIDTH,
                BUTTON_HEIGHT, "Copy Appearance" ) );
        GuiButton pasteButton = new GuiButton( PASTE_BUTTON_ID, rightX, copyPasteY, BUTTON_WIDTH,
                BUTTON_HEIGHT, "Paste Appearance" );
        pasteButton.enabled = clipboard != null;
        buttonList.add( pasteButton );

        buttonList.add( new GuiButton( CLOSE_BUTTON_ID, width / 2 - BUTTON_WIDTH / 2,
                copyPasteY + ROW_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT, "Close" ) );
    }

    @Override
    public void drawScreen( int mouseX, int mouseY, float partialTicks ) {
        drawDefaultBackground();

        for ( int i = 0; i < LABELS.length && i < buttonList.size(); i++ ) {
            buttonList.get( i ).displayString = LABELS[ i ] + ": " + getCurrentValue( i );
        }

        int rows = ( LABELS.length + 1 ) / 2;
        int topY = height / 2 - ( rows * ROW_SPACING + ROW_SPACING * 2 ) / 2;
        String title = isDouble ? "Crosswalk Signal 12-Inch Configuration"
                : "Crosswalk Signal 16-Inch Configuration";
        drawCenteredString( fontRenderer, title, width / 2, topY - 14, 0xFFFFFF );

        super.drawScreen( mouseX, mouseY, partialTicks );
    }

    private String getCurrentValue( int actionOrdinal ) {
        if ( actionOrdinal >= CrosswalkConfigAction.values().length ) return "N/A";

        switch ( CrosswalkConfigAction.values()[ actionOrdinal ] ) {
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
            case CYCLE_BULB_TYPE:
                if ( !isDouble ) return "Fixed (Symbol)";
                return tileEntity.getBulbType().getFriendlyName();
            default:
                return "N/A";
        }
    }

    @Override
    protected void actionPerformed( GuiButton button ) throws IOException {
        if ( button.id == CLOSE_BUTTON_ID ) {
            mc.displayGuiScreen( null );
        }
        else if ( button.id == COPY_BUTTON_ID ) {
            clipboard = new AppearanceClipboard( tileEntity.getBodyColor(),
                    tileEntity.getVisorColor(), tileEntity.getVisorType(),
                    tileEntity.getBulbType() );
            initGui(); // rebuild so the Paste button becomes enabled
        }
        else if ( button.id == PASTE_BUTTON_ID ) {
            if ( clipboard != null ) {
                CsmRoads.NETWORK.sendToServer( new CrosswalkAppearancePacket( blockPos,
                        clipboard.bodyColor.toNBT(), clipboard.visorColor.toNBT(),
                        clipboard.visorType.toNBT(), clipboard.bulbType.toNBT() ) );
            }
        }
        else if ( button.id >= 0 && button.id < CrosswalkConfigAction.values().length ) {
            CsmRoads.NETWORK.sendToServer( new CrosswalkConfigPacket( blockPos, button.id ) );
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
