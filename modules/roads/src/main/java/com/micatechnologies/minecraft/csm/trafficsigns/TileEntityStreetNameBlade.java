package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityDynamicStreetSign;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignData;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignMount;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * What a post-top street name blade says: the dynamic street sign's document, unchanged, with
 * its mount held to this block's bracket.
 *
 * <p>Subclassing the dynamic street sign's tile entity is what lets the blade reuse that sign's
 * editor, its update packet and its renderer without any of the three learning about a second
 * kind of sign -- all three accept it on an {@code instanceof}. The only things that differ are
 * the mount, which is the block's rather than the player's, and the render box, which has to
 * cover a blade crossing the post as well as one running along it.</p>
 *
 * <p>Two concrete subclasses exist rather than one class used by both blocks, because Core
 * registers a tile entity once per class and warns on a second block claiming the same one.</p>
 *
 * @version 1.0
 * @since 2026.9.20
 */
public abstract class TileEntityStreetNameBlade extends TileEntityDynamicStreetSign {

  /**
   * The bracket this blade is carried on.
   *
   * @return the mount to hold the sign document to
   *
   * @since 1.0
   */
  protected abstract StreetSignMount mount();

  /**
   * The document, with its mount coerced to this block's bracket.
   *
   * <p>Coerced on the way out rather than trusted from the document, so a blade keeps the right
   * hardware whatever it was saved with -- a document pasted from a hanging blade, or one from
   * a world written before these blocks existed, still comes back mounted on its post.</p>
   */
  @Override
  public StreetSignData getSignData() {
    StreetSignData data = super.getSignData();
    if (data.getMountType() != mount()) {
      data.setMountType(mount());
    }
    return data;
  }

  /** The editor shows the mount and does not offer to change it; the block decides it. */
  @Override
  public boolean isMountFixed() {
    return true;
  }

  /**
   * A box wide enough for a blade in any direction, since a crossing pair reaches out along
   * both axes and a long street name reaches a long way along each.
   */
  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(pos.getX() - 15, pos.getY() - 2, pos.getZ() - 15,
        pos.getX() + 16, pos.getY() + 3, pos.getZ() + 16);
  }
}
