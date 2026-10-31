package com.micatechnologies.minecraft.csm.venues.ballpark;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.venues.VenuesGuiProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * An outfield wall block carrying a distance marker ({@code outfield_wall_marker}): the wall,
 * straight or 45 degree, with a distance painted on its pad (330, 375, 400). The number is
 * {@link TileEntityOutfieldMarkerRenderer}'s, about 1.4 blocks tall, centred across the marker
 * block's top edge: put the marker in the lower block of a two-block wall and the number spans
 * both.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing, as {@link BlockOutfieldWall}'s; bit 2 diagonal.
 * A click opens the editor (the distance, up to five characters, and its colour);
 * {@code /blockdata x y z {txt:"375",fc:14}} sets them. Placed by hand it is straight, or
 * diagonal if placed while sneaking.</p>
 *
 * @since 2026.10
 */
public class BlockOutfieldWallMarker extends BlockOutfieldWall implements ICsmTileEntityProvider {

  /**
   * Constructs the marker.
   *
   * @param registryName its registry name
   */
  public BlockOutfieldWallMarker(String registryName) {
    super(registryName, 2);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(DIAGONAL, placer.isSneaking());
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
    return TileEntityOutfieldMarker.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityoutfieldmarker";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityOutfieldMarker();
  }
}
