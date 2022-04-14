package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

/**
 * The {@link TileEntity} implementation for the {@link BlockTrafficSignalController} block.
 *
 * @author Mica Technologies
 * @version 2022.1.0
 * @see AbstractTickableTileEntity
 * @since 2020.7.0
 */
@ElementsCitySuperMod.ModElement.Tag
public class TileEntityTrafficSignalController extends AbstractTickableTileEntity
{
    //region: Constant Fields

    /**
     * The key used to store the index of the current {@link TrafficSignalControllerMode} in the {@link NBTTagCompound}
     * of the {@link TileEntity}.
     */
    private static final String NBT_MODE_INDEX_KEY = "mode";

    /**
     * The default {@link TrafficSignalControllerMode} of the {@link TileEntity}.
     */
    private static final TrafficSignalControllerMode NBT_MODE_DEFAULT = TrafficSignalControllerMode.FLASH;

    /**
     * The key used to store the index of the current {@link TrafficSignalPhase} in the {@link NBTTagCompound} of the
     * {@link TileEntity}.
     */
    private static final String NBT_PHASE_INDEX_KEY = "phase";

    /**
     * The index of the default current {@link TrafficSignalPhase} of the {@link TileEntity}.
     */
    private static final int NBT_PHASE_INDEX_DEFAULT = 0;

    //endregion

    //region: Instance Fields

    /**
     * The current {@link TrafficSignalControllerMode} of the {@link TileEntity}.
     */
    private TrafficSignalControllerMode currentMode;

    /**
     * The index of the current {@link TrafficSignalPhase} of the {@link TileEntity}.
     */
    private int currentPhase;

    //endregion

    //region: Tile Entity Methods

    /**
     * Method which returns the NBT tag compound with the tile entity's NBT data.
     *
     * @param compound the NBT tag compound to write the tile entity's NBT data to
     *
     * @return the NBT tag compound with the tile entity's NBT data
     *
     * @since 2022.1.0
     */
    @Override
    public NBTTagCompound writeNBT( NBTTagCompound compound ) {
        // Write current mode
        compound.setInteger( NBT_MODE_INDEX_KEY, currentMode.getId() );

        // Write current phase
        compound.setInteger( NBT_PHASE_INDEX_KEY, currentPhase );

        // Return the compound
        return compound;
    }

    /**
     * Method which reads the tile entity's NBT data from the supplied NBT tag compound.
     *
     * @param compound the NBT tag compound to read the tile entity's NBT data from
     *
     * @since 2022.1.0
     */
    @Override
    public void readNBT( NBTTagCompound compound ) {
        // Read current mode, and use default if not found
        if ( compound.hasKey( NBT_MODE_INDEX_KEY ) ) {
            currentMode = TrafficSignalControllerMode.getMode( compound.getInteger( NBT_MODE_INDEX_KEY ) );
        }
        else {
            currentMode = NBT_MODE_DEFAULT;
        }

        // Read current phase, and use default if not found
        if ( compound.hasKey( NBT_PHASE_INDEX_KEY ) ) {
            currentPhase = compound.getInteger( NBT_PHASE_INDEX_KEY );
        }
        else {
            currentPhase = NBT_PHASE_INDEX_DEFAULT;
        }
    }

    /**
     * Method which returns the tick rate of the tile entity.
     *
     * @return the tick rate of the tile entity
     *
     * @since 2022.1.0
     */
    @Override
    public long getTickRate() {
        if ( currentMode == null ) {
            return NBT_MODE_DEFAULT.getTickRate();
        }
        return currentMode.getTickRate();
    }

    /**
     * Method to handle the tick event of the tile entity.
     *
     * @since 2022.1.0
     */
    @Override
    public void onTick() {
        // Check if tile entity valid (still has associated block)
        boolean isTileValid = getWorld().getBlockState( getPos() )
                                        .getBlock() instanceof BlockTrafficSignalController.BlockCustom;
        if ( isTileValid ) {
            cycleSignals();
        }
        else {
            System.err.println( "Skipping tick of traffic signal controller tile entity, because the " +
                                        "controller has been deleted. This tile entity should be/should have " +
                                        "been deleted by Minecraft! Try reloading the map." );
        }
    }

    /**
     * Returns a boolean indicating if the tile entity should also tick on the client side. By default, the tile entity
     * will always tick on the server side, and in the event of singleplayer/local mode, the host client is considered
     * the server.
     *
     * @return a boolean indicating if the tile entity should also tick on the client side
     */
    @Override
    public boolean doClientTick() {
        return false;
    }

    //endregion

    //region: Player Interaction Methods

    public String switchMode() {
        // Switch to the next mode
        currentMode = currentMode.getNextMode();

        // Return the mode name
        return currentMode.getName();
    }

    //endregion

    //region: Signal Logic Methods

    private void cycleSignals() {
        // Check if controller has power
        boolean controllerPowered = getWorld().isBlockIndirectlyGettingPowered( getPos() ) > 0;

        if ( controllerPowered ) {

        }
        else {

        }
    }

    public boolean linkDevice( BlockPos signalPos ) {
        return false;
    }

    public boolean unlinkDevice( BlockPos signalPos ) {
        return false;
    }

    public int getCircuitCount() {
        return 0;
    }

    //endregion
}
