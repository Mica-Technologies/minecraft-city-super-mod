package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalTickableRequester;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.ParametersAreNonnullByDefault;

public abstract class AbstractBlockTrafficSignalTickableRequester extends AbstractBlockTrafficSignalRequester
{

    public AbstractBlockTrafficSignalTickableRequester( Material p_i45394_1_ ) {
        super( p_i45394_1_ );
    }

    /**
     * Creates a new tile entity instance for this block.
     *
     * @param world The world in which the tile entity will be created.
     * @param i     The metadata value of the block.
     *
     * @return A new tile entity instance.
     *
     * @see TileEntity
     * @see TileEntityTrafficSignalTickableRequester
     */
    @Override
    @ParametersAreNonnullByDefault
    public abstract TileEntity createNewTileEntity( World world, int i );
}
