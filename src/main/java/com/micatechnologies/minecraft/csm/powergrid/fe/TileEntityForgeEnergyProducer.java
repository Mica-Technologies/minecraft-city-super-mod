package com.micatechnologies.minecraft.csm.powergrid.fe;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

@ElementsCitySuperMod.ModElement.Tag
public class TileEntityForgeEnergyProducer extends TileEntity implements IEnergyStorage, ITickable
{
    private static final String NBT_TICK_RATE_KEY = "tickRate";
    private              int    tickRate          = 1;

    @Override
    public void readFromNBT( NBTTagCompound p_readFromNBT_1_ ) {
        super.readFromNBT( p_readFromNBT_1_ );
        if ( p_readFromNBT_1_.hasKey( NBT_TICK_RATE_KEY ) ) {
            tickRate = p_readFromNBT_1_.getInteger( NBT_TICK_RATE_KEY );
        }
        else {
            tickRate = 1;
        }
    }

    @Override
    public NBTTagCompound writeToNBT( NBTTagCompound p_writeToNBT_1_ ) {
        p_writeToNBT_1_.setInteger( NBT_TICK_RATE_KEY, tickRate );
        return super.writeToNBT( p_writeToNBT_1_ );
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

    @Override
    public void update() {
        // This is called every tick, need to check if it is time to act
        if ( !getWorld().isRemote && getWorld().getTotalWorldTime() % tickRate == 0L ) {
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
    }

    @Override
    @Nullable
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        writeToNBT( nbtTagCompound );
        int metadata = getBlockMetadata();
        return new SPacketUpdateTileEntity( this.pos, metadata, nbtTagCompound );
    }

    @Override
    public void onDataPacket( NetworkManager networkManager, SPacketUpdateTileEntity pkt ) {
        readFromNBT( pkt.getNbtCompound() );
    }

    @Override
    public NBTTagCompound getUpdateTag()
    {
        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        writeToNBT( nbtTagCompound );
        return nbtTagCompound;
    }

    @Override
    public void handleUpdateTag( NBTTagCompound nbtTagCompound )
    {
        this.readFromNBT( nbtTagCompound );
    }
}
