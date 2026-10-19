package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import java.util.Locale;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A cathedral door leaf's pointed head ({@code cathedral_door_<variant>_arch}): three courses of
 * leaf above a {@link BlockCathedralDoor}, cut by the arch, with the stone beside the arch in the
 * leaf's plane. It is part of the leaf -- it swings with it, its cells store the same four bits, and
 * the leaf's master is found through it -- so everything the leaf does it does.
 *
 * <p>Actual state: which course it is ({@code course}, counted up from the leaf), and whether the
 * leaf is one of a pair ({@code paired}): a pair's two heads make one arch four blocks across, a
 * single leaf's head a lancet two across. Both arches rise three blocks. The stone is marked fixed
 * in the models, so the swing renderer leaves it in the wall.</p>
 *
 * @version 1.0
 * @since 2026.10
 */
public class BlockCathedralDoorArch extends BlockCathedralDoor {

  /**
   * The stone beside the arch (the spandrel), one of the Building module's veneers: cast stone
   * where a jamb beside the arch is cast stone, ashlar otherwise. SHARED with
   * {@code gen_cathedral_doors.STONES}.
   *
   * @since 1.0
   */
  public enum Stone implements IStringSerializable {
    ASHLAR, CAST;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }

    /**
     * The spandrel's stone for the jambs at {@code a} and {@code b}: cast if either is a cast stone
     * block (the block, its stairs, slab or fence).
     *
     * @since 1.0
     */
    static Stone of(IBlockAccess world, BlockPos a, BlockPos b) {
      return cast(world, a) || cast(world, b) ? CAST : ASHLAR;
    }

    private static boolean cast(IBlockAccess world, BlockPos p) {
      ResourceLocation name = world.getBlockState(p).getBlock().getRegistryName();
      return name != null && name.getPath().startsWith("stone_cast");
    }
  }

  /** Actual state only: the course of the head, 0 just above the leaf. */
  public static final PropertyInteger COURSE = PropertyInteger.create("course", 0, 2);
  /** Actual state only: whether the leaf is one of a pair. */
  public static final PropertyBool PAIRED = PropertyBool.create("paired");
  /** Actual state only: the stone beside the arch, from the jambs either side of it. */
  public static final PropertyEnum<Stone> STONE = PropertyEnum.create("stone", Stone.class);

  /**
   * Constructs a {@link BlockCathedralDoorArch}.
   *
   * @param registryName {@code cathedral_door_<variant>_arch}
   *
   * @since 1.0
   */
  public BlockCathedralDoorArch(String registryName) {
    super(registryName);
  }

  @Override
  protected IBlockState initialState(IBlockState state) {
    return state.withProperty(COURSE, 0).withProperty(PAIRED, false)
        .withProperty(STONE, Stone.ASHLAR);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, OPEN, HINGE, SWING, COLUMN, COURSE, PAIRED,
        STONE);
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    int course = 0;
    while (course < 2 && sameKind(worldIn.getBlockState(pos.down(course + 1)), state)) {
      course++;
    }
    Column column = column(worldIn::getBlockState, pos, state);
    EnumFacing latch = hingeSide(state).getOpposite();
    BlockPos latchCell = column == Column.HINGE ? pos.offset(latch) : pos;
    IBlockState beyond = worldIn.getBlockState(latchCell.offset(latch));
    boolean paired = beyond.getBlock() == this
        && beyond.getValue(FACING) == state.getValue(FACING)
        && beyond.getValue(HINGE) != state.getValue(HINGE);
    // The jambs either side of the whole arch, at this course: the one beside this leaf's hinge
    // column, and the one past its latch column (past the pair's other leaf over a pair).
    EnumFacing hinge = hingeSide(state);
    BlockPos hingeCell = column == Column.HINGE ? pos : pos.offset(hinge);
    Stone stone = Stone.of(worldIn, hingeCell.offset(hinge),
        latchCell.offset(latch, paired ? 3 : 1));
    return withSwing(state.withProperty(COLUMN, column).withProperty(COURSE, course)
        .withProperty(PAIRED, paired).withProperty(STONE, stone), worldIn, pos);
  }

  /**
   * Places the head on the leaf below: three courses above its top row, across both its columns,
   * hung as the leaf is.
   *
   * @since 1.0
   */
  @Override
  boolean placeFromItem(World world, BlockPos pos, EntityPlayer player) {
    IBlockState below = world.getBlockState(pos.down());
    if (!(below.getBlock() instanceof BlockCathedralDoor)
        || below.getBlock() instanceof BlockCathedralDoorArch
        || !((BlockCathedralDoor) below.getBlock()).variant().equals(variant())) {
      return refuse(world, player, "gui.csm.door.cathedral.no_leaf");
    }
    BlockCathedralDoor leaf = (BlockCathedralDoor) below.getBlock();
    BlockPos master = leaf.master(world::getBlockState, pos.down(), below);
    int top = 0;
    while (top + 1 < MAX_ROWS && leaf.sameKind(world.getBlockState(master.up(top + 1)), below)) {
      top++;
    }
    BlockPos h = master.up(top + 1);
    BlockPos l = h.offset(hingeSide(below).getOpposite());
    for (int y = 0; y < 3; y++) {
      if (!clear(world, h.up(y)) || !clear(world, l.up(y))) {
        return refuse(world, player, "gui.csm.door.cathedral.no_leaf");
      }
    }
    IBlockState state = getDefaultState().withProperty(FACING, below.getValue(FACING))
        .withProperty(HINGE, below.getValue(HINGE)).withProperty(OPEN, below.getValue(OPEN));
    for (int y = 0; y < 3; y++) {
      world.setBlockState(h.up(y), state, 3);
      world.setBlockState(l.up(y), state, 3);
    }
    return true;
  }
}
