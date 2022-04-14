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
 * @version 2.0
 * @see AbstractTickableTileEntity
 * @since 2020.7.0
 */
@ElementsCitySuperMod.ModElement.Tag
public class TileEntityTrafficSignalController extends AbstractTickableTileEntity
{
    //region: Constant Fields

    /**
     * The key used to store the current {@link TrafficSignalControllerMode} in the {@link NBTTagCompound} of the
     * {@link TileEntity}.
     *
     * @since 2.0
     */
    private static final String NBT_MODE_KEY = "mode";

    /**
     * The key used to store the current {@link TrafficSignalPhase} in the {@link NBTTagCompound} of the
     * {@link TileEntity}.
     *
     * @since 2.0
     */
    private static final String NBT_PHASE_KEY = "phase";

    /**
     * The key used to store the list of {@link TrafficSignalControllerCircuits} of the {@link TileEntity}
     *
     * @since 2.0
     */
    private static final String NBT_CIRCUITS_KEY = "circuits";

    //endregion

    //region: Instance Fields

    /**
     * The current {@link TrafficSignalControllerMode} of the {@link TileEntity}.
     *
     * @since 2.0
     */
    private TrafficSignalControllerMode currentMode = TrafficSignalControllerMode.FLASH;

    /**
     * The index of the current {@link TrafficSignalPhase} of the {@link TileEntity}.
     *
     * @since 2.0
     */
    private TrafficSignalPhase currentPhase = new TrafficSignalPhase( -1, 200, 100,
                                                                      TrafficSignalPhaseApplicability.NONE );

    /**
     * The list of {@link TrafficSignalControllerCircuits} of the {@link TileEntity}.
     *
     * @since 2.0
     */
    private TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();

    //endregion

    //region: Tile Entity Methods

    /**
     * Method which returns the NBT tag compound with the tile entity's NBT data.
     *
     * @param compound the NBT tag compound to write the tile entity's NBT data to
     *
     * @return the NBT tag compound with the tile entity's NBT data
     *
     * @since 1.0
     */
    @Override
    public NBTTagCompound writeNBT( NBTTagCompound compound ) {
        if ( currentMode == null ) {
            throw new IllegalArgumentException( "The current mode of the tile entity is null!" );
        }

        // Write current mode
        compound.setInteger( NBT_MODE_KEY, currentMode.getId() );

        // Write current phase
        compound.setTag( NBT_PHASE_KEY, currentPhase.toNBT() );

        // Write circuits
        compound.setTag( NBT_CIRCUITS_KEY, circuits.toNBT() );

        // Return the compound
        return compound;
    }

    /**
     * Method which reads the tile entity's NBT data from the supplied NBT tag compound.
     *
     * @param compound the NBT tag compound to read the tile entity's NBT data from
     *
     * @since 1.0
     */
    @Override
    public void readNBT( NBTTagCompound compound ) {
        // Read current mode, and use default if not found
        if ( compound.hasKey( NBT_MODE_KEY ) ) {
            currentMode = TrafficSignalControllerMode.getMode( compound.getInteger( NBT_MODE_KEY ) );
        }

        // Read current phase, and use default if not found
        if ( compound.hasKey( NBT_PHASE_KEY ) ) {
            currentPhase = TrafficSignalPhase.fromNBT( compound.getCompoundTag( NBT_PHASE_KEY ) );
        }

        // Read circuits, and use default if not found
        if ( compound.hasKey( NBT_CIRCUITS_KEY ) ) {
            circuits = TrafficSignalControllerCircuits.fromNBT( compound.getCompoundTag( NBT_CIRCUITS_KEY ) );
        }
    }

    /**
     * Method which returns the tick rate of the tile entity.
     *
     * @return the tick rate of the tile entity
     *
     * @since 2.0
     */
    @Override
    public long getTickRate() {
        return currentMode.getTickRate();
    }

    /**
     * Method to handle the tick event of the tile entity.
     *
     * @since 1.1
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
     *
     * @since 2.0
     */
    @Override
    public boolean doClientTick() {
        return false;
    }

    //endregion

    //region: Player Interaction Methods

    /**
     * Method which handles the switch mode interaction by a player. The traffic signal controller will switch to the
     * next mode, and the name of the new mode will be returned.
     *
     * @return the name of the new traffic signal controller mode
     *
     * @since 2.0
     */
    public String switchMode() {
        // Switch to the next mode
        currentMode = currentMode.getNextMode();

        // Return the mode name
        return currentMode.getName();
    }

    //endregion

    //region: Signal Logic Methods

    /**
     * Method which handles the cycling of the traffic signal controller. The traffic signal controller will cycle the
     * current phase based on the current power supply status, current mode, and the sensor values.
     *
     * @since 1.0
     */
    private void cycleSignals() {
        // Check if controller has power
        boolean controllerPowered = getWorld().isBlockIndirectlyGettingPowered( getPos() ) > 0;

        // Store old phase to check if phase changed
        TrafficSignalPhase oldPhase = currentPhase;

        // Get the next phase
        currentPhase = TrafficSignalControllerUtilities.getNextPhaseForCircuits( currentPhase, circuits,
                                                                                 controllerPowered );

        // Apply the phase if it has changed
        if ( !oldPhase.equals( currentPhase ) ) {
            TrafficSignalControllerUtilities.applyPhase( currentPhase, getWorld() );
        }
    }

    public boolean linkDevice( BlockPos blockPos, AbstractBlockControllableSignal.SIGNAL_SIDE signalSide, int circuit )
    {
        // Create boolean to store if the device link was added
        boolean addedLink = false;

        // Get adjusted circuit index
        int adjustedCircuit = circuit - 1;

        // Process device linking if not already linked
        if ( !circuits.isDeviceLinked( blockPos ) ) {
            // Get the circuit to link to
            TrafficSignalControllerCircuit circuitToLink = circuits.getCircuit( adjustedCircuit );

            // Link to the proper list
            if ( signalSide == AbstractBlockControllableSignal.SIGNAL_SIDE.LEFT ) {
                circuitToLink.linkLeftSignal( blockPos );
            }
            else if ( signalSide == AbstractBlockControllableSignal.SIGNAL_SIDE.THROUGH ) {
                circuitToLink.linkAheadSignal( blockPos );
            }
            else if ( signalSide == AbstractBlockControllableSignal.SIGNAL_SIDE.RIGHT ) {
                circuitToLink.linkRightSignal( blockPos );
            }
            else if ( signalSide == AbstractBlockControllableSignal.SIGNAL_SIDE.PEDESTRIAN ) {
                circuitToLink.linkPedestrianSignal( blockPos );
            }
            else if ( signalSide == AbstractBlockControllableSignal.SIGNAL_SIDE.PROTECTED ) {
                circuitToLink.linkProtectedSignal( blockPos );
            }
            else if ( signalSide == AbstractBlockControllableSignal.SIGNAL_SIDE.FLASHING_LEFT ) {
                circuitToLink.linkHybridLeftSignal( blockPos );
            }
            else if ( signalSide == AbstractBlockControllableSignal.SIGNAL_SIDE.NA_SENSOR ) {
                circuitToLink.linkSensor( blockPos );
            }

        }

        // Return boolean indicating if the device link was added
        return addedLink;
    }

    public boolean unlinkDevice( BlockPos signalPos ) {
        return false;
    }

    public int getCircuitCount() {
        return circuits.getCircuitCount();
    }

    //endregion
}
