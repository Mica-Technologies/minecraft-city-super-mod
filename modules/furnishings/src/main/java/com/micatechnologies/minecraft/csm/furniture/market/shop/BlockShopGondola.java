package com.micatechnologies.minecraft.csm.furniture.market.shop;

import com.micatechnologies.minecraft.csm.furniture.market.BlockGondola;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockBookcase;
import com.micatechnologies.minecraft.csm.furniture.residential.ResidentialStorageHelper;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.world.IBlockAccess;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

/**
 * Shop gondola shelving, and the shop end cap: the Market &amp; Store gondola with its shelves
 * bare, the item the shop sells drawn on them by {@link TileEntityShopRenderer} and its price on
 * a label at the shelf edge. It joins and stacks with every gondola and end cap, stocked or a
 * shop, so a shop shelf sits in a run of ordinary shelving as one more bay.
 *
 * <p>It is a gondola in looks only: it extends the bookcase as {@link BlockGondola} does, without
 * the sale tags (a click here is the shop's, {@link ShopBlocks}).</p>
 *
 * @since 2026.10
 */
public class BlockShopGondola extends BlockBookcase implements ICsmTileEntityProvider,
    IShopBlock {

  private final ShopDisplay display;

  /**
   * Constructs a shop gondola.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   * @param display      where its item is drawn
   */
  public BlockShopGondola(String registryName, int[] box, ShopDisplay display) {
    super(registryName, box);
    this.display = display;
  }

  @Override
  protected boolean continues(IBlockAccess world, BlockPos pos, EnumFacing facing,
      EnumFacing side) {
    IBlockState other = world.getBlockState(pos.offset(side));
    return (other.getBlock() instanceof BlockGondola
        || other.getBlock() instanceof BlockShopGondola) && other.getValue(FACING) == facing;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }

  /** What it held drops where it stood. */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    ResidentialStorageHelper.dropContents(world, pos);
    super.breakBlock(world, pos, state);
  }

  @Override
  public ShopDisplay getShopDisplay() {
    return display;
  }

  // --- the shop ------------------------------------------------------------------------

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    return ShopBlocks.activate(world, pos, player, hand, this);
  }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    ShopBlocks.placed(world, pos, this, placer);
  }

  @Override
  public boolean removedByPlayer(@Nonnull IBlockState state, World world, @Nonnull BlockPos pos,
      @Nonnull EntityPlayer player, boolean willHarvest) {
    return ShopBlocks.mayBreak(world, pos, player, this)
        && super.removedByPlayer(state, world, pos, player, willHarvest);
  }

  @Override
  @SuppressWarnings("deprecation")
  public float getPlayerRelativeBlockHardness(@Nonnull IBlockState state,
      @Nonnull EntityPlayer player, @Nonnull World world, @Nonnull BlockPos pos) {
    return ShopBlocks.mayDig(world, pos, player, this)
        ? super.getPlayerRelativeBlockHardness(state, player, world, pos) : 0.0F;
  }

  @Override
  public float getExplosionResistance(World world, BlockPos pos, @Nullable Entity exploder,
      Explosion explosion) {
    return ShopBlocks.RESISTANCE;
  }

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityShop.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityshop";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityShop();
  }
}
