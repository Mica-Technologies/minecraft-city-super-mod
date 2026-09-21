package com.micatechnologies.minecraft.csm.signage;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The block a player places for an advertising board: the board's controller. It holds the
 * board's size and its ads ({@link TileEntityAdBoard}), builds the rest of the board out of
 * {@link BlockAdBoardPart parts} when its screen asks for a size, and draws the ad across all of
 * it. One class, constructed by registry name, per {@link AdBoardKind}.
 *
 * <p>The controller is in the board's bottom row; the board grows up from it and to its left,
 * right or both, as its {@link AdBoardAlign} says. A new board is the controller alone, and its
 * screen opens as it is placed.</p>
 */
public class BlockAdBoard extends AbstractBlockAdBoard implements ICsmTileEntityProvider {

  public BlockAdBoard(String registryName) {
    super(registryName);
  }

  /** The ad faces the player who places it, with its back to the wall they placed it on. */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(@Nonnull World worldIn, @Nonnull BlockPos pos,
      @Nonnull EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
      @Nonnull EntityLivingBase placer) {
    EnumFacing face = placer.getHorizontalFacing().getOpposite();
    return getDefaultState().withProperty(FACING, face)
        .withProperty(TAG, AdBoards.freeTag(worldIn, kind(), face, pos));
  }

  @Override
  public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    TileEntity te = worldIn.getTileEntity(pos);
    if (te instanceof TileEntityAdBoard) {
      ((TileEntityAdBoard) te).initialise(kind(), pos);
    }
    if (worldIn.isRemote && placer instanceof EntityPlayer) {
      SignageGuiProvider.open((EntityPlayer) placer, worldIn, pos);
    }
  }

  /** A board lit by redstone follows the signal at its controller. */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    if (worldIn.isRemote) {
      return;
    }
    TileEntity te = worldIn.getTileEntity(pos);
    if (te instanceof TileEntityAdBoard) {
      ((TileEntityAdBoard) te).setPowered(worldIn.isBlockPowered(pos));
    }
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World worldIn, int meta) {
    return new TileEntityAdBoard();
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityAdBoard.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityadboard";
  }
}
