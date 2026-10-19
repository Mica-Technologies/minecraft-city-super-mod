package com.micatechnologies.minecraft.csm.healthcare.patient;

import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcare;
import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcarePair;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import com.micatechnologies.minecraft.csm.healthcare.wards.BlockHospitalBed;
import com.micatechnologies.minecraft.csm.healthcare.wards.BlockHospitalBedFoot;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The patient simulation mannequin ({@code simulation_mannequin}): a teaching manikin in a gown,
 * lying on its back. It is a block of its own set in the cell ABOVE what it lies on, and it
 * draws itself sunk down onto that surface, so a bed or table under it is not replaced.
 *
 * <p>What it lies on is read from the cell below ({@link #REST}, actual state, never stored):</p>
 * <ul>
 *   <li>a hospital bed's head cell: on the mattress at the bed's height, its back raised with the
 *   bed's head section; set it facing the way the bed faces (its feet towards the foot);</li>
 *   <li>a hospital bed's foot cell: the same, drawn one cell further back, so it may be set over
 *   either cell of a bed;</li>
 *   <li>the exam table's first cell (the raised head end), facing the table's
 *   {@code facing.rotateYCCW()} (its feet towards the second cell): on the pad, back raised;</li>
 *   <li>any other cell of an exam, autopsy or dissection table: flat on the top, its middle on
 *   the line between the table's two cells, feet the way it faces;</li>
 *   <li>anything else (an operating table from another mod, a full block): flat on the floor of
 *   its own cell.</li>
 * </ul>
 * <p>The body is drawn centred on its cell's front edge (the line between a bed's two cells), a
 * little under two blocks long. Placed by hand it takes the facing above from what it is put on.
 * It has no collision. <b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), the way its
 * feet point.</p>
 *
 * @since 2026.10
 */
public class BlockSimulationMannequin extends BlockHealthcare {

  /** What the mannequin lies on. The order is the blockstate's; append only. */
  public enum Rest implements IStringSerializable {
    FLAT(0, false),
    BED_LOW(9.6, false),
    BED_HIGH(13.6, false),
    BED_LOW_FOOT(9.6, true),
    BED_HIGH_FOOT(13.6, true),
    EXAM(11.25, false),
    EXAM_FLAT(11.25, false),
    TABLE(10.5, false);

    /** The surface's height in the cell below, in sixteenths (0: the floor of its own cell). */
    private final double surface;
    /** Drawn one cell further back (over a bed's foot). */
    private final boolean back;

    Rest(double surface, boolean back) {
      this.surface = surface;
      this.back = back;
    }

    /**
     * Whether the body is drawn one cell further back, over a bed's foot.
     *
     * @return true over a bed's foot cell
     */
    public boolean isBack() {
      return back;
    }

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase();
    }
  }

  /** What it lies on. Actual state. */
  public static final PropertyEnum<Rest> REST = PropertyEnum.create("rest", Rest.class);

  private static final IProperty<?>[] ACTUAL = {REST};
  private static final AxisAlignedBB[] BOXES = new AxisAlignedBB[Rest.values().length];

  static {
    for (Rest r : Rest.values()) {
      double y0 = r == Rest.FLAT ? 0 : (r.surface - 16) / 16.0;
      BOXES[r.ordinal()] = new AxisAlignedBB(4 / 16.0, y0, 0, 12 / 16.0, y0 + 4 / 16.0, 1);
    }
  }

  /** Constructs the mannequin. */
  public BlockSimulationMannequin() {
    super("simulation_mannequin", new int[]{4, 0, 0, 12, 4, 16}, HealthcareMaterial.CLOTH,
        Mount.FLOOR, false);
  }

  @Override
  protected IProperty<?>[] actualProperties() {
    return ACTUAL;
  }

  @Override
  protected IBlockState defaultExtras(IBlockState state) {
    return state.withProperty(REST, Rest.FLAT);
  }

  private static String name(Block block) {
    return block.getRegistryName() == null ? "" : block.getRegistryName().getPath();
  }

  private static boolean table(String name) {
    return "exam_table".equals(name) || "autopsy_table".equals(name)
        || "dissection_table".equals(name);
  }

  /**
   * What a mannequin facing {@code facing} lies on, from the state under it.
   *
   * @param world  the world
   * @param below  the cell under the mannequin
   * @param facing the mannequin's facing
   *
   * @return the rest
   */
  public static Rest restOn(IBlockAccess world, BlockPos below, EnumFacing facing) {
    IBlockState s = world.getBlockState(below);
    Block b = s.getBlock();
    if (b instanceof BlockHospitalBed) {
      return s.getValue(BlockHospitalBed.HIGH) ? Rest.BED_HIGH : Rest.BED_LOW;
    }
    if (b instanceof BlockHospitalBedFoot) {
      IBlockState a = s.getActualState(world, below);
      return a.getValue(BlockHospitalBed.HIGH) ? Rest.BED_HIGH_FOOT : Rest.BED_LOW_FOOT;
    }
    String n = name(b);
    if (b instanceof BlockHealthcarePair && table(n)) {
      if ("exam_table".equals(n)) {
        return !s.getValue(BlockHealthcarePair.SECOND)
            && facing == s.getValue(FACING).rotateYCCW() ? Rest.EXAM : Rest.EXAM_FLAT;
      }
      return Rest.TABLE;
    }
    return Rest.FLAT;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos) {
    IBlockState s = super.getActualState(state, world, pos);
    return s.withProperty(REST, restOn(world, pos.down(), s.getValue(FACING)));
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOXES[state.getValue(REST).ordinal()];
  }

  /** Placed by hand on a bed or a table, it lies along it. */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    IBlockState state = super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta,
        placer);
    IBlockState s = world.getBlockState(pos.down());
    Block b = s.getBlock();
    if (b instanceof BlockHospitalBed || b instanceof BlockHospitalBedFoot) {
      return state.withProperty(FACING, s.getValue(FACING));
    }
    if (b instanceof BlockHealthcarePair && table(name(b))) {
      EnumFacing f = s.getValue(FACING);
      return state.withProperty(FACING, s.getValue(BlockHealthcarePair.SECOND) ? f.rotateY()
          : f.rotateYCCW());
    }
    return state;
  }
}
