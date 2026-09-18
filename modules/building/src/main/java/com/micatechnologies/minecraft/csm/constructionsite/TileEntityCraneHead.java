package com.micatechnologies.minecraft.csm.constructionsite;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import javax.annotation.Nonnull;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A tower crane head: everything about the crane that is not its mast.
 *
 * <p>The head block renders nothing itself; this tile entity's renderer draws the whole slewing
 * unit, so its configuration lives here -- model, livery, jib length, slew, trolley, hook drop
 * and luff -- and so does the reach the renderer needs to be culled and faded correctly. Every
 * value is clamped wherever it is set, so a bad NBT tag or packet cannot draw a crane a thousand
 * blocks long.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class TileEntityCraneHead extends AbstractTileEntity {

  /** Jib length limits, in blocks, for a 1x1 mast; a 2x2 mast doubles both. */
  public static final int MIN_JIB = 8;
  public static final int MAX_JIB = 40;

  /** Hook drop limit below the jib, in blocks. */
  public static final int MAX_HOOK_DROP = 250;

  /** Luffing jib angle limits, degrees above horizontal. */
  public static final float MIN_LUFF = 15F;
  public static final float MAX_LUFF = 80F;

  /** How far above the head the tallest model reaches, in blocks, for the render box. */
  private static final int TOP_ALLOWANCE = 20;

  private CraneModel model = CraneModel.FLAT_TOP;
  private CraneLivery livery = CraneLivery.YELLOW;
  private int jibLength = 24;
  private float slew = 0F;
  private float trolley = 0.6F;
  private int hookDrop = 10;
  private float luff = 60F;

  /** Whether the mast under the head is a 2x2, and where its centre is from this block's. */
  private boolean large = false;
  private double centreX = 0.0;
  private double centreZ = 0.0;

  // --- configuration ---------------------------------------------------------------------------

  public CraneModel getModel() {
    return model;
  }

  public CraneLivery getLivery() {
    return livery;
  }

  public int getJibLength() {
    return jibLength;
  }

  public float getSlew() {
    return slew;
  }

  public float getTrolley() {
    return trolley;
  }

  public int getHookDrop() {
    return hookDrop;
  }

  public float getLuff() {
    return luff;
  }

  public boolean isLarge() {
    return large;
  }

  public double getCentreX() {
    return centreX;
  }

  public double getCentreZ() {
    return centreZ;
  }

  /**
   * The size factor for everything the renderer draws: 2 on a 2x2 mast, 1 on a 1x1.
   *
   * @return the scale
   *
   * @since 1.0
   */
  public int getScale() {
    return large ? 2 : 1;
  }

  /**
   * Sets the whole configuration, clamping every value, and pushes it to clients.
   *
   * @since 1.0
   */
  public void setConfiguration(CraneModel model, CraneLivery livery, int jibLength, float slew,
      float trolley, int hookDrop, float luff) {
    this.model = model == null ? CraneModel.FLAT_TOP : model;
    this.livery = livery == null ? CraneLivery.YELLOW : livery;
    this.jibLength = MathHelper.clamp(jibLength, MIN_JIB * getScale(), MAX_JIB * getScale());
    this.slew = MathHelper.wrapDegrees(slew);
    this.trolley = MathHelper.clamp(trolley, 0F, 1F);
    this.hookDrop = MathHelper.clamp(hookDrop, 1, MAX_HOOK_DROP);
    this.luff = MathHelper.clamp(luff, MIN_LUFF, MAX_LUFF);
    if (world != null) {
      markDirtySync(world, pos, true);
    }
  }

  /**
   * Records the mast the head stands on: its size, and for a 2x2 where the section's centre is.
   * Called by the block on placement.
   *
   * @param large   whether the mast is a 2x2
   * @param centreX the section centre's x offset from this block's centre, in blocks
   * @param centreZ the section centre's z offset
   * @param livery  the mast's livery, which a new head takes on
   *
   * @since 1.0
   */
  public void setMast(boolean large, double centreX, double centreZ, CraneLivery livery) {
    this.large = large;
    this.centreX = centreX;
    this.centreZ = centreZ;
    this.livery = livery;
    this.jibLength = MathHelper.clamp(jibLength * getScale(), MIN_JIB * getScale(),
        MAX_JIB * getScale());
  }

  // --- persistence -----------------------------------------------------------------------------

  private static final String KEY_MODEL = "m";
  private static final String KEY_LIVERY = "l";
  private static final String KEY_JIB = "j";
  private static final String KEY_SLEW = "s";
  private static final String KEY_TROLLEY = "t";
  private static final String KEY_HOOK = "h";
  private static final String KEY_LUFF = "f";
  private static final String KEY_LARGE = "g";
  private static final String KEY_CX = "cx";
  private static final String KEY_CZ = "cz";

  @Override
  public void readNBT(NBTTagCompound compound) {
    large = compound.getBoolean(KEY_LARGE);
    centreX = MathHelper.clamp(compound.getDouble(KEY_CX), -1.0, 1.0);
    centreZ = MathHelper.clamp(compound.getDouble(KEY_CZ), -1.0, 1.0);
    model = CraneModel.fromOrdinal(compound.getInteger(KEY_MODEL));
    livery = CraneLivery.fromOrdinal(compound.getInteger(KEY_LIVERY));
    jibLength = compound.hasKey(KEY_JIB)
        ? MathHelper.clamp(compound.getInteger(KEY_JIB), MIN_JIB * getScale(),
        MAX_JIB * getScale()) : 24 * getScale();
    slew = MathHelper.wrapDegrees(compound.getFloat(KEY_SLEW));
    trolley = compound.hasKey(KEY_TROLLEY)
        ? MathHelper.clamp(compound.getFloat(KEY_TROLLEY), 0F, 1F) : 0.6F;
    hookDrop = compound.hasKey(KEY_HOOK)
        ? MathHelper.clamp(compound.getInteger(KEY_HOOK), 1, MAX_HOOK_DROP) : 10;
    luff = compound.hasKey(KEY_LUFF)
        ? MathHelper.clamp(compound.getFloat(KEY_LUFF), MIN_LUFF, MAX_LUFF) : 60F;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(KEY_MODEL, model.ordinal());
    compound.setInteger(KEY_LIVERY, livery.ordinal());
    compound.setInteger(KEY_JIB, jibLength);
    compound.setFloat(KEY_SLEW, slew);
    compound.setFloat(KEY_TROLLEY, trolley);
    compound.setInteger(KEY_HOOK, hookDrop);
    compound.setFloat(KEY_LUFF, luff);
    compound.setBoolean(KEY_LARGE, large);
    compound.setDouble(KEY_CX, centreX);
    compound.setDouble(KEY_CZ, centreZ);
    return compound;
  }

  // --- rendering -------------------------------------------------------------------------------

  /**
   * A hash of everything the renderer's geometry depends on -- not the slew, which it applies as
   * a rotation outside the compiled geometry. A change here recompiles it.
   *
   * @return the key
   *
   * @since 1.0
   */
  public int renderKey() {
    int k = model.ordinal();
    k = k * 31 + livery.ordinal();
    k = k * 31 + jibLength;
    k = k * 31 + getScale();
    k = k * 31 + Math.round(trolley * 1000F);
    k = k * 31 + hookDrop;
    k = k * 31 + Math.round(luff * 10F);
    return k;
  }

  /**
   * Frees the renderer's compiled geometry when the head goes away. Client only; the renderer
   * class is never touched on a server.
   *
   * @since 1.0
   */
  @Override
  public void invalidate() {
    super.invalidate();
    if (world != null && world.isRemote) {
      TileEntityCraneHeadRenderer.release(pos);
    }
  }

  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    if (world != null && world.isRemote) {
      TileEntityCraneHeadRenderer.release(pos);
    }
  }

  /**
   * How far the crane reaches from its centre in any direction it could slew to, in blocks.
   *
   * @return the horizontal reach
   *
   * @since 1.0
   */
  public double getReach() {
    return jibLength + 2.0 * getScale();
  }

  /**
   * A box around everything the crane can draw: a square the jib's reach on every side, since it
   * can slew any way, from the hook's lowest drop to the top of the tallest model. Never the
   * infinite box, which switches frustum culling off.
   *
   * @since 1.0
   */
  @Override
  @SideOnly(Side.CLIENT)
  @Nonnull
  public AxisAlignedBB getRenderBoundingBox() {
    double r = getReach();
    double cx = pos.getX() + 0.5 + centreX;
    double cz = pos.getZ() + 0.5 + centreZ;
    return new AxisAlignedBB(cx - r, pos.getY() - hookDrop - 2, cz - r,
        cx + r, pos.getY() + TOP_ALLOWANCE * getScale(), cz + r);
  }

  /**
   * The long-range distance plus the crane's reach, as the span wire does: measured from the
   * head, a fixed 128 would drop the far end of a long jib while it was still on screen.
   *
   * @since 1.0
   */
  @Override
  @SideOnly(Side.CLIENT)
  public double getMaxRenderDistanceSquared() {
    double d = LONG_RANGE_RENDER_DISTANCE + getReach();
    return d * d;
  }
}
