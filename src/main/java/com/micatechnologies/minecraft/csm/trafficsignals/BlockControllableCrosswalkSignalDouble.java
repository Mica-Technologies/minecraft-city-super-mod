package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableCrosswalkSignalNew;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkDisplayType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * New custom-rendered crosswalk signal with "DON'T WALK"/"WALK" text display. Consolidates all
 * double-worded crosswalk mount variants into one block with configurable mount type, visor, and
 * body color.
 */
public class BlockControllableCrosswalkSignalDouble
        extends AbstractBlockControllableCrosswalkSignalNew {

    @Override
    public String getBlockRegistryName() {
        return "controllablecrosswalkdoublenew";
    }

    @Override
    public CrosswalkDisplayType getDisplayType() {
        return CrosswalkDisplayType.TEXT;
    }

    @Override
    public AxisAlignedBB getBlockBoundingBox( IBlockState state, IBlockAccess source,
            BlockPos pos ) {
        // Taller body for double-worded text display
        return new AxisAlignedBB( 0.125, -0.1875, 0.0, 0.875, 1.5625, 0.6875 );
    }
}
