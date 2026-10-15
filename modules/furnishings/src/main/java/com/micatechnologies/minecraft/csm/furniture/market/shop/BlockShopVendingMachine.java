package com.micatechnologies.minecraft.csm.furniture.market.shop;

import com.micatechnologies.minecraft.csm.codeutils.ICsmGlassFronted;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialTall;
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
 * A vending machine that is a shop: two blocks tall, its glass front showing rows of the item it
 * sells ({@link TileEntityShopRenderer}), lit from inside. The shop is held in the lower half.
 * A click is the shop's ({@link ShopBlocks}).
 *
 * @since 2026.10
 */
public class BlockShopVendingMachine extends BlockResidentialTall implements IShopBlock,
    ICsmGlassFronted {

  /** The light its front gives. */
  public static final int LIGHT = 9;

  private final ShopDisplay display;

  /**
   * Constructs a vending machine.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths from the floor (up to 32)
   * @param display      where its item is drawn, from the floor of the lower half
   */
  public BlockShopVendingMachine(String registryName, int[] box, ShopDisplay display) {
    super(registryName, box, false, TileEntityShop.STOCK_SLOTS, null, null);
    this.display = display;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return LIGHT;
  }

  @Override
  public int getLightValue(@Nonnull IBlockState state, IBlockAccess world,
      @Nonnull BlockPos pos) {
    return LIGHT;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.TRANSLUCENT;
  }

  @Override
  public ShopDisplay getShopDisplay() {
    return display;
  }

  @Override
  public BlockPos getShopPos(IBlockAccess world, BlockPos pos) {
    return getStoragePos(world, pos);
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

  /**
   * The checks come first: the game calls this before {@link #removedByPlayer}, and breaking
   * either half here clears the other, so a refused break must not get this far.
   */
  @Override
  public void onBlockHarvested(World world, BlockPos pos, IBlockState state,
      EntityPlayer player) {
    if (ShopBlocks.mayBreak(world, pos, player, this)) {
      super.onBlockHarvested(world, pos, state, player);
    }
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
    return !state.getValue(UPPER);
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
    return (meta & 4) != 0 ? null : new TileEntityShop();
  }
}
