package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.tabs.CsmTabTrafficSignals;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal.SIGNAL_SIDE;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.*;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.PreviousFormatTrafficSignalCircuit;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * Tile entity for the traffic signal controller block.
 *
 * @author Mica Technologies
 * @version 2.0
 */
@ElementsCitySuperMod.ModElement.Tag
public class TileEntityTrafficSignalController extends AbstractTickableTileEntity
{

    //region: Instance Fields

    /**
     * The current mode of the traffic signal controller.
     *
     * @since 2.0
     */
    private TrafficSignalControllerMode mode = TrafficSignalControllerMode.FLASH;

    /**
     * The current operating mode of the traffic signal controller.
     *
     * @since 2.0
     */
    private TrafficSignalControllerMode operatingMode = mode;

    /**
     * Boolean indicating whether the traffic signal controller is currently paused.
     *
     * @since 2.0
     */
    private boolean paused = false;

    /**
     * The list of circuits for the traffic signal controller.
     *
     * @since 2.0
     */
    private TrafficSignalControllerCircuits circuits = new TrafficSignalControllerCircuits();

    /**
     * The overlaps for the traffic signal controller.
     *
     * @since 2.0
     */
    private TrafficSignalControllerOverlaps overlaps = new TrafficSignalControllerOverlaps();

    /**
     * The list of cached phases for the traffic signal controller.
     *
     * @since 2.0
     */
    private TrafficSignalPhases cachedPhases = new TrafficSignalPhases( getWorld(), circuits );

    /**
     * The time of the last phase change for the traffic signal controller.
     *
     * @since 2.0
     */
    private long lastPhaseChangeTime = -1;

    /**
     * The time of the last phase applicability change for the traffic signal controller.
     *
     * @since 2.0
     */
    private long lastPhaseApplicabilityChangeTime = -1;

    /**
     * The time of the last pedestrian phase for the traffic signal controller.
     *
     * @since 2.0
     */
    private long lastPedPhaseTime = -1;

    /**
     * The current phase for the traffic signal controller.
     *
     * @since 2.0
     */
    private TrafficSignalPhase currentPhase = null;

    /**
     * The current fault message for the traffic signal controller. This is used to display fault messages to the user
     * when the traffic signal controller is in fault mode.
     * <p>
     * If the traffic signal controller is not in fault mode, this will be an empty string.
     *
     * @since 2.0
     */
    private String currentFaultMessage = "";

    /**
     * Boolean indicating whether the traffic signal controller should fallback to flash mode at night.
     *
     * @since 2.0
     */
    private boolean nightlyFallbackToFlashMode = false;

    /**
     * Boolean indicating whether the traffic signal controller should fallback to flash mode after a power loss.
     *
     * @since 2.0
     */
    private boolean powerLossFallbackToFlashMode = false;

    /**
     * The overlap pedestrian signals setting for the traffic signal controller.
     *
     * @since 2.0
     */
    private boolean overlapPedestrianSignals = false;

    /**
     * The yellow time for the traffic signal controller.
     *
     * @since 2.0
     */
    private long yellowTime = 80;

    /**
     * The flashing don't walk time for the traffic signal controller.
     *
     * @since 2.0
     */
    private long flashDontWalkTime = 300;

    /**
     * The flashing all red time for the traffic signal controller.
     *
     * @since 2.0
     */
    private long allRedTime = 60;

    /**
     * The minimum service time when servicing requests to the traffic signal controller in
     * {@link TrafficSignalControllerMode#REQUESTABLE} mode.
     *
     * @since 2.0
     */
    private long minRequestableServiceTime = 500;

    /**
     * The maximum service time when servicing requests to the traffic signal controller in
     * {@link TrafficSignalControllerMode#REQUESTABLE} mode.
     *
     * @since 2.0
     */
    private long maxRequestableServiceTime = 2400;

    /**
     * The minimum green time when servicing circuits configured to the traffic signal controller in
     * {@link TrafficSignalControllerMode#NORMAL} mode.
     *
     * @since 2.0
     */
    private long minGreenTime = 400;

    /**
     * The maximum green time when servicing circuits configured to the traffic signal controller in
     * {@link TrafficSignalControllerMode#NORMAL} mode.
     *
     * @since 2.0
     */
    private long maxGreenTime = 1800;

    /**
     * The secondary minimum green time when servicing circuits configured to the traffic signal controller in
     * {@link TrafficSignalControllerMode#NORMAL} mode.
     *
     * @since 2.0
     */
    private long minGreenTimeSecondary = 200;

    /**
     * The secondary maximum green time when servicing circuits configured to the traffic signal controller in
     * {@link TrafficSignalControllerMode#NORMAL} mode.
     *
     * @since 2.0
     */
    private long maxGreenTimeSecondary = 1200;

    /**
     * The dedicated pedestrian signal time when servicing circuits configured to the traffic signal controller in
     * {@link TrafficSignalControllerMode#NORMAL} mode.
     *
     * @since 2.0
     */
    private long dedicatedPedSignalTime = 300;

    /**
     * Boolean which alternates between true and false every time the traffic signal controller is updated. This is used
     * to determine when the traffic signal controller should flash signals in flash mode.
     *
     * @since 2.0
     */
    private boolean alternatingFlash = false;

    /**
     * The {@link NBTTagCompound} containing the traffic signal controller previous NBT formatted data. This is used to
     * upgrade the previous NBT format on tick.
     */
    private NBTTagCompound previousNbt = null;

    /**
     * Boolean indicating whether the traffic signal controller upgraded data from the previous NBT format. This is used
     * to determine whether to remove the previous NBT format on NBT save.
     */
    private boolean upgradedPreviousNBTFormat = false;

    //endregion

    //region: Tile Entity/NBT Methods

    /**
     * Returns the tick rate of the traffic signal controller tile entity.
     *
     * @return the tick rate of the traffic signal controller tile entity
     *
     * @since 2.0
     */
    @Override
    public long getTickRate() {
        return operatingMode.getTickRate();
    }

    /**
     * Handles the tick event for the traffic signal controller.
     *
     * @since 2.0
     */
    @Override
    public void onTick() {
        // Place the entire tick event in a try/catch block to catch any exceptions and enter fault state
        try {
            // Verify integrity of cached phases and reset controller if necessary
            if ( !cachedPhases.verifyPhaseCount() ) {
                resetController( true, false );
            }

            // Check for previous NBT data format and load it if present
            if ( !upgradedPreviousNBTFormat && previousNbt != null ) {
                try {
                    System.err.println(
                            "Importing previous NBT data format for traffic signal controller at " + getPos() );
                    importPreviousNBTDataFormat( getWorld(), previousNbt );
                    previousNbt = null;
                    upgradedPreviousNBTFormat = true;
                    resetController( true, false );
                    System.err.println(
                            "Successfully imported previous NBT data format for traffic signal controller at " +
                                    getPos() );
                }
                catch ( Exception e ) {
                    currentFaultMessage = "Failed to import previous NBT data format!";
                    operatingMode = TrafficSignalControllerMode.FORCED_FAULT;
                }
            }

            // Pass tick event to traffic signal controller ticker
            long tickTime = getWorld().getTotalWorldTime();
            long timeSinceLastPhaseChange = tickTime - lastPhaseChangeTime;
            long timeSinceLastPhaseApplicabilityChange = tickTime - lastPhaseApplicabilityChangeTime;
            TrafficSignalPhase newPhase = TrafficSignalControllerTicker.tick( getWorld(), mode, operatingMode, circuits,
                                                                              overlaps, cachedPhases, currentPhase,
                                                                              timeSinceLastPhaseApplicabilityChange,
                                                                              timeSinceLastPhaseChange,
                                                                              alternatingFlash,
                                                                              overlapPedestrianSignals, yellowTime,
                                                                              flashDontWalkTime, allRedTime,
                                                                              minRequestableServiceTime,
                                                                              maxRequestableServiceTime, minGreenTime,
                                                                              maxGreenTime, minGreenTimeSecondary,
                                                                              maxGreenTimeSecondary,
                                                                              dedicatedPedSignalTime );

            // If the phase index has changed, update the phase
            if ( newPhase != null ) {
                // Store previous phase temporarily
                TrafficSignalPhase previousPhase = currentPhase;

                // Update current phase and last phase change time
                currentPhase = newPhase;
                lastPhaseChangeTime = tickTime;
                if ( currentPhase.getApplicability() == TrafficSignalPhaseApplicability.PEDESTRIAN ) {
                    lastPedPhaseTime = lastPhaseChangeTime;
                }

                // Update last phase applicability change time, if applicable
                if ( previousPhase == null || previousPhase.getApplicability() != currentPhase.getApplicability() ) {
                    lastPhaseApplicabilityChangeTime = lastPhaseChangeTime;
                }

                // Change to the indicated phase (if valid)
                currentPhase.apply( getWorld() );
            }
            // If the current phase is null (and newPhase is null also), enter fault state
            else if ( currentPhase == null ) {
                enterFaultState( "An invalid phase condition was encountered for the " + mode.getName() + " mode." );
                System.err.println( "Traffic signal controller error: Invalid phase condition for mode " +
                                            mode.getName() +
                                            " on controller at " +
                                            getPos() );
            }
        }
        // If an exception is caught, enter fault state
        catch ( Exception e ) {
            enterFaultState( "A critical error occurred while ticking for the " + mode.getName() + " mode." );
            System.err.println( "Traffic signal controller error: An exception was caught while ticking for mode " +
                                        mode.getName() +
                                        " on controller at " +
                                        getPos() );
            e.printStackTrace();
        }

        // Update alternating flash boolean
        alternatingFlash = !alternatingFlash;

        // Update the operating mode
        updateOperatingMode();

    }

    /**
     * Returns a boolean indicating if the traffic signal controller tile entity should also tick on the client side. By
     * default, the traffic signal controller tile entity will always tick on the server side, and in the event of
     * single-player/local mode, the host client is considered the server.
     * <p>
     * This method is overridden to return false, as the traffic signal controller tile entity should not tick on the
     * client side.
     * </p>
     *
     * @return a boolean indicating if the traffic signal controller tile entity should also tick on the client side.
     *         This method always returns false.
     *
     * @since 2.0
     */
    @Override
    public boolean doClientTick() {
        return false;
    }

    /**
     * Returns a boolean indicating if the traffic signal controller tile entity ticking should be paused. If the
     * traffic signal controller tile entity is paused, the tick event will not be called.
     * <p>
     * This method returns true if the traffic signal controller is not powered, and false otherwise.
     * </p>
     *
     * @return a boolean indicating if the traffic signal controller tile entity ticking should be paused. This method
     *         returns true if the traffic signal controller is not powered, and false otherwise.
     *
     * @since 2.0
     */
    @Override
    public boolean pauseTicking() {
        boolean shouldPause = !getWorld().isBlockPowered( getPos() ) && !powerLossFallbackToFlashMode;

        // Mark dirty if the paused state has changed
        if ( shouldPause != paused ) {
            // Turn off all signals if the controller is not powered
            if ( shouldPause ) {
                circuits.powerOffAllSignals( getWorld() );
            }

            paused = shouldPause;
            markDirtySync( getWorld(), getPos() );
        }

        return paused;
    }

    /**
     * Returns the specified NBT tag compound with the {@link TileEntityTrafficSignalController}'s NBT data.
     *
     * @param compound the NBT tag compound to write the {@link TileEntityTrafficSignalController}'s NBT data to
     *
     * @return the NBT tag compound with the {@link TileEntityTrafficSignalController}'s NBT data
     *
     * @since 2.0
     */
    @Override
    public NBTTagCompound writeNBT( NBTTagCompound compound ) {
        // Write the mode to NBT
        compound.setInteger( TrafficSignalControllerNBTKeys.MODE, mode.toNBT() );

        // Write the operating mode to NBT
        compound.setInteger( TrafficSignalControllerNBTKeys.OPERATING_MODE, operatingMode.toNBT() );

        // Write the circuits to NBT
        compound.setTag( TrafficSignalControllerNBTKeys.CIRCUITS, circuits.toNBT() );

        // Write the overlaps to NBT
        compound.setTag( TrafficSignalControllerNBTKeys.OVERLAPS, overlaps.toNBT() );

        // Write the paused state to NBT
        compound.setBoolean( TrafficSignalControllerNBTKeys.PAUSED, paused );

        // Write the cached phases to NBT
        compound.setTag( TrafficSignalControllerNBTKeys.CACHED_PHASES, cachedPhases.toNBT() );

        // Write the last phase change time to NBT
        compound.setLong( TrafficSignalControllerNBTKeys.LAST_PHASE_CHANGE_TIME, lastPhaseChangeTime );

        // Write the last phase applicability change time to NBT
        compound.setLong( TrafficSignalControllerNBTKeys.LAST_PHASE_APPLICABILITY_CHANGE_TIME,
                          lastPhaseApplicabilityChangeTime );

        // Write the last pedestrian phase time to NBT
        compound.setLong( TrafficSignalControllerNBTKeys.LAST_PEDESTRIAN_PHASE_TIME, lastPedPhaseTime );

        // Write the current phase to NBT if non-null
        if ( currentPhase != null ) {
            compound.setTag( TrafficSignalControllerNBTKeys.CURRENT_PHASE, currentPhase.toNBT() );
        }
        else {
            compound.removeTag( TrafficSignalControllerNBTKeys.CURRENT_PHASE );
        }

        // Write the current fault message to NBT
        compound.setString( TrafficSignalControllerNBTKeys.CURRENT_FAULT_MESSAGE, currentFaultMessage );

        // Write the nightly fallback to flash mode setting to NBT
        compound.setBoolean( TrafficSignalControllerNBTKeys.NIGHTLY_FALLBACK_FLASH_MODE, nightlyFallbackToFlashMode );

        // Write the power loss fallback to flash mode setting to NBT
        compound.setBoolean( TrafficSignalControllerNBTKeys.POWER_LOSS_FALLBACK_FLASH_MODE,
                             powerLossFallbackToFlashMode );

        // Write the overlap pedestrian signals setting to NBT
        compound.setBoolean( TrafficSignalControllerNBTKeys.OVERLAP_PEDESTRIAN_SIGNALS, overlapPedestrianSignals );

        // Write the yellow time to NBT
        compound.setLong( TrafficSignalControllerNBTKeys.YELLOW_TIME, yellowTime );

        // Write the flashing don't walk time to NBT
        compound.setLong( TrafficSignalControllerNBTKeys.FLASH_DONT_WALK_TIME, flashDontWalkTime );

        // Write the all red time to NBT
        compound.setLong( TrafficSignalControllerNBTKeys.ALL_RED_TIME, allRedTime );

        // Write the minimum requestable service time to NBT
        compound.setLong( TrafficSignalControllerNBTKeys.MIN_REQUESTABLE_SERVICE_TIME, minRequestableServiceTime );

        // Write the maximum requestable service time to NBT
        compound.setLong( TrafficSignalControllerNBTKeys.MAX_REQUESTABLE_SERVICE_TIME, maxRequestableServiceTime );

        // Write the minimum green time to NBT
        compound.setLong( TrafficSignalControllerNBTKeys.MIN_GREEN_TIME, minGreenTime );

        // Write the maximum green time to NBT
        compound.setLong( TrafficSignalControllerNBTKeys.MAX_GREEN_TIME, maxGreenTime );

        // Write the secondary minimum green time to NBT
        compound.setLong( TrafficSignalControllerNBTKeys.MIN_GREEN_TIME_SECONDARY, minGreenTimeSecondary );

        // Write the secondary maximum green time to NBT
        compound.setLong( TrafficSignalControllerNBTKeys.MAX_GREEN_TIME_SECONDARY, maxGreenTimeSecondary );

        // Write the dedicated pedestrian signal time to NBT
        compound.setLong( TrafficSignalControllerNBTKeys.DEDICATED_PED_SIGNAL_TIME, dedicatedPedSignalTime );

        // Write the upgrade previous NBT data format flag to NBT
        compound.setBoolean( TrafficSignalControllerNBTKeys.UPGRADED_PREVIOUS_NBT_FORMAT, upgradedPreviousNBTFormat );

        // Return the NBT tag compound with previous NBT data format removed
        if ( upgradedPreviousNBTFormat && hasPreviousNBTDataFormat( compound ) ) {
            removePreviousNBTDataFormat( compound );
            System.out.println( "Removed previous NBT data format from traffic signal controller at " + getPos() );
        }
        return compound;
    }

    /**
     * Processes the reading of the {@link TileEntityTrafficSignalController}'s NBT data from the supplied NBT tag
     * compound.
     *
     * @param compound the NBT tag compound to read the {@link TileEntityTrafficSignalController}'s NBT data from
     *
     * @since 2.0
     */
    @Override
    public void readNBT( NBTTagCompound compound ) {
        // Load the traffic signal controller mode
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.MODE ) ) {
            mode = TrafficSignalControllerMode.fromNBT( compound.getInteger( TrafficSignalControllerNBTKeys.MODE ) );
        }

        // Load the traffic signal controller operating mode
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.OPERATING_MODE ) ) {
            operatingMode = TrafficSignalControllerMode.fromNBT(
                    compound.getInteger( TrafficSignalControllerNBTKeys.OPERATING_MODE ) );
        }
        else {
            operatingMode = mode;
        }

        // Load the traffic signal controller circuits
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.CIRCUITS ) ) {
            circuits = TrafficSignalControllerCircuits.fromNBT(
                    compound.getCompoundTag( TrafficSignalControllerNBTKeys.CIRCUITS ) );
        }

        // Load the traffic signal controller overlaps
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.OVERLAPS ) ) {
            overlaps = TrafficSignalControllerOverlaps.fromNBT(
                    compound.getCompoundTag( TrafficSignalControllerNBTKeys.OVERLAPS ) );
        }

        // Load the traffic signal controller paused state
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.PAUSED ) ) {
            paused = compound.getBoolean( TrafficSignalControllerNBTKeys.PAUSED );
        }

        // Load the traffic signal controller cached phases
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.CACHED_PHASES ) ) {
            cachedPhases = TrafficSignalPhases.fromNBT(
                    compound.getCompoundTag( TrafficSignalControllerNBTKeys.CACHED_PHASES ) );
        }

        // Load the traffic signal controller last phase change time
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.LAST_PHASE_CHANGE_TIME ) ) {
            lastPhaseChangeTime = compound.getLong( TrafficSignalControllerNBTKeys.LAST_PHASE_CHANGE_TIME );
        }

        // Load the traffic signal controller last phase applicability change time
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.LAST_PHASE_APPLICABILITY_CHANGE_TIME ) ) {
            lastPhaseApplicabilityChangeTime = compound.getLong(
                    TrafficSignalControllerNBTKeys.LAST_PHASE_APPLICABILITY_CHANGE_TIME );
        }

        // Load the traffic signal controller last pedestrian phase time
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.LAST_PEDESTRIAN_PHASE_TIME ) ) {
            lastPedPhaseTime = compound.getLong( TrafficSignalControllerNBTKeys.LAST_PEDESTRIAN_PHASE_TIME );
        }

        // Load the traffic signal controller current phase
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.CURRENT_PHASE ) ) {
            currentPhase = TrafficSignalPhase.fromNBT(
                    compound.getCompoundTag( TrafficSignalControllerNBTKeys.CURRENT_PHASE ) );
        }

        // Load the traffic signal controller current fault message
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.CURRENT_FAULT_MESSAGE ) ) {
            currentFaultMessage = compound.getString( TrafficSignalControllerNBTKeys.CURRENT_FAULT_MESSAGE );
        }

        // Load the traffic signal controller nightly fallback to flash mode setting
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.NIGHTLY_FALLBACK_FLASH_MODE ) ) {
            nightlyFallbackToFlashMode = compound.getBoolean(
                    TrafficSignalControllerNBTKeys.NIGHTLY_FALLBACK_FLASH_MODE );
        }

        // Load the traffic signal controller power loss fallback to flash mode setting
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.POWER_LOSS_FALLBACK_FLASH_MODE ) ) {
            powerLossFallbackToFlashMode = compound.getBoolean(
                    TrafficSignalControllerNBTKeys.POWER_LOSS_FALLBACK_FLASH_MODE );
        }

        // Load the traffic signal controller overlap pedestrian signals setting
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.OVERLAP_PEDESTRIAN_SIGNALS ) ) {
            overlapPedestrianSignals = compound.getBoolean( TrafficSignalControllerNBTKeys.OVERLAP_PEDESTRIAN_SIGNALS );
        }

        // Load the traffic signal controller yellow time
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.YELLOW_TIME ) ) {
            yellowTime = compound.getLong( TrafficSignalControllerNBTKeys.YELLOW_TIME );
        }

        // Load the traffic signal controller flashing don't walk time
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.FLASH_DONT_WALK_TIME ) ) {
            flashDontWalkTime = compound.getLong( TrafficSignalControllerNBTKeys.FLASH_DONT_WALK_TIME );
        }

        // Load the traffic signal controller all red time
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.ALL_RED_TIME ) ) {
            allRedTime = compound.getLong( TrafficSignalControllerNBTKeys.ALL_RED_TIME );
        }

        // Load the traffic signal controller minimum requestable service time
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.MIN_REQUESTABLE_SERVICE_TIME ) ) {
            minRequestableServiceTime = compound.getLong( TrafficSignalControllerNBTKeys.MIN_REQUESTABLE_SERVICE_TIME );
        }

        // Load the traffic signal controller maximum requestable service time
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.MAX_REQUESTABLE_SERVICE_TIME ) ) {
            maxRequestableServiceTime = compound.getLong( TrafficSignalControllerNBTKeys.MAX_REQUESTABLE_SERVICE_TIME );
        }

        // Load the traffic signal controller minimum green time
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.MIN_GREEN_TIME ) ) {
            minGreenTime = compound.getLong( TrafficSignalControllerNBTKeys.MIN_GREEN_TIME );
        }

        // Load the traffic signal controller maximum green time
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.MAX_GREEN_TIME ) ) {
            maxGreenTime = compound.getLong( TrafficSignalControllerNBTKeys.MAX_GREEN_TIME );
        }

        // Load the traffic signal controller secondary minimum green time
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.MIN_GREEN_TIME_SECONDARY ) ) {
            minGreenTimeSecondary = compound.getLong( TrafficSignalControllerNBTKeys.MIN_GREEN_TIME_SECONDARY );
        }

        // Load the traffic signal controller secondary maximum green time
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.MAX_GREEN_TIME_SECONDARY ) ) {
            maxGreenTimeSecondary = compound.getLong( TrafficSignalControllerNBTKeys.MAX_GREEN_TIME_SECONDARY );
        }

        // Load the traffic signal controller dedicated pedestrian signal time
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.DEDICATED_PED_SIGNAL_TIME ) ) {
            dedicatedPedSignalTime = compound.getLong( TrafficSignalControllerNBTKeys.DEDICATED_PED_SIGNAL_TIME );
        }

        // Load the traffic signal controller upgrade previous NBT data format flag
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.UPGRADED_PREVIOUS_NBT_FORMAT ) ) {
            upgradedPreviousNBTFormat = compound.getBoolean(
                    TrafficSignalControllerNBTKeys.UPGRADED_PREVIOUS_NBT_FORMAT );
        }

        // Check for any previous NBT data format keys
        if ( !upgradedPreviousNBTFormat && hasPreviousNBTDataFormat( compound ) ) {
            previousNbt = compound;
        }
    }

    /**
     * Utility method which returns a boolean indicating whether the provided {@link NBTTagCompound} has the previous
     * NBT data format.
     *
     * @param compound The {@link NBTTagCompound} to check for the previous NBT data format.
     *
     * @return {@code true} if the provided {@link NBTTagCompound} has the previous NBT data format, {@code false}
     *         otherwise.
     *
     * @since 2.0
     */
    public static boolean hasPreviousNBTDataFormat( NBTTagCompound compound ) {

        // Check for any previous NBT data format keys
        boolean previousNBTDataFormatFound = false;
        for ( String key : TrafficSignalControllerNBTKeys.V1_KEY_LIST ) {
            if ( compound.hasKey( key ) ) {
                previousNBTDataFormatFound = true;
                break;
            }
        }

        return previousNBTDataFormatFound;
    }

    /**
     * Utility method which removes the previous NBT data format from the provided {@link NBTTagCompound}.
     *
     * @param compound The {@link NBTTagCompound} to remove the previous NBT data format from.
     *
     * @since 2.0
     */
    public static void removePreviousNBTDataFormat( NBTTagCompound compound ) {

        // Remove any previous NBT data format keys
        for ( String key : TrafficSignalControllerNBTKeys.V1_KEY_LIST ) {
            if ( compound.hasKey( key ) ) {
                compound.removeTag( key );
            }
        }
    }

    /**
     * Utility method which imports a previous NBT data formatted circuit ({@link PreviousFormatTrafficSignalCircuit})
     * to the current/new format circuit ({@link TrafficSignalControllerCircuit}) and adds it to the traffic signal
     * controller's {@link TrafficSignalControllerCircuits} list, {@link #circuits}.
     *
     * @param linkWorld           The {@link World} to link the imported circuit to.
     * @param importSourceCircuit The {@link PreviousFormatTrafficSignalCircuit} to import the previous NBT data
     *                            formatted circuit ({@link PreviousFormatTrafficSignalCircuit}) from.
     *
     * @since 2.0
     */
    public void importPreviousNBTDataFormatCircuit( World linkWorld,
                                                    PreviousFormatTrafficSignalCircuit importSourceCircuit )
    {
        // Create import destination circuit object
        TrafficSignalControllerCircuit importDestinationCircuit = new TrafficSignalControllerCircuit();

        // Get signal lists from imported old circuit
        List< BlockPos > importedAheadSignals = importSourceCircuit.getAheadSignals();
        System.out.println( "importedAheadSignals: " + importedAheadSignals.size() );
        List< BlockPos > importedLeftSignals = importSourceCircuit.getLeftSignals();
        System.out.println( "importedLeftSignals: " + importedLeftSignals.size() );
        List< BlockPos > importedHybridLeftSignals = importSourceCircuit.getHybridLeftSignals();
        System.out.println( "importedHybridLeftSignals: " + importedHybridLeftSignals.size() );
        List< BlockPos > importedRightSignals = importSourceCircuit.getRightSignals();
        System.out.println( "importedRightSignals: " + importedRightSignals.size() );
        List< BlockPos > importedPedestrianSignals = importSourceCircuit.getPedestrianSignals();
        System.out.println( "importedPedestrianSignals: " + importedPedestrianSignals.size() );
        List< BlockPos > importedProtectedSignals = importSourceCircuit.getProtectedSignals();
        System.out.println( "importedProtectedSignals: " + importedProtectedSignals.size() );
        List< BlockPos > importedSensors = importSourceCircuit.getSensors();
        System.out.println( "importedSensors: " + importedSensors.size() );

        // Loop through lists and add to new circuit format with proper facing direction and/or APS list
        importDestinationCircuit.tryLinkDevicesMigration( linkWorld, importedSensors, true );
        importDestinationCircuit.tryLinkDevicesMigration( linkWorld, importedRightSignals, false );
        importDestinationCircuit.tryLinkDevicesMigration( linkWorld, importedProtectedSignals, false );
        importDestinationCircuit.tryLinkDevicesMigration( linkWorld, importedPedestrianSignals, false );
        importDestinationCircuit.tryLinkDevicesMigration( linkWorld, importedHybridLeftSignals, false );
        importDestinationCircuit.tryLinkDevicesMigration( linkWorld, importedAheadSignals, false );
        importDestinationCircuit.tryLinkDevicesMigration( linkWorld, importedLeftSignals, false );
        importDestinationCircuit.tryLinkDevicesMigration( linkWorld, importedPedestrianSignals, false );

        // Add completed import destination circuit object
        circuits.addCircuit( importDestinationCircuit );
        System.out.println( "Imported circuit with " + importDestinationCircuit.getSize() + " devices." );
    }

    /**
     * Utility method which imports the previous NBT data format from the provided {@link NBTTagCompound}.
     *
     * @param importWorld The {@link World} to link the imported circuit to.
     * @param compound    The {@link NBTTagCompound} to import the previous NBT data format from.
     *
     * @since 2.0
     */
    public void importPreviousNBTDataFormat( World importWorld, NBTTagCompound compound ) {

        // Import serialized signals list (if present)
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.V1_KEY_SERIALIZED_SIGNAL_CIRCUIT_LIST ) ) {
            System.out.println( "Importing previous NBT data format..." );
            String serializedSignalCircuitList = compound.getString(
                    TrafficSignalControllerNBTKeys.V1_KEY_SERIALIZED_SIGNAL_CIRCUIT_LIST );
            String[] serializedSignalCircuits = serializedSignalCircuitList.split(
                    TrafficSignalControllerNBTKeys.V1_SERIALIZED_SIGNAL_CIRCUIT_LIST_SEPARATOR );
            for ( String serializedSignalCircuit : serializedSignalCircuits ) {
                PreviousFormatTrafficSignalCircuit importedCircuit = new PreviousFormatTrafficSignalCircuit(
                        serializedSignalCircuit );
                importPreviousNBTDataFormatCircuit( importWorld, importedCircuit );
            }
            System.out.println( "Imported " + circuits.getCircuitCount() + " circuits." );
        }
        else {
            System.out.println( "No previous NBT data formatted circuits to import." );
        }

        // Import mode
        if ( compound.hasKey( TrafficSignalControllerNBTKeys.V1_KEY_CURRENT_MODE ) ) {
            int previousMode = compound.getInteger( TrafficSignalControllerNBTKeys.V1_KEY_CURRENT_MODE );
            if ( previousMode == TrafficSignalControllerNBTKeys.V1_CURRENT_MODE_FLASH ) {
                mode = TrafficSignalControllerMode.FLASH;
            }
            else if ( previousMode == TrafficSignalControllerNBTKeys.V1_CURRENT_MODE_STANDARD ) {
                mode = TrafficSignalControllerMode.NORMAL;
            }
            else if ( previousMode == TrafficSignalControllerNBTKeys.V1_CURRENT_MODE_STANDARD_FLASH_NIGHT ) {
                mode = TrafficSignalControllerMode.NORMAL;
                nightlyFallbackToFlashMode = true;
            }
            else if ( previousMode == TrafficSignalControllerNBTKeys.V1_CURRENT_MODE_STANDARD_FLASH_NO_POWER ) {
                mode = TrafficSignalControllerMode.NORMAL;
                powerLossFallbackToFlashMode = true;
            }
            else if ( previousMode == TrafficSignalControllerNBTKeys.V1_CURRENT_MODE_STANDARD_FLASH_NO_POWER_NIGHT ) {
                mode = TrafficSignalControllerMode.NORMAL;
                nightlyFallbackToFlashMode = true;
                powerLossFallbackToFlashMode = true;
            }
            else if ( previousMode == TrafficSignalControllerNBTKeys.V1_CURRENT_MODE_METER ) {
                mode = TrafficSignalControllerMode.RAMP_METER_FULL_TIME;
            }
            else if ( previousMode == TrafficSignalControllerNBTKeys.V1_CURRENT_MODE_REQUESTABLE ) {
                mode = TrafficSignalControllerMode.REQUESTABLE;
            }
            else {
                mode = TrafficSignalControllerMode.FORCED_FAULT;
            }
        }
        else {
            System.out.println( "No previous NBT data formatted mode to import." );
        }
    }

    //endregion

    //region: Getters and Setters

    /**
     * Gets the traffic signal controller's fallback to flash mode at night setting.
     *
     * @return true if the traffic signal controller's fallback to flash mode at night is enabled, false otherwise
     *
     * @since 2.0
     */
    public boolean getNightlyFallbackToFlashMode() {
        return nightlyFallbackToFlashMode;
    }

    /**
     * Sets the traffic signal controller's fallback to flash mode at night setting.
     *
     * @param nightlyFallbackToFlashMode true if the traffic signal controller's fallback to flash mode at night setting
     *                                   should be enabled, false otherwise
     *
     * @since 2.0
     */
    public void setNightlyFallbackToFlashMode( boolean nightlyFallbackToFlashMode ) {
        if ( this.nightlyFallbackToFlashMode != nightlyFallbackToFlashMode ) {
            this.nightlyFallbackToFlashMode = nightlyFallbackToFlashMode;
            resetController( false, true );
        }
    }

    /**
     * Gets the traffic signal controller's fallback to flash during power loss setting.
     *
     * @return true if the traffic signal controller's fallback to flash during power loss setting is enabled, false
     *         otherwise
     *
     * @since 2.0
     */
    public boolean getPowerLossFallbackToFlashMode() {
        return powerLossFallbackToFlashMode;
    }

    /**
     * Sets the traffic signal controller's fallback to flash during power loss setting.
     *
     * @param powerLossFallbackToFlashMode true if the traffic signal controller's fallback to flash during power loss
     *                                     setting should be enabled, false otherwise
     *
     * @since 2.0
     */
    public void setPowerLossFallbackToFlashMode( boolean powerLossFallbackToFlashMode ) {
        if ( this.powerLossFallbackToFlashMode != powerLossFallbackToFlashMode ) {
            this.powerLossFallbackToFlashMode = powerLossFallbackToFlashMode;
            resetController( false, true );
        }
    }

    /**
     * Gets the traffic signal controller's overlap pedestrian signals setting.
     *
     * @return true if the traffic signal controller's overlap pedestrian signals setting is enabled, false otherwise
     *
     * @since 2.0
     */
    public boolean getOverlapPedestrianSignals() {
        return overlapPedestrianSignals;
    }

    /**
     * Sets the traffic signal controller's overlap pedestrian signals setting.
     *
     * @param overlapPedestrianSignals true if the traffic signal controller's overlap pedestrian signals setting should
     *                                 be enabled, false otherwise
     *
     * @since 2.0
     */
    public void setOverlapPedestrianSignals( boolean overlapPedestrianSignals ) {
        if ( this.overlapPedestrianSignals != overlapPedestrianSignals ) {
            this.overlapPedestrianSignals = overlapPedestrianSignals;
            resetController( false, true );
        }
    }

    //endregion

    //region: Signal Controller Methods

    /**
     * Updates the operating traffic signal controller mode based on the current traffic signal controller state.
     *
     * @since 2.0
     */
    public void updateOperatingMode() {
        TrafficSignalControllerMode desiredOperatingMode;

        // If the traffic signal controller is currently in a fault state, return the fault mode
        if ( isInFaultState() ) {
            desiredOperatingMode = TrafficSignalControllerMode.FORCED_FAULT;
        }
        // If the traffic signal controller is currently in a fallback flash mode, return the flash mode
        else if ( isInFallbackFlashMode() ) {
            desiredOperatingMode = TrafficSignalControllerMode.FLASH;
        }
        // Otherwise, return the configured mode
        else {
            desiredOperatingMode = mode;
        }

        setOperatingMode( desiredOperatingMode );
    }

    /**
     * Sets the operating traffic signal controller mode. This is the mode that the traffic signal controller is
     * currently operating in. This may be different from the configured mode if the traffic signal controller is
     * currently in a fault state, or has fallen back to flash mode.
     *
     * @param newOperatingMode the new operating mode to set
     *
     * @since 2.0
     */
    private void setOperatingMode( TrafficSignalControllerMode newOperatingMode ) {
        if ( operatingMode != newOperatingMode ) {
            operatingMode = newOperatingMode;
            resetController( false, false );
        }
    }

    /**
     * Returns a boolean indicating if the traffic signal controller is currently in fallback flash mode.
     *
     * @return true if the traffic signal controller is currently in  fallback flash mode, false otherwise.
     *
     * @since 2.0
     */
    public boolean isInFallbackFlashMode() {
        boolean inFallbackFlashMode = false;

        // Set true if nightly fallback to flash mode is enabled and it is currently night time
        if ( nightlyFallbackToFlashMode && !getWorld().isDaytime() ) {
            inFallbackFlashMode = true;
        }

        // Set true if power loss fallback to flash mode is enabled and the traffic signal controller is currently
        // in a power loss state
        if ( powerLossFallbackToFlashMode && getWorld().isBlockIndirectlyGettingPowered( getPos() ) <= 0 ) {
            inFallbackFlashMode = true;
        }

        // Return false otherwise
        return inFallbackFlashMode;
    }

    /**
     * Returns a boolean indicating if the traffic signal controller is currently in a fault state.
     *
     * @return true if the traffic signal controller is currently in a fault state, false otherwise.
     *
     * @since 2.0
     */
    public boolean isInFaultState() {
        return currentFaultMessage != null && !currentFaultMessage.isEmpty();
    }

    /**
     * Returns the current fault message if the traffic signal controller is currently in a fault state, or an empty
     * string otherwise.
     *
     * @return the current fault message if the traffic signal controller is currently in a fault state, or an empty
     *         string otherwise.
     *
     * @since 2.0
     */
    public String getCurrentFaultMessage() {
        return currentFaultMessage;
    }

    /**
     * Enters a fault state with the specified fault message.
     *
     * @param faultMessage the fault message to display
     *
     * @since 2.0
     */
    private void enterFaultState( String faultMessage ) {
        // Store current fault message
        currentFaultMessage = faultMessage;

        // Switch to fault mode
        operatingMode = TrafficSignalControllerMode.FORCED_FAULT;
        resetController( false, false );
    }

    /**
     * Clears the fault state if the traffic signal controller is currently in a fault state.
     *
     * @since 2.0
     */
    public void clearFaultState() {
        // Clear current fault message
        currentFaultMessage = "";

        // Switch to configured mode
        operatingMode = mode;
        resetController( false, true );
    }

    /**
     * Resets the traffic signal controller, then regenerates the cached {@link TrafficSignalPhases} (if desired), and
     * forces a tick operation (if desired). This is useful to force an update when a device is linked, unlinked, or the
     * traffic signal controller mode was changed.
     *
     * @param regeneratePhaseCache true to force a regeneration of the cached {@link TrafficSignalPhases}, false
     *                             otherwise.
     * @param forceTick            true to force a tick the traffic signal controller after resetting the traffic signal
     *                             controller, false otherwise.
     *
     * @since 2.0
     */
    private void resetController( boolean regeneratePhaseCache, boolean forceTick ) {
        lastPhaseChangeTime = -1;
        currentPhase = null;
        if ( regeneratePhaseCache ) {
            cachedPhases = new TrafficSignalPhases( getWorld(), circuits );
        }
        if ( forceTick ) {
            onTick();
        }
        markDirtySync( getWorld(), getPos() );
    }

    /**
     * Forcibly power off all signals connected to the traffic signal controller. This is useful when removing a traffic
     * signal controller from the world.
     *
     * @since 2.0
     */
    public void forciblyPowerOff() {
        // Power off all signals
        if ( circuits != null ) {
            for ( TrafficSignalControllerCircuit circuit : circuits.getCircuits() ) {
                circuit.powerOffAllSignals( getWorld() );
            }
        }

        // Set the mode to off
        mode = TrafficSignalControllerMode.MANUAL_OFF;
        operatingMode = TrafficSignalControllerMode.MANUAL_OFF;
    }

    /**
     * Switches the traffic signal controller to the next mode and returns the name of the new mode.
     *
     * @return The name of the new mode.
     *
     * @since 2.0
     */
    public String switchMode() {
        // Switch to next mode if not in fault state
        if ( !isInFaultState() ) {
            mode = mode.getNextMode();
            operatingMode = mode;
            resetController( false, true );
        }
        return mode.getName();
    }

    /**
     * Returns the current count of signal circuits for the traffic signal controller.
     *
     * @return the current count of signal circuits for the traffic signal controller
     *
     * @since 2.0
     */
    public int getSignalCircuitCount() {
        return circuits.getCircuitCount();
    }

    /**
     * Links the device at the specified {@link BlockPos} to the circuit with the specified circuit number. The device
     * will be linked as the specified {@link SIGNAL_SIDE}.
     *
     * @param pos           the {@link BlockPos} of the device to link
     * @param signalSide    the {@link SIGNAL_SIDE} of the device to link
     * @param circuitNumber the circuit number to link the device to
     *
     * @return true if the device was successfully linked, false otherwise
     *
     * @since 2.0
     */
    public boolean linkDevice( BlockPos pos, SIGNAL_SIDE signalSide, int circuitNumber )
    {
        // Return false if device is already linked
        boolean linked = !circuits.isDeviceLinked( pos ) &&
                circuits.getCircuit( circuitNumber - 1 ).linkDevice( pos, signalSide );

        if ( linked ) {
            resetController( true, true );
        }
        return linked;
    }

    /**
     * Creates an overlap from the specified source {@link BlockPos} to the specified target {@link BlockPos}.
     *
     * @param overlapSource the {@link BlockPos} of the source device
     * @param overlapTarget the {@link BlockPos} of the target device
     *
     * @return true if the overlap was successfully created, false otherwise
     *
     * @since 2.0
     */
    public boolean createOverlap( BlockPos overlapSource, BlockPos overlapTarget )
    {
        // Create overlap
        boolean created = overlaps.addOverlap( overlapSource, overlapTarget );

        if ( created ) {
            resetController( true, true );
        }
        return created;
    }

    /**
     * Unlinks the device at the specified {@link BlockPos}.
     *
     * @param pos the {@link BlockPos} of the device to unlink
     *
     * @return true if the device was successfully unlinked, false otherwise
     *
     * @since 2.0
     */
    public boolean unlinkDevice( BlockPos pos ) {
        // Return true if device it was unlinked
        boolean unlinked = circuits.unlinkDevice( pos );

        // Remove overlaps if they exist
        if ( unlinked ) {
            overlaps.removeOverlaps( pos );
        }

        if ( unlinked ) {
            resetController( true, true );
        }
        return unlinked;
    }

    //endregion
}
