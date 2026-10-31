package com.micatechnologies.minecraft.csm.venues.lines;

import com.micatechnologies.minecraft.csm.codeutils.CsmExtendedBlockState;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * A court's or a track's painted cell ({@code court_acrylic_painted}, {@code track_painted}): it shows
 * the piece of the court or track that falls on it, found from the nearest anchor of its family
 * within {@link LineArtFamily#REACH} cells whose layout reaches it. One state: metadata is always
 * 0 and everything it shows (the layout, its place, the colours) is the anchor's. Where no anchor
 * reaches it shows its plain surface.
 *
 * <p>The search reads at most 625 blocks, only when the block's chunk section is built: lay these
 * only where a line is ({@code --layout} does), plain surface blocks elsewhere.</p>
 *
 * @since 2026.10
 */
public class BlockLinePainted extends BlockLineCell {

  /**
   * Constructs a painted cell.
   *
   * @param registryName its registry name
   * @param surface      what it is made of
   * @param family       the anchors it reads
   */
  public BlockLinePainted(String registryName, Surface surface, LineArtFamily family) {
    super(registryName, surface, family, null);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmExtendedBlockState(this, new IProperty[0], new IUnlistedProperty[]{KEY});
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState();
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return 0;
  }
}
