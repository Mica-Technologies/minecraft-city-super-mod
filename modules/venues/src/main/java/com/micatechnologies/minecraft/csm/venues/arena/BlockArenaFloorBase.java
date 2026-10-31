package com.micatechnologies.minecraft.csm.venues.arena;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A full, opaque floor block of an arena's event floor or a ballpark: rink ice (as slippery as
 * vanilla ice) or a maple court, the rubber matting round a rink, a ballpark's turf and clay. The subclasses differ only in what picks
 * their top: metadata ({@link BlockArenaFloor}), an anchor's facing and kind
 * ({@link BlockArenaFloorAnchor}) or the anchor found nearby ({@link BlockArenaFloorPainted}).
 *
 * <p>The registry name and one constructor argument are stashed for the base constructor, which
 * asks for the name and builds the block state before the subclass's fields are set.</p>
 *
 * @since 2026.10
 */
public abstract class BlockArenaFloorBase extends AbstractBlock {

  /** What a floor is made of. */
  public enum Surface {
    /** Rink ice: packed ice, slippery. */
    ICE(Material.PACKED_ICE, SoundType.GLASS, "pickaxe", 0.8F, true),
    /** Rubber matting. */
    RUBBER(Material.ROCK, SoundType.CLOTH, "pickaxe", 1.0F, false),
    /** Maple strip flooring. */
    WOOD(Material.WOOD, SoundType.WOOD, "axe", 1.5F, false),
    /** A ballpark's turf. */
    TURF(Material.GRASS, SoundType.PLANT, "shovel", 0.6F, false),
    /** A ballpark's infield clay, packing clay and warning track. */
    CLAY(Material.GROUND, SoundType.GROUND, "shovel", 0.6F, false);

    final Material material;
    final SoundType sound;
    final String tool;
    final float hardness;
    final boolean slippery;

    Surface(Material material, SoundType sound, String tool, float hardness, boolean slippery) {
      this.material = material;
      this.sound = sound;
      this.tool = tool;
      this.hardness = hardness;
      this.slippery = slippery;
    }
  }

  /** The registry name and the subclass's argument, for the base constructor. */
  private static final ThreadLocal<Object[]> PENDING = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a floor block.
   *
   * @param registryName its registry name
   * @param surface      what it is made of
   * @param arg          the subclass's argument, available from {@link #pendingArg()} while the
   *                     base constructor runs
   */
  protected BlockArenaFloorBase(String registryName, Surface surface, Object arg) {
    super(stash(registryName, surface, arg), surface.sound, surface.tool, 0, surface.hardness,
        surface.hardness * 3.0F, 0.0F, 255);
    this.registryName = registryName;
    if (surface.slippery) {
      slipperiness = 0.98F;
    }
    PENDING.remove();
  }

  private static Material stash(String registryName, Surface surface, Object arg) {
    PENDING.set(new Object[]{registryName, arg});
    return surface.material;
  }

  /** The subclass's constructor argument, while the base constructor runs; null after. */
  @Nullable
  protected static Object pendingArg() {
    Object[] p = PENDING.get();
    return p == null ? null : p[1];
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : (String) PENDING.get()[0];
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return SQUARE_BOUNDING_BOX;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return true;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return true;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }
}
