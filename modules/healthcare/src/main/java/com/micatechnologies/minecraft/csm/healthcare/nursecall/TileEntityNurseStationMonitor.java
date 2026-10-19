package com.micatechnologies.minecraft.csm.healthcare.nursecall;

/**
 * The nurses' station monitors' room lamps: which of their linked master panel's six rooms are
 * calling, one bit each ({@code rooms}, NBT byte), exactly as the panel's own
 * ({@link TileEntityNurseCallPanel}). It does not tick; the network sets it when one of the
 * panel's calls starts or ends, and the counter's baked model reads it through the actual
 * state.
 *
 * @since 2026.10
 */
public class TileEntityNurseStationMonitor extends TileEntityNurseCallPanel {

}
