package com.micatechnologies.minecraft.csm.furniture.market.shop;

import com.micatechnologies.minecraft.csm.furniture.market.BlockDisplayCase;
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
 * A shop display case: the Market &amp; Store's glass-fronted counter case with its shelf bare,
 * the item it sells drawn behind the glass by {@link TileEntityShopRenderer}. Cases side by side
 * join as the other display cases do; its lights still follow redstone, but a click is the
 * shop's ({@link ShopBlocks}), so a sneaking click no longer switches them.
 *
 * @since 2026.10
 */
public class BlockShopDisplayCase extends BlockDisplayCase implements IShopBlock {

  private final ShopDisplay display;

  /**
   * Constructs a shop display case.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   * @param group        the cases it joins, or null for itself
   * @param display      where its item is drawn
   */
  public BlockShopDisplayCase(String registryName, int[] box, @Nullable String group,
      ShopDisplay display) {
    super(registryName, box, group);
    this.display = display;
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
