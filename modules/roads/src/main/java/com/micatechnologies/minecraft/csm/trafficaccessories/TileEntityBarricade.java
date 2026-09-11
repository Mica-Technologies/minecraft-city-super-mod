package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * What a barricade is carrying: a mounted sign, and warning lights on either end.
 *
 * <p>The sign is stored as the REGISTRY NAME of the sign block that was mounted, not as an index
 * into a list of allowed signs. Nothing here has to know which signs exist, so a sign added to
 * the mod later can be mounted without this being touched, and a sign removed from the mod
 * leaves a barricade that simply draws no sign rather than one pointing at the wrong art.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class TileEntityBarricade extends AbstractTileEntity {

  /** The NBT key the flasher setting is stored under. */
  private static final String NBT_FLASHERS = "flashers";

  /** The NBT key the mounted sign's registry name is stored under. */
  private static final String NBT_SIGN = "sign";

  /** Which warning lights this barricade carries. */
  private BarricadeFlashers flashers = BarricadeFlashers.NONE;

  /** The registry name of the mounted sign block, or null for none. */
  @Nullable
  private ResourceLocation sign;

  /**
   * Where in its own flash cycle this barricade starts, in milliseconds.
   *
   * <p>Deliberately not persisted, and deliberately random: the lights along a real closure are
   * each on their own timer and drift apart. Same device as the drums use.</p>
   */
  private final long strobeOffset = ThreadLocalRandom.current().nextLong(1000L);

  /**
   * Gets which warning lights this barricade carries.
   *
   * @return the flasher setting
   *
   * @since 1.0
   */
  public BarricadeFlashers getFlashers() {
    return flashers;
  }

  /**
   * Sets which warning lights this barricade carries.
   *
   * @param flashers the flasher setting
   *
   * @since 1.0
   */
  public void setFlashers(BarricadeFlashers flashers) {
    this.flashers = flashers == null ? BarricadeFlashers.NONE : flashers;
  }

  /**
   * Gets the registry name of the mounted sign block, or null if none is mounted.
   *
   * @return the sign's registry name, or null
   *
   * @since 1.0
   */
  @Nullable
  public ResourceLocation getSign() {
    return sign;
  }

  /**
   * Sets the mounted sign block by registry name, or clears it with null.
   *
   * @param sign the sign's registry name, or null to clear
   *
   * @since 1.0
   */
  public void setSign(@Nullable ResourceLocation sign) {
    this.sign = sign;
  }

  /**
   * Gets the mounted sign as a block, or null if none is mounted or the block no longer exists.
   *
   * @return the sign block, or null
   *
   * @since 1.0
   */
  @Nullable
  public Block getSignBlock() {
    if (sign == null) {
      return null;
    }
    Block block = Block.REGISTRY.getObject(sign);
    // The registry answers with air rather than null for a name it does not know, which is what
    // a sign removed from the mod since this barricade was placed would look like.
    return block == null || block == net.minecraft.init.Blocks.AIR ? null : block;
  }

  /**
   * Gets where in its own flash cycle this barricade starts, in milliseconds.
   *
   * @return the offset
   *
   * @since 1.0
   */
  public long getStrobeOffset() {
    return strobeOffset;
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    flashers = BarricadeFlashers.fromOrdinal(compound.getInteger(NBT_FLASHERS));
    String raw = compound.getString(NBT_SIGN);
    sign = raw == null || raw.isEmpty() ? null : new ResourceLocation(raw);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(NBT_FLASHERS, flashers.ordinal());
    compound.setString(NBT_SIGN, sign == null ? "" : sign.toString());
    return compound;
  }

  /**
   * A mounted sign stands above the rails and a run of barricades is wider than one cell, so the
   * render box is widened to match rather than left at the block.
   *
   * @return the render bounding box
   *
   * @since 1.0
   */
  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(
        pos.getX() - 1.0, pos.getY(), pos.getZ() - 1.0,
        pos.getX() + 2.0, pos.getY() + 4.0, pos.getZ() + 2.0);
  }

  /** Drawn as far as the signals and the arrow board it stands with. */
  @Override
  public double getMaxRenderDistanceSquared() {
    return LONG_RANGE_RENDER_DISTANCE_SQUARED;
  }
}
