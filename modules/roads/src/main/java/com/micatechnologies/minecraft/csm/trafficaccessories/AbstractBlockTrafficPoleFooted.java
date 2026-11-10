package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmPostPassesThrough;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTrafficPoleIgnored;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.NotNull;

/**
 * A painted steel traffic pole that draws its own foot: the bolted base it stands on.
 *
 * <p>The thin, thick and impact resistant poles are plain tubes, stacked. Where an upright one
 * stands on the ground it now shows a base (GitHub #348), in the style its owner picked with the
 * Street Light Configuration Tool: a square bolted plate (the default, so every pole already in
 * a world gets one), a squircle plate, a slim square plate, a tall pedestal base, or none.
 *
 * <p><b>One property for everything at the foot.</b> {@link #FOOT} is the stored choice and, in
 * the actual state, what is drawn: the choice where the pole stands on the ground, {@code none}
 * where it does not, and {@code extend} where it stands on a guardrail and reaches down through
 * it instead ({@link ICsmPostPassesThrough}). That last value replaces
 * {@link AbstractBlockTrafficPole#EXTEND_DOWN} for these poles: a base and an extension are never
 * drawn together, so one property of six values carries both where two would have multiplied to
 * ten, and every value multiplies the variants a Forge blockstate bakes.
 *
 * <p><b>Upright poles are always drawn facing up.</b> A pole placed looking up at it faces
 * {@code DOWN}, and its foot is then at the other end of the model. The tube is the same either
 * way round, so {@link #getActualState} stands every upright pole the usual way before anything
 * else is worked out, and the blockstate needs one foot model per style, not two.
 *
 * <p>The metadata keeps the facing in the six values the poles always stored, read back as the
 * default base; 6 to 9 are an upright pole with one of the other four choices.
 *
 * <p>Base models, the impact pole and every one of these poles' blockstates come from
 * {@code dev-env-utils/scripts/gen_pole_bases.py}.
 *
 * @since 2026.11
 */
public abstract class AbstractBlockTrafficPoleFooted extends AbstractBlockTrafficPole {

  /**
   * What the foot of the pole shows. The first five are the owner's choices, in the order the
   * configuration tool steps through them; {@link #EXTEND} is never stored.
   */
  public enum Foot implements IStringSerializable {
    /** A square plate with a nut cover at each corner. */
    SQUARE("Standard square bolted base"),
    /** The same plate with its corners rounded off. */
    SQUIRCLE("Standard squircle bolted base"),
    /** A thin square plate on exposed anchor bolts. */
    SLIM("Slim square bolted base"),
    /** A tall, tapered square base, as the pedestal pole stands on. */
    PEDESTAL("Pedestal base"),
    /** Nothing: the tube runs straight into the ground. */
    NONE("No base"),
    /** One more block of tube, reaching down through a guardrail to the ground. */
    EXTEND("Extension");

    private final String friendlyName;

    Foot(String friendlyName) {
      this.friendlyName = friendlyName;
    }

    @Override
    public String getName() {
      return name().toLowerCase();
    }

    /**
     * The name the Street Light Configuration Tool reports.
     *
     * @return the display name
     */
    public String getFriendlyName() {
      return friendlyName;
    }

    /**
     * The choice after this one, for the configuration tool: through the five an owner can
     * pick and round again.
     *
     * @return the next choice
     */
    public Foot nextChoice() {
      return values()[(ordinal() + 1) % EXTEND.ordinal()];
    }
  }

  /** The stored choice of base, and in the actual state what the foot draws. */
  public static final PropertyEnum<Foot> FOOT = PropertyEnum.create("foot", Foot.class);

  /** First metadata value of an upright pole with a base other than the default. */
  private static final int META_UPRIGHT_CHOICES = 6;

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, MOUNT_EAST, MOUNT_WEST, MOUNT_UP, MOUNT_DOWN,
        FOOT);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    EnumFacing facing = state.getValue(FACING);
    Foot foot = state.getValue(FOOT);
    if (facing.getAxis() != EnumFacing.Axis.Y || foot == Foot.SQUARE || foot == Foot.EXTEND) {
      return facing.getIndex();
    }
    return META_UPRIGHT_CHOICES + foot.ordinal() - 1;
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    int choice = meta - META_UPRIGHT_CHOICES + 1;
    if (choice >= 1 && choice < Foot.EXTEND.ordinal()) {
      return getDefaultState().withProperty(FACING, EnumFacing.UP)
          .withProperty(FOOT, Foot.values()[choice]);
    }
    return super.getStateFromMeta(meta);
  }

  /**
   * The flank mounts come from the superclass, worked out for the pole standing the usual way
   * up; this adds the foot.
   */
  @Override
  @SuppressWarnings("deprecation")
  public @NotNull IBlockState getActualState(@NotNull IBlockState state,
      @NotNull IBlockAccess worldIn, @NotNull BlockPos pos) {
    boolean upright = state.getValue(FACING).getAxis() == EnumFacing.Axis.Y;
    Foot choice = state.getValue(FOOT);
    IBlockState stood = upright ? state.withProperty(FACING, EnumFacing.UP) : state;
    return super.getActualState(stood, worldIn, pos)
        .withProperty(FOOT, upright ? footAt(worldIn, pos, choice) : Foot.NONE);
  }

  /**
   * What the foot of an upright pole shows, from the block under it.
   *
   * @param worldIn the world
   * @param pos     the pole's position
   * @param choice  the owner's choice of base
   *
   * @return the foot to draw
   */
  private static Foot footAt(IBlockAccess worldIn, BlockPos pos, Foot choice) {
    BlockPos below = pos.down();
    Block belowBlock = worldIn.getBlockState(below).getBlock();
    if (belowBlock instanceof ICsmPostPassesThrough) {
      return Foot.EXTEND;
    }
    // Hanging in the open, or over plants, snow layers, water: nothing to stand on.
    if (worldIn.isAirBlock(below) || belowBlock.isReplaceable(worldIn, below)
        || BlockTrafficPoleConcrete.isNaturalClutter(belowBlock)) {
      return Foot.NONE;
    }
    // More pole below, or a CSM block that is its own mounting hardware (a hand-placed pole
    // base, a connector, a cabinet): the tube meets it flush.
    if (belowBlock instanceof AbstractBlockTrafficPole
        || belowBlock instanceof ICsmTrafficPoleIgnored) {
      return Foot.NONE;
    }
    return choice == Foot.EXTEND ? Foot.SQUARE : choice;
  }
}
