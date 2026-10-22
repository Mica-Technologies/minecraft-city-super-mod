package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

/**
 * A detector in two finishes, white and black, as one block (the user's request, 2026-10-22):
 * the finish is stored above the facing (metadata 0-5 white, 6-11 black), so every detector
 * placed before the black one existed stays white, and carried in the item's damage, so each
 * finish is its own creative stack. The blockstate swaps the textures by {@code black}.
 *
 * @since 2026.10
 */
public class BlockFireAlarmSmokeDetector extends BlockFireAlarmDetectorFactory {

  public static final PropertyBool BLACK = PropertyBool.create("black");

  public BlockFireAlarmSmokeDetector(String registryName, AxisAlignedBB boundingBox) {
    super(registryName, boundingBox);
  }

  @Nonnull
  @Override
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, BLACK);
  }

  @Nonnull
  @Override
  public IBlockState getStateFromMeta(int meta) {
    boolean black = meta >= 6 && meta < 12;
    return getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta % 6))
        .withProperty(BLACK, black);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getIndex() + (state.getValue(BLACK) ? 6 : 0);
  }

  @Nonnull
  @Override
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(BLACK, meta == 1);
  }

  @Override
  public int damageDropped(IBlockState state) {
    return state.getValue(BLACK) ? 1 : 0;
  }

  @Nonnull
  @Override
  public ItemStack getPickBlock(@Nonnull IBlockState state, @Nonnull RayTraceResult target,
      @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player) {
    return new ItemStack(this, 1, damageDropped(state));
  }

  @Override
  public void getSubBlocks(@Nonnull CreativeTabs tab, @Nonnull NonNullList<ItemStack> items) {
    items.add(new ItemStack(this, 1, 0));
    items.add(new ItemStack(this, 1, 1));
  }

  @Override
  protected ItemBlock createItemBlock() {
    return new ItemBlockFinish(this);
  }

  @Override
  public void registerModels() {
    Item item = Item.getItemFromBlock(this);
    Csm.proxy.setCustomModelResourceLocation(item, 0, "inventory_white");
    Csm.proxy.setCustomModelResourceLocation(item, 1, "inventory_black");
  }

  /** The item: damage 1 is the black finish, named {@code tile.<name>.black.name}. */
  private static final class ItemBlockFinish extends ItemBlock {

    ItemBlockFinish(Block block) {
      super(block);
      setHasSubtypes(true);
      setMaxDamage(0);
    }

    @Override
    public int getMetadata(int damage) {
      return damage;
    }

    @Nonnull
    @Override
    public String getTranslationKey(@Nonnull ItemStack stack) {
      return super.getTranslationKey(stack) + (stack.getMetadata() == 1 ? ".black" : "");
    }
  }
}
