package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal.SIGNAL_SIDE;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

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
     * The list of cached phases for the traffic signal controller.
     *
     * @since 2.0
     */
    private TrafficSignalPhases cachedPhases = new TrafficSignalPhases( circuits );

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
    private boolean overlapPedestrianSignals = true;

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

            // Pass tick event to traffic signal controller ticker
            long tickTime = getWorld().getTotalWorldTime();
            long timeSinceLastPhaseChange = tickTime - lastPhaseChangeTime;
            long timeSinceLastPhaseApplicabilityChange = tickTime - lastPhaseApplicabilityChangeTime;
            TrafficSignalPhase newPhase = TrafficSignalControllerTicker.tick( getWorld(), mode, operatingMode, circuits,
                                                                              cachedPhases, currentPhase,
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
        boolean shouldPause = !getWorld().isBlockPowered( getPos() );

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

        // Return the NBT tag compound with previous NBT data format removed
        return removePreviousNBTDataFormat( compound );
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
        // Check for previous NBT data format
        NBTTagCompound readCompound = compound;
        if ( hasPreviousNBTDataFormat( compound ) ) {
            // Load the previous NBT data with the new format
            readCompound = loadPreviousNBTDataFormat( compound );
        }

        // Load the traffic signal controller mode
        if ( readCompound.hasKey( TrafficSignalControllerNBTKeys.MODE ) ) {
            mode = TrafficSignalControllerMode.fromNBT(
                    readCompound.getInteger( TrafficSignalControllerNBTKeys.MODE ) );
        }

        // Load the traffic signal controller operating mode
        if ( readCompound.hasKey( TrafficSignalControllerNBTKeys.OPERATING_MODE ) ) {
            operatingMode = TrafficSignalControllerMode.fromNBT(
                    readCompound.getInteger( TrafficSignalControllerNBTKeys.OPERATING_MODE ) );
        }
        else {
            operatingMode = mode;
        }

        // Load the traffic signal controller circuits
        if ( readCompound.hasKey( TrafficSignalControllerNBTKeys.CIRCUITS ) ) {
            circuits = TrafficSignalControllerCircuits.fromNBT(
                    readCompound.getCompoundTag( TrafficSignalControllerNBTKeys.CIRCUITS ) );
        }

        // Load the traffic signal controller paused state
        if ( readCompound.hasKey( TrafficSignalControllerNBTKeys.PAUSED ) ) {
            paused = readCompound.getBoolean( TrafficSignalControllerNBTKeys.PAUSED );
        }

        // Load the traffic signal controller cached phases
        if ( readCompound.hasKey( TrafficSignalControllerNBTKeys.CACHED_PHASES ) ) {
            cachedPhases = TrafficSignalPhases.fromNBT(
                    readCompound.getCompoundTag( TrafficSignalControllerNBTKeys.CACHED_PHASES ) );
        }

        // Load the traffic signal controller last phase change time
        if ( readCompound.hasKey( TrafficSignalControllerNBTKeys.LAST_PHASE_CHANGE_TIME ) ) {
            lastPhaseChangeTime = readCompound.getLong( TrafficSignalControllerNBTKeys.LAST_PHASE_CHANGE_TIME );
        }

        // Load the traffic signal controller last phase applicability change time
        if ( readCompound.hasKey( TrafficSignalControllerNBTKeys.LAST_PHASE_APPLICABILITY_CHANGE_TIME ) ) {
            lastPhaseApplicabilityChangeTime = readCompound.getLong(
                    TrafficSignalControllerNBTKeys.LAST_PHASE_APPLICABILITY_CHANGE_TIME );
        }

        // Load the traffic signal controller last pedestrian phase time
        if ( readCompound.hasKey( TrafficSignalControllerNBTKeys.LAST_PEDESTRIAN_PHASE_TIME ) ) {
            lastPedPhaseTime = readCompound.getLong( TrafficSignalControllerNBTKeys.LAST_PEDESTRIAN_PHASE_TIME );
        }

        // Load the traffic signal controller current phase
        if ( readCompound.hasKey( TrafficSignalControllerNBTKeys.CURRENT_PHASE ) ) {
            currentPhase = TrafficSignalPhase.fromNBT(
                    readCompound.getCompoundTag( TrafficSignalControllerNBTKeys.CURRENT_PHASE ) );
        }

        // Load the traffic signal controller current fault message
        if ( readCompound.hasKey( TrafficSignalControllerNBTKeys.CURRENT_FAULT_MESSAGE ) ) {
            currentFaultMessage = readCompound.getString( TrafficSignalControllerNBTKeys.CURRENT_FAULT_MESSAGE );
        }

        // Load the traffic signal controller nightly fallback to flash mode setting
        if ( readCompound.hasKey( TrafficSignalControllerNBTKeys.NIGHTLY_FALLBACK_FLASH_MODE ) ) {
            nightlyFallbackToFlashMode = readCompound.getBoolean(
                    TrafficSignalControllerNBTKeys.NIGHTLY_FALLBACK_FLASH_MODE );
        }

        // Load the traffic signal controller power loss fallback to flash mode setting
        if ( readCompound.hasKey( TrafficSignalControllerNBTKeys.POWER_LOSS_FALLBACK_FLASH_MODE ) ) {
            powerLossFallbackToFlashMode = readCompound.getBoolean(
                    TrafficSignalControllerNBTKeys.POWER_LOSS_FALLBACK_FLASH_MODE );
        }

        // Load the traffic signal controller overlap pedestrian signals setting
        if ( readCompound.hasKey( TrafficSignalControllerNBTKeys.OVERLAP_PEDESTRIAN_SIGNALS ) ) {
            overlapPedestrianSignals = readCompound.getBoolean(
                    TrafficSignalControllerNBTKeys.OVERLAP_PEDESTRIAN_SIGNALS );
        }

        // Load the traffic signal controller yellow time
        if ( readCompound.hasKey( TrafficSignalControllerNBTKeys.YELLOW_TIME ) ) {
            yellowTime = readCompound.getLong( TrafficSignalControllerNBTKeys.YELLOW_TIME );
        }

        // Load the traffic signal controller flashing don't walk time
        if ( readCompound.hasKey( TrafficSignalControllerNBTKeys.FLASH_DONT_WALK_TIME ) ) {
            flashDontWalkTime = readCompound.getLong( TrafficSignalControllerNBTKeys.FLASH_DONT_WALK_TIME );
        }

        // Load the traffic signal controller all red time
        if ( readCompound.hasKey( TrafficSignalControllerNBTKeys.ALL_RED_TIME ) ) {
            allRedTime = readCompound.getLong( TrafficSignalControllerNBTKeys.ALL_RED_TIME );
        }

        // Load the traffic signal controller minimum requestable service time
        if ( readCompound.hasKey( TrafficSignalControllerNBTKeys.MIN_REQUESTABLE_SERVICE_TIME ) ) {
            minRequestableServiceTime = readCompound.getLong(
                    TrafficSignalControllerNBTKeys.MIN_REQUESTABLE_SERVICE_TIME );
        }

        // Load the traffic signal controller maximum requestable service time
        if ( readCompound.hasKey( TrafficSignalControllerNBTKeys.MAX_REQUESTABLE_SERVICE_TIME ) ) {
            maxRequestableServiceTime = readCompound.getLong(
                    TrafficSignalControllerNBTKeys.MAX_REQUESTABLE_SERVICE_TIME );
        }

        // Load the traffic signal controller minimum green time
        if ( readCompound.hasKey( TrafficSignalControllerNBTKeys.MIN_GREEN_TIME ) ) {
            minGreenTime = readCompound.getLong( TrafficSignalControllerNBTKeys.MIN_GREEN_TIME );
        }

        // Load the traffic signal controller maximum green time
        if ( readCompound.hasKey( TrafficSignalControllerNBTKeys.MAX_GREEN_TIME ) ) {
            maxGreenTime = readCompound.getLong( TrafficSignalControllerNBTKeys.MAX_GREEN_TIME );
        }

        // Load the traffic signal controller secondary minimum green time
        if ( readCompound.hasKey( TrafficSignalControllerNBTKeys.MIN_GREEN_TIME_SECONDARY ) ) {
            minGreenTimeSecondary = readCompound.getLong( TrafficSignalControllerNBTKeys.MIN_GREEN_TIME_SECONDARY );
        }

        // Load the traffic signal controller secondary maximum green time
        if ( readCompound.hasKey( TrafficSignalControllerNBTKeys.MAX_GREEN_TIME_SECONDARY ) ) {
            maxGreenTimeSecondary = readCompound.getLong( TrafficSignalControllerNBTKeys.MAX_GREEN_TIME_SECONDARY );
        }

        // Load the traffic signal controller dedicated pedestrian signal time
        if ( readCompound.hasKey( TrafficSignalControllerNBTKeys.DEDICATED_PED_SIGNAL_TIME ) ) {
            dedicatedPedSignalTime = readCompound.getLong( TrafficSignalControllerNBTKeys.DEDICATED_PED_SIGNAL_TIME );
        }
    }

    /**
     * Utility method which returns a boolean indicating whether the provided NBTTagCompound has the previous NBT data
     * format.
     *
     * @param compound The NBTTagCompound to check for the previous NBT data format.
     *
     * @return True if the provided NBTTagCompound has the previous NBT data format, false otherwise.
     *
     * @since 1.0
     */
    @Deprecated
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
     * Utility method which removes the previous NBT data format from the provided NBTTagCompound.
     *
     * @param compound The NBTTagCompound to remove the previous NBT data format from.
     *
     * @return The provided NBTTagCompound with the previous NBT data format removed.
     *
     * @since 1.0
     */
    @Deprecated
    public static NBTTagCompound removePreviousNBTDataFormat( NBTTagCompound compound ) {

        // Remove any previous NBT data format keys
        for ( String key : TrafficSignalControllerNBTKeys.V1_KEY_LIST ) {
            if ( compound.hasKey( key ) ) {
                compound.removeTag( key );
            }
        }

        return compound;
    }

    /**
     * Utility method which loads the previous NBT data format as the current NBT data format.
     *
     * @param compound The NBTTagCompound to load the previous NBT data format from.
     *
     * @return The provided NBTTagCompound with the previous NBT data loaded to the current NBT data format.
     *
     * @since 1.0
     */
    @Deprecated
    public static NBTTagCompound loadPreviousNBTDataFormat( NBTTagCompound compound ) {
        return removePreviousNBTDataFormat( compound );
    }

    //endregion

    //region: Getters and Setters

    /**
     * Returns the traffic signal controller's fallback to flash mode at night setting.
     *
     * @return true if the traffic signal controller's fallback to flash mode at night is enabled, false otherwise
     */
    public boolean isNightlyFallbackToFlashMode() {
        return nightlyFallbackToFlashMode;
    }

    /**
     * Sets the traffic signal controller's fallback to flash mode at night setting.
     *
     * @param nightlyFallbackToFlashMode true if the traffic signal controller's fallback to flash mode at night should
     *                                   be enabled, false otherwise
     */
    public void setNightlyFallbackToFlashMode( boolean nightlyFallbackToFlashMode ) {
        if ( this.nightlyFallbackToFlashMode != nightlyFallbackToFlashMode ) {
            this.nightlyFallbackToFlashMode = nightlyFallbackToFlashMode;
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
            cachedPhases = new TrafficSignalPhases( circuits );
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

        if ( unlinked ) {
            resetController( true, true );
        }
        return unlinked;
    }

    //endregion
}
