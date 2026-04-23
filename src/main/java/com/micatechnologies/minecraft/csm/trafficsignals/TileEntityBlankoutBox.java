package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.BlankoutBoxType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.BlankoutBoxVisorType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkMountType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.math.AxisAlignedBB;

public class TileEntityBlankoutBox extends AbstractTileEntity {

    private TrafficSignalBodyColor bodyColor = TrafficSignalBodyColor.FLAT_BLACK;
    private TrafficSignalBodyColor visorColor = TrafficSignalBodyColor.FLAT_BLACK;
    private BlankoutBoxVisorType visorType = BlankoutBoxVisorType.NONE;
    private CrosswalkMountType mountType = CrosswalkMountType.BASE;
    private TrafficSignalBodyTilt bodyTilt = TrafficSignalBodyTilt.NONE;
    private BlankoutBoxType blankoutType = BlankoutBoxType.DONT_WALK;
    private boolean dirty = true;

    // region NBT Keys

    private static final String NBT_BODY_COLOR = "bC";
    private static final String NBT_VISOR_COLOR = "vC";
    private static final String NBT_VISOR_TYPE = "vT";
    private static final String NBT_MOUNT_TYPE = "mT";
    private static final String NBT_BODY_TILT = "tlt";
    private static final String NBT_BLANKOUT_TYPE = "boT";

    // endregion

    // region NBT Read/Write

    @Override
    public void readNBT( NBTTagCompound compound ) {
        bodyColor = TrafficSignalBodyColor.fromNBT( readInt( compound, NBT_BODY_COLOR ) );
        visorColor = TrafficSignalBodyColor.fromNBT( readInt( compound, NBT_VISOR_COLOR ) );
        visorType = BlankoutBoxVisorType.fromNBT( readInt( compound, NBT_VISOR_TYPE ) );
        mountType = CrosswalkMountType.fromNBT( readInt( compound, NBT_MOUNT_TYPE ) );
        bodyTilt = TrafficSignalBodyTilt.fromNBT( readInt( compound, NBT_BODY_TILT ) );
        blankoutType = BlankoutBoxType.fromNBT( readInt( compound, NBT_BLANKOUT_TYPE ) );
        dirty = true;
    }

    private static int readInt( NBTTagCompound compound, String key ) {
        if ( compound.hasKey( key ) ) return compound.getInteger( key );
        return 0;
    }

    @Override
    public NBTTagCompound writeNBT( NBTTagCompound compound ) {
        compound.setInteger( NBT_BODY_COLOR, bodyColor.toNBT() );
        compound.setInteger( NBT_VISOR_COLOR, visorColor.toNBT() );
        compound.setInteger( NBT_VISOR_TYPE, visorType.toNBT() );
        compound.setInteger( NBT_MOUNT_TYPE, mountType.toNBT() );
        compound.setInteger( NBT_BODY_TILT, bodyTilt.toNBT() );
        compound.setInteger( NBT_BLANKOUT_TYPE, blankoutType.toNBT() );
        return compound;
    }

    // endregion

    // region Data Packet Handling

    @Override
    public void onDataPacket( NetworkManager networkManager, SPacketUpdateTileEntity pkt ) {
        super.onDataPacket( networkManager, pkt );
        dirty = true;
    }

    // endregion

    // region Render Distance

    @Override
    public double getMaxRenderDistanceSquared() {
        return 128.0 * 128.0;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(
            pos.getX() - 1.0, pos.getY() - 1.0, pos.getZ() - 1.0,
            pos.getX() + 2.0, pos.getY() + 2.0, pos.getZ() + 2.0 );
    }

    // endregion

    // region Getters

    public TrafficSignalBodyColor getBodyColor() {
        return bodyColor;
    }

    public TrafficSignalBodyColor getVisorColor() {
        return visorColor;
    }

    public BlankoutBoxVisorType getVisorType() {
        return visorType;
    }

    public CrosswalkMountType getMountType() {
        return mountType;
    }

    public TrafficSignalBodyTilt getBodyTilt() {
        return bodyTilt;
    }

    public BlankoutBoxType getBlankoutType() {
        return blankoutType;
    }

    // endregion

    // region Setters

    public void setMountType( CrosswalkMountType type ) {
        this.mountType = type;
        dirty = true;
        if ( world != null && !world.isRemote ) {
            markDirtySync( world, pos, true );
        }
    }

    // endregion

    // region Cycling (for config tool / GUI)

    public TrafficSignalBodyColor getNextBodyPaintColor() {
        bodyColor = bodyColor.getNextColor();
        dirty = true;
        if ( world != null && !world.isRemote ) {
            markDirtySync( world, pos, true );
        }
        return bodyColor;
    }

    public TrafficSignalBodyColor getNextVisorPaintColor() {
        visorColor = visorColor.getNextColor();
        dirty = true;
        if ( world != null && !world.isRemote ) {
            markDirtySync( world, pos, true );
        }
        return visorColor;
    }

    public BlankoutBoxVisorType getNextVisorType() {
        visorType = visorType.getNextVisorType();
        dirty = true;
        if ( world != null && !world.isRemote ) {
            markDirtySync( world, pos, true );
        }
        return visorType;
    }

    public CrosswalkMountType getNextMountType() {
        mountType = mountType.getNextMountType();
        dirty = true;
        if ( world != null && !world.isRemote ) {
            markDirtySync( world, pos, true );
        }
        return mountType;
    }

    public TrafficSignalBodyTilt getNextBodyTilt() {
        bodyTilt = bodyTilt.getNextTilt();
        dirty = true;
        if ( world != null && !world.isRemote ) {
            markDirtySync( world, pos, true );
        }
        return bodyTilt;
    }

    public BlankoutBoxType getNextBlankoutType() {
        blankoutType = blankoutType.getNextType();
        dirty = true;
        if ( world != null && !world.isRemote ) {
            markDirtySync( world, pos, true );
        }
        return blankoutType;
    }

    // endregion

    // region Dirty Flag

    public boolean isStateDirty() {
        return dirty;
    }

    public void clearDirtyFlag() {
        dirty = false;
    }

    // endregion
}
