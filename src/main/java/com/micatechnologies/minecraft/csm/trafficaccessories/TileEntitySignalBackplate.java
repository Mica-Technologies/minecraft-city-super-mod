package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * The tile entity behind a signal backplate.
 *
 * <p>It stores nothing, and that is deliberate. Its whole job is to exist: a block without a tile
 * entity cannot have a tile entity renderer, and a plate needs one the moment it has to be drawn
 * anywhere other than exactly where its block sits -- lifted with a head that has risen to meet a
 * span wire, and in time rotated or offset in its own right.
 *
 * <p>Everything the renderer needs it reads from the block state and from the head next door, both
 * of which are already the authority on it. Keeping this empty means no new NBT, nothing to
 * synchronise, and nothing that can fall out of step with the blocks around it.
 */
public class TileEntitySignalBackplate extends AbstractTileEntity {

  /**
   * A plate reaches well past its own block -- the models run from roughly two units below it to
   * ten above -- and it is drawn lifted on top of that. A box of one block would cull it while it
   * was still plainly on screen.
   */
  private static final double RENDER_MARGIN = 2.0;

  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(pos).grow(RENDER_MARGIN);
  }

  /**
   * Releases the plate's baked geometry when this block goes or its chunk unloads.
   *
   * <p>Without this the cache would hold a display list for every plate a player has ever walked
   * past, which is the leak the cache was built to avoid.
   */
  @Override
  public void invalidate() {
    super.invalidate();
    if (world != null && world.isRemote) {
      TileEntitySignalBackplateRenderer.cleanupDisplayList(pos);
    }
  }

  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    if (world != null && world.isRemote) {
      TileEntitySignalBackplateRenderer.cleanupDisplayList(pos);
    }
  }
}
