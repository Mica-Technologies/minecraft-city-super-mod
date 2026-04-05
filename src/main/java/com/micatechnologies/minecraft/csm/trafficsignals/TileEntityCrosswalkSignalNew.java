package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkBulbType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkMountType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkVisorType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;

/**
 * Tile entity for the new custom-rendered crosswalk signals. Combines customization fields (body
 * color, visor color/type, mount type, body tilt) with the countdown learning system from
 * {@link TileEntityCrosswalkSignal}.
 */
public class TileEntityCrosswalkSignalNew extends AbstractTickableTileEntity {

    // region Customization Fields

    private TrafficSignalBodyColor bodyColor = TrafficSignalBodyColor.FLAT_BLACK;
    private TrafficSignalBodyColor visorColor = TrafficSignalBodyColor.FLAT_BLACK;
    private CrosswalkVisorType visorType = CrosswalkVisorType.NONE;
    private CrosswalkMountType mountType = CrosswalkMountType.BASE;
    private TrafficSignalBodyTilt bodyTilt = TrafficSignalBodyTilt.NONE;
    private CrosswalkBulbType bulbType = CrosswalkBulbType.WORDED;
    private boolean dirty = true;
    private boolean powerLossOff = true;

    // endregion

    // region Countdown Fields

    private static final int VERIFY_TOLERANCE_TICKS = 30;

    private int learnedClearanceTicks = -1;
    private int currentCountdown = -1;
    private int lastColorState = -1;
    private boolean measuring = false;
    private int measureTicks = 0;
    private boolean verifying = false;
    private int verifyTicks = 0;

    // endregion

    // region NBT Keys

    private static final String NBT_BODY_COLOR = "bodyColor";
    private static final String NBT_VISOR_COLOR = "visorColor";
    private static final String NBT_VISOR_TYPE = "visorType";
    private static final String NBT_MOUNT_TYPE = "mountType";
    private static final String NBT_BODY_TILT = "bodyTilt";
    private static final String NBT_BULB_TYPE = "bulbType";
    private static final String NBT_LEARNED_CLEARANCE_TICKS = "learnedClearanceTicks";
    private static final String NBT_CURRENT_COUNTDOWN = "currentCountdown";
    private static final String NBT_LAST_COLOR_STATE = "lastColorState";
    private static final String NBT_MEASURING = "measuring";
    private static final String NBT_MEASURE_TICKS = "measureTicks";
    private static final String NBT_VERIFYING = "verifying";
    private static final String NBT_VERIFY_TICKS = "verifyTicks";

    // endregion

    // region NBT Read/Write

    @Override
    public void readNBT( NBTTagCompound compound ) {
        bodyColor = TrafficSignalBodyColor.fromNBT( compound.getInteger( NBT_BODY_COLOR ) );
        visorColor = TrafficSignalBodyColor.fromNBT( compound.getInteger( NBT_VISOR_COLOR ) );
        visorType = CrosswalkVisorType.fromNBT( compound.getInteger( NBT_VISOR_TYPE ) );
        mountType = CrosswalkMountType.fromNBT( compound.getInteger( NBT_MOUNT_TYPE ) );
        bodyTilt = TrafficSignalBodyTilt.fromNBT( compound.getInteger( NBT_BODY_TILT ) );
        bulbType = CrosswalkBulbType.fromNBT( compound.getInteger( NBT_BULB_TYPE ) );
        learnedClearanceTicks = compound.getInteger( NBT_LEARNED_CLEARANCE_TICKS );
        currentCountdown = compound.getInteger( NBT_CURRENT_COUNTDOWN );
        lastColorState = compound.getInteger( NBT_LAST_COLOR_STATE );
        measuring = compound.getBoolean( NBT_MEASURING );
        measureTicks = compound.getInteger( NBT_MEASURE_TICKS );
        verifying = compound.getBoolean( NBT_VERIFYING );
        verifyTicks = compound.getInteger( NBT_VERIFY_TICKS );
        dirty = true;
    }

    @Override
    public NBTTagCompound writeNBT( NBTTagCompound compound ) {
        compound.setInteger( NBT_BODY_COLOR, bodyColor.toNBT() );
        compound.setInteger( NBT_VISOR_COLOR, visorColor.toNBT() );
        compound.setInteger( NBT_VISOR_TYPE, visorType.toNBT() );
        compound.setInteger( NBT_MOUNT_TYPE, mountType.toNBT() );
        compound.setInteger( NBT_BODY_TILT, bodyTilt.toNBT() );
        compound.setInteger( NBT_BULB_TYPE, bulbType.toNBT() );
        compound.setInteger( NBT_LEARNED_CLEARANCE_TICKS, learnedClearanceTicks );
        compound.setInteger( NBT_CURRENT_COUNTDOWN, currentCountdown );
        compound.setInteger( NBT_LAST_COLOR_STATE, lastColorState );
        compound.setBoolean( NBT_MEASURING, measuring );
        compound.setInteger( NBT_MEASURE_TICKS, measureTicks );
        compound.setBoolean( NBT_VERIFYING, verifying );
        compound.setInteger( NBT_VERIFY_TICKS, verifyTicks );
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

    // endregion

    // region Customization Getters

    public TrafficSignalBodyColor getBodyColor() {
        return bodyColor;
    }

    public TrafficSignalBodyColor getVisorColor() {
        return visorColor;
    }

    public CrosswalkVisorType getVisorType() {
        return visorType;
    }

    public CrosswalkMountType getMountType() {
        return mountType;
    }

    public TrafficSignalBodyTilt getBodyTilt() {
        return bodyTilt;
    }

    public CrosswalkBulbType getBulbType() {
        return bulbType;
    }

    // endregion

    // region Customization Setters (for migration)

    public void setBodyColor( TrafficSignalBodyColor color ) {
        this.bodyColor = color;
        dirty = true;
        if ( world != null && !world.isRemote ) {
            markDirtySync( world, pos, true );
        }
    }

    public void setVisorColor( TrafficSignalBodyColor color ) {
        this.visorColor = color;
        dirty = true;
        if ( world != null && !world.isRemote ) {
            markDirtySync( world, pos, true );
        }
    }

    public void setVisorType( CrosswalkVisorType type ) {
        this.visorType = type;
        dirty = true;
        if ( world != null && !world.isRemote ) {
            markDirtySync( world, pos, true );
        }
    }

    public void setMountType( CrosswalkMountType type ) {
        this.mountType = type;
        dirty = true;
        if ( world != null && !world.isRemote ) {
            markDirtySync( world, pos, true );
        }
    }

    public void setBodyTilt( TrafficSignalBodyTilt tilt ) {
        this.bodyTilt = tilt;
        dirty = true;
        if ( world != null && !world.isRemote ) {
            markDirtySync( world, pos, true );
        }
    }

    public void setBulbType( CrosswalkBulbType type ) {
        this.bulbType = type;
        dirty = true;
        if ( world != null && !world.isRemote ) {
            markDirtySync( world, pos, true );
        }
    }

    public void setLearnedClearanceTicks( int ticks ) {
        this.learnedClearanceTicks = ticks;
        if ( world != null && !world.isRemote ) {
            markDirtySync( world, pos, true );
        }
    }

    // endregion

    // region Customization Cycling (for config tool)

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

    public CrosswalkVisorType getNextVisorType() {
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

    public CrosswalkBulbType getNextBulbType() {
        bulbType = bulbType.getNextBulbType();
        dirty = true;
        if ( world != null && !world.isRemote ) {
            markDirtySync( world, pos, true );
        }
        return bulbType;
    }

    // endregion

    // region Dirty Flag (for renderer display list caching)

    public boolean isStateDirty() {
        return dirty;
    }

    public boolean isPowerLossOff() {
        return powerLossOff;
    }

    public void setPowerLossOff( boolean powerLossOff ) {
        this.powerLossOff = powerLossOff;
    }

    public void clearDirtyFlag() {
        dirty = false;
    }

    // endregion

    // region Countdown Logic (from TileEntityCrosswalkSignal)

    public int getCurrentCountdown() {
        return currentCountdown;
    }

    public boolean hasLearnedTiming() {
        return learnedClearanceTicks > 0;
    }

    @Override
    public boolean doClientTick() {
        return false;
    }

    @Override
    public boolean pauseTicking() {
        return false;
    }

    @Override
    public long getTickRate() {
        return 1;
    }

    @Override
    public void onTick() {
        if ( world == null || world.isRemote ) {
            return;
        }

        IBlockState state = world.getBlockState( pos );
        if ( !( state.getBlock() instanceof AbstractBlockControllableSignal ) ) {
            return;
        }

        int currentColor = state.getValue( AbstractBlockControllableSignal.COLOR );

        if ( currentColor != lastColorState ) {
            onColorChanged( lastColorState, currentColor );
            lastColorState = currentColor;
        }

        // Accumulate measurement ticks
        if ( measuring ) {
            measureTicks++;
        }
        if ( verifying ) {
            verifyTicks++;
        }

        // Decrement countdown every second (20 ticks)
        if ( currentCountdown > 0 && currentColor == AbstractBlockControllableSignal.SIGNAL_YELLOW ) {
            if ( world.getTotalWorldTime() % 20 == 0 ) {
                currentCountdown--;
                markDirtySync( world, pos, true );
            }
        }
    }

    private void onColorChanged( int oldColor, int newColor ) {
        // WALK -> CLEARANCE: start measuring or start countdown
        if ( oldColor == AbstractBlockControllableSignal.SIGNAL_GREEN &&
                newColor == AbstractBlockControllableSignal.SIGNAL_YELLOW ) {
            if ( learnedClearanceTicks <= 0 ) {
                // First cycle: start measuring
                measuring = true;
                measureTicks = 0;
                currentCountdown = -1;
            }
            else {
                // Subsequent cycles: start countdown and verify
                currentCountdown = learnedClearanceTicks / 20;
                verifying = true;
                verifyTicks = 0;
            }
            markDirtySync( world, pos, true );
        }
        // CLEARANCE -> DON'T WALK or OFF: finalize measurement/verification
        else if ( oldColor == AbstractBlockControllableSignal.SIGNAL_YELLOW &&
                ( newColor == AbstractBlockControllableSignal.SIGNAL_RED ||
                        newColor == AbstractBlockControllableSignal.SIGNAL_OFF ) ) {
            if ( measuring ) {
                // Round to nearest 10 ticks (half second)
                learnedClearanceTicks = Math.round( measureTicks / 10.0f ) * 10;
                measuring = false;
                measureTicks = 0;
            }
            if ( verifying ) {
                int roundedVerify = Math.round( verifyTicks / 10.0f ) * 10;
                if ( Math.abs( roundedVerify - learnedClearanceTicks ) > VERIFY_TOLERANCE_TICKS ) {
                    // Timing changed significantly — reset and re-learn
                    learnedClearanceTicks = -1;
                }
                verifying = false;
                verifyTicks = 0;
            }
            currentCountdown = -1;
            markDirtySync( world, pos, true );
        }
        // CLEARANCE -> WALK: pedestrian recycle (normal operation)
        else if ( oldColor == AbstractBlockControllableSignal.SIGNAL_YELLOW &&
                newColor == AbstractBlockControllableSignal.SIGNAL_GREEN ) {
            // Graceful stop — don't reset learned timing
            measuring = false;
            measureTicks = 0;
            verifying = false;
            verifyTicks = 0;
            currentCountdown = -1;
            markDirtySync( world, pos, true );
        }
        // Any other unexpected transition: clean up gracefully
        else {
            if ( measuring || verifying ) {
                measuring = false;
                measureTicks = 0;
                verifying = false;
                verifyTicks = 0;
                currentCountdown = -1;
                markDirtySync( world, pos, true );
            }
        }
    }

    // endregion
}
