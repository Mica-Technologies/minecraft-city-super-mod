package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A utility box carrying an ID number decal the player can set: right-click it with an empty
 * hand to edit the number. {@link TileEntityUtilityBoxLabel} holds the number and
 * {@link TileEntityUtilityBoxLabelRenderer} draws it.
 *
 * @version 1.0
 */
public class BlockUtilityBoxLabelled extends BlockUtilityBox implements ICsmTileEntityProvider {

  /** The number editor's GUI id, unique across the mod. */
  public static final int GUI_ID = 32;

  public BlockUtilityBoxLabelled(String registryName, UtilityBoxSpec spec) {
    super(registryName, spec);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (world.isRemote) {
      player.openGui(Csm.instance, GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
    }
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityUtilityBoxLabel.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityutilityboxlabel";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityUtilityBoxLabel();
  }
}
