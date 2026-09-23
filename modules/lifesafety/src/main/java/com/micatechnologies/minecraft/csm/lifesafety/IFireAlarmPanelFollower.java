package com.micatechnologies.minecraft.csm.lifesafety;

/**
 * A device that follows a fire alarm control panel's state without being one of its appliances or
 * initiating devices: a magnetic door holder that lets go when the panel alarms, a remote
 * annunciator that shows it. The {@link ItemFireAlarmLinker} links one by storing the panel on the
 * device's {@link TileEntityFireAlarmSensor}, as it does for a pull station, but does not add it to
 * the panel's own lists: the panel has nothing to tell it, the device looks.
 *
 * @since 2026.9
 */
public interface IFireAlarmPanelFollower {

}
