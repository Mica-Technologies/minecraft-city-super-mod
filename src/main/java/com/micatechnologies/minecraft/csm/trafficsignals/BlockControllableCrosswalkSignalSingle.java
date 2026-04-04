package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableCrosswalkSignalNew;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkDisplayType;

/**
 * New custom-rendered crosswalk signal with hand/man symbol display. Consolidates all single-face
 * crosswalk mount variants into one block with configurable mount type, visor, and body color.
 */
public class BlockControllableCrosswalkSignalSingle
        extends AbstractBlockControllableCrosswalkSignalNew {

    @Override
    public String getBlockRegistryName() {
        return "controllablecrosswalksinglenew";
    }

    @Override
    public CrosswalkDisplayType getDisplayType() {
        return CrosswalkDisplayType.SYMBOL;
    }
}
