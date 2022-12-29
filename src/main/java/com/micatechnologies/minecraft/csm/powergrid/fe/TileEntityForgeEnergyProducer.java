package com.micatechnologies.minecraft.csm.powergrid.fe;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
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
        return i;
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
        return true;
    }

    @Override
    public void update() {
        if ( !world.isRemote ) {
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
}
