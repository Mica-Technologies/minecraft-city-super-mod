package com.micatechnologies.minecraft.csm.powergrid.fe;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class TileEntityForgeEnergyProducer extends AbstractTickableTileEntity implements IEnergyStorage
{
    private static final String NBT_TICK_RATE_KEY = "tickRate";
    private              int    tickRate          = 1;

    /**
     * Abstract method which must be implemented to return the NBT tag compound with the tile entity's NBT data.
     *
     * @param compound the NBT tag compound to write the tile entity's NBT data to
     *
     * @return the NBT tag compound with the tile entity's NBT data
     */
    @Override
    public NBTTagCompound writeNBT( NBTTagCompound compound ) {
        compound.setInteger( NBT_TICK_RATE_KEY, tickRate );
        return compound;
    }

    /**
     * Abstract method which must be implemented to process the reading of the tile entity's NBT data from the supplied
     * NBT tag compound.
     *
     * @param compound the NBT tag compound to read the tile entity's NBT data from
     */
    @Override
    public void readNBT( NBTTagCompound compound ) {
        if ( compound.hasKey( NBT_TICK_RATE_KEY ) ) {
            tickRate = compound.getInteger( NBT_TICK_RATE_KEY );
        }
        else {
            tickRate = 1;
        }
    }

    @Override
    public boolean hasCapability( Capability< ? > p_hasCapability_1_, @Nullable EnumFacing p_hasCapability_2_ )
    {
        return p_hasCapability_1_ == CapabilityEnergy.ENERGY ||
                super.hasCapability( p_hasCapability_1_, p_hasCapability_2_ );
    }

    @Nullable
    @Override
    public < T > T getCapability( Capability< T > p_getCapability_1_, @Nullable EnumFacing p_getCapability_2_ )
    {
        return p_getCapability_1_ == CapabilityEnergy.ENERGY ?
               CapabilityEnergy.ENERGY.cast( this ) :
               super.getCapability( p_getCapability_1_, p_getCapability_2_ );
    }

    @Override
    public synchronized int receiveEnergy( int i, boolean b ) {
        return 0;
    }

    @Override
    public int extractEnergy( int i, boolean b ) {
        return i;
    }

    @Override
    public int getEnergyStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMaxEnergyStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return false;
    }

    public int incrementTickRate() {
        if ( tickRate == 1 ) {
            tickRate += 9;
        }
        else {
            tickRate += 10;
        }
        if ( tickRate > 200 ) {
            tickRate = 1;
        }
        markDirty();
        return tickRate;
    }

    /**
     * Abstract method which must be implemented to return the tick rate of the tile entity.
     *
     * @return the tick rate of the tile entity
     */
    @Override
    public long getTickRate() {
        return tickRate;
    }

    /**
     * Abstract method which must be implemented to handle the tick event of the tile entity.
     */
    @Override
    public void onTick() {
        try {
            // Only provide power if redstone enabled
            if ( getWorld().isBlockPowered( pos ) ) {
                for ( EnumFacing facing : EnumFacing.values() ) {
                    BlockPos offset = pos.offset( facing );
                    TileEntity tileEntity = world.getTileEntity( offset );
                    if ( tileEntity != null ) {
                        if ( tileEntity.hasCapability( CapabilityEnergy.ENERGY, facing.getOpposite() ) ) {
                            IEnergyStorage energyStorage = tileEntity.getCapability( CapabilityEnergy.ENERGY,
                                                                                     facing.getOpposite() );
                            if ( energyStorage != null ) {
                                energyStorage.receiveEnergy( Integer.MAX_VALUE, false );
                            }
                        }
                    }
                }
            }
        }
        catch ( Exception e ) {
            System.err.println( "An error occurred while ticking a Forge energy infinite producer: " );
            e.printStackTrace( System.err );
        }
    }

    /**
     * Abstract method which must be implemented to return a boolean indicating if the tile entity should also tick on
     * the client side. By default, the tile entity will always tick on the server side, and in the event of
     * singleplayer/local mode, the host client is considered the server.
     *
     * @return a boolean indicating if the tile entity should also tick on the client side
     */
    @Override
    public boolean doClientTick() {
        return false;
    }

    /**
     * Abstract method which must be implemented to return a boolean indicating if the tile entity ticking should be
     * paused. If the tile entity is paused, the tick event will not be called.
     *
     * @return a boolean indicating if the tile entity ticking should be paused
     */
    @Override
    public boolean pauseTicking() {
        return false;
    }
}
