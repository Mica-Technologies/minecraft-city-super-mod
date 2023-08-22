package com.micatechnologies.minecraft.csm.powergrid.fe;

import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class TileEntityForgeEnergyConsumer extends TileEntity implements IEnergyStorage, ITickable
{

    private static final int    energyConsume   = 6;
    private static final int    tickRate        = 40;
    private static final String NBT_ENERGY_KEY  = "energy";
    private static final int    storedEnergyMax = 24;
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
        // Receive energy from the grid (up to max capacity)
        int energyReceived = Math.min( storedEnergyMax - storedEnergy, i );
        storedEnergy += energyReceived;
        return energyReceived;
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
            markDirty();
        }
        return consumed;
    }

    @Override
    public void update() {
        // This is called every tick, need to check if it is time to act
        World world = getWorld();
        if ( !world.isRemote && world.getTotalWorldTime() % tickRate == 0L ) {
            try {
                // Get block and world information
                BlockPos blockPos = getPos();
                IBlockState blockState = world.getBlockState( blockPos );

                // Consume power from Forge Energy (true if power, false if none)
                boolean isGridPowered = consumeEnergy( energyConsume );

                // Check if block is already outputting power
                PropertyBool blockPoweredProperty = BlockForgeEnergyToRedstone.POWERED;
                boolean isBlockPowered = blockState.getValue( blockPoweredProperty );

                // Update block power state if grid power does not match
                if ( isBlockPowered != isGridPowered ) {
                    world.setBlockState( blockPos, blockState.withProperty( blockPoweredProperty, isGridPowered ), 3 );
                }
            }
            catch ( Exception e ) {
                System.err.println( "An error occurred while ticking a Forge energy to redstone converter: " );
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
