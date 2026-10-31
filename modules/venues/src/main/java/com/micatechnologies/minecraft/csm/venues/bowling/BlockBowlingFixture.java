package com.micatechnologies.minecraft.csm.venues.bowling;

import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A bowling centre's fixture: the scorer's console a pair of lanes shares (a pedestal with a
 * touch screen and a keypad), the shoe cubbies behind the counter (twelve pigeonholes of rental
 * shoes) or the can of shoe spray on the counter. Decor: baked models, nothing ticks.
 *
 * <p><b>Metadata</b>: the facing (S 0, W 1, N 2, E 3), the way its front faces: the console's
 * screen towards the settee, the cubbies' openings and the spray's label towards the customer.</p>
 *
 * @since 2026.10
 */
public class BlockBowlingFixture extends BlockVenue {

  /** Which fixture. */
  public enum Kind {
    /** The scorer's console. */
    CONSOLE(Material.IRON, SoundType.METAL, box(4, 0, 4, 12, 15, 12)),
    /** The shoe cubbies. */
    CUBBIES(Material.WOOD, SoundType.WOOD, box(0, 0, 5, 16, 16, 16)),
    /** Shoe spray. */
    SPRAY(Material.IRON, SoundType.METAL, box(4, 0, 5, 12, 5, 11));

    final Material material;
    final SoundType sound;
    final AxisAlignedBB box;

    Kind(Material material, SoundType sound, AxisAlignedBB box) {
      this.material = material;
      this.sound = sound;
      this.box = box;
    }
  }

  private final Kind kind;

  /**
   * Constructs a fixture.
   *
   * @param registryName its registry name
   * @param kind         which fixture
   */
  public BlockBowlingFixture(String registryName, Kind kind) {
    super(registryName, kind.material, kind.sound, 1.0F);
    this.kind = kind;
  }

  private static AxisAlignedBB box(double x0, double y0, double z0, double x1, double y1,
      double z1) {
    return new AxisAlignedBB(x0 / 16.0, y0 / 16.0, z0 / 16.0, x1 / 16.0, y1 / 16.0, z1 / 16.0);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return kind.box;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
