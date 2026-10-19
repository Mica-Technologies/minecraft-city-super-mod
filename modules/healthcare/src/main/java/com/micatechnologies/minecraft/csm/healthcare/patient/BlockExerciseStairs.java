package com.micatechnologies.minecraft.csm.healthcare.patient;

import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcarePair;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The rehabilitation gym's training stairs ({@code exercise_stairs}): two cells wide, three steps
 * up from each end to a platform in the middle, with a handrail along both sides, so a patient
 * practises going up one way and down the other.
 *
 * <p>A {@link BlockHealthcarePair} (second cell to the viewer's right). The steps rise a quarter
 * block each and run across the two cells, so a player walks up and over them; each cell
 * collides as its own steps, not as one box.</p>
 *
 * @since 2026.10
 */
public class BlockExerciseStairs extends BlockHealthcarePair {

  /** The first cell's steps and platform facing north, in sixteenths: x0, x1, height. */
  private static final double[][] FIRST_STEPS = {{0, 7, 12}, {7, 11, 8}, {11, 15, 4}};
  /** The second cell's, in its own sixteenths. */
  private static final double[][] SECOND_STEPS = {{1, 5, 4}, {5, 9, 8}, {9, 16, 12}};
  private static final double Z0 = 2 / 16.0;
  private static final double Z1 = 14 / 16.0;

  private static final AxisAlignedBB[] FIRST_BOXES = boxes(FIRST_STEPS);
  private static final AxisAlignedBB[] SECOND_BOXES = boxes(SECOND_STEPS);

  /** Constructs the stairs. */
  public BlockExerciseStairs() {
    super("exercise_stairs", new int[]{0, 0, 2, 16, 12, 14}, HealthcareMaterial.WOOD,
        Mount.FLOOR, Shape.WIDE);
  }

  private static AxisAlignedBB[] boxes(double[][] steps) {
    AxisAlignedBB[] out = new AxisAlignedBB[steps.length];
    for (int i = 0; i < steps.length; i++) {
      out[i] = new AxisAlignedBB(steps[i][0] / 16.0, 0, Z0, steps[i][1] / 16.0,
          steps[i][2] / 16.0, Z1);
    }
    return out;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
      AxisAlignedBB entityBox, List<AxisAlignedBB> boxes, @Nullable Entity entity,
      boolean isActualState) {
    EnumFacing facing = state.getValue(FACING);
    for (AxisAlignedBB b : state.getValue(SECOND) ? SECOND_BOXES : FIRST_BOXES) {
      addCollisionBoxToList(pos, entityBox, boxes, RotationUtils.rotateBoundingBoxByFacing(b,
          facing));
    }
  }
}
