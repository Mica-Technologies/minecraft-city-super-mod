package com.micatechnologies.minecraft.csm.venues.av;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.venues.VenuesGuiProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The scrolling-text piece of an LED ribbon board ({@code led_ribbon_board_text}): a straight
 * length whose tile entity holds a line of text, which {@link TileEntityRibbonTextRenderer}
 * draws once across the straight run that starts here and carries on to the right (as seen from
 * the floor), over a band in its background colour. Put it at the left end of the stretch the
 * text should cross; the run stops at the first cell that is not a straight facing the same way,
 * or at another text piece.
 *
 * <p><b>Metadata</b>: the facing (S 0, W 1, N 2, E 3). A click opens its text editor;
 * {@code /blockdata} sets {@code txt}, {@code fg}, {@code bg} and {@code spd}
 * ({@link TileEntityRibbonText}).</p>
 *
 * @since 2026.10
 */
public class BlockLedRibbonText extends BlockLedRibbon implements ICsmTileEntityProvider {

  /**
   * Constructs the text piece.
   *
   * @param registryName its registry name
   */
  public BlockLedRibbonText(String registryName) {
    super(registryName);
  }

  @Override
  protected IBlockState defaultState() {
    return blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(PHASE, 0);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, PHASE);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex();
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    if (world.isRemote) {
      VenuesGuiProvider.open(player, world, pos, VenuesGuiProvider.TEXT_GUI_ID);
    }
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityRibbonText.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityvenueribbontext";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityRibbonText();
  }
}
