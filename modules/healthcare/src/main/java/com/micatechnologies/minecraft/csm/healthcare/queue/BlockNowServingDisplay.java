package com.micatechnologies.minecraft.csm.healthcare.queue;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcare;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The now-serving display ({@code now_serving_display}): a wall screen showing NOW SERVING, the
 * number its queue is serving, and the desk to go to. Its case and the dark screen are baked;
 * {@link TileEntityNowServingDisplayRenderer} draws the lit screen and its text, the second and
 * last renderer in the module besides the ED tracking board.
 *
 * <p><b>Metadata</b>: the facing out of the wall in bits 0-2 (N 2, S 3, W 4, E 5). <b>NBT</b>
 * (optional, {@code /blockdata}): {@code number} (0-999, 0 shows dashes), {@code desk} (0-99,
 * 0 shows no desk line). Linked to a check-in kiosk or call console by
 * {@code /csmhealthcare queue link}, it shows that queue's number whenever it changes.</p>
 *
 * @since 2026.10
 */
public class BlockNowServingDisplay extends BlockHealthcare implements ICsmTileEntityProvider {

  /** Constructs the display. */
  public BlockNowServingDisplay() {
    super("now_serving_display", new int[]{0, 3, 13, 16, 13, 16}, HealthcareMaterial.PLASTIC,
        Mount.WALL, false);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityNowServingDisplay.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitynowservingdisplay";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityNowServingDisplay();
  }

  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
    if (!world.isRemote) {
      QueueNetwork.get(world).forgetDisplay(pos.toLong());
    }
    super.breakBlock(world, pos, state);
  }
}
