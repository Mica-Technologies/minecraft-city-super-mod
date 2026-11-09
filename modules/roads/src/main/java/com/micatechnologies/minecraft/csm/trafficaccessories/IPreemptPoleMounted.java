package com.micatechnologies.minecraft.csm.trafficaccessories;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;

/**
 * A preemption device clamped to the side of a vertical pole (#338): the pole-side version of the
 * preempt detector or of a confirmation light, for a junction whose heads stand on posts or
 * pedestals with no mast arm or span wire to carry them.
 *
 * <p>Each one is a subclass of the arm-mounted device it copies, so linking, detection and
 * lighting are that device's own; all this adds is how it mounts. Its model
 * ({@code gen_preempt_detector.py}, {@code <name>_pole}) is the device without its mount stub,
 * standing in the cell in front of the pole on a bracket that runs back into the pole's cell and
 * ends in a clamp band round it, as the GridSmart bell camera stands beside its pole. The pole is
 * behind the way it faces. Devices stack up one pole a cell apart: a detector with a
 * confirmation light above or below it.</p>
 *
 * @since 2026.11
 */
public interface IPreemptPoleMounted {

  /**
   * The facing a pole-side device takes when placed: the face of the pole that was clicked, so it
   * stands in front of the pole facing whoever placed it, the pole behind it; or, placed on a top
   * or bottom face, back toward the player.
   *
   * @param clicked the face of the block that was clicked
   * @param placer  who placed it
   *
   * @return its horizontal facing
   */
  static EnumFacing placementFacing(EnumFacing clicked, EntityLivingBase placer) {
    if (clicked != null && clicked.getAxis().isHorizontal()) {
      return clicked;
    }
    return placer.getHorizontalFacing().getOpposite();
  }
}
