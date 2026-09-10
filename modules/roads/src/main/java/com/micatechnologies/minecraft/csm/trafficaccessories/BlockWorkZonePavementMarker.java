package com.micatechnologies.minecraft.csm.trafficaccessories;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A temporary pavement marker: the small folded tab taped down a lane line while the permanent
 * markings are missing.
 *
 * <p>It draws at full brightness whatever the light around it, which is how the beaded strip
 * along its top edge reads as retroreflective rather than as a painted stripe. The strip is the
 * point of the device: a marker that goes dark at night is a marker that is not doing its job,
 * and these are put out precisely for the nights between milling a road and re-striping it.</p>
 *
 * <p>Minecraft 1.12 has no per-face emissive on a baked model, so this is the whole tab rather
 * than the strip alone. The alternative is a tile entity renderer drawing the strip over the
 * model, which is how every other lit thing in this mod works — and it costs a tile entity and a
 * draw call for each marker, which a device meant to be laid out in lines of dozens cannot
 * afford. At this size the difference is not visible; the tab is small enough that a lit body
 * reads as plastic catching headlights.</p>
 *
 * <p>It does not emit light. Nothing around it is lit any differently, which is correct — a
 * retroreflector returns a driver's own beam and illuminates nothing.</p>
 *
 * @version 1.0
 * @see BlockWorkZoneDeviceRotatable
 * @since 2026.9
 */
public class BlockWorkZonePavementMarker extends BlockWorkZoneDeviceRotatable {

  /**
   * Sky and block light both at maximum, packed the way the renderer expects them.
   *
   * @since 1.0
   */
  private static final int FULLBRIGHT = 0x00F000F0;

  /**
   * Constructs a {@link BlockWorkZonePavementMarker} instance.
   *
   * @param registryName the registry name of the marker
   * @param boundingBox  the bounding box of the marker, in block space
   *
   * @since 1.0
   */
  public BlockWorkZonePavementMarker(String registryName, AxisAlignedBB boundingBox) {
    super(registryName, boundingBox);
  }

  /**
   * Overridden method which draws this block at full brightness regardless of the light reaching
   * it, so its reflective strip stays visible after dark.
   *
   * @param state  the block state
   * @param source the block access
   * @param pos    the block position
   *
   * @return the packed lightmap coordinates to draw this block with
   *
   * @since 1.0
   */
  @Override
  public int getPackedLightmapCoords(IBlockState state, IBlockAccess source, BlockPos pos) {
    return FULLBRIGHT;
  }
}
