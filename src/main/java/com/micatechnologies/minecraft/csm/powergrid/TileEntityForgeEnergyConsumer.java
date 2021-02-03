package com.micatechnologies.minecraft.csm.powergrid;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

@ElementsCitySuperMod.ModElement.Tag
public class TileEntityForgeEnergyConsumer extends TileEntity implements IEnergyStorage
{
    private static final String NBT_ENERGY_KEY  = "energy";
    private static final int    storedEnergyMax = 6;
    private              int    storedEnergy;

    @Override
    public void readFromNBT( NBTTagCompound p_readFromNBT_1_ ) {
        super.readFromNBT( p_readFromNBT_1_ );
        this.storedEnergy = p_readFromNBT_1_.getInteger( NBT_ENERGY_KEY );
    }

    @Override
    public NBTTagCompound writeToNBT( NBTTagCompound p_writeToNBT_1_ ) {
        p_writeToNBT_1_.setInteger( NBT_ENERGY_KEY, storedEnergy );
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
        int previousEnergy = storedEnergy;
        int expectedAdd = i + storedEnergy;
        storedEnergy = Math.min( expectedAdd, storedEnergyMax );
        return storedEnergy - previousEnergy;
    }

    @Override
    public int extractEnergy( int i, boolean b ) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return storedEnergy;
    }

    @Override
    public int getMaxEnergyStored() {
        return storedEnergyMax;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    public synchronized boolean consumeEnergy( int i ) {
        boolean consumed = false;
        if ( storedEnergy >= i ) {
            storedEnergy -= i;
            consumed = true;
        }
        return consumed;
    }
}
