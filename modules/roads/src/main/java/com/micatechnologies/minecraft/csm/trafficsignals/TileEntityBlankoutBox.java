package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.BlankoutBoxType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.BlankoutBoxVisorType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkMountType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficLightMountKit;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
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
        // A sync also arrives when the block behind changed (see the block's neighborChanged), so
        // the renderer's cached look behind is out of date.
        invalidateBehindCache();
        long appearanceBefore = appearanceKey();
        bodyColor = TrafficSignalBodyColor.fromNBT( readInt( compound, NBT_BODY_COLOR ) );
        visorColor = TrafficSignalBodyColor.fromNBT( readInt( compound, NBT_VISOR_COLOR ) );
        visorType = BlankoutBoxVisorType.fromNBT( readInt( compound, NBT_VISOR_TYPE ) );
        mountType = CrosswalkMountType.fromNBT( readInt( compound, NBT_MOUNT_TYPE ) );
        bodyTilt = TrafficSignalBodyTilt.fromNBT( readInt( compound, NBT_BODY_TILT ) );
        blankoutType = BlankoutBoxType.fromNBT( readInt( compound, NBT_BLANKOUT_TYPE ) );
        // Only the housing is compiled into the renderer's display list; the message
        // (blankoutType) is drawn live on top of it, so changing it must not throw the list away.
        if ( appearanceKey() != appearanceBefore ) {
            dirty = true;
        }
    }

    /** Packs every field the compiled housing depends on, so a change of any of them is seen. */
    private long appearanceKey() {
        return ordinalOf( bodyColor )
            | ( ordinalOf( visorColor ) << 8 )
            | ( ordinalOf( visorType ) << 16 )
            | ( ordinalOf( mountType ) << 24 )
            | ( ordinalOf( bodyTilt ) << 32 );
    }

    private static long ordinalOf( Enum<?> value ) {
        return value == null ? 0xFFL : value.ordinal() & 0xFFL;
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
        // readNBT sets the dirty flag when, and only when, the compiled housing changed.
        super.onDataPacket( networkManager, pkt );
    }

    // endregion

    // region Render Distance

    @Override
    public double getMaxRenderDistanceSquared() {
        return LONG_RANGE_RENDER_DISTANCE_SQUARED;
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

    // region Mount Kit Behind

    /** How long {@link #isBehindMountKit} trusts its last look, in ticks. */
    private static final long BEHIND_RECHECK_TICKS = 20L;

    private long behindCheckedAt = Long.MIN_VALUE;

    /** The facing {@link #behindMountKit} was read for; the cell behind moves with it. */
    private EnumFacing behindCheckedFacing;

    private boolean behindMountKit;

    /**
     * Whether a traffic light mount kit is directly behind this box, which the renderer asks every
     * frame for a {@code BASE} mount (the body is pushed back onto the kit).
     *
     * <p>Cached, because the render rules forbid reading the world per frame. The client never
     * hears {@code neighborChanged}, so the server's copy syncs this tile entity when the cell
     * behind changes and {@link #readNBT} drops the cache; the expiry after
     * {@link #BEHIND_RECHECK_TICKS} is the backstop for what no sync covers -- the cell behind
     * sitting in a chunk the client had not loaded yet when this box was first drawn, or the sync
     * overtaking the block change it announces.</p>
     *
     * @param facing the way this box faces
     *
     * @return {@code true} if the cell behind holds a mount kit
     */
    public boolean isBehindMountKit( EnumFacing facing ) {
        long now = world != null ? world.getTotalWorldTime() : 0L;
        if ( behindCheckedAt == Long.MIN_VALUE || facing != behindCheckedFacing
                || now < behindCheckedAt || now - behindCheckedAt >= BEHIND_RECHECK_TICKS ) {
            behindMountKit = world != null
                    && world.getBlockState( pos.offset( facing.getOpposite() ) ).getBlock()
                    instanceof BlockTrafficLightMountKit;
            behindCheckedFacing = facing;
            behindCheckedAt = now;
        }
        return behindMountKit;
    }

    /** Makes the next {@link #isBehindMountKit} look at the world again. */
    public void invalidateBehindCache() {
        behindCheckedAt = Long.MIN_VALUE;
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


  /**
   * Releases this position's cached display list when the tile entity goes away -- block broken,
   * block replaced, or the tile entity otherwise invalidated.
   *
   * <p>Without this (and {@link #onChunkUnload()}), the renderer's cache kept the entry and its
   * OpenGL list handle for the rest of the session: the handle holds driver and GPU memory, not
   * just heap, so touring a large city accumulated one per block ever rendered.</p>
   */
  @Override
  public void invalidate() {
    super.invalidate();
    if (world != null && world.isRemote) {
      TileEntityBlankoutBoxRenderer.cleanupDisplayList(pos);
    }
  }

  /**
   * Releases this position's cached display list when the chunk unloads. This is the common case
   * -- a player walking away from a block, rather than breaking it.
   */
  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    if (world != null && world.isRemote) {
      TileEntityBlankoutBoxRenderer.cleanupDisplayList(pos);
    }
  }

    /**
     * No baked model reads this tile entity -- only its special renderer, which reads it every
     * frame -- so a sync never needs the chunk section rebuilt. That includes the sync the block
     * sends when the cell behind changes: the mount kit look it refreshes is the renderer's alone.
     */
    @Override
    protected long getBakedModelKey() {
        return 0L;
    }
}
