package com.micatechnologies.minecraft.csm.venues.field;

import net.minecraft.block.state.IBlockState;

/**
 * A field block whose top is a picture cut into tinted parts ({@link BlockFieldPainted},
 * {@link BlockFieldAnchor}): {@link FieldTint} needs the quarter turns its picture is shown at to
 * know which way its west and east parts, and its stripe, lie in the world.
 *
 * @since 2026.10
 */
public interface IFieldCell {

  /**
   * The quarter turns clockwise from north the cell's picture is drawn at.
   *
   * @param actualState the cell's actual state (as a chunk is built)
   *
   * @return 0 to 3
   */
  int tintRotation(IBlockState actualState);
}
