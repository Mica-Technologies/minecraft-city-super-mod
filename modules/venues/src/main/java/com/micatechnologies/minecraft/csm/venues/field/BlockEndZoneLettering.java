package com.micatechnologies.minecraft.csm.venues.field;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import com.micatechnologies.minecraft.csm.venues.VenuesGuiProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * End zone lettering ({@code football_end_zone_lettering}): a marker in the air cell over the
 * middle of an end zone whose renderer paints the words across the end zone, flat on the turf,
 * the bottoms of the letters towards the goal line, so a reader on the field sees them upright.
 * The words, their height, the widest they may run and their colours are the tile entity's
 * ({@link TileEntityEndZoneLettering}); a click opens the venue text editor.
 *
 * <p><b>Metadata</b>: the facing (S 0, W 1, N 2, E 3), towards the field (the goal line). No
 * collision; a thin box to click.</p>
 *
 * @since 2026.10
 */
public class BlockEndZoneLettering extends BlockVenue implements ICsmTileEntityProvider {

  private static final AxisAlignedBB BOX = new AxisAlignedBB(0, 0, 0, 1, 0.0625, 1);

  /**
   * Constructs the lettering block.
   *
   * @param registryName its registry name
   */
  public BlockEndZoneLettering(String registryName) {
    super(registryName, Material.CIRCUITS, SoundType.CLOTH, 0.5F);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }

  @Nullable
  @Override
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
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
    return TileEntityEndZoneLettering.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityendzonelettering";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityEndZoneLettering();
  }
}
