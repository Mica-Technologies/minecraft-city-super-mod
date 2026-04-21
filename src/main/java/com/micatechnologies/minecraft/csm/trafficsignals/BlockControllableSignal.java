package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.ICsmRetiringBlock;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbStyle;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalSectionInfo;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
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
  final boolean addon;
  final boolean allowsHorizontalFlip;

  /**
   * ThreadLocal used to pass the registry name to the superclass constructor. The AbstractBlock
   * constructor calls getBlockRegistryName() before subclass fields are initialized, so we
   * store the name here before calling super() and read it in getBlockRegistryName().
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  BlockControllableSignal(Builder builder) {
    super(initRegistryName(builder));
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
    this.addon = builder.addon;
    // Static-horizontal and add-on signals don't offer the flip toggle: the former are
    // already horizontal with nothing to change, and the latter derive horizontal from
    // their adjacent main signal. Otherwise honor the builder's explicit choice.
    this.allowsHorizontalFlip = builder.allowsHorizontalFlip && !builder.horizontal
        && !builder.addon;
  }

  private static Material initRegistryName(Builder builder) {
    PENDING_REGISTRY_NAME.set(builder.registryName);
    return Material.ROCK;
  }

  @Override
  public String getBlockRegistryName() {
    // During super() construction, registryName is still null — read from ThreadLocal
    if (registryName != null) {
      return registryName;
    }
    return PENDING_REGISTRY_NAME.get();
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

  // -- World-aware layout overrides for add-on + TE-flip horizontal detection --

  /**
   * Returns true when this block's configured horizontal orientation (static-horizontal
   * flag or player-set TE flip) is active at {@code pos}. This deliberately excludes the
   * add-on's scan-neighbours-for-horizontal-main path so it can be safely called from
   * inside that scan without recursing.
   */
  private boolean isConfiguredHorizontal(IBlockAccess world, BlockPos pos) {
    if (horizontal) return true;
    if (!allowsHorizontalFlip) return false;
    TileEntity te = world.getTileEntity(pos);
    return te instanceof TileEntityTrafficSignalHead
        && ((TileEntityTrafficSignalHead) te).isHorizontalFlip();
  }

  /**
   * Checks whether the given block + world position reports horizontal orientation via its
   * static flag OR via a player-set flip on its TE. Safe to call on any adjacent signal
   * head — never triggers neighbour-scan recursion.
   */
  private static boolean neighbourIsHorizontal(
      AbstractBlockControllableSignalHead neighbourBlock,
      IBlockAccess world, BlockPos pos) {
    if (neighbourBlock.isHorizontal()) return true;
    if (neighbourBlock instanceof BlockControllableSignal
        && ((BlockControllableSignal) neighbourBlock).isConfiguredHorizontal(world, pos)) {
      return true;
    }
    return false;
  }

  /**
   * For add-on signals, detects whether an adjacent signal is horizontal by scanning
   * along the facing axis and vertically. Returns the static value for non-addon blocks.
   */
  private boolean detectAdjacentHorizontal(IBlockAccess world, BlockPos pos) {
    if (!addon) return false;
    IBlockState state = world.getBlockState(pos);
    if (!state.getProperties().containsKey(FACING)) return false;
    EnumFacing facing = state.getValue(FACING);

    // Scan along facing axis (forward/back) for the primary signal
    for (EnumFacing dir : new EnumFacing[]{facing, facing.getOpposite()}) {
      BlockPos checkPos = pos.offset(dir);
      IBlockState checkState = world.getBlockState(checkPos);
      if (checkState.getBlock() instanceof AbstractBlockControllableSignalHead) {
        return neighbourIsHorizontal(
            (AbstractBlockControllableSignalHead) checkState.getBlock(), world, checkPos);
      }
    }

    // Scan vertically (the primary signal may be above if this is below it)
    for (int dy = 1; dy <= 3; dy++) {
      for (BlockPos checkPos : new BlockPos[]{pos.up(dy), pos.down(dy)}) {
        IBlockState checkState = world.getBlockState(checkPos);
        if (checkState.getBlock() instanceof AbstractBlockControllableSignalHead) {
          return neighbourIsHorizontal(
              (AbstractBlockControllableSignalHead) checkState.getBlock(), world, checkPos);
        }
      }
    }

    // Check left/right (horizontal add-ons are placed beside the main signal,
    // up to 3 blocks away for double add-ons with gaps)
    IBlockState myState = world.getBlockState(pos);
    if (myState.getProperties().containsKey(FACING)) {
      EnumFacing myFacing = myState.getValue(FACING);
      for (EnumFacing side : new EnumFacing[]{myFacing.rotateY(), myFacing.rotateYCCW()}) {
        for (int dist = 1; dist <= 3; dist++) {
          BlockPos checkPos = pos.offset(side, dist);
          IBlockState checkState = world.getBlockState(checkPos);
          if (checkState.getBlock() instanceof AbstractBlockControllableSignalHead) {
            return neighbourIsHorizontal(
                (AbstractBlockControllableSignalHead) checkState.getBlock(), world, checkPos);
          }
        }
      }
    }

    return false;
  }

  @Override
  public boolean allowsHorizontalFlip() {
    return allowsHorizontalFlip;
  }

  @Override
  public boolean isHorizontal(IBlockAccess world, BlockPos pos) {
    if (isConfiguredHorizontal(world, pos)) return true;
    if (addon && detectAdjacentHorizontal(world, pos)) return true;
    return false;
  }

  /**
   * True when this signal needs its vertical-authored positions swapped into horizontal
   * layout at render time. Statically-horizontal blocks authored their positions in the
   * horizontal layout already, so they don't need the swap even though {@code isHorizontal}
   * is true.
   */
  private boolean needsLayoutSwap(IBlockAccess world, BlockPos pos) {
    if (horizontal) return false; // already authored horizontally
    return isHorizontal(world, pos);
  }

  @Override
  public float[] getSectionYPositions(int sectionCount, IBlockAccess world, BlockPos pos) {
    if (needsLayoutSwap(world, pos)) {
      // In horizontal mode, all sections are at the same Y (no vertical stacking)
      return new float[sectionCount];
    }
    return getSectionYPositions(sectionCount);
  }

  @Override
  public float[] getSectionXPositions(int sectionCount, IBlockAccess world, BlockPos pos) {
    if (needsLayoutSwap(world, pos)) {
      // Swap: original Y positions become X positions, with signalYOffset folded in
      float[] origY = getSectionYPositions(sectionCount);
      float yOff = getSignalYOffset();
      float[] xPositions = new float[sectionCount];
      for (int i = 0; i < sectionCount; i++) {
        xPositions[i] = origY[i] + yOff;
      }
      return xPositions;
    }
    return getSectionXPositions(sectionCount);
  }

  @Override
  public float getSignalYOffset(IBlockAccess world, BlockPos pos) {
    if (needsLayoutSwap(world, pos)) {
      // Y offset folded into X positions in horizontal mode
      return 0.0f;
    }
    return getSignalYOffset();
  }

  @Override
  public int[] getTiltPivotOffset(IBlockAccess world, BlockPos pos) {
    if (!addon || !detectAdjacentHorizontal(world, pos)) {
      return super.getTiltPivotOffset(world, pos);
    }
    return findMainSignalOffset(world, pos);
  }

  /**
   * Finds the main (non-addon) horizontal signal near this add-on and returns the
   * block offset from this position to that signal. Scans laterally up to 3 blocks
   * (for double add-ons with gaps) and along the facing axis.
   */
  private int[] findMainSignalOffset(IBlockAccess world, BlockPos pos) {
    IBlockState state = world.getBlockState(pos);
    if (!state.getProperties().containsKey(FACING)) return new int[]{0, 0, 0};
    EnumFacing facing = state.getValue(FACING);

    // Check left/right (perpendicular to facing), up to 3 blocks
    for (EnumFacing side : new EnumFacing[]{facing.rotateY(), facing.rotateYCCW()}) {
      for (int dist = 1; dist <= 3; dist++) {
        BlockPos checkPos = pos.offset(side, dist);
        if (isMainHorizontalSignal(world, checkPos)) {
          return new int[]{
              checkPos.getX() - pos.getX(),
              checkPos.getY() - pos.getY(),
              checkPos.getZ() - pos.getZ()};
        }
      }
    }

    // Check along facing axis
    for (EnumFacing dir : new EnumFacing[]{facing, facing.getOpposite()}) {
      BlockPos checkPos = pos.offset(dir);
      if (isMainHorizontalSignal(world, checkPos)) {
        return new int[]{
            checkPos.getX() - pos.getX(),
            checkPos.getY() - pos.getY(),
            checkPos.getZ() - pos.getZ()};
      }
    }

    return new int[]{0, 0, 0};
  }

  private boolean isMainHorizontalSignal(IBlockAccess world, BlockPos pos) {
    IBlockState state = world.getBlockState(pos);
    if (!(state.getBlock() instanceof AbstractBlockControllableSignalHead)) return false;
    AbstractBlockControllableSignalHead block =
        (AbstractBlockControllableSignalHead) state.getBlock();
    // Must be horizontal and NOT an addon itself
    return block.isHorizontal()
        && !(block instanceof BlockControllableSignal && ((BlockControllableSignal) block).addon);
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
    boolean addon = false;
    boolean allowsHorizontalFlip = true;
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

    /**
     * Marks this signal as an add-on. Add-on signals automatically detect adjacent
     * horizontal signals and adapt their layout (rotating body 90° and swapping
     * Y positions to X positions).
     */
    public Builder addon(boolean addon) {
      this.addon = addon;
      return this;
    }

    /**
     * Opts this signal out of the player-facing horizontal-flip toggle in the signal head
     * configuration GUI. Default is {@code true} (toggle available). Specialty layouts whose
     * geometry doesn't round-trip through a 90° rotation cleanly (doghouse, etc.) should
     * pass {@code false}. Static-horizontal and add-on signals auto-opt-out regardless.
     */
    public Builder allowsHorizontalFlip(boolean allowed) {
      this.allowsHorizontalFlip = allowed;
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
