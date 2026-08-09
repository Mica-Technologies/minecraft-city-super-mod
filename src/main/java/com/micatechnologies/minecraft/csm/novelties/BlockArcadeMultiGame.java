package com.micatechnologies.minecraft.csm.novelties;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The multi-game arcade cabinet: a playable machine carrying every title in {@link ArcadeCatalog}.
 * Right-clicking opens a select screen listing the installed games with this cabinet's record for
 * each; sneak-right-clicking just plays the attract sound, so it can be used as scenery without
 * opening a screen.
 *
 * <p>The decorative cabinets elsewhere in this package are unchanged — this block exists so the new
 * games have a home of their own rather than being installed over them. It shares their model and
 * footprint and differs only in its textures.</p>
 *
 * @author Mica Technologies
 * @since 2026.8
 */
public class BlockArcadeMultiGame extends AbstractBlockRotatableNSEW
    implements ICsmTileEntityProvider {

  /**
   * The GUI ID used to open the arcade screen. Distinct from every other GUI in
   * {@code CsmGuiHandler}.
   *
   * @since 1.0
   */
  public static final int GUI_ID = 21;

  /**
   * Constructs a {@link BlockArcadeMultiGame} with the shared cabinet material properties.
   *
   * @since 1.0
   */
  public BlockArcadeMultiGame() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 1F, 0);
  }

  /**
   * Retrieves the registry name of the block.
   *
   * @return the registry name of the block
   *
   * @since 1.0
   */
  @Override
  public String getBlockRegistryName() {
    return "arcademultigame";
  }

  /**
   * Retrieves the bounding box of the block. The cabinet model occupies exactly one column, two
   * blocks tall, and stops at the cell boundary so there is no invisible wall where the player
   * stands to use it.
   *
   * @param state  the block state
   * @param source the block access
   * @param pos    the block position
   *
   * @return the bounding box of the block
   *
   * @since 1.0
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 2.000000, 1.000000);
  }

  /**
   * Retrieves whether the block is an opaque cube.
   *
   * @param state the block state
   *
   * @return {@code false}; the cabinet is not a full opaque cube
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  /**
   * Retrieves whether the block is a full cube.
   *
   * @param state the block state
   *
   * @return {@code false}; the cabinet is taller than one block and does not fill its own cell
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  /**
   * Retrieves whether the block connects to redstone.
   *
   * @param state  the block state
   * @param access the block access
   * @param pos    the block position
   * @param facing the block facing direction
   *
   * @return {@code false}; the cabinet has no redstone behaviour
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockConnectsRedstone(IBlockState state,
      IBlockAccess access,
      BlockPos pos,
      @Nullable
      EnumFacing facing) {
    return false;
  }

  /**
   * Retrieves the block's render layer.
   *
   * @return the block's render layer
   *
   * @since 1.0
   */
  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }

  /**
   * Handles right-clicking the cabinet. A plain click opens the game select screen; sneaking plays
   * the attract sound without opening it.
   *
   * @param world  the world
   * @param pos    the block position
   * @param state  the block state
   * @param player the interacting player
   * @param hand   the hand used
   * @param facing the face clicked
   * @param hitX   the X coordinate of the hit
   * @param hitY   the Y coordinate of the hit
   * @param hitZ   the Z coordinate of the hit
   *
   * @return {@code true}; the click is always consumed
   *
   * @since 1.0
   */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }

    if (!world.isRemote) {
      // A vanilla mechanical click stands in for the coin door. The decorative cabinets each have
      // their own recorded attract loop; this one deliberately ships no new audio.
      world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.6F, 1.4F);
    }

    if (player.isSneaking()) {
      return true;
    }

    // Matches the powered-computer pattern: call openGui unguarded on both sides. Client-side it
    // dispatches straight through the GUI handler; server-side it sends the open-GUI packet.
    player.openGui(Csm.instance, GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
    return true;
  }

  /**
   * Retrieves the tile entity class backing the cabinet.
   *
   * @return {@link TileEntityArcadeCabinet}
   *
   * @since 1.0
   */
  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityArcadeCabinet.class;
  }

  /**
   * Retrieves the registered name of the arcade cabinet tile entity.
   *
   * @return the tile entity name
   *
   * @since 1.0
   */
  @Override
  public String getTileEntityName() {
    return "tileentityarcadecabinet";
  }

  /**
   * Creates the tile entity backing this cabinet.
   *
   * @param worldIn the world
   * @param meta    the block meta value
   *
   * @return a new {@link TileEntityArcadeCabinet}
   *
   * @since 1.0
   */
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityArcadeCabinet();
  }
}
