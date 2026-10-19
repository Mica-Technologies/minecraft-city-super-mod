package com.micatechnologies.minecraft.csm.healthcare.clinical;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcare;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * The ED tracking board ({@code ed_tracking_board}): a wall monitor listing the emergency
 * department's beds, seven a board, from {@link EdTrackingSchedule}. Its bezel and the screen's
 * bands are baked; {@link TileEntityEdTrackingBoardRenderer} draws the screen lit and the text.
 *
 * <p><b>Metadata</b>: the facing out of the wall in bits 0-2 (N 2, S 3, W 4, E 5). <b>NBT</b>
 * (optional, {@code /blockdata}): {@code beds} (1-40, default 24), the beds the department has;
 * {@code seed} (int, default 0), so two departments show different patients. Boards facing the
 * same way side by side and stacked are a bank and page on through the beds, as the Transit flight
 * boards do; nothing links them.</p>
 *
 * @since 2026.10
 */
public class BlockEdTrackingBoard extends BlockHealthcare implements ICsmTileEntityProvider {

  /** Constructs the board. */
  public BlockEdTrackingBoard() {
    super("ed_tracking_board", new int[]{0, 3, 13, 16, 13, 16}, HealthcareMaterial.PLASTIC,
        Mount.WALL, false);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityEdTrackingBoard.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityedtrackingboard";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityEdTrackingBoard();
  }
}
