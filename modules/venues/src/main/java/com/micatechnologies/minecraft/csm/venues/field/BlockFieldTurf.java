package com.micatechnologies.minecraft.csm.venues.field;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.arena.BlockArenaFloorBase;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A plain football or soccer field block: {@code field_turf} (metadata 0 turf in the light mowing
 * stripe, 1 the dark stripe, 2 white paint, 3 the surround off the field) or
 * {@code football_end_zone} (metadata the end zone's colour, a venue colour 0-15). The dark stripe
 * and the end zone colour are tints on one shared model, so a field's painted cells, which take
 * their tone from the turf beside them ({@link FieldTint}), match it exactly.
 *
 * <p>The surround is the same light turf as the field, but a painted cell's tone never comes from
 * it: lay it outside the lines, so a touchline's cells take the field's stripe rather than the
 * grass beyond. A sneak-click with an empty hand steps the kind.</p>
 *
 * @since 2026.10
 */
public class BlockFieldTurf extends BlockArenaFloorBase {

  /** Which block. */
  public enum Kind {
    /** Turf, paint and surround. */
    TURF("kind", 4),
    /** An end zone's colour. */
    END_ZONE("colour", 16);

    final String property;
    final int count;

    Kind(String property, int count) {
      this.property = property;
      this.count = count;
    }
  }

  /** {@code field_turf} metadata: the light stripe. */
  public static final int LIGHT = 0;
  /** {@code field_turf} metadata: the dark stripe. */
  public static final int DARK = 1;
  /** {@code field_turf} metadata: white paint. */
  public static final int PAINT = 2;
  /** {@code field_turf} metadata: the surround. */
  public static final int SURROUND = 3;

  private PropertyInteger value;
  private final Kind kind;

  /**
   * Constructs a field block.
   *
   * @param registryName its registry name
   * @param kind         which block
   */
  public BlockFieldTurf(String registryName, Kind kind) {
    super(registryName, Surface.TURF, kind);
    this.kind = kind;
    setDefaultState(blockState.getBaseState().withProperty(value, 0));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    Kind k = (Kind) pendingArg();
    value = PropertyInteger.create(k.property, 0, k.count - 1);
    return new CsmBlockStateContainer(this, value);
  }

  /** Whether this is the end zone. */
  public boolean isEndZone() {
    return kind == Kind.END_ZONE;
  }

  /** The kind or colour a state holds. */
  public int value(IBlockState state) {
    return state.getValue(value);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(value, meta >= 0 && meta < kind.count ? meta : 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(value);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!player.isSneaking() || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      world.setBlockState(pos, state.withProperty(value, (state.getValue(value) + 1) % kind.count),
          3);
    }
    return true;
  }
}
