package com.micatechnologies.minecraft.csm.trafficsignals;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;

public class AbstractBlockControllableCrosswalkAccessory extends AbstractBlockControllableSignal
{
    public AbstractBlockControllableCrosswalkAccessory( Material p_i46399_1_, MapColor p_i46399_2_ ) {
        super( p_i46399_1_, p_i46399_2_ );
    }

    public AbstractBlockControllableCrosswalkAccessory( Material p_i45394_1_ ) {
        super( p_i45394_1_ );
    }

    @Override
    public SIGNAL_SIDE getSignalSide() {
        return SIGNAL_SIDE.CROSSWALK;
    }

}