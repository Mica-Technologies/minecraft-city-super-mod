package com.micatechnologies.minecraft.csm.codeutils;

/**
 * Marker interface for CSM blocks that should not have snow layers accumulate on top of
 * them during natural precipitation. Vanilla's snow placement test passes when the block
 * below reports a solid UP face (via {@code isSideSolid}); {@link AbstractBlock} checks
 * for this marker and returns false from {@code isSideSolid(..., UP)} so the snow-layer
 * placement skips these blocks.
 *
 * <p>Applied to thin / rendered-geometry blocks like traffic signs, traffic signal heads,
 * and traffic accessories where a full 1x1 snow layer hovering over the block's model
 * would look visually wrong.
 */
public interface ICsmNoSnowAccumulation {
}
