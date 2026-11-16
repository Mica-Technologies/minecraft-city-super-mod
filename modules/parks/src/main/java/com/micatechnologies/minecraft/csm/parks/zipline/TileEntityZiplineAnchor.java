package com.micatechnologies.minecraft.csm.parks.zipline;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A zipline anchor's link: the other end of its line. A launch anchor's is the landing anchor its
 * cable runs down to, and the launch anchor draws the cable ({@link TileEntityZiplineCableRenderer});
 * a landing anchor's is the launch anchor that ends on it, kept so breaking either end takes the
 * line down.
 *
 * @version 1.0
 * @since 2026.11
 */
public class TileEntityZiplineAnchor extends AbstractTileEntity {

  private static final String NBT_LINK = "zL";
  private static final String NBT_LIGHTS = "zLt";

  @Nullable
  private BlockPos link;
  /** A launch anchor's line lights, drawn along the cable. */
  private boolean lights;
  /** The line, for a launch anchor with a link; worked out again when the link changes. */
  @Nullable
  private transient ZiplineLine line;

  @Nullable
  public BlockPos getLink() {
    return link;
  }

  /** Whether this is a launch anchor (its block says). */
  public boolean isLaunch() {
    if (world == null) {
      return false;
    }
    net.minecraft.block.Block block = world.getBlockState(pos).getBlock();
    return block instanceof BlockZiplineAnchor && ((BlockZiplineAnchor) block).isLaunch();
  }

  @Override
  public void invalidate() {
    super.invalidate();
    if (world != null && world.isRemote) {
      TileEntityZiplineCableRenderer.cleanupDisplayLists(pos);
    }
  }

  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    if (world != null && world.isRemote) {
      TileEntityZiplineCableRenderer.cleanupDisplayLists(pos);
    }
  }

  /**
   * Sets or clears the other end, and sends it to the clients with this tick's block changes, so
   * a build that places the anchors and strings the line in one tick does not reach a client ahead
   * of the anchor blocks themselves.
   *
   * @param other the other anchor, or null
   *
   * @since 1.0
   */
  public void setLink(@Nullable BlockPos other) {
    link = other == null ? null : other.toImmutable();
    line = null;
    if (world != null && !world.isRemote) {
      markDirtyQueueSync(world, pos);
    }
  }

  public boolean hasLights() {
    return lights;
  }

  /**
   * Turns the line's lights on or off, and sends it to the clients.
   *
   * @since 1.0
   */
  public void setLights(boolean on) {
    lights = on;
    if (world != null && !world.isRemote) {
      markDirtyQueueSync(world, pos);
    }
  }

  /**
   * This launch anchor's line, or null if it is not a launch anchor or has no landing.
   *
   * @since 1.0
   */
  @Nullable
  public ZiplineLine line() {
    if (link == null || !isLaunch()) {
      return null;
    }
    if (line == null || !line.sameAs(pos, link)) {
      line = ZiplineLine.of(pos, link);
    }
    return line;
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    if (compound.hasKey(NBT_LINK)) {
      link = BlockPos.fromLong(compound.getLong(NBT_LINK));
    } else {
      link = null;
    }
    lights = compound.getBoolean(NBT_LIGHTS);
    line = null;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (link != null) {
      compound.setLong(NBT_LINK, link.toLong());
    }
    if (lights) {
      compound.setBoolean(NBT_LIGHTS, true);
    }
    return compound;
  }

  /** No baked model reads the link: the cable is drawn by the renderer. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }

  /** The whole cable, so it is drawn while any of it is in view. */
  @Override
  @SideOnly(Side.CLIENT)
  @Nonnull
  public AxisAlignedBB getRenderBoundingBox() {
    if (link == null) {
      return new AxisAlignedBB(pos);
    }
    return new AxisAlignedBB(pos, link).grow(1.0);
  }

  /** As far as the longest line, so a cable does not vanish while its far end is in view. */
  @Override
  @SideOnly(Side.CLIENT)
  public double getMaxRenderDistanceSquared() {
    return 256.0 * 256.0;
  }
}
