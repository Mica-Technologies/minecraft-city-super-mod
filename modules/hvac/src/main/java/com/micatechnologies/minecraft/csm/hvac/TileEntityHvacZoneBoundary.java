package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmServerThread;

/**
 * Puts an HVAC Zone Boundary marker in its world's {@link HvacZoneBoundaries} while its chunk is
 * loaded (#305). Holds no data and never ticks. The block listener rescans the rooms beside a
 * marker placed or broken, a second later, by when this has run. Both run on the server thread,
 * even when an async world editor loads or unloads the chunk (#375).
 *
 * @author Mica Technologies
 * @since 2026.10
 */
public class TileEntityHvacZoneBoundary extends AbstractTileEntity {

  @Override
  public void onLoad() {
    super.onLoad();
    CsmServerThread.runOnLoad(this, () -> {
      HvacThermalWorld w = HvacThermal.get(world);
      if (w != null) {
        w.zoneBoundaries.add(pos);
      }
    });
  }

  @Override
  public void onChunkUnload() {
    CsmServerThread.runOnUnloadByPos(this, this::unregister);
    super.onChunkUnload();
  }

  @Override
  public void invalidate() {
    CsmServerThread.runOnUnloadByPos(this, this::unregister);
    super.invalidate();
  }

  private void unregister() {
    HvacThermalWorld w = HvacThermal.peek(world);
    if (w != null) {
      w.zoneBoundaries.remove(pos);
    }
  }
}
