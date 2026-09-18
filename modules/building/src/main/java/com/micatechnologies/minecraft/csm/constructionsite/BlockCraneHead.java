package com.micatechnologies.minecraft.csm.constructionsite;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A tower crane head: placed on the top of a crane mast, it stands for everything above it.
 *
 * <p>The block draws nothing itself. Its tile entity's renderer draws the whole slewing unit --
 * ring, jib, counter-jib, cab, trolley and hook -- because on a 2x2 mast that unit is centred on
 * the section, not on whichever quarter the head was put on, and because the jib can be forty or
 * eighty blocks long and slew any way. See {@link TileEntityCraneHead}.</p>
 *
 * <p>It can only be placed on a mast top. The block itself has no collision: the deck, walkways
 * and cab are made solid by {@link CraneCollision}, which follows the slew and reaches the whole
 * length of the jib, where a block's collision could not.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockCraneHead extends AbstractBlock implements ICsmTileEntityProvider {

  /**
   * Constructs a {@link BlockCraneHead}.
   *
   * @since 1.0
   */
  public BlockCraneHead() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 3F, 12F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "crane_head";
  }

  /**
   * Only on the top of a crane mast.
   *
   * @since 1.0
   */
  @Override
  public boolean canPlaceBlockAt(World worldIn, @Nonnull BlockPos pos) {
    return super.canPlaceBlockAt(worldIn, pos)
        && worldIn.getBlockState(pos.down()).getBlock() instanceof BlockCraneMast;
  }

  /**
   * Records the mast under the head: its size, the section centre for a 2x2, and its livery,
   * which a new head takes on.
   *
   * @since 1.0
   */
  @Override
  public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
    TileEntity te = worldIn.getTileEntity(pos);
    if (!(te instanceof TileEntityCraneHead)) {
      return;
    }
    BlockPos below = pos.down();
    IBlockState mast = worldIn.getBlockState(below);
    if (!(mast.getBlock() instanceof BlockCraneMast)) {
      return;
    }
    CraneLivery livery = mast.getValue(BlockCraneMast.LIVERY);
    if (mast.getBlock() instanceof BlockCraneMastLarge) {
      BlockCraneMastLarge.Corner corner = mast.getActualState(worldIn, below)
          .getValue(BlockCraneMastLarge.CORNER);
      double dx = corner == BlockCraneMastLarge.Corner.NW
          || corner == BlockCraneMastLarge.Corner.SW ? 0.5 : -0.5;
      double dz = corner == BlockCraneMastLarge.Corner.NW
          || corner == BlockCraneMastLarge.Corner.NE ? 0.5 : -0.5;
      ((TileEntityCraneHead) te).setMast(true, dx, dz, livery);
    } else {
      ((TileEntityCraneHead) te).setMast(false, 0.0, 0.0, livery);
    }
    ((TileEntityCraneHead) te).markDirtySync(worldIn, pos, true);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityCraneHead.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitycranehead";
  }

  @Override
  public TileEntity createNewTileEntity(@Nonnull World worldIn, int meta) {
    return new TileEntityCraneHead();
  }

  /**
   * The renderer draws the whole head; the block contributes nothing to the chunk mesh.
   *
   * @since 1.0
   */
  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public EnumBlockRenderType getRenderType(IBlockState state) {
    return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
  }

  /**
   * Opens the crane's configuration with an empty hand, from any block of the crane, so a crane
   * whose head is two hundred blocks up can be set from the foot of its mast. True on both sides,
   * or the server goes on to use the held item; the screen opens on the client.
   *
   * @since 1.0
   */
  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !playerIn.getHeldItem(hand).isEmpty()
        || CraneLocator.findHead(worldIn, pos) == null) {
      return false;
    }
    if (worldIn.isRemote) {
      playerIn.openGui(Csm.instance, BuildingGuiProvider.CRANE_GUI_ID, worldIn, pos.getX(),
          pos.getY(), pos.getZ());
    }
    return true;
  }

  /**
   * None: see {@link CraneCollision}.
   *
   * @since 1.0
   */
  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, @Nonnull World worldIn,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn,
      boolean isActualState) {
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nullable
  public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState,
      @Nonnull IBlockAccess worldIn, @Nonnull BlockPos pos) {
    return NULL_AABB;
  }

  @Override
  @Nonnull
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return SQUARE_BOUNDING_BOX;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
