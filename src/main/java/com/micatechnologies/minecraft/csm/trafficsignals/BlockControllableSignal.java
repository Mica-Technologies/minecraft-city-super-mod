package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.ICsmRetiringBlock;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbStyle;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalSectionInfo;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.block.material.Material;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Parameterized factory class for traffic signal head blocks. Replaces ~119 boilerplate
 * subclasses of {@link AbstractBlockControllableSignalHead} that only differ by configuration
 * data (registry name, signal side, section info, layout parameters, etc.).
 *
 * <p>All factory instances share the same tile entity class
 * ({@link TileEntityTrafficSignalHead}) and are functionally identical to the
 * original per-block subclasses.</p>
 *
 * <p>Use {@link Builder} to construct instances. When {@link Builder#retiring} is called, the
 * builder produces a package-private subclass that implements {@link ICsmRetiringBlock} so that
 * only blocks scheduled for replacement incur random-tick overhead.</p>
 *
 * @since 2026.4
 */
public class BlockControllableSignal extends AbstractBlockControllableSignalHead {

  final String registryName;
  final SIGNAL_SIDE signalSide;
  final boolean flash;
  final Supplier<TrafficSignalSectionInfo[]> sectionInfoSupplier;
  final boolean horizontal;
  @Nullable final float[] sectionYPositions;
  @Nullable final float[] sectionXPositions;
  @Nullable final int[] sectionSizes;
  final float signalYOffset;
  final boolean hasSignalYOffset;
  final boolean lightAllOnRedYellow;
  @Nullable final TrafficSignalBulbStyle enforcedBulbStyle;

  BlockControllableSignal(Builder builder) {
    super(Material.ROCK);
    this.registryName = builder.registryName;
    this.signalSide = builder.signalSide;
    this.flash = builder.flash;
    this.sectionInfoSupplier = builder.sectionInfoSupplier;
    this.horizontal = builder.horizontal;
    this.sectionYPositions = builder.sectionYPositions;
    this.sectionXPositions = builder.sectionXPositions;
    this.sectionSizes = builder.sectionSizes;
    this.signalYOffset = builder.signalYOffset;
    this.hasSignalYOffset = builder.hasSignalYOffset;
    this.lightAllOnRedYellow = builder.lightAllOnRedYellow;
    this.enforcedBulbStyle = builder.enforcedBulbStyle;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName;
  }

  @Override
  public SIGNAL_SIDE getSignalSide(World world, BlockPos blockPos) {
    return signalSide;
  }

  @Override
  public boolean doesFlash() {
    return flash;
  }

  @Override
  public TrafficSignalSectionInfo[] getDefaultTrafficSignalSectionInfo() {
    return sectionInfoSupplier.get();
  }

  @Override
  public boolean isHorizontal() {
    return horizontal;
  }

  @Override
  public float[] getSectionYPositions(int sectionCount) {
    return sectionYPositions != null ? sectionYPositions : super.getSectionYPositions(sectionCount);
  }

  @Override
  public float[] getSectionXPositions(int sectionCount) {
    return sectionXPositions != null ? sectionXPositions : super.getSectionXPositions(sectionCount);
  }

  @Override
  public int[] getSectionSizes(int sectionCount) {
    return sectionSizes != null ? sectionSizes : super.getSectionSizes(sectionCount);
  }

  @Override
  public float getSignalYOffset() {
    return hasSignalYOffset ? signalYOffset : super.getSignalYOffset();
  }

  @Override
  public boolean shouldLightAllSections(int colorState) {
    if (lightAllOnRedYellow) {
      return colorState == 0 || colorState == 1;
    }
    return super.shouldLightAllSections(colorState);
  }

  @Override
  public TrafficSignalBulbStyle getEnforcedBulbStyle() {
    return enforcedBulbStyle != null ? enforcedBulbStyle : super.getEnforcedBulbStyle();
  }

  // -- Package-private retiring subclass --

  /**
   * A {@link BlockControllableSignal} that implements {@link ICsmRetiringBlock} to schedule
   * automatic replacement when the world ticks the block.
   */
  static final class Retiring extends BlockControllableSignal implements ICsmRetiringBlock {

    private final String replacementBlockId;
    @Nullable private final TrafficSignalBodyTilt replacementBodyTilt;

    Retiring(Builder builder) {
      super(builder);
      this.replacementBlockId = builder.replacementBlockId;
      this.replacementBodyTilt = builder.replacementBodyTilt;
    }

    @Override
    public String getReplacementBlockId() {
      return replacementBlockId;
    }

    @Override
    public void configureReplacement(World world, BlockPos pos, NBTTagCompound oldTileEntityNBT) {
      ICsmRetiringBlock.super.configureReplacement(world, pos, oldTileEntityNBT);
      if (replacementBodyTilt != null) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityTrafficSignalHead) {
          ((TileEntityTrafficSignalHead) te).setBodyTilt(replacementBodyTilt);
        }
      }
    }
  }

  /**
   * Builder for constructing {@link BlockControllableSignal} instances.
   */
  public static class Builder {
    final String registryName;
    final SIGNAL_SIDE signalSide;
    final boolean flash;
    final Supplier<TrafficSignalSectionInfo[]> sectionInfoSupplier;
    boolean horizontal = false;
    float[] sectionYPositions = null;
    float[] sectionXPositions = null;
    int[] sectionSizes = null;
    float signalYOffset = 0.0f;
    boolean hasSignalYOffset = false;
    boolean lightAllOnRedYellow = false;
    TrafficSignalBulbStyle enforcedBulbStyle = null;
    String replacementBlockId = null;
    TrafficSignalBodyTilt replacementBodyTilt = null;

    public Builder(String registryName, SIGNAL_SIDE signalSide, boolean flash,
        Supplier<TrafficSignalSectionInfo[]> sectionInfoSupplier) {
      this.registryName = registryName;
      this.signalSide = signalSide;
      this.flash = flash;
      this.sectionInfoSupplier = sectionInfoSupplier;
    }

    public Builder horizontal(boolean horizontal) {
      this.horizontal = horizontal;
      return this;
    }

    public Builder sectionYPositions(float... positions) {
      this.sectionYPositions = positions;
      return this;
    }

    public Builder sectionXPositions(float... positions) {
      this.sectionXPositions = positions;
      return this;
    }

    public Builder sectionSizes(int... sizes) {
      this.sectionSizes = sizes;
      return this;
    }

    public Builder signalYOffset(float offset) {
      this.signalYOffset = offset;
      this.hasSignalYOffset = true;
      return this;
    }

    public Builder lightAllOnRedYellow(boolean lightAll) {
      this.lightAllOnRedYellow = lightAll;
      return this;
    }

    public Builder enforcedBulbStyle(TrafficSignalBulbStyle style) {
      this.enforcedBulbStyle = style;
      return this;
    }

    public Builder retiring(String replacementBlockId) {
      this.replacementBlockId = replacementBlockId;
      return this;
    }

    public Builder retiring(String replacementBlockId, TrafficSignalBodyTilt bodyTilt) {
      this.replacementBlockId = replacementBlockId;
      this.replacementBodyTilt = bodyTilt;
      return this;
    }

    public BlockControllableSignal build() {
      if (replacementBlockId != null) {
        return new Retiring(this);
      }
      return new BlockControllableSignal(this);
    }
  }
}
