package com.micatechnologies.minecraft.csm.trafficsignals.logic;

/**
 * Marker interface identifying a block as a traffic signal sensor, regardless of which rotation base
 * class it extends. Both {@link AbstractBlockTrafficSignalSensor} (four-direction NSEW rotation) and
 * {@link AbstractBlockTrafficSignalSensorHZEight} (eight-direction HZEight rotation) implement this,
 * so the player-facing sensor tools ({@code ItemSensorZoneTool}, {@code ItemSignalLinkTool}) can
 * recognize and program <i>every</i> sensor without depending on a single concrete base class.
 *
 * <p>All implementors back the sensor with a
 * {@link com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalSensor}, whose
 * corner-region detection is independent of the block's {@code FACING}. Code that needs to read or
 * write a sensor's cardinal facing (e.g. the configuration tool's re-orient mode) must still narrow
 * to the concrete {@link AbstractBlockTrafficSignalSensor}, because the HZEight variant exposes a
 * different (non-{@link net.minecraft.block.BlockHorizontal#FACING}) facing property.</p>
 *
 * @author Mica Technologies
 * @since 1.0
 */
public interface ITrafficSignalSensor {

}
