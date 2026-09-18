package com.micatechnologies.minecraft.csm.constructionsite;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * One quarter of a 2x2 tower crane mast section. Four of them side by side make a section, and
 * sections stack into a mast to the build limit, as the 1x1 does.
 *
 * <p>Each quarter works out which corner of its section it is from the quarters beside it, so
 * only the livery is stored. A quarter with a neighbour to the east and none to the west is on the
 * west side, and so on; one with neighbours on both sides or neither, which a proper 2x2 never
 * has, draws as a north-west quarter. The faces are two blocks wide, so each face's X lacing spans
 * a panel two blocks high, and a quarter draws the lower or upper half of it by whether its y is
 * even or odd -- absolute, so stacked sections stay in step wherever the mast starts. The models
 * come from {@code dev-env-utils/scripts/gen_crane.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockCraneMastLarge extends BlockCraneMast {

  /** Which corner of the 2x2 section. Actual state only. */
  public static final PropertyEnum<Corner> CORNER = PropertyEnum.create("corner", Corner.class);

  /** Which half of the two-block lacing panel. Actual state only. */
  public static final PropertyEnum<Half> HALF = PropertyEnum.create("half", Half.class);

  /** Each quarter's one chord, at the section's outer corner. */
  private static final AxisAlignedBB[] CHORD = {
      BlockSiteProp.box16(0.5, 0, 0.5, 3, 16, 3),
      BlockSiteProp.box16(13, 0, 0.5, 15.5, 16, 3),
      BlockSiteProp.box16(13, 0, 13, 15.5, 16, 15.5),
      BlockSiteProp.box16(0.5, 0, 13, 3, 16, 15.5),
  };

  @Override
  public String getBlockRegistryName() {
    return "crane_mast_large";
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, LIVERY, DOWN, CORNER, HALF);
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    return super.getActualState(state, worldIn, pos)
        .withProperty(CORNER, corner(worldIn, pos))
        .withProperty(HALF, (pos.getY() & 1) == 0 ? Half.LOWER : Half.UPPER);
  }

  private Corner corner(IBlockAccess world, BlockPos pos) {
    boolean east = world.getBlockState(pos.east()).getBlock() == this;
    boolean west = world.getBlockState(pos.west()).getBlock() == this;
    boolean south = world.getBlockState(pos.south()).getBlock() == this;
    boolean north = world.getBlockState(pos.north()).getBlock() == this;
    boolean westSide = !(west && !east);
    boolean northSide = !(north && !south);
    if (northSide) {
      return westSide ? Corner.NW : Corner.NE;
    }
    return westSide ? Corner.SW : Corner.SE;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, @Nonnull World worldIn,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn,
      boolean isActualState) {
    addCollisionBoxToList(pos, entityBox, collidingBoxes,
        CHORD[corner(worldIn, pos).ordinal()]);
  }

  /**
   * A quarter's corner of its section, in the order the chords above are listed.
   *
   * @since 1.0
   */
  public enum Corner implements IStringSerializable {
    NW("nw"), NE("ne"), SE("se"), SW("sw");

    private final String name;

    Corner(String name) {
      this.name = name;
    }

    @Override
    @Nonnull
    public String getName() {
      return name;
    }
  }

  /**
   * Which half of a two-block lacing panel a quarter draws.
   *
   * @since 1.0
   */
  public enum Half implements IStringSerializable {
    LOWER("lower"), UPPER("upper");

    private final String name;

    Half(String name) {
      this.name = name;
    }

    @Override
    @Nonnull
    public String getName() {
      return name;
    }
  }
}
