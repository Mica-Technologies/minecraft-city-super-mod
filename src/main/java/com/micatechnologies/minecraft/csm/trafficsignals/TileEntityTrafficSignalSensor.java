package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * The {@link TileEntityTrafficSignalSensor} is a {@link TileEntity} which is paired with an
 * {@link AbstractBlockTrafficSignalSensor} implementation. The {@link TileEntityTrafficSignalSensor} is used to detect
 * entities within a specified region (defined by two corner positions) and report the number of entities detected to
 * the paired {@link TileEntityTrafficSignalController}.
 *
 * @version 2.0
 * @see AbstractBlockTrafficSignalSensor
 * @see TileEntityTrafficSignalController
 * @see TileEntity
 * @since 2023.2.0
 */
@ElementsCitySuperMod.ModElement.Tag
public class TileEntityTrafficSignalSensor extends AbstractTileEntity
{
    /**
     * The key for storing and retrieving the {@link TileEntityTrafficSignalSensor}'s first scan corner from NBT data.
     *
     * @since 1.0
     */
    private static final String SCAN_CORNER_1_KEY = "blockPos1";

    /**
     * The key for storing and retrieving the {@link TileEntityTrafficSignalSensor}'s second scan corner from NBT data.
     *
     * @since 1.0
     */
    private static final String SCAN_CORNER_2_KEY = "blockPos2";

    /**
     * The {@link TileEntityTrafficSignalSensor}'s first scan corner.
     *
     * @since 1.0
     */
    private BlockPos scanCorner1;

    /**
     * The {@link TileEntityTrafficSignalSensor}'s second scan corner.
     *
     * @since 1.0
     */
    private BlockPos scanCorner2;

    /**
     * Returns the specified NBT tag compound with the {@link TileEntityTrafficSignalSensor}'s NBT data.
     *
     * @param compound the NBT tag compound to write the {@link TileEntityTrafficSignalSensor}'s NBT data to
     *
     * @return the NBT tag compound with the {@link TileEntityTrafficSignalSensor}'s NBT data
     *
     * @since 2.0
     */
    @Override
    public NBTTagCompound writeNBT( NBTTagCompound compound ) {

        // Write the first corner to NBT
        if ( scanCorner1 != null ) {
            compound.setLong( SCAN_CORNER_1_KEY, scanCorner1.toLong() );
        }
        else {
            compound.removeTag( SCAN_CORNER_1_KEY );
        }

        // Write the second corner to NBT
        if ( scanCorner2 != null ) {
            compound.setLong( SCAN_CORNER_2_KEY, scanCorner2.toLong() );
        }
        else {
            compound.removeTag( SCAN_CORNER_2_KEY );
        }

        // Return the NBT tag compound
        return compound;
    }

    /**
     * Processes the reading of the {@link TileEntityTrafficSignalSensor}'s NBT data from the supplied NBT tag
     * compound.
     *
     * @param compound the NBT tag compound to read the {@link TileEntityTrafficSignalSensor}'s NBT data from
     *
     * @since 2.0
     */
    @Override
    public void readNBT( NBTTagCompound compound ) {

        // Read the first corner from NBT
        if ( compound.hasKey( SCAN_CORNER_1_KEY ) ) {
            scanCorner1 = BlockPos.fromLong( compound.getLong( SCAN_CORNER_1_KEY ) );
        }
        else {
            scanCorner1 = null;
        }

        // Read the second corner from NBT
        if ( compound.hasKey( SCAN_CORNER_2_KEY ) ) {
            scanCorner2 = BlockPos.fromLong( compound.getLong( SCAN_CORNER_2_KEY ) );
        }
        else {
            scanCorner2 = null;
        }
    }

    /**
     * Sets the two corners of the {@link TileEntityTrafficSignalSensor}'s scan region.
     *
     * @param blockPos1 the first corner of the {@link TileEntityTrafficSignalSensor}'s scan region
     * @param blockPos2 the second corner of the {@link TileEntityTrafficSignalSensor}'s scan region
     *
     * @return true if previously set corners were overwritten, false otherwise
     *
     * @since 1.0
     */
    public boolean setScanCorners( BlockPos blockPos1, BlockPos blockPos2 ) {
        boolean overwroteExisting = scanCorner1 != null && scanCorner2 != null;
        scanCorner1 = blockPos1;
        scanCorner2 = blockPos2;
        markDirtySync( getWorld(), getPos() );
        return overwroteExisting;
    }

    /**
     * Scans for eligible entities within the {@link TileEntityTrafficSignalSensor}'s scan region and returns the number
     * of entities found. Eligible entities are {@link EntityVillager} and {@link EntityPlayer}.
     *
     * @return the number of eligible entities found within the {@link TileEntityTrafficSignalSensor}'s scan region
     *
     * @since 1.0
     */
    public int scanEntities() {
        int count = 0;
        if ( world != null && scanCorner1 != null && scanCorner2 != null ) {
            AxisAlignedBB scanRange = new AxisAlignedBB( scanCorner1, scanCorner2 );
            List< Entity > entitiesWithinAABBExcludingEntity = world.getEntitiesWithinAABBExcludingEntity( null,
                                                                                                           scanRange );
            for ( Entity entity : entitiesWithinAABBExcludingEntity ) {
                if ( entity instanceof EntityVillager || entity instanceof EntityPlayer ) {
                    count++;
                }
            }
        }
        return count;
    }
}
