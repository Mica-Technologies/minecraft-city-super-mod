package com.micatechnologies.minecraft.csm.lifesafety;

/**
 * The tile entity of a detector that is also a strobe (the Gentex 710CS-C). It is a
 * {@link TileEntityFireAlarmSensor} in every way, so the detector links and reports as any
 * detector does; the class exists only so the strobe flash renderer can be bound to these
 * devices alone rather than to every pull station and detector in the world.
 *
 * @since 2026.9
 */
public class TileEntityFireAlarmSensorStrobe extends TileEntityFireAlarmSensor {

}
