package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;

/**
 * A class implementation of an easily indexed and accessed array of {@link TrafficSignalPhase}s which can be easily
 * serialized and deserialized as Minecraft NBT data.
 *
 * @author Mica Technologies
 * @version 1.0
 * @see TrafficSignalPhase
 * @see #fromNBT(NBTTagCompound)
 * @see #toNBT()
 * @since 2023.2.0
 */
public class TrafficSignalPhases
{

    /**
     * The phase index value for the off phase.
     *
     * @since 1.0
     */
    public static final int PHASE_INDEX_OFF = 0;

    /**
     * The phase index value for the first flash phase.
     *
     * @since 1.0
     */
    public static final int PHASE_INDEX_FLASH_1 = 1;

    /**
     * The phase index value for the second flash phase.
     *
     * @since 1.0
     */
    public static final int PHASE_INDEX_FLASH_2 = 2;

    /**
     * The phase index value for the first fault phase.
     *
     * @since 1.0
     */
    public static final int PHASE_INDEX_FAULT_1 = 3;

    /**
     * The phase index value for the second fault phase.
     *
     * @since 1.0
     */
    public static final int PHASE_INDEX_FAULT_2 = 4;

    /**
     * The phase index value for the all red phase.
     *
     * @since 1.0
     */
    public static final int PHASE_INDEX_ALL_RED = 5;

    /**
     * The phase index value for the ramp meter disabled phase.
     *
     * @since 1.0
     */
    public static final int PHASE_INDEX_RAMP_METER_DISABLED = 6;

    /**
     * The phase index value for the ramp meter starting phase.
     *
     * @since 1.0
     */
    public static final int PHASE_INDEX_RAMP_METER_STARTING = 7;

    /**
     * The phase index value for the ramp meter first flash phase.
     *
     * @since 1.0
     */
    public static final int PHASE_INDEX_RAMP_METER_FLASH_1 = 8;

    /**
     * The phase index value for the ramp meter second flash phase.
     *
     * @since 1.0
     */
    public static final int PHASE_INDEX_RAMP_METER_FLASH_2 = 9;

    /**
     * The phase index value for the requestable default green phase.
     *
     * @since 1.0
     */
    public static final int PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN = 10;

    /**
     * The phase index value for the requestable default green + flashing don't walk phase.
     *
     * @since 1.0
     */
    public static final int PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW = 11;

    /**
     * The phase index value for the first requestable default green + flashing don't walk + flashing yellow HAWK
     * (High-Intensity Activated crossWalK beacon) phase.
     *
     * @since 1.0
     */
    public static final int PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK_1 = 12;

    /**
     * The phase index value for the second requestable default green + flashing don't walk + flashing yellow HAWK
     * (High-Intensity Activated crossWalK beacon) phase.
     *
     * @since 1.0
     */
    public static final int PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK_2 = 13;

    /**
     * The phase index value for the requestable default yellow phase.
     *
     * @since 1.0
     */
    public static final int PHASE_INDEX_REQUESTABLE_DEFAULT_YELLOW = 14;

    /**
     * The phase index value for the requestable default red phase.
     *
     * @since 1.0
     */
    public static final int PHASE_INDEX_REQUESTABLE_DEFAULT_RED = 15;

    /**
     * The phase index value for the requestable service green phase.
     *
     * @since 1.0
     */
    public static final int PHASE_INDEX_REQUESTABLE_SERVICE_GREEN = 16;

    /**
     * The phase index value for the requestable service green + flashing don't walk phase.
     *
     * @since 1.0
     */
    public static final int PHASE_INDEX_REQUESTABLE_SERVICE_GREEN_FLASH_DW = 17;

    /**
     * The phase index value for the first requestable service green + flashing don't walk + flashing yellow HAWK
     * (High-Intensity Activated crossWalK beacon) phase.
     *
     * @since 1.0
     */
    public static final int PHASE_INDEX_REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK_1 = 18;

    /**
     * The phase index value for the second requestable service green + flashing don't walk + flashing yellow HAWK
     * (High-Intensity Activated crossWalK beacon) phase.
     *
     * @since 1.0
     */
    public static final int PHASE_INDEX_REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK_2 = 19;

    /**
     * The phase index value for the requestable service yellow phase.
     *
     * @since 1.0
     */
    public static final int PHASE_INDEX_REQUESTABLE_SERVICE_YELLOW = 20;

    /**
     * The phase index value for the requestable service red phase.
     *
     * @since 1.0
     */
    public static final int PHASE_INDEX_REQUESTABLE_SERVICE_RED = 21;

    /**
     * The number of {@link TrafficSignalPhase}s in the array.
     *
     * @since 1.0
     */
    private static final int PHASE_INDEX_COUNT = 22;

    /**
     * The array of {@link TrafficSignalPhase}s.
     *
     * @since 1.0
     */
    private final TrafficSignalPhase[] phases;

    /**
     * Creates a new {@link TrafficSignalPhases} object with the applicable {@link TrafficSignalPhase}s from the given
     * {@link TrafficSignalControllerMode} and {@link TrafficSignalControllerCircuits}.
     *
     * @param world                           The {@link World} that the traffic signal controller is in.
     * @param trafficSignalControllerCircuits The {@link TrafficSignalControllerCircuits} of the traffic signal
     *                                        controller.
     *
     * @since 1.0
     */
    public TrafficSignalPhases( World world, TrafficSignalControllerCircuits trafficSignalControllerCircuits )
    {
        // Create a new TrafficSignalPhase array
        phases = new TrafficSignalPhase[ PHASE_INDEX_COUNT ];

        // Create a new TrafficSignalPhase object for off phase
        TrafficSignalPhase offPhase = new TrafficSignalPhase( TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
                                                              TrafficSignalPhaseApplicability.NO_POWER );
        for ( TrafficSignalControllerCircuit circuit : trafficSignalControllerCircuits.getCircuits() ) {
            offPhase.addOffSignals( circuit.getFlashingLeftSignals() );
            offPhase.addOffSignals( circuit.getFlashingRightSignals() );
            offPhase.addOffSignals( circuit.getLeftSignals() );
            offPhase.addOffSignals( circuit.getRightSignals() );
            offPhase.addOffSignals( circuit.getThroughSignals() );
            offPhase.addOffSignals( circuit.getProtectedSignals() );
            offPhase.addOffSignals( circuit.getPedestrianSignals() );
            offPhase.addOffSignals( circuit.getPedestrianBeaconSignals() );
            offPhase.addOffSignals( circuit.getPedestrianAccessorySignals() );
        }
        phases[ PHASE_INDEX_OFF ] = offPhase;

        // Create a new TrafficSignalPhase object for first flash phase
        TrafficSignalPhase flashPhase1 = new TrafficSignalPhase( TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
                                                                 TrafficSignalPhaseApplicability.NONE );
        int circuitIndex = 1;
        for ( TrafficSignalControllerCircuit circuit : trafficSignalControllerCircuits.getCircuits() ) {
            // First circuit should flash yellow, even circuits should be off, odd circuits should flash red
            if ( circuitIndex == 1 ) {
                // Check if circuit has protected signals
                boolean hasProtectedSignals = circuit.getProtectedSignals().size() > 0;

                // Get should flash filtered signal lists
                Tuple< List< BlockPos >, List< BlockPos > > flashingLeftSignals
                        = TrafficSignalControllerTickerUtilities.filterSignalsByShouldFlash( world,
                                                                                             circuit.getFlashingLeftSignals() );
                Tuple< List< BlockPos >, List< BlockPos > > flashingRightSignals
                        = TrafficSignalControllerTickerUtilities.filterSignalsByShouldFlash( world,
                                                                                             circuit.getFlashingRightSignals() );
                Tuple< List< BlockPos >, List< BlockPos > > leftSignals
                        = TrafficSignalControllerTickerUtilities.filterSignalsByShouldFlash( world,
                                                                                             circuit.getLeftSignals() );
                Tuple< List< BlockPos >, List< BlockPos > > rightSignals
                        = TrafficSignalControllerTickerUtilities.filterSignalsByShouldFlash( world,
                                                                                             circuit.getRightSignals() );
                Tuple< List< BlockPos >, List< BlockPos > > throughSignals
                        = TrafficSignalControllerTickerUtilities.filterSignalsByShouldFlash( world,
                                                                                             circuit.getThroughSignals() );
                Tuple< List< BlockPos >, List< BlockPos > > protectedSignals
                        = TrafficSignalControllerTickerUtilities.filterSignalsByShouldFlash( world,
                                                                                             circuit.getProtectedSignals() );
                Tuple< List< BlockPos >, List< BlockPos > > pedestrianBeaconSignals
                        = TrafficSignalControllerTickerUtilities.filterSignalsByShouldFlash( world,
                                                                                             circuit.getPedestrianBeaconSignals() );
                flashPhase1.addRedSignals( flashingLeftSignals.getFirst() );
                flashPhase1.addOffSignals( flashingLeftSignals.getSecond() );
                flashPhase1.addRedSignals( leftSignals.getFirst() );
                flashPhase1.addOffSignals( leftSignals.getSecond() );
                flashPhase1.addYellowSignals( throughSignals.getFirst() );
                flashPhase1.addOffSignals( throughSignals.getSecond() );
                flashPhase1.addYellowSignals( pedestrianBeaconSignals.getFirst() );
                flashPhase1.addOffSignals( pedestrianBeaconSignals.getSecond() );
                flashPhase1.addOffSignals( circuit.getPedestrianSignals() );
                flashPhase1.addOffSignals( circuit.getPedestrianAccessorySignals() );
                if ( hasProtectedSignals ) {
                    flashPhase1.addRedSignals( flashingRightSignals.getFirst() );
                    flashPhase1.addOffSignals( flashingRightSignals.getSecond() );
                    flashPhase1.addRedSignals( rightSignals.getFirst() );
                    flashPhase1.addOffSignals( rightSignals.getSecond() );
                    flashPhase1.addYellowSignals( protectedSignals.getFirst() );
                    flashPhase1.addOffSignals( protectedSignals.getSecond() );
                }
                else {
                    flashPhase1.addYellowSignals( flashingRightSignals.getFirst() );
                    flashPhase1.addOffSignals( flashingRightSignals.getSecond() );
                    flashPhase1.addYellowSignals( rightSignals.getFirst() );
                    flashPhase1.addOffSignals( rightSignals.getSecond() );
                    flashPhase1.addRedSignals( protectedSignals.getFirst() );
                    flashPhase1.addOffSignals( protectedSignals.getSecond() );
                }
            }
            else if ( circuitIndex % 2 == 0 ) {
                flashPhase1.addOffSignals( circuit.getFlashingLeftSignals() );
                flashPhase1.addOffSignals( circuit.getFlashingRightSignals() );
                flashPhase1.addOffSignals( circuit.getLeftSignals() );
                flashPhase1.addOffSignals( circuit.getRightSignals() );
                flashPhase1.addOffSignals( circuit.getThroughSignals() );
                flashPhase1.addOffSignals( circuit.getProtectedSignals() );
                flashPhase1.addOffSignals( circuit.getPedestrianSignals() );
                flashPhase1.addOffSignals( circuit.getPedestrianBeaconSignals() );
                flashPhase1.addOffSignals( circuit.getPedestrianAccessorySignals() );
            }
            else {
                // Get should flash filtered signal lists
                Tuple< List< BlockPos >, List< BlockPos > > flashingLeftSignals
                        = TrafficSignalControllerTickerUtilities.filterSignalsByShouldFlash( world,
                                                                                             circuit.getFlashingLeftSignals() );
                Tuple< List< BlockPos >, List< BlockPos > > flashingRightSignals
                        = TrafficSignalControllerTickerUtilities.filterSignalsByShouldFlash( world,
                                                                                             circuit.getFlashingRightSignals() );
                Tuple< List< BlockPos >, List< BlockPos > > leftSignals
                        = TrafficSignalControllerTickerUtilities.filterSignalsByShouldFlash( world,
                                                                                             circuit.getLeftSignals() );
                Tuple< List< BlockPos >, List< BlockPos > > rightSignals
                        = TrafficSignalControllerTickerUtilities.filterSignalsByShouldFlash( world,
                                                                                             circuit.getRightSignals() );
                Tuple< List< BlockPos >, List< BlockPos > > throughSignals
                        = TrafficSignalControllerTickerUtilities.filterSignalsByShouldFlash( world,
                                                                                             circuit.getThroughSignals() );
                Tuple< List< BlockPos >, List< BlockPos > > protectedSignals
                        = TrafficSignalControllerTickerUtilities.filterSignalsByShouldFlash( world,
                                                                                             circuit.getProtectedSignals() );
                Tuple< List< BlockPos >, List< BlockPos > > pedestrianBeaconSignals
                        = TrafficSignalControllerTickerUtilities.filterSignalsByShouldFlash( world,
                                                                                             circuit.getPedestrianBeaconSignals() );
                flashPhase1.addRedSignals( flashingLeftSignals.getFirst() );
                flashPhase1.addOffSignals( flashingLeftSignals.getSecond() );
                flashPhase1.addRedSignals( flashingRightSignals.getFirst() );
                flashPhase1.addOffSignals( flashingRightSignals.getSecond() );
                flashPhase1.addRedSignals( leftSignals.getFirst() );
                flashPhase1.addOffSignals( leftSignals.getSecond() );
                flashPhase1.addRedSignals( rightSignals.getFirst() );
                flashPhase1.addOffSignals( rightSignals.getSecond() );
                flashPhase1.addRedSignals( throughSignals.getFirst() );
                flashPhase1.addOffSignals( throughSignals.getSecond() );
                flashPhase1.addRedSignals( protectedSignals.getFirst() );
                flashPhase1.addOffSignals( protectedSignals.getSecond() );
                flashPhase1.addRedSignals( pedestrianBeaconSignals.getFirst() );
                flashPhase1.addOffSignals( pedestrianBeaconSignals.getSecond() );
                flashPhase1.addOffSignals( circuit.getPedestrianSignals() );
                flashPhase1.addOffSignals( circuit.getPedestrianAccessorySignals() );
            }
            circuitIndex++;
        }
        phases[ PHASE_INDEX_FLASH_1 ] = flashPhase1;

        // Create a new TrafficSignalPhase object for second flash phase
        TrafficSignalPhase flashPhase2 = new TrafficSignalPhase( TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
                                                                 TrafficSignalPhaseApplicability.NONE );
        circuitIndex = 1;
        for ( TrafficSignalControllerCircuit circuit : trafficSignalControllerCircuits.getCircuits() ) {
            // Odd circuits should be off, even circuits should flash red
            if ( circuitIndex % 2 == 0 ) {
                // Get should flash filtered signal lists
                Tuple< List< BlockPos >, List< BlockPos > > flashingLeftSignals
                        = TrafficSignalControllerTickerUtilities.filterSignalsByShouldFlash( world,
                                                                                             circuit.getFlashingLeftSignals() );
                Tuple< List< BlockPos >, List< BlockPos > > flashingRightSignals
                        = TrafficSignalControllerTickerUtilities.filterSignalsByShouldFlash( world,
                                                                                             circuit.getFlashingRightSignals() );
                Tuple< List< BlockPos >, List< BlockPos > > leftSignals
                        = TrafficSignalControllerTickerUtilities.filterSignalsByShouldFlash( world,
                                                                                             circuit.getLeftSignals() );
                Tuple< List< BlockPos >, List< BlockPos > > rightSignals
                        = TrafficSignalControllerTickerUtilities.filterSignalsByShouldFlash( world,
                                                                                             circuit.getRightSignals() );
                Tuple< List< BlockPos >, List< BlockPos > > throughSignals
                        = TrafficSignalControllerTickerUtilities.filterSignalsByShouldFlash( world,
                                                                                             circuit.getThroughSignals() );
                Tuple< List< BlockPos >, List< BlockPos > > protectedSignals
                        = TrafficSignalControllerTickerUtilities.filterSignalsByShouldFlash( world,
                                                                                             circuit.getProtectedSignals() );
                Tuple< List< BlockPos >, List< BlockPos > > pedestrianBeaconSignals
                        = TrafficSignalControllerTickerUtilities.filterSignalsByShouldFlash( world,
                                                                                             circuit.getPedestrianBeaconSignals() );
                flashPhase2.addRedSignals( flashingLeftSignals.getFirst() );
                flashPhase2.addOffSignals( flashingLeftSignals.getSecond() );
                flashPhase2.addRedSignals( flashingRightSignals.getFirst() );
                flashPhase2.addOffSignals( flashingRightSignals.getSecond() );
                flashPhase2.addRedSignals( leftSignals.getFirst() );
                flashPhase2.addOffSignals( leftSignals.getSecond() );
                flashPhase2.addRedSignals( rightSignals.getFirst() );
                flashPhase2.addOffSignals( rightSignals.getSecond() );
                flashPhase2.addRedSignals( throughSignals.getFirst() );
                flashPhase2.addOffSignals( throughSignals.getSecond() );
                flashPhase2.addRedSignals( protectedSignals.getFirst() );
                flashPhase2.addOffSignals( protectedSignals.getSecond() );
                flashPhase2.addRedSignals( pedestrianBeaconSignals.getFirst() );
                flashPhase2.addOffSignals( pedestrianBeaconSignals.getSecond() );
                flashPhase2.addOffSignals( circuit.getPedestrianSignals() );
                flashPhase2.addOffSignals( circuit.getPedestrianAccessorySignals() );
            }
            else {
                flashPhase2.addOffSignals( circuit.getFlashingLeftSignals() );
                flashPhase2.addOffSignals( circuit.getFlashingRightSignals() );
                flashPhase2.addOffSignals( circuit.getLeftSignals() );
                flashPhase2.addOffSignals( circuit.getRightSignals() );
                flashPhase2.addOffSignals( circuit.getThroughSignals() );
                flashPhase2.addOffSignals( circuit.getProtectedSignals() );
                flashPhase2.addOffSignals( circuit.getPedestrianBeaconSignals() );
                flashPhase2.addOffSignals( circuit.getPedestrianSignals() );
                flashPhase2.addOffSignals( circuit.getPedestrianAccessorySignals() );
            }
            circuitIndex++;
        }
        phases[ PHASE_INDEX_FLASH_2 ] = flashPhase2;

        // Create a new TrafficSignalPhase object for first fault phase
        TrafficSignalPhase faultPhase1 = new TrafficSignalPhase( TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
                                                                 TrafficSignalPhaseApplicability.NONE );
        circuitIndex = 1;
        for ( TrafficSignalControllerCircuit circuit : trafficSignalControllerCircuits.getCircuits() ) {
            // Even circuits should be off, odd circuits should flash red
            if ( circuitIndex % 2 == 0 ) {
                faultPhase1.addOffSignals( circuit.getFlashingLeftSignals() );
                faultPhase1.addOffSignals( circuit.getFlashingRightSignals() );
                faultPhase1.addOffSignals( circuit.getLeftSignals() );
                faultPhase1.addOffSignals( circuit.getRightSignals() );
                faultPhase1.addOffSignals( circuit.getThroughSignals() );
                faultPhase1.addOffSignals( circuit.getProtectedSignals() );
                faultPhase1.addOffSignals( circuit.getPedestrianBeaconSignals() );
                faultPhase1.addOffSignals( circuit.getPedestrianSignals() );
                faultPhase1.addOffSignals( circuit.getPedestrianAccessorySignals() );
            }
            else {
                faultPhase1.addRedSignals( circuit.getFlashingLeftSignals() );
                faultPhase1.addRedSignals( circuit.getFlashingRightSignals() );
                faultPhase1.addRedSignals( circuit.getLeftSignals() );
                faultPhase1.addRedSignals( circuit.getRightSignals() );
                faultPhase1.addRedSignals( circuit.getThroughSignals() );
                faultPhase1.addRedSignals( circuit.getProtectedSignals() );
                faultPhase1.addRedSignals( circuit.getPedestrianBeaconSignals() );
                faultPhase1.addOffSignals( circuit.getPedestrianSignals() );
                faultPhase1.addOffSignals( circuit.getPedestrianAccessorySignals() );
            }
            circuitIndex++;
        }
        phases[ PHASE_INDEX_FAULT_1 ] = faultPhase1;

        // Create a new TrafficSignalPhase object for second flash phase
        TrafficSignalPhase faultPhase2 = new TrafficSignalPhase( TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
                                                                 TrafficSignalPhaseApplicability.NONE );
        circuitIndex = 1;
        for ( TrafficSignalControllerCircuit circuit : trafficSignalControllerCircuits.getCircuits() ) {
            // Odd circuits should be off, even circuits should flash red
            if ( circuitIndex % 2 == 0 ) {
                faultPhase2.addRedSignals( circuit.getFlashingLeftSignals() );
                faultPhase2.addRedSignals( circuit.getFlashingRightSignals() );
                faultPhase2.addRedSignals( circuit.getLeftSignals() );
                faultPhase2.addRedSignals( circuit.getRightSignals() );
                faultPhase2.addRedSignals( circuit.getThroughSignals() );
                faultPhase2.addRedSignals( circuit.getProtectedSignals() );
                faultPhase2.addRedSignals( circuit.getPedestrianBeaconSignals() );
                faultPhase2.addOffSignals( circuit.getPedestrianSignals() );
                faultPhase2.addOffSignals( circuit.getPedestrianAccessorySignals() );
            }
            else {
                faultPhase2.addOffSignals( circuit.getFlashingLeftSignals() );
                faultPhase2.addOffSignals( circuit.getFlashingRightSignals() );
                faultPhase2.addOffSignals( circuit.getLeftSignals() );
                faultPhase2.addOffSignals( circuit.getRightSignals() );
                faultPhase2.addOffSignals( circuit.getThroughSignals() );
                faultPhase2.addOffSignals( circuit.getProtectedSignals() );
                faultPhase2.addOffSignals( circuit.getPedestrianBeaconSignals() );
                faultPhase2.addOffSignals( circuit.getPedestrianSignals() );
                faultPhase2.addOffSignals( circuit.getPedestrianAccessorySignals() );
            }
            circuitIndex++;
        }
        phases[ PHASE_INDEX_FAULT_2 ] = faultPhase2;

        // Create a new TrafficSignalPhase object for all red phase
        TrafficSignalPhase allRedPhase = new TrafficSignalPhase( TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
                                                                 TrafficSignalPhaseApplicability.NONE );
        for ( TrafficSignalControllerCircuit circuit : trafficSignalControllerCircuits.getCircuits() ) {
            allRedPhase.addRedSignals( circuit.getFlashingLeftSignals() );
            allRedPhase.addRedSignals( circuit.getFlashingRightSignals() );
            allRedPhase.addRedSignals( circuit.getLeftSignals() );
            allRedPhase.addRedSignals( circuit.getRightSignals() );
            allRedPhase.addRedSignals( circuit.getThroughSignals() );
            allRedPhase.addRedSignals( circuit.getProtectedSignals() );
            allRedPhase.addRedSignals( circuit.getPedestrianBeaconSignals() );
            allRedPhase.addDontWalkSignals( circuit.getPedestrianSignals() );
            allRedPhase.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
        }
        phases[ PHASE_INDEX_ALL_RED ] = allRedPhase;

        // Create a new TrafficSignalPhase object for ramp meter disabled phase
        TrafficSignalPhase rampMeterDisabledPhase = new TrafficSignalPhase( TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE,
                                                                            null,
                                                                            TrafficSignalPhaseApplicability.RAMP_METER_DISABLED );
        for ( TrafficSignalControllerCircuit circuit : trafficSignalControllerCircuits.getCircuits() ) {
            rampMeterDisabledPhase.addOffSignals( circuit.getFlashingLeftSignals() );
            rampMeterDisabledPhase.addOffSignals( circuit.getFlashingRightSignals() );
            rampMeterDisabledPhase.addGreenSignals( circuit.getLeftSignals() );
            rampMeterDisabledPhase.addGreenSignals( circuit.getRightSignals() );
            rampMeterDisabledPhase.addGreenSignals( circuit.getThroughSignals() );
            rampMeterDisabledPhase.addOffSignals( circuit.getProtectedSignals() );
            rampMeterDisabledPhase.addOffSignals( circuit.getPedestrianBeaconSignals() );
            rampMeterDisabledPhase.addOffSignals( circuit.getPedestrianSignals() );
            rampMeterDisabledPhase.addOffSignals( circuit.getPedestrianAccessorySignals() );
        }
        phases[ PHASE_INDEX_RAMP_METER_DISABLED ] = rampMeterDisabledPhase;

        // Create a new TrafficSignalPhase object for ramp meter starting phase
        TrafficSignalPhase rampMeterStartingPhase = new TrafficSignalPhase( TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE,
                                                                            null,
                                                                            TrafficSignalPhaseApplicability.RAMP_METER_STARTING );
        for ( TrafficSignalControllerCircuit circuit : trafficSignalControllerCircuits.getCircuits() ) {
            rampMeterStartingPhase.addYellowSignals( circuit.getFlashingLeftSignals() );
            rampMeterStartingPhase.addYellowSignals( circuit.getFlashingRightSignals() );
            rampMeterStartingPhase.addYellowSignals( circuit.getLeftSignals() );
            rampMeterStartingPhase.addYellowSignals( circuit.getRightSignals() );
            rampMeterStartingPhase.addYellowSignals( circuit.getThroughSignals() );
            rampMeterStartingPhase.addOffSignals( circuit.getProtectedSignals() );
            rampMeterStartingPhase.addOffSignals( circuit.getPedestrianBeaconSignals() );
            rampMeterStartingPhase.addOffSignals( circuit.getPedestrianSignals() );
            rampMeterStartingPhase.addOffSignals( circuit.getPedestrianAccessorySignals() );
        }
        phases[ PHASE_INDEX_RAMP_METER_STARTING ] = rampMeterStartingPhase;

        // Create a new TrafficSignalPhase object for ramp meter first flash phase
        TrafficSignalPhase rampMeterFlash1Phase = new TrafficSignalPhase( TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE,
                                                                          null, TrafficSignalPhaseApplicability.NONE );
        for ( TrafficSignalControllerCircuit circuit : trafficSignalControllerCircuits.getCircuits() ) {
            rampMeterFlash1Phase.addYellowSignals( circuit.getFlashingLeftSignals() );
            rampMeterFlash1Phase.addYellowSignals( circuit.getFlashingRightSignals() );
            rampMeterFlash1Phase.addYellowSignals( circuit.getLeftSignals() );
            rampMeterFlash1Phase.addYellowSignals( circuit.getRightSignals() );
            rampMeterFlash1Phase.addYellowSignals( circuit.getThroughSignals() );
            rampMeterFlash1Phase.addOffSignals( circuit.getProtectedSignals() );
            rampMeterFlash1Phase.addOffSignals( circuit.getPedestrianBeaconSignals() );
            rampMeterFlash1Phase.addFlashDontWalkSignals( circuit.getPedestrianSignals() );
            rampMeterFlash1Phase.addOffSignals( circuit.getPedestrianAccessorySignals() );
        }
        phases[ PHASE_INDEX_RAMP_METER_FLASH_1 ] = rampMeterFlash1Phase;

        // Create a new TrafficSignalPhase object for ramp meter second flash phase
        TrafficSignalPhase rampMeterFlash2Phase = new TrafficSignalPhase( TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE,
                                                                          null, TrafficSignalPhaseApplicability.NONE );
        for ( TrafficSignalControllerCircuit circuit : trafficSignalControllerCircuits.getCircuits() ) {
            rampMeterFlash2Phase.addOffSignals( circuit.getFlashingLeftSignals() );
            rampMeterFlash2Phase.addOffSignals( circuit.getFlashingRightSignals() );
            rampMeterFlash2Phase.addOffSignals( circuit.getLeftSignals() );
            rampMeterFlash2Phase.addOffSignals( circuit.getRightSignals() );
            rampMeterFlash2Phase.addOffSignals( circuit.getThroughSignals() );
            rampMeterFlash2Phase.addOffSignals( circuit.getProtectedSignals() );
            rampMeterFlash2Phase.addOffSignals( circuit.getPedestrianBeaconSignals() );
            rampMeterFlash2Phase.addFlashDontWalkSignals( circuit.getPedestrianSignals() );
            rampMeterFlash2Phase.addOffSignals( circuit.getPedestrianAccessorySignals() );
        }
        phases[ PHASE_INDEX_RAMP_METER_FLASH_2 ] = rampMeterFlash2Phase;

        // Create a new TrafficSignalPhase object for requestable default green phase
        TrafficSignalPhase requestableDefaultGreenPhase = new TrafficSignalPhase(
                TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
                TrafficSignalPhaseApplicability.REQUESTABLE_DEFAULT_GREEN );
        circuitIndex = 1;
        for ( TrafficSignalControllerCircuit circuit : trafficSignalControllerCircuits.getCircuits() ) {
            // Handle the first circuit
            if ( circuitIndex == 1 ) {
                requestableDefaultGreenPhase.addOffSignals( circuit.getFlashingLeftSignals() );
                requestableDefaultGreenPhase.addOffSignals( circuit.getFlashingRightSignals() );
                requestableDefaultGreenPhase.addGreenSignals( circuit.getLeftSignals() );
                requestableDefaultGreenPhase.addGreenSignals( circuit.getRightSignals() );
                requestableDefaultGreenPhase.addGreenSignals( circuit.getThroughSignals() );
                requestableDefaultGreenPhase.addGreenSignals( circuit.getProtectedSignals() );
                requestableDefaultGreenPhase.addOffSignals( circuit.getPedestrianBeaconSignals() );
                requestableDefaultGreenPhase.addWalkSignals( circuit.getPedestrianSignals() );
                requestableDefaultGreenPhase.addWalkSignals( circuit.getPedestrianAccessorySignals() );
            }
            // Handle second + higher circuits
            else {
                requestableDefaultGreenPhase.addRedSignals( circuit.getFlashingLeftSignals() );
                requestableDefaultGreenPhase.addRedSignals( circuit.getFlashingRightSignals() );
                requestableDefaultGreenPhase.addRedSignals( circuit.getLeftSignals() );
                requestableDefaultGreenPhase.addRedSignals( circuit.getRightSignals() );
                requestableDefaultGreenPhase.addRedSignals( circuit.getThroughSignals() );
                requestableDefaultGreenPhase.addRedSignals( circuit.getProtectedSignals() );
                requestableDefaultGreenPhase.addRedSignals( circuit.getPedestrianBeaconSignals() );
                requestableDefaultGreenPhase.addDontWalkSignals( circuit.getPedestrianSignals() );
                requestableDefaultGreenPhase.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
            }
            circuitIndex++;
        }
        phases[ PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN ] = requestableDefaultGreenPhase;

        // Create a new TrafficSignalPhase object for requestable default green + flashing don't walk phase
        TrafficSignalPhase requestableDefaultGreenFlashDwPhase = new TrafficSignalPhase(
                TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
                TrafficSignalPhaseApplicability.REQUESTABLE_DEFAULT_GREEN_FLASH_DW );
        circuitIndex = 1;
        for ( TrafficSignalControllerCircuit circuit : trafficSignalControllerCircuits.getCircuits() ) {
            // Handle the first circuit
            if ( circuitIndex == 1 ) {
                requestableDefaultGreenFlashDwPhase.addOffSignals( circuit.getFlashingLeftSignals() );
                requestableDefaultGreenFlashDwPhase.addOffSignals( circuit.getFlashingRightSignals() );
                requestableDefaultGreenFlashDwPhase.addGreenSignals( circuit.getLeftSignals() );
                requestableDefaultGreenFlashDwPhase.addGreenSignals( circuit.getRightSignals() );
                requestableDefaultGreenFlashDwPhase.addGreenSignals( circuit.getThroughSignals() );
                requestableDefaultGreenFlashDwPhase.addGreenSignals( circuit.getProtectedSignals() );
                requestableDefaultGreenFlashDwPhase.addOffSignals( circuit.getPedestrianBeaconSignals() );
                requestableDefaultGreenFlashDwPhase.addFlashDontWalkSignals( circuit.getPedestrianSignals() );
                requestableDefaultGreenFlashDwPhase.addFlashDontWalkSignals( circuit.getPedestrianAccessorySignals() );
            }
            // Handle second + higher circuits
            else {
                requestableDefaultGreenFlashDwPhase.addRedSignals( circuit.getFlashingLeftSignals() );
                requestableDefaultGreenFlashDwPhase.addRedSignals( circuit.getFlashingRightSignals() );
                requestableDefaultGreenFlashDwPhase.addRedSignals( circuit.getLeftSignals() );
                requestableDefaultGreenFlashDwPhase.addRedSignals( circuit.getRightSignals() );
                requestableDefaultGreenFlashDwPhase.addRedSignals( circuit.getThroughSignals() );
                requestableDefaultGreenFlashDwPhase.addRedSignals( circuit.getProtectedSignals() );
                requestableDefaultGreenFlashDwPhase.addGreenSignals( circuit.getPedestrianBeaconSignals() );
                requestableDefaultGreenFlashDwPhase.addDontWalkSignals( circuit.getPedestrianSignals() );
                requestableDefaultGreenFlashDwPhase.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
            }
            circuitIndex++;
        }
        phases[ PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW ] = requestableDefaultGreenFlashDwPhase;

        // Create a new TrafficSignalPhase object for first requestable default green + flashing don't walk + flashing
        // yellow HAWK (High-Intensity Activated crossWalK) phase
        TrafficSignalPhase requestableDefaultGreenFlashDwHawkPhase1 = new TrafficSignalPhase(
                TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
                TrafficSignalPhaseApplicability.REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK );
        circuitIndex = 1;
        for ( TrafficSignalControllerCircuit circuit : trafficSignalControllerCircuits.getCircuits() ) {
            // Handle the first circuit
            if ( circuitIndex == 1 ) {
                requestableDefaultGreenFlashDwHawkPhase1.addOffSignals( circuit.getFlashingLeftSignals() );
                requestableDefaultGreenFlashDwHawkPhase1.addOffSignals( circuit.getFlashingRightSignals() );
                requestableDefaultGreenFlashDwHawkPhase1.addGreenSignals( circuit.getLeftSignals() );
                requestableDefaultGreenFlashDwHawkPhase1.addGreenSignals( circuit.getRightSignals() );
                requestableDefaultGreenFlashDwHawkPhase1.addGreenSignals( circuit.getThroughSignals() );
                requestableDefaultGreenFlashDwHawkPhase1.addGreenSignals( circuit.getProtectedSignals() );
                requestableDefaultGreenFlashDwHawkPhase1.addYellowSignals( circuit.getPedestrianBeaconSignals() );
                requestableDefaultGreenFlashDwHawkPhase1.addFlashDontWalkSignals( circuit.getPedestrianSignals() );
                requestableDefaultGreenFlashDwHawkPhase1.addFlashDontWalkSignals(
                        circuit.getPedestrianAccessorySignals() );
            }
            // Handle second + higher circuits
            else {
                requestableDefaultGreenFlashDwHawkPhase1.addRedSignals( circuit.getFlashingLeftSignals() );
                requestableDefaultGreenFlashDwHawkPhase1.addRedSignals( circuit.getFlashingRightSignals() );
                requestableDefaultGreenFlashDwHawkPhase1.addRedSignals( circuit.getLeftSignals() );
                requestableDefaultGreenFlashDwHawkPhase1.addRedSignals( circuit.getRightSignals() );
                requestableDefaultGreenFlashDwHawkPhase1.addRedSignals( circuit.getThroughSignals() );
                requestableDefaultGreenFlashDwHawkPhase1.addRedSignals( circuit.getProtectedSignals() );
                requestableDefaultGreenFlashDwHawkPhase1.addGreenSignals( circuit.getPedestrianBeaconSignals() );
                requestableDefaultGreenFlashDwHawkPhase1.addDontWalkSignals( circuit.getPedestrianSignals() );
                requestableDefaultGreenFlashDwHawkPhase1.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
            }
            circuitIndex++;
        }
        phases[ PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK_1 ] = requestableDefaultGreenFlashDwHawkPhase1;

        // Create a new TrafficSignalPhase object for second requestable default green + flashing don't walk + flashing
        // yellow HAWK (High-Intensity Activated crossWalK) phase
        TrafficSignalPhase requestableDefaultGreenFlashDwHawkPhase2 = new TrafficSignalPhase(
                TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
                TrafficSignalPhaseApplicability.REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK );
        circuitIndex = 1;
        for ( TrafficSignalControllerCircuit circuit : trafficSignalControllerCircuits.getCircuits() ) {
            // Handle the first circuit
            if ( circuitIndex == 1 ) {
                requestableDefaultGreenFlashDwHawkPhase2.addOffSignals( circuit.getFlashingLeftSignals() );
                requestableDefaultGreenFlashDwHawkPhase2.addOffSignals( circuit.getFlashingRightSignals() );
                requestableDefaultGreenFlashDwHawkPhase2.addGreenSignals( circuit.getLeftSignals() );
                requestableDefaultGreenFlashDwHawkPhase2.addGreenSignals( circuit.getRightSignals() );
                requestableDefaultGreenFlashDwHawkPhase2.addGreenSignals( circuit.getThroughSignals() );
                requestableDefaultGreenFlashDwHawkPhase2.addGreenSignals( circuit.getProtectedSignals() );
                requestableDefaultGreenFlashDwHawkPhase2.addOffSignals( circuit.getPedestrianBeaconSignals() );
                requestableDefaultGreenFlashDwHawkPhase2.addFlashDontWalkSignals( circuit.getPedestrianSignals() );
                requestableDefaultGreenFlashDwHawkPhase2.addFlashDontWalkSignals(
                        circuit.getPedestrianAccessorySignals() );
            }
            // Handle second + higher circuits
            else {
                requestableDefaultGreenFlashDwHawkPhase2.addRedSignals( circuit.getFlashingLeftSignals() );
                requestableDefaultGreenFlashDwHawkPhase2.addRedSignals( circuit.getFlashingRightSignals() );
                requestableDefaultGreenFlashDwHawkPhase2.addRedSignals( circuit.getLeftSignals() );
                requestableDefaultGreenFlashDwHawkPhase2.addRedSignals( circuit.getRightSignals() );
                requestableDefaultGreenFlashDwHawkPhase2.addRedSignals( circuit.getThroughSignals() );
                requestableDefaultGreenFlashDwHawkPhase2.addRedSignals( circuit.getProtectedSignals() );
                requestableDefaultGreenFlashDwHawkPhase2.addGreenSignals( circuit.getPedestrianBeaconSignals() );
                requestableDefaultGreenFlashDwHawkPhase2.addDontWalkSignals( circuit.getPedestrianSignals() );
                requestableDefaultGreenFlashDwHawkPhase2.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
            }
            circuitIndex++;
        }
        phases[ PHASE_INDEX_REQUESTABLE_DEFAULT_GREEN_FLASH_DW_HAWK_2 ] = requestableDefaultGreenFlashDwHawkPhase2;

        // Create a new TrafficSignalPhase object for requestable default yellow phase
        TrafficSignalPhase requestableDefaultYellowPhase = new TrafficSignalPhase(
                TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
                TrafficSignalPhaseApplicability.REQUESTABLE_DEFAULT_YELLOW );
        circuitIndex = 1;
        for ( TrafficSignalControllerCircuit circuit : trafficSignalControllerCircuits.getCircuits() ) {
            // Handle the first circuit
            if ( circuitIndex == 1 ) {
                requestableDefaultYellowPhase.addYellowSignals( circuit.getFlashingLeftSignals() );
                requestableDefaultYellowPhase.addYellowSignals( circuit.getFlashingRightSignals() );
                requestableDefaultYellowPhase.addYellowSignals( circuit.getLeftSignals() );
                requestableDefaultYellowPhase.addYellowSignals( circuit.getRightSignals() );
                requestableDefaultYellowPhase.addYellowSignals( circuit.getThroughSignals() );
                requestableDefaultYellowPhase.addYellowSignals( circuit.getProtectedSignals() );
                requestableDefaultYellowPhase.addYellowSignals( circuit.getPedestrianBeaconSignals() );
                requestableDefaultYellowPhase.addDontWalkSignals( circuit.getPedestrianSignals() );
                requestableDefaultYellowPhase.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
            }
            // Handle second + higher circuits
            else {
                requestableDefaultYellowPhase.addRedSignals( circuit.getFlashingLeftSignals() );
                requestableDefaultYellowPhase.addRedSignals( circuit.getFlashingRightSignals() );
                requestableDefaultYellowPhase.addRedSignals( circuit.getLeftSignals() );
                requestableDefaultYellowPhase.addRedSignals( circuit.getRightSignals() );
                requestableDefaultYellowPhase.addRedSignals( circuit.getThroughSignals() );
                requestableDefaultYellowPhase.addRedSignals( circuit.getProtectedSignals() );
                requestableDefaultYellowPhase.addGreenSignals( circuit.getPedestrianBeaconSignals() );
                requestableDefaultYellowPhase.addDontWalkSignals( circuit.getPedestrianSignals() );
                requestableDefaultYellowPhase.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
            }
            circuitIndex++;
        }
        phases[ PHASE_INDEX_REQUESTABLE_DEFAULT_YELLOW ] = requestableDefaultYellowPhase;

        // Create a new TrafficSignalPhase object for requestable default red phase
        TrafficSignalPhase requestableDefaultRedPhase = new TrafficSignalPhase(
                TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
                TrafficSignalPhaseApplicability.REQUESTABLE_DEFAULT_RED );
        circuitIndex = 1;
        for ( TrafficSignalControllerCircuit circuit : trafficSignalControllerCircuits.getCircuits() ) {
            // Handle the first circuit
            if ( circuitIndex == 1 ) {
                requestableDefaultRedPhase.addRedSignals( circuit.getFlashingLeftSignals() );
                requestableDefaultRedPhase.addRedSignals( circuit.getFlashingRightSignals() );
                requestableDefaultRedPhase.addRedSignals( circuit.getLeftSignals() );
                requestableDefaultRedPhase.addRedSignals( circuit.getRightSignals() );
                requestableDefaultRedPhase.addRedSignals( circuit.getThroughSignals() );
                requestableDefaultRedPhase.addRedSignals( circuit.getProtectedSignals() );
                requestableDefaultRedPhase.addRedSignals( circuit.getPedestrianBeaconSignals() );
                requestableDefaultRedPhase.addDontWalkSignals( circuit.getPedestrianSignals() );
                requestableDefaultRedPhase.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
            }
            // Handle second + higher circuits
            else {
                requestableDefaultRedPhase.addRedSignals( circuit.getFlashingLeftSignals() );
                requestableDefaultRedPhase.addRedSignals( circuit.getFlashingRightSignals() );
                requestableDefaultRedPhase.addRedSignals( circuit.getLeftSignals() );
                requestableDefaultRedPhase.addRedSignals( circuit.getRightSignals() );
                requestableDefaultRedPhase.addRedSignals( circuit.getThroughSignals() );
                requestableDefaultRedPhase.addRedSignals( circuit.getProtectedSignals() );
                requestableDefaultRedPhase.addGreenSignals( circuit.getPedestrianBeaconSignals() );
                requestableDefaultRedPhase.addDontWalkSignals( circuit.getPedestrianSignals() );
                requestableDefaultRedPhase.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
            }
            circuitIndex++;
        }
        phases[ PHASE_INDEX_REQUESTABLE_DEFAULT_RED ] = requestableDefaultRedPhase;

        // Create a new TrafficSignalPhase object for requestable service green phase
        TrafficSignalPhase requestableServiceGreenPhase = new TrafficSignalPhase(
                TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
                TrafficSignalPhaseApplicability.REQUESTABLE_SERVICE_GREEN );
        circuitIndex = 1;
        for ( TrafficSignalControllerCircuit circuit : trafficSignalControllerCircuits.getCircuits() ) {
            // Handle the first circuit
            if ( circuitIndex == 1 ) {
                requestableServiceGreenPhase.addRedSignals( circuit.getFlashingLeftSignals() );
                requestableServiceGreenPhase.addRedSignals( circuit.getFlashingRightSignals() );
                requestableServiceGreenPhase.addRedSignals( circuit.getLeftSignals() );
                requestableServiceGreenPhase.addRedSignals( circuit.getRightSignals() );
                requestableServiceGreenPhase.addRedSignals( circuit.getThroughSignals() );
                requestableServiceGreenPhase.addRedSignals( circuit.getProtectedSignals() );
                requestableServiceGreenPhase.addRedSignals( circuit.getPedestrianBeaconSignals() );
                requestableServiceGreenPhase.addDontWalkSignals( circuit.getPedestrianSignals() );
                requestableServiceGreenPhase.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
            }
            // Handle second + higher circuits
            else {
                requestableServiceGreenPhase.addOffSignals( circuit.getFlashingLeftSignals() );
                requestableServiceGreenPhase.addOffSignals( circuit.getFlashingRightSignals() );
                requestableServiceGreenPhase.addGreenSignals( circuit.getLeftSignals() );
                requestableServiceGreenPhase.addGreenSignals( circuit.getRightSignals() );
                requestableServiceGreenPhase.addGreenSignals( circuit.getThroughSignals() );
                requestableServiceGreenPhase.addGreenSignals( circuit.getProtectedSignals() );
                requestableServiceGreenPhase.addOffSignals( circuit.getPedestrianBeaconSignals() );
                requestableServiceGreenPhase.addWalkSignals( circuit.getPedestrianSignals() );
                requestableServiceGreenPhase.addWalkSignals( circuit.getPedestrianAccessorySignals() );
            }
            circuitIndex++;
        }
        phases[ PHASE_INDEX_REQUESTABLE_SERVICE_GREEN ] = requestableServiceGreenPhase;

        // Create a new TrafficSignalPhase object for requestable service green + flashing don't walk phase
        TrafficSignalPhase requestableServiceGreenFlashDwPhase = new TrafficSignalPhase(
                TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
                TrafficSignalPhaseApplicability.REQUESTABLE_SERVICE_GREEN_FLASH_DW );
        circuitIndex = 1;
        for ( TrafficSignalControllerCircuit circuit : trafficSignalControllerCircuits.getCircuits() ) {
            // Handle the first circuit
            if ( circuitIndex == 1 ) {
                requestableServiceGreenFlashDwPhase.addRedSignals( circuit.getFlashingLeftSignals() );
                requestableServiceGreenFlashDwPhase.addRedSignals( circuit.getFlashingRightSignals() );
                requestableServiceGreenFlashDwPhase.addRedSignals( circuit.getLeftSignals() );
                requestableServiceGreenFlashDwPhase.addRedSignals( circuit.getRightSignals() );
                requestableServiceGreenFlashDwPhase.addRedSignals( circuit.getThroughSignals() );
                requestableServiceGreenFlashDwPhase.addRedSignals( circuit.getProtectedSignals() );
                requestableServiceGreenFlashDwPhase.addGreenSignals( circuit.getPedestrianBeaconSignals() );
                requestableServiceGreenFlashDwPhase.addDontWalkSignals( circuit.getPedestrianSignals() );
                requestableServiceGreenFlashDwPhase.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
            }
            // Handle second + higher circuits
            else {
                requestableServiceGreenFlashDwPhase.addOffSignals( circuit.getFlashingLeftSignals() );
                requestableServiceGreenFlashDwPhase.addOffSignals( circuit.getFlashingRightSignals() );
                requestableServiceGreenFlashDwPhase.addGreenSignals( circuit.getLeftSignals() );
                requestableServiceGreenFlashDwPhase.addGreenSignals( circuit.getRightSignals() );
                requestableServiceGreenFlashDwPhase.addGreenSignals( circuit.getThroughSignals() );
                requestableServiceGreenFlashDwPhase.addGreenSignals( circuit.getProtectedSignals() );
                requestableServiceGreenFlashDwPhase.addOffSignals( circuit.getPedestrianBeaconSignals() );
                requestableServiceGreenFlashDwPhase.addFlashDontWalkSignals( circuit.getPedestrianSignals() );
                requestableServiceGreenFlashDwPhase.addFlashDontWalkSignals( circuit.getPedestrianAccessorySignals() );
            }
            circuitIndex++;
        }
        phases[ PHASE_INDEX_REQUESTABLE_SERVICE_GREEN_FLASH_DW ] = requestableServiceGreenFlashDwPhase;

        // Create a new TrafficSignalPhase object for first requestable service green + flashing don't walk + flashing
        // yellow HAWK (High-Intensity Activated crossWalK) phase
        TrafficSignalPhase requestableServiceGreenFlashDwHawkPhase1 = new TrafficSignalPhase(
                TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
                TrafficSignalPhaseApplicability.REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK );
        circuitIndex = 1;
        for ( TrafficSignalControllerCircuit circuit : trafficSignalControllerCircuits.getCircuits() ) {
            // Handle the first circuit
            if ( circuitIndex == 1 ) {
                requestableServiceGreenFlashDwHawkPhase1.addRedSignals( circuit.getFlashingLeftSignals() );
                requestableServiceGreenFlashDwHawkPhase1.addRedSignals( circuit.getFlashingRightSignals() );
                requestableServiceGreenFlashDwHawkPhase1.addRedSignals( circuit.getLeftSignals() );
                requestableServiceGreenFlashDwHawkPhase1.addRedSignals( circuit.getRightSignals() );
                requestableServiceGreenFlashDwHawkPhase1.addRedSignals( circuit.getThroughSignals() );
                requestableServiceGreenFlashDwHawkPhase1.addRedSignals( circuit.getProtectedSignals() );
                requestableServiceGreenFlashDwHawkPhase1.addGreenSignals( circuit.getPedestrianBeaconSignals() );
                requestableServiceGreenFlashDwHawkPhase1.addDontWalkSignals( circuit.getPedestrianSignals() );
                requestableServiceGreenFlashDwHawkPhase1.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
            }
            // Handle second + higher circuits
            else {
                requestableServiceGreenFlashDwHawkPhase1.addOffSignals( circuit.getFlashingLeftSignals() );
                requestableServiceGreenFlashDwHawkPhase1.addOffSignals( circuit.getFlashingRightSignals() );
                requestableServiceGreenFlashDwHawkPhase1.addGreenSignals( circuit.getLeftSignals() );
                requestableServiceGreenFlashDwHawkPhase1.addGreenSignals( circuit.getRightSignals() );
                requestableServiceGreenFlashDwHawkPhase1.addGreenSignals( circuit.getThroughSignals() );
                requestableServiceGreenFlashDwHawkPhase1.addGreenSignals( circuit.getProtectedSignals() );
                requestableServiceGreenFlashDwHawkPhase1.addYellowSignals( circuit.getPedestrianBeaconSignals() );
                requestableServiceGreenFlashDwHawkPhase1.addFlashDontWalkSignals( circuit.getPedestrianSignals() );
                requestableServiceGreenFlashDwHawkPhase1.addFlashDontWalkSignals(
                        circuit.getPedestrianAccessorySignals() );
            }
            circuitIndex++;
        }
        phases[ PHASE_INDEX_REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK_1 ] = requestableServiceGreenFlashDwHawkPhase1;

        // Create a new TrafficSignalPhase object for second requestable service green + flashing don't walk + flashing
        // yellow HAWK (High-Intensity Activated crossWalK) phase
        TrafficSignalPhase requestableServiceGreenFlashDwHawkPhase2 = new TrafficSignalPhase(
                TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
                TrafficSignalPhaseApplicability.REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK );
        circuitIndex = 1;
        for ( TrafficSignalControllerCircuit circuit : trafficSignalControllerCircuits.getCircuits() ) {
            // Handle the first circuit
            if ( circuitIndex == 1 ) {
                requestableServiceGreenFlashDwHawkPhase2.addRedSignals( circuit.getFlashingLeftSignals() );
                requestableServiceGreenFlashDwHawkPhase2.addRedSignals( circuit.getFlashingRightSignals() );
                requestableServiceGreenFlashDwHawkPhase2.addRedSignals( circuit.getLeftSignals() );
                requestableServiceGreenFlashDwHawkPhase2.addRedSignals( circuit.getRightSignals() );
                requestableServiceGreenFlashDwHawkPhase2.addRedSignals( circuit.getThroughSignals() );
                requestableServiceGreenFlashDwHawkPhase2.addRedSignals( circuit.getProtectedSignals() );
                requestableServiceGreenFlashDwHawkPhase2.addGreenSignals( circuit.getPedestrianBeaconSignals() );
                requestableServiceGreenFlashDwHawkPhase2.addDontWalkSignals( circuit.getPedestrianSignals() );
                requestableServiceGreenFlashDwHawkPhase2.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
            }
            // Handle second + higher circuits
            else {
                requestableServiceGreenFlashDwHawkPhase2.addOffSignals( circuit.getFlashingLeftSignals() );
                requestableServiceGreenFlashDwHawkPhase2.addOffSignals( circuit.getFlashingRightSignals() );
                requestableServiceGreenFlashDwHawkPhase2.addGreenSignals( circuit.getLeftSignals() );
                requestableServiceGreenFlashDwHawkPhase2.addGreenSignals( circuit.getRightSignals() );
                requestableServiceGreenFlashDwHawkPhase2.addGreenSignals( circuit.getThroughSignals() );
                requestableServiceGreenFlashDwHawkPhase2.addGreenSignals( circuit.getProtectedSignals() );
                requestableServiceGreenFlashDwHawkPhase2.addOffSignals( circuit.getPedestrianBeaconSignals() );
                requestableServiceGreenFlashDwHawkPhase2.addFlashDontWalkSignals( circuit.getPedestrianSignals() );
                requestableServiceGreenFlashDwHawkPhase2.addFlashDontWalkSignals(
                        circuit.getPedestrianAccessorySignals() );
            }
            circuitIndex++;
        }
        phases[ PHASE_INDEX_REQUESTABLE_SERVICE_GREEN_FLASH_DW_HAWK_2 ] = requestableServiceGreenFlashDwHawkPhase2;

        // Create a new TrafficSignalPhase object for requestable service yellow phase
        TrafficSignalPhase requestableServiceYellowPhase = new TrafficSignalPhase(
                TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
                TrafficSignalPhaseApplicability.REQUESTABLE_SERVICE_YELLOW );
        circuitIndex = 1;
        for ( TrafficSignalControllerCircuit circuit : trafficSignalControllerCircuits.getCircuits() ) {
            // Handle the first circuit
            if ( circuitIndex == 1 ) {
                requestableServiceYellowPhase.addRedSignals( circuit.getFlashingLeftSignals() );
                requestableServiceYellowPhase.addRedSignals( circuit.getFlashingRightSignals() );
                requestableServiceYellowPhase.addRedSignals( circuit.getLeftSignals() );
                requestableServiceYellowPhase.addRedSignals( circuit.getRightSignals() );
                requestableServiceYellowPhase.addRedSignals( circuit.getThroughSignals() );
                requestableServiceYellowPhase.addRedSignals( circuit.getProtectedSignals() );
                requestableServiceYellowPhase.addGreenSignals( circuit.getPedestrianBeaconSignals() );
                requestableServiceYellowPhase.addDontWalkSignals( circuit.getPedestrianSignals() );
                requestableServiceYellowPhase.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
            }
            // Handle second + higher circuits
            else {
                requestableServiceYellowPhase.addYellowSignals( circuit.getFlashingLeftSignals() );
                requestableServiceYellowPhase.addYellowSignals( circuit.getFlashingRightSignals() );
                requestableServiceYellowPhase.addYellowSignals( circuit.getLeftSignals() );
                requestableServiceYellowPhase.addYellowSignals( circuit.getRightSignals() );
                requestableServiceYellowPhase.addYellowSignals( circuit.getThroughSignals() );
                requestableServiceYellowPhase.addYellowSignals( circuit.getProtectedSignals() );
                requestableServiceYellowPhase.addYellowSignals( circuit.getPedestrianBeaconSignals() );
                requestableServiceYellowPhase.addDontWalkSignals( circuit.getPedestrianSignals() );
                requestableServiceYellowPhase.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
            }
            circuitIndex++;
        }
        phases[ PHASE_INDEX_REQUESTABLE_SERVICE_YELLOW ] = requestableServiceYellowPhase;

        // Create a new TrafficSignalPhase object for requestable service red phase
        TrafficSignalPhase requestableServiceRedPhase = new TrafficSignalPhase(
                TrafficSignalPhase.CIRCUIT_NOT_APPLICABLE, null,
                TrafficSignalPhaseApplicability.REQUESTABLE_SERVICE_RED );
        circuitIndex = 1;
        for ( TrafficSignalControllerCircuit circuit : trafficSignalControllerCircuits.getCircuits() ) {
            // Handle the first circuit
            if ( circuitIndex == 1 ) {
                requestableServiceRedPhase.addRedSignals( circuit.getFlashingLeftSignals() );
                requestableServiceRedPhase.addRedSignals( circuit.getFlashingRightSignals() );
                requestableServiceRedPhase.addRedSignals( circuit.getLeftSignals() );
                requestableServiceRedPhase.addRedSignals( circuit.getRightSignals() );
                requestableServiceRedPhase.addRedSignals( circuit.getThroughSignals() );
                requestableServiceRedPhase.addRedSignals( circuit.getProtectedSignals() );
                requestableServiceRedPhase.addGreenSignals( circuit.getPedestrianBeaconSignals() );
                requestableServiceRedPhase.addDontWalkSignals( circuit.getPedestrianSignals() );
                requestableServiceRedPhase.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
            }
            // Handle second + higher circuits
            else {
                requestableServiceRedPhase.addRedSignals( circuit.getFlashingLeftSignals() );
                requestableServiceRedPhase.addRedSignals( circuit.getFlashingRightSignals() );
                requestableServiceRedPhase.addRedSignals( circuit.getLeftSignals() );
                requestableServiceRedPhase.addRedSignals( circuit.getRightSignals() );
                requestableServiceRedPhase.addRedSignals( circuit.getThroughSignals() );
                requestableServiceRedPhase.addRedSignals( circuit.getProtectedSignals() );
                requestableServiceRedPhase.addRedSignals( circuit.getPedestrianBeaconSignals() );
                requestableServiceRedPhase.addDontWalkSignals( circuit.getPedestrianSignals() );
                requestableServiceRedPhase.addDontWalkSignals( circuit.getPedestrianAccessorySignals() );
            }
            circuitIndex++;
        }
        phases[ PHASE_INDEX_REQUESTABLE_SERVICE_RED ] = requestableServiceRedPhase;
    }

    /**
     * Private constructor for creating a new {@link TrafficSignalPhases} object from the given array of
     * {@link TrafficSignalPhase}s from an {@link NBTTagCompound}.
     *
     * @param phases The array of {@link TrafficSignalPhase}s from the {@link NBTTagCompound}.
     *
     * @since 1.0
     */
    private TrafficSignalPhases( TrafficSignalPhase[] phases ) {
        this.phases = phases;
    }

    /**
     * Gets the {@link TrafficSignalPhase} at the given index.
     *
     * @param phaseIndex The index of the {@link TrafficSignalPhase} to get.
     *
     * @return The {@link TrafficSignalPhase} at the given index.
     *
     * @throws IndexOutOfBoundsException If the given index is out of bounds.
     * @since 1.0
     */
    public TrafficSignalPhase getPhase( int phaseIndex )
    {
        return phases[ phaseIndex ];
    }

    /**
     * Gets the number of {@link TrafficSignalPhase}s in this {@link TrafficSignalPhases} object.
     *
     * @return The number of {@link TrafficSignalPhase}s in this {@link TrafficSignalPhases} object.
     *
     * @since 1.0
     */
    public int getPhaseCount()
    {
        return phases.length;
    }

    /**
     * Verifies that the number of {@link TrafficSignalPhase}s in this {@link TrafficSignalPhases} object is equal to
     * the expected number of phases, defined by {@link #PHASE_INDEX_COUNT}.
     *
     * @return {@code true} if the number of {@link TrafficSignalPhase}s in this {@link TrafficSignalPhases} object is
     *         equal to the expected number of phases, defined by {@link #PHASE_INDEX_COUNT}, {@code false} otherwise.
     */
    public boolean verifyPhaseCount()
    {
        return getPhaseCount() == PHASE_INDEX_COUNT;
    }

    /**
     * Gets a {@link TrafficSignalPhases} object from the given {@link NBTTagCompound}.
     *
     * @param nbt The {@link NBTTagCompound} containing the data for the {@link TrafficSignalPhases} object in NBT
     *            format.
     *
     * @return The {@link TrafficSignalPhases} object from the given {@link NBTTagCompound}.
     *
     * @throws IllegalArgumentException If the given {@link NBTTagCompound} is null.
     * @see #toNBT()
     * @since 1.0
     */
    public static TrafficSignalPhases fromNBT( NBTTagCompound nbt )
    {
        // Validate the NBT
        if ( nbt == null ) {
            throw new IllegalArgumentException( "The NBT cannot be null." );
        }

        // Create new phases array using phase count
        TrafficSignalPhase[] phases = new TrafficSignalPhase[ nbt.getSize() ];

        // Deserialize phases
        for ( int i = 0; i < phases.length; i++ ) {
            phases[ i ] = TrafficSignalPhase.fromNBT( nbt.getCompoundTag( String.valueOf( i ) ) );
        }

        // Return the TrafficSignalPhases object
        return new TrafficSignalPhases( phases );
    }

    /**
     * Gets an {@link NBTTagCompound} containing the data for this {@link TrafficSignalPhases} object in NBT format.
     *
     * @return The {@link NBTTagCompound} containing the data for this {@link TrafficSignalPhases} object in NBT
     *         format.
     *         <p>
     *         The returned {@link NBTTagCompound} can be used to reconstruct the {@link TrafficSignalPhases} object.
     *         </p>
     *
     * @see #fromNBT(NBTTagCompound)
     * @since 1.0
     */
    public NBTTagCompound toNBT()
    {
        // Create the compound
        NBTTagCompound compound = new NBTTagCompound();

        // Add the list of phases
        for ( int i = 0; i < phases.length; i++ ) {
            // Get the phase
            TrafficSignalPhase phase = phases[ i ];

            // Add the phase to the compound
            compound.setTag( String.valueOf( i ), phase.toNBT() );
        }

        // Return the compound
        return compound;
    }

    /**
     * Checks if the given {@link Object} is equal to this {@link TrafficSignalPhases} object.
     *
     * @param o The {@link Object} to check.
     *
     * @return {@code true} if the given {@link Object} is equal to this {@link TrafficSignalPhases} object,
     *         {@code false} otherwise.
     *
     * @since 1.0
     */
    @Override
    public boolean equals( Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }
        TrafficSignalPhases that = ( TrafficSignalPhases ) o;
        return Arrays.equals( phases, that.phases );
    }

    /**
     * Gets the hash code of this {@link TrafficSignalPhases} object.
     *
     * @return The hash code of this {@link TrafficSignalPhases} object.
     *
     * @since 1.0
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode( phases );
    }
}
