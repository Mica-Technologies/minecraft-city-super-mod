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

    //region: Static/Constant Fields

    /**
     * The key for storing and retrieving the controller's mode from NBT data.
     *
     * @since 2.0
     */
    private static final String NBT_KEY_MODE = "tcMode";

    /**
     * The key for storing and retrieving the controller's operating mode from NBT data.
     *
     * @since 2.0
     */
    private static final String NBT_KEY_OPERATING_MODE = "tcOperatingMode";

    /**
     * The key for storing and retrieving the traffic signal controller's paused state from NBT data.
     *
     * @since 2.0
     */
    private static final String NBT_KEY_PAUSED = "tcPaused";

    /**
     * The key for storing and retrieving the traffic signal controller's circuits from NBT data.
     *
     * @since 2.0
     */
    private static final String NBT_KEY_CIRCUITS = "tcCircuits";

    /**
     * The key for storing and retrieving the traffic signal controller's cached phases from NBT data.
     *
     * @since 2.0
     */
    private static final String NBT_KEY_CACHED_PHASES = "tcCachedPhases";

    /**
     * The key for storing and retrieving the traffic signal controller's last phase change time from NBT data.
     *
     * @since 2.0
     */
    private static final String NBT_KEY_LAST_PHASE_CHANGE_TIME = "tcLastPhaseChangeTime";

    /**
     * The key for storing and retrieving the traffic signal controller's last phase applicability change time from NBT
     * data.
     *
     * @since 2.0
     */
    private static final String NBT_KEY_LAST_PHASE_APPLICABILITY_CHANGE_TIME = "tcLastPhaseApplicabilityChangeTime";

    /**
     * The key for storing and retrieving the traffic signal controller's current phase from NBT data.
     *
     * @since 2.0
     */
    private static final String NBT_KEY_CURRENT_PHASE = "tcCurrentPhase";

    /**
     * The key for storing and retrieving the traffic signal controller's current fault message from NBT data.
     *
     * @since 2.0
     */
    private static final String NBT_KEY_CURRENT_FAULT_MESSAGE = "tcCurrentFaultMessage";

    /**
     * The key for storing and retrieving the traffic signal controller's nightly fallback to flash mode setting from
     * NBT data.
     *
     * @since 2.0
     */
    private static final String NBT_KEY_NIGHTLY_FALLBACK_FLASH_MODE = "tcNightlyFallbackToFlashMode";

    /**
     * The key for storing and retrieving the traffic signal controller's power loss fallback to flash mode setting from
     * NBT data.
     *
     * @since 2.0
     */
    private static final String NBT_KEY_POWER_LOSS_FALLBACK_FLASH_MODE = "tcPowerLossFallbackToFlashMode";

    /**
     * The key for storing and retrieving the traffic signal controller's yellow time (milliseconds) setting from NBT
     * data.
     *
     * @since 2.0
     */
    private static final String NBT_KEY_YELLOW_TIME_MS = "tcYellowTimeMs";

    /**
     * The key for storing and retrieving the traffic signal controller's flashing don't walk time (milliseconds)
     * setting from NBT data.
     *
     * @since 2.0
     */
    private static final String NBT_KEY_FLASH_DONT_WALK_TIME_MS = "tcFlashDontWalkTimeMs";

    /**
     * The key for storing and retrieving the traffic signal controller's all red time (milliseconds) setting from NBT
     * data.
     *
     * @since 2.0
     */
    private static final String NBT_KEY_ALL_RED_TIME_MS = "tcAllRedTimeMs";

    /**
     * The key for storing and retrieving the traffic signal controller's minimum requestable service time
     * (milliseconds) setting from NBT data.
     *
     * @since 2.0
     */
    private static final String NBT_KEY_MIN_REQUESTABLE_SERVICE_TIME_MS = "tcMinRequestableServiceTimeMs";

    /**
     * The key for storing and retrieving the traffic signal controller's maximum requestable service time
     * (milliseconds) setting from NBT data.
     *
     * @since 2.0
     */
    private static final String NBT_KEY_MAX_REQUESTABLE_SERVICE_TIME_MS = "tcMaxRequestableServiceTimeMs";

    //endregion

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
     * The yellow time (milliseconds) for the traffic signal controller.
     *
     * @since 2.0
     */
    private long yellowTimeMs = 4000L;

    /**
     * The flashing don't walk time (milliseconds) for the traffic signal controller.
     *
     * @since 2.0
     */
    private long flashDontWalkTimeMs = 15000L;

    /**
     * The flashing all red time (milliseconds) for the traffic signal controller.
     *
     * @since 2.0
     */
    private long allRedTimeMs = 3000L;

    /**
     * The minimum service time (milliseconds) when servicing requests to the traffic signal controller in
     * {@link TrafficSignalControllerMode#REQUESTABLE} mode.
     *
     * @since 2.0
     */
    private long minRequestableServiceTimeMs = 25000L;

    /**
     * The maximum service time (milliseconds) when servicing requests to the traffic signal controller in
     * {@link TrafficSignalControllerMode#REQUESTABLE} mode.
     *
     * @since 2.0
     */
    private long maxRequestableServiceTimeMs = 120000L;

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
            long timeSinceLastPhaseChange = System.currentTimeMillis() - lastPhaseChangeTime;
            long timeSinceLastPhaseApplicabilityChange = System.currentTimeMillis() - lastPhaseApplicabilityChangeTime;
            TrafficSignalPhase newPhase = TrafficSignalControllerTicker.tick( getWorld(), mode, operatingMode, circuits,
                                                                              cachedPhases, currentPhase,
                                                                              timeSinceLastPhaseApplicabilityChange,
                                                                              timeSinceLastPhaseChange,
                                                                              alternatingFlash, yellowTimeMs,
                                                                              flashDontWalkTimeMs, allRedTimeMs,
                                                                              minRequestableServiceTimeMs,
                                                                              maxRequestableServiceTimeMs );

            // If the phase index has changed, update the phase
            if ( newPhase != null ) {
                // Store previous phase temporarily
                TrafficSignalPhase previousPhase = currentPhase;

                // Update current phase and last phase change time
                currentPhase = newPhase;
                lastPhaseChangeTime = System.currentTimeMillis();

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
        compound.setInteger( NBT_KEY_MODE, mode.toNBT() );

        // Write the operating mode to NBT
        compound.setInteger( NBT_KEY_OPERATING_MODE, operatingMode.toNBT() );

        // Write the circuits to NBT
        compound.setTag( NBT_KEY_CIRCUITS, circuits.toNBT() );

        // Write the paused state to NBT
        compound.setBoolean( NBT_KEY_PAUSED, paused );

        // Write the cached phases to NBT
        compound.setTag( NBT_KEY_CACHED_PHASES, cachedPhases.toNBT() );

        // Write the last phase change time to NBT
        compound.setLong( NBT_KEY_LAST_PHASE_CHANGE_TIME, lastPhaseChangeTime );

        // Write the last phase applicability change time to NBT
        compound.setLong( NBT_KEY_LAST_PHASE_APPLICABILITY_CHANGE_TIME, lastPhaseApplicabilityChangeTime );

        // Write the current phase to NBT if non-null
        if ( currentPhase != null ) {
            compound.setTag( NBT_KEY_CURRENT_PHASE, currentPhase.toNBT() );
        }
        else {
            compound.removeTag( NBT_KEY_CURRENT_PHASE );
        }

        // Write the current fault message to NBT
        compound.setString( NBT_KEY_CURRENT_FAULT_MESSAGE, currentFaultMessage );

        // Write the nightly fallback to flash mode setting to NBT
        compound.setBoolean( NBT_KEY_NIGHTLY_FALLBACK_FLASH_MODE, nightlyFallbackToFlashMode );

        // Write the power loss fallback to flash mode setting to NBT
        compound.setBoolean( NBT_KEY_POWER_LOSS_FALLBACK_FLASH_MODE, powerLossFallbackToFlashMode );

        // Write the yellow time (ms) to NBT
        compound.setLong( NBT_KEY_YELLOW_TIME_MS, yellowTimeMs );

        // Write the flashing don't walk time (ms) to NBT
        compound.setLong( NBT_KEY_FLASH_DONT_WALK_TIME_MS, flashDontWalkTimeMs );

        // Write the all red time (ms) to NBT
        compound.setLong( NBT_KEY_ALL_RED_TIME_MS, allRedTimeMs );

        // Write the minimum requestable service time (ms) to NBT
        compound.setLong( NBT_KEY_MIN_REQUESTABLE_SERVICE_TIME_MS, minRequestableServiceTimeMs );

        // Write the maximum requestable service time (ms) to NBT
        compound.setLong( NBT_KEY_MAX_REQUESTABLE_SERVICE_TIME_MS, maxRequestableServiceTimeMs );

        // Return the NBT tag compound
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
        if ( compound.hasKey( NBT_KEY_MODE ) ) {
            mode = TrafficSignalControllerMode.fromNBT( compound.getInteger( NBT_KEY_MODE ) );
        }

        // Load the traffic signal controller operating mode
        if ( compound.hasKey( NBT_KEY_OPERATING_MODE ) ) {
            operatingMode = TrafficSignalControllerMode.fromNBT( compound.getInteger( NBT_KEY_OPERATING_MODE ) );
        }
        else {
            operatingMode = mode;
        }

        // Load the traffic signal controller circuits
        if ( compound.hasKey( NBT_KEY_CIRCUITS ) ) {
            circuits = TrafficSignalControllerCircuits.fromNBT( compound.getCompoundTag( NBT_KEY_CIRCUITS ) );
        }

        // Load the traffic signal controller paused state
        if ( compound.hasKey( NBT_KEY_PAUSED ) ) {
            paused = compound.getBoolean( NBT_KEY_PAUSED );
        }

        // Load the traffic signal controller cached phases
        if ( compound.hasKey( NBT_KEY_CACHED_PHASES ) ) {
            cachedPhases = TrafficSignalPhases.fromNBT( compound.getCompoundTag( NBT_KEY_CACHED_PHASES ) );
        }

        // Load the traffic signal controller last phase change time
        if ( compound.hasKey( NBT_KEY_LAST_PHASE_CHANGE_TIME ) ) {
            lastPhaseChangeTime = compound.getLong( NBT_KEY_LAST_PHASE_CHANGE_TIME );
        }

        // Load the traffic signal controller last phase applicability change time
        if ( compound.hasKey( NBT_KEY_LAST_PHASE_APPLICABILITY_CHANGE_TIME ) ) {
            lastPhaseApplicabilityChangeTime = compound.getLong( NBT_KEY_LAST_PHASE_APPLICABILITY_CHANGE_TIME );
        }

        // Load the traffic signal controller current phase
        if ( compound.hasKey( NBT_KEY_CURRENT_PHASE ) ) {
            currentPhase = TrafficSignalPhase.fromNBT( compound.getCompoundTag( NBT_KEY_CURRENT_PHASE ) );
        }

        // Load the traffic signal controller current fault message
        if ( compound.hasKey( NBT_KEY_CURRENT_FAULT_MESSAGE ) ) {
            currentFaultMessage = compound.getString( NBT_KEY_CURRENT_FAULT_MESSAGE );
        }

        // Load the traffic signal controller nightly fallback to flash mode setting
        if ( compound.hasKey( NBT_KEY_NIGHTLY_FALLBACK_FLASH_MODE ) ) {
            nightlyFallbackToFlashMode = compound.getBoolean( NBT_KEY_NIGHTLY_FALLBACK_FLASH_MODE );
        }

        // Load the traffic signal controller power loss fallback to flash mode setting
        if ( compound.hasKey( NBT_KEY_POWER_LOSS_FALLBACK_FLASH_MODE ) ) {
            powerLossFallbackToFlashMode = compound.getBoolean( NBT_KEY_POWER_LOSS_FALLBACK_FLASH_MODE );
        }

        // Load the traffic signal controller yellow time (ms)
        if ( compound.hasKey( NBT_KEY_YELLOW_TIME_MS ) ) {
            yellowTimeMs = compound.getLong( NBT_KEY_YELLOW_TIME_MS );
        }

        // Load the traffic signal controller flashing don't walk time (ms)
        if ( compound.hasKey( NBT_KEY_FLASH_DONT_WALK_TIME_MS ) ) {
            flashDontWalkTimeMs = compound.getLong( NBT_KEY_FLASH_DONT_WALK_TIME_MS );
        }

        // Load the traffic signal controller all red time (ms)
        if ( compound.hasKey( NBT_KEY_ALL_RED_TIME_MS ) ) {
            allRedTimeMs = compound.getLong( NBT_KEY_ALL_RED_TIME_MS );
        }

        // Load the traffic signal controller minimum requestable service time (ms)
        if ( compound.hasKey( NBT_KEY_MIN_REQUESTABLE_SERVICE_TIME_MS ) ) {
            minRequestableServiceTimeMs = compound.getLong( NBT_KEY_MIN_REQUESTABLE_SERVICE_TIME_MS );
        }

        // Load the traffic signal controller maximum requestable service time (ms)
        if ( compound.hasKey( NBT_KEY_MAX_REQUESTABLE_SERVICE_TIME_MS ) ) {
            maxRequestableServiceTimeMs = compound.getLong( NBT_KEY_MAX_REQUESTABLE_SERVICE_TIME_MS );
        }
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
