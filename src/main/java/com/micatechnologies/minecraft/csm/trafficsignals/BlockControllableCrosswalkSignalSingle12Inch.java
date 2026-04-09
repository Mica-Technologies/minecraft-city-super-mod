package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableCrosswalkSignalNew;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkDisplayType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * New custom-rendered single-section 12-inch crosswalk signal with bimodal hand/man LED display.
 * Uses the narrower 12-inch housing from the stacked signal but as a single section without
 * countdown.
 */
public class BlockControllableCrosswalkSignalSingle12Inch
        extends AbstractBlockControllableCrosswalkSignalNew {

    @Override
    public String getBlockRegistryName() {
        return "controllablecrosswalksingle12inch";
    }

    @Override
    public CrosswalkDisplayType getDisplayType() {
        return CrosswalkDisplayType.SYMBOL_12INCH;
    }

    @Override
    public AxisAlignedBB getBlockBoundingBox( IBlockState state, IBlockAccess source,
            BlockPos pos ) {
        // 12-inch single section: X=2-14 (0.125-0.875), Y=0-12 (0.0-0.75), Z=4-10 (0.25-0.625)
        return new AxisAlignedBB( 0.125, 0.0, 0.0, 0.875, 0.75, 0.625 );
    }
}
