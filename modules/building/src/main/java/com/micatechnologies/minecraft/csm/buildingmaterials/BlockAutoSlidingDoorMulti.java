package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * An automatic sliding door whose leaf is more than one cell: a wide door
 * ({@code auto_door_wide_slider_<finish>}), two cells drawing one panel two blocks wide that slides
 * two blocks, or a telescoping door ({@code auto_door_tele_slider_<finish>}), two or three cells
 * each a whole panel on its own plane, the cell {@code i} from the hinge side sliding {@code i + 1}
 * blocks so that they all stack over one sidelite.
 *
 * <p>A leaf is a run of these cells side by side, facing the same way and hinged the same way (the
 * side they slide to). Where a cell is in its leaf, counted from the hinge side, is the actual-state
 * {@link #CELL}, read from its neighbours, so it costs no metadata: it picks the cell's model and how
 * far it slides. Two leaves hinged apart, meeting in the middle, are a bi-parting door.</p>
 *
 * <ul>
 *   <li><b>Placing.</b> A cell placed beside one of its kind joins that one's leaf, hinged the same
 *   way, while the leaf is short of full (two cells wide, three telescoping); sneaking, or a full
 *   leaf, makes it the start of the other leaf of a bi-parting pair instead. With no neighbour, the
 *   hinge goes on the side of the opening that was clicked.</li>
 *   <li><b>One door.</b> Every cell in the run (both leaves) opens and shuts together, by the
 *   sensor, redstone, a click or a keypad; a lock on any cell holds the run shut against someone
 *   outside; and it shuts only when the doorway along the whole run is clear.</li>
 *   <li><b>Models.</b> Shut, JSON; open, OBJ (a two- or three-block slide is past where a JSON
 *   element may reach), made by {@code gen_doors.py} from the same parts.</li>
 * </ul>
 *
 * @version 1.0
 * @since 2026.11
 */
public class BlockAutoSlidingDoorMulti extends BlockAutoSlidingDoor {

  /** Where a cell is in its leaf, counted from the hinge side. Actual state only. */
  public static final PropertyInteger CELL = PropertyInteger.create("cell", 0, 2);

  /** How far along the wall a run of cells is followed, either way. */
  private static final int MAX_RUN = 6;

  /** SHARED with gen_doors.TELE_Z: a telescoping cell's plane, by cell, in blocks. */
  private static final double[][] TELE_Z = {{4.75 / 16, 6.25 / 16}, {3.0 / 16, 4.5 / 16},
      {1.25 / 16, 2.75 / 16}};

  /**
   * Constructs a {@link BlockAutoSlidingDoorMulti}.
   *
   * @param registryName {@code auto_door_wide_slider_<finish>} or
   *                     {@code auto_door_tele_slider_<finish>}
   *
   * @since 1.0
   */
  public BlockAutoSlidingDoorMulti(String registryName) {
    super(registryName);
  }

  /** Whether this is the wide door (one panel over two cells), not the telescoping one. */
  public boolean wide() {
    return getBlockRegistryName().contains("_wide_");
  }

  /** How many cells make a full leaf. SHARED with gen_doors.AUTO_MULTI_CELLS. */
  int leafCells() {
    return wide() ? 2 : 3;
  }

  @Override
  public double slide(IBlockState door) {
    int blocks = wide() ? 2 : door.getValue(CELL) + 1;
    return blocks - STRIP;
  }

  // --- state ------------------------------------------------------------------------------------

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, HALF, FACING, OPEN, HINGE, CLOSER, SWING, REVERSED,
        CELL);
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    IBlockState door = super.getActualState(state, worldIn, pos);
    BlockPos lowerPos = lower(state, pos);
    EnumFacing f = door.getValue(FACING);
    Hinge hinge = door.getValue(HINGE);
    EnumFacing toHinge = hingeSide(f, hinge);
    int n = 0;
    BlockPos p = lowerPos.offset(toHinge);
    while (n < MAX_RUN && sameLeaf(worldIn, p, f, hinge)) {
      n++;
      p = p.offset(toHinge);
    }
    return door.withProperty(CELL, n % leafCells());
  }

  /** The side a door with this hinge slides to. */
  private static EnumFacing hingeSide(EnumFacing f, Hinge hinge) {
    return hinge == Hinge.LEFT ? f.rotateYCCW() : f.rotateY();
  }

  /**
   * The facing and hinge of a cell of this kind whose lower half is at {@code p}, read straight from
   * its two halves (not its actual state, which would read its neighbours in turn), or null.
   */
  @Nullable
  private Object[] cellAt(IBlockAccess world, BlockPos p) {
    IBlockState lower = world.getBlockState(p);
    IBlockState upper = world.getBlockState(p.up());
    if (lower.getBlock() != this || lower.getValue(HALF) != Half.LOWER
        || upper.getBlock() != this || upper.getValue(HALF) != Half.UPPER) {
      return null;
    }
    return new Object[]{lower.getValue(FACING), upper.getValue(HINGE)};
  }

  private boolean sameLeaf(IBlockAccess world, BlockPos p, EnumFacing f, Hinge hinge) {
    Object[] c = cellAt(world, p);
    return c != null && c[0] == f && c[1] == hinge;
  }

  /** How many cells are in the leaf of the cell at {@code lowerPos}. */
  private int leafSize(IBlockAccess world, BlockPos lowerPos, EnumFacing f, Hinge hinge) {
    int n = 1;
    for (EnumFacing d : new EnumFacing[]{f.rotateY(), f.rotateYCCW()}) {
      BlockPos p = lowerPos.offset(d);
      for (int i = 0; i < MAX_RUN && sameLeaf(world, p, f, hinge); i++) {
        n++;
        p = p.offset(d);
      }
    }
    return n;
  }

  /** Every cell of the door the cell at {@code lowerPos} is in: the run along the wall. */
  private List<BlockPos> run(World world, BlockPos lowerPos) {
    List<BlockPos> out = new ArrayList<>();
    out.add(lowerPos);
    Object[] c = cellAt(world, lowerPos);
    if (c == null) {
      return out;
    }
    EnumFacing f = (EnumFacing) c[0];
    for (EnumFacing d : new EnumFacing[]{f.rotateY(), f.rotateYCCW()}) {
      BlockPos p = lowerPos.offset(d);
      for (int i = 0; i < MAX_RUN; i++) {
        Object[] o = cellAt(world, p);
        if (o == null || o[0] != f) {
          break;
        }
        out.add(p);
        p = p.offset(d);
      }
    }
    return out;
  }

  // --- placing ------------------------------------------------------------------------------------

  /**
   * The hinge, from the neighbours: beside a cell of this kind, on its latch side, this one is the
   * leaf's new lead (same hinge) unless the leaf is full or the player sneaks, when it starts the
   * other leaf (hinged apart, the two meeting here); on its hinge side, it is the leaf's new last
   * cell (same hinge) while the leaf is not full. Otherwise the side of the opening clicked.
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing f = placer.getHorizontalFacing();
    EnumFacing left = f.rotateYCCW();
    Hinge hinge = null;
    for (EnumFacing d : new EnumFacing[]{left, left.getOpposite()}) {
      BlockPos x = pos.offset(d);
      Object[] c = cellAt(worldIn, x);
      if (c == null || c[0] != f) {
        continue;
      }
      Hinge theirs = (Hinge) c[1];
      boolean full = leafSize(worldIn, x, f, theirs) >= leafCells();
      if (hingeSide(f, theirs) == d) {
        // On its latch side: lead of its leaf, or the start of the other leaf.
        hinge = !full && !placer.isSneaking() ? theirs
            : hingeSide(f, Hinge.LEFT) == d.getOpposite() ? Hinge.LEFT : Hinge.RIGHT;
        break;
      }
      if (!full) {
        // On its hinge side: the leaf's new last cell.
        hinge = theirs;
        break;
      }
    }
    if (hinge == null) {
      double along = (hitX - 0.5) * left.getXOffset() + (hitZ - 0.5) * left.getZOffset();
      hinge = along > 0 ? Hinge.LEFT : Hinge.RIGHT;
    }
    PENDING_HINGE.set(hinge);
    return getDefaultState().withProperty(FACING, f);
  }

  // --- one door -----------------------------------------------------------------------------------

  @Override
  void setOpen(World world, BlockPos lowerPos, boolean open, boolean byHand) {
    for (BlockPos p : run(world, lowerPos)) {
      swing(world, p, open, byHand);
    }
  }

  @Override
  boolean held(World world, BlockPos lowerPos) {
    for (BlockPos p : run(world, lowerPos)) {
      if (DoorLocks.holds(world, p)) {
        return true;
      }
    }
    return false;
  }

  @Override
  boolean inTheWay(World world, BlockPos lowerPos, IBlockState door) {
    for (BlockPos p : run(world, lowerPos)) {
      if (occupied(world, p, door.getValue(FACING))) {
        return true;
      }
    }
    return false;
  }

  // --- shape ------------------------------------------------------------------------------------

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    IBlockState door = getActualState(state, source, pos);
    double z0 = SLIDER_Z0;
    double z1 = SLIDER_Z1;
    if (!wide()) {
      int cell = door.getValue(CELL);
      z0 = TELE_Z[cell][0];
      z1 = TELE_Z[cell][1];
    }
    AxisAlignedBB north;
    if (!door.getValue(OPEN)) {
      north = new AxisAlignedBB(0, 0, z0, 1, 1, z1);
    } else {
      // The panel has gone from the cell: a sliver at the side it slid to, enough to click.
      boolean left = door.getValue(HINGE) == Hinge.LEFT;
      north = new AxisAlignedBB(left ? 0 : 1 - STRIP, 0, z0, left ? STRIP : 1, 1, z1);
    }
    return BlockGarageDoor.turn(north, door.getValue(FACING));
  }
}
