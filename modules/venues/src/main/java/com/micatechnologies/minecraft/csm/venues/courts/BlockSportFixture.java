package com.micatechnologies.minecraft.csm.venues.courts;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A one-block court or track fixture: the tennis ball machine, a hurdle (its height a variant),
 * a starting block, the shot put toe board. Decor: baked models, nothing ticks.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3): the way a ball machine shoots,
 * the way the runners go over a hurdle or out of the blocks, the way the shot is put; for the
 * hurdle bits 2-3 its height (0 0.762 m, the women's 400 m; 1 0.838 m, the 100 m; 2 0.914 m, the
 * men's 400 m; 3 1.067 m, the 110 m), which a sneak-click with an empty hand steps. The toe board
 * stands on the circle's centre cell and is drawn on the circle's front edge.</p>
 *
 * @since 2026.10
 */
public class BlockSportFixture extends BlockVenue {

  /** Which fixture. */
  public enum Kind {
    BALL_MACHINE(1, Material.IRON, SoundType.METAL, box(3, 0, 1.5, 13, 15, 14.5), true),
    HURDLE(4, Material.IRON, SoundType.METAL, box(0, 0, 4, 16, 12, 15), true),
    STARTING_BLOCK(1, Material.IRON, SoundType.METAL, box(4.5, 0, 1, 11.5, 3, 15), false),
    TOE_BOARD(1, Material.WOOD, SoundType.WOOD, box(5, 0, 5, 11, 1, 11), false);

    final int variants;
    final Material material;
    final SoundType sound;
    final AxisAlignedBB box;
    final boolean solid;

    Kind(int variants, Material material, SoundType sound, AxisAlignedBB box, boolean solid) {
      this.variants = variants;
      this.material = material;
      this.sound = sound;
      this.box = box;
      this.solid = solid;
    }
  }

  /** The hurdle's heights, in pixels. */
  static final double[] HURDLE_PX = {12.19, 13.41, 14.62, 17.07};

  private static final ThreadLocal<Kind> PENDING = new ThreadLocal<>();

  private PropertyInteger variant;
  private final Kind kind;

  public BlockSportFixture(String registryName, Kind kind) {
    super(stash(registryName, kind), kind.material, kind.sound, 1.0F);
    this.kind = kind;
    PENDING.remove();
    IBlockState s = blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH);
    if (variant != null) {
      s = s.withProperty(variant, 0);
    }
    setDefaultState(s);
  }

  private static String stash(String registryName, Kind kind) {
    PENDING.set(kind);
    return registryName;
  }

  private static AxisAlignedBB box(double x0, double y0, double z0, double x1, double y1,
      double z1) {
    return new AxisAlignedBB(x0 / 16.0, y0 / 16.0, z0 / 16.0, x1 / 16.0, y1 / 16.0, z1 / 16.0);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    Kind k = PENDING.get();
    if (k != null && k.variants > 1) {
      variant = PropertyInteger.create("variant", 0, k.variants - 1);
      return new CsmBlockStateContainer(this, FACING, variant);
    }
    return new CsmBlockStateContainer(this, FACING);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    IBlockState s = getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
    if (variant != null) {
      s = s.withProperty(variant, Math.min(kind.variants - 1, (meta >> 2) & 3));
    }
    return s;
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    int meta = state.getValue(FACING).getHorizontalIndex();
    return variant == null ? meta : meta | state.getValue(variant) << 2;
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    if (kind == Kind.HURDLE && variant != null) {
      double h = HURDLE_PX[state.getValue(variant)] / 16.0;
      return new AxisAlignedBB(0, 0, kind.box.minZ, 1, Math.min(1.0, h + 0.05), kind.box.maxZ);
    }
    return kind.box;
  }

  @Nullable
  @Override
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return kind.solid ? super.getCollisionBoundingBox(state, source, pos) : NULL_AABB;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (variant == null || !player.isSneaking() || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      int v = (state.getValue(variant) + 1) % kind.variants;
      world.setBlockState(pos, state.withProperty(variant, v), 3);
      player.sendStatusMessage(new TextComponentTranslation("csm.venues.track.hurdle." + v),
          true);
    }
    return true;
  }
}
