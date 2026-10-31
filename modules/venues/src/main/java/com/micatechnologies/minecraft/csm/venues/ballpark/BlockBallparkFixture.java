package com.micatechnologies.minecraft.csm.venues.ballpark;

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
 * A ballpark fixture that is one block turned to a facing: home plate, a base, the pitcher's
 * rubber, the dugout's helmet rack, bat rack and water cooler, the batting cage's roof net and
 * the pitching machine. Some keep a variant in bits 2-3:
 *
 * <ul>
 *   <li>{@code home_plate}: 0 its point on the block's back edge, as the field lays it (the
 *   plate's point is home's corner, where the foul lines meet), 1 centred in the block;</li>
 *   <li>{@code baseball_base}, {@code softball_base}: 0 first, 1 second, 2 third base, each
 *   placed off the block's middle to where a full-size diamond laid with its centre line along
 *   the grid puts it and turned 45 degrees to the base paths; 3 centred and square to the
 *   block. Softball's first base is the double base, its orange half in foul ground.</li>
 * </ul>
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3): the field's centre line, towards
 * centre field, for the plate and the bases; the way the rack or machine faces otherwise. A
 * sneak-click with an empty hand steps the variant.</p>
 *
 * @since 2026.10
 */
public class BlockBallparkFixture extends BlockVenue {

  /** Which fixture. */
  public enum Kind {
    /** Home plate. */
    HOME_PLATE(2, "home_plate", Material.ROCK, SoundType.STONE, box(0, 0, 0, 16, 0.6, 16), true),
    /** A base. */
    BASE(4, "base", Material.CLOTH, SoundType.CLOTH, box(0, 0, 0, 16, 1.6, 16), true),
    /** The pitcher's rubber, flush in any ground. */
    RUBBER(1, null, Material.ROCK, SoundType.STONE, box(3, 0, 6.5, 13, 0.6, 9.5), true),
    /** Helmet cubbies against the dugout wall. */
    HELMET_RACK(1, null, Material.IRON, SoundType.METAL, box(0.5, 2, 10, 15.5, 15, 16), false),
    /** A bat rack against the dugout wall. */
    BAT_RACK(1, null, Material.WOOD, SoundType.WOOD, box(1, 0, 11, 15, 14.6, 15.5), false),
    /** A ten-gallon water cooler on its stand. */
    WATER_COOLER(1, null, Material.IRON, SoundType.METAL, box(3, 0, 3.5, 15, 15.5, 12.5), false),
    /** A batting cage's roof net at the top of its cell: no collision. */
    NET_ROOF(1, null, Material.CLOTH, SoundType.CLOTH, box(0, 15, 0, 16, 16, 16), false),
    /** A two-wheel pitching machine on its stand. */
    PITCHING_MACHINE(1, null, Material.IRON, SoundType.METAL, box(2, 0, 2, 14, 15, 15), false);

    final int variants;
    @Nullable
    final String key;
    final Material material;
    final SoundType sound;
    final AxisAlignedBB box;
    final boolean flat;

    Kind(int variants, @Nullable String key, Material material, SoundType sound,
        AxisAlignedBB box, boolean flat) {
      this.variants = variants;
      this.key = key;
      this.material = material;
      this.sound = sound;
      this.box = box;
      this.flat = flat;
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
  public BlockBallparkFixture(String registryName, Kind kind) {
    super(stash(registryName, kind), kind.material, kind.sound, kind.flat ? 0.8F : 1.5F);
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
    return kind == Kind.NET_ROOF ? NULL_AABB : super.getCollisionBoundingBox(state, source, pos);
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
      player.sendStatusMessage(new TextComponentTranslation("csm.venues.ballpark.variant",
          new TextComponentTranslation("csm.venues.ballpark.variant." + kind.key + "." + v)),
          true);
    }
    return true;
  }
}
