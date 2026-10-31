package com.micatechnologies.minecraft.csm.venues.field;

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
 * A football or soccer sideline fixture: an end zone pylon, the down box (its number, 1-4, the
 * variant), a chain stake, a length of the ten yard chain lying on the ground, a sideline heater,
 * a misting fan, a corner flag or the fourth official's board. Decor: baked models, nothing ticks.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3); for the down box bits 2-3 the
 * down less one. A sneak-click with an empty hand on the down box steps the down. The pylon, the
 * chain and the flag have no collision.</p>
 *
 * @since 2026.10
 */
public class BlockFieldFixture extends BlockVenue {

  /** Which fixture. */
  public enum Kind {
    /** An end zone pylon: soft orange foam, 4 in square and 18 in tall. */
    PYLON(1, Material.CLOTH, SoundType.CLOTH, box(6.75, 0, 6.75, 9.25, 7.5, 9.25), false),
    /** The down box: a pole with the down on a box at its top. */
    DOWN_BOX(4, Material.IRON, SoundType.METAL, box(6.5, 0, 6.5, 9.5, 24, 9.5), true),
    /** A chain stake: a pole with a red head. */
    CHAIN_STAKE(1, Material.IRON, SoundType.METAL, box(6.5, 0, 6.5, 9.5, 24, 9.5), true),
    /** A length of the ten yard chain, lying along the facing's axis. */
    CHAIN(1, Material.IRON, SoundType.METAL, box(7, 0, 0, 9, 1, 16), false),
    /** A sideline heater: a propane cylinder, a mast, a burner and a reflector. */
    HEATER(1, Material.IRON, SoundType.METAL, box(4.5, 0, 4.5, 11.5, 24, 11.5), true),
    /** A misting drum fan on a wheeled stand. */
    FAN(1, Material.IRON, SoundType.METAL, box(1.5, 0, 3, 14.5, 17, 14), true),
    /** A soccer corner flag on its post. */
    CORNER_FLAG(1, Material.CLOTH, SoundType.CLOTH, box(7, 0, 7, 9, 24, 9), false),
    /** The fourth official's board on a stand. */
    BOARD(1, Material.IRON, SoundType.METAL, box(3, 0, 6, 13, 18, 10), true);

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

  /** The kind, for the base constructor, which builds the block state before fields are set. */
  private static final ThreadLocal<Kind> PENDING = new ThreadLocal<>();

  private PropertyInteger variant;
  private final Kind kind;

  /**
   * Constructs a fixture.
   *
   * @param registryName its registry name
   * @param kind         which fixture
   */
  public BlockFieldFixture(String registryName, Kind kind) {
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
      player.sendStatusMessage(new TextComponentTranslation("csm.venues.football.down", v + 1),
          true);
    }
    return true;
  }
}
