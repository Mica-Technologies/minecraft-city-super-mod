package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/**
 * A trailer-mounted arrow board, whose lit lamps glow and whose display is switched in world.
 *
 * <p>Right-clicking cycles the pattern rather than opening a screen. The board has five of them
 * and a cycle is one click; a screen would be more machinery than the choice is worth, and this
 * matches the colour cycling the mount blocks in this package already use.</p>
 *
 * <p>Only the chassis is a model. The mast, the panel and the lamp grid are drawn by
 * {@link TileEntityArrowBoardRenderer}, because every display an arrow board has is ANIMATED and
 * a baked texture can only show one moment of a sequence. The pattern therefore never reaches
 * the blockstate at all — the renderer reads it from the tile entity.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockWorkZoneArrowBoard extends BlockWorkZoneDeviceDiagonal
    implements ICsmTileEntityProvider {

  /**
   * Constructs a {@link BlockWorkZoneArrowBoard} instance.
   *
   * @param registryName the registry name of the board
   * @param boundingBox  the bounding box of the board, in block space
   *
   * @since 1.0
   */
  public BlockWorkZoneArrowBoard(String registryName, AxisAlignedBB boundingBox) {
    super(registryName, boundingBox);
  }

  /**
   * Cycles the displayed pattern.
   *
   * @param world  the world the block is in
   * @param pos    the block position
   * @param state  the block state
   * @param player the player
   * @param hand   the hand used
   * @param facing the face clicked
   * @param hitX   the x coordinate of the hit
   * @param hitY   the y coordinate of the hit
   * @param hitZ   the z coordinate of the hit
   *
   * @return true if the click was handled
   *
   * @since 1.0
   */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    TileEntity tileEntity = world.getTileEntity(pos);
    if (!(tileEntity instanceof TileEntityArrowBoard)) {
      return false;
    }
    TileEntityArrowBoard board = (TileEntityArrowBoard) tileEntity;
    if (!world.isRemote) {
      // Sneaking steps backwards. With seven modes, one-way cycling leaves the mode just behind
      // the current one six clicks away.
      ArrowBoardPattern next = player.isSneaking()
          ? board.getPattern().previous() : board.getPattern().next();
      board.setPattern(next);
      board.markDirtySync(world, pos, state);
      player.sendMessage(new TextComponentString(
          TextFormatting.YELLOW + "Arrow board: " + TextFormatting.WHITE + next.getFriendlyName()));
    }
    return true;
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityArrowBoard();
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityArrowBoard.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityworkzonearrowboard";
  }

}
