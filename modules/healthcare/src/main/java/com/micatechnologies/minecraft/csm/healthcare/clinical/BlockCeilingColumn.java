package com.micatechnologies.minecraft.csm.healthcare.clinical;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcare;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import java.util.Random;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.entity.player.EntityPlayer;

/**
 * A cell of a ceiling-hung piece's drop column ({@code ceiling_boom_column}, hidden): a stainless
 * tube, with the ceiling flange on the top cell ({@link #TOP}, actual state: the block above is
 * not a column). Any metadata; it stores nothing. See {@link CeilingHung}.
 *
 * @since 2026.10
 */
public class BlockCeilingColumn extends BlockHealthcare {

  /** The top cell, under the ceiling: drawn with the ceiling flange. */
  public static final PropertyBool TOP = PropertyBool.create("top");

  private static final IProperty<?>[] ACTUAL = {TOP};

  private static Block instance;

  /** Constructs the column. */
  public BlockCeilingColumn() {
    super("ceiling_boom_column", new int[]{5, 0, 5, 11, 16, 11}, HealthcareMaterial.METAL,
        Mount.FLOOR, true);
    instance = this;
  }

  /**
   * The column block.
   *
   * @return the registered column
   */
  public static Block get() {
    return instance != null ? instance : CsmRegistry.getBlock("ceiling_boom_column");
  }

  @Override
  protected IProperty<?>[] actualProperties() {
    return ACTUAL;
  }

  @Override
  protected IBlockState defaultExtras(IBlockState state) {
    return state.withProperty(TOP, false);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos) {
    return super.getActualState(state, world, pos).withProperty(TOP,
        !(world.getBlockState(pos.up()).getBlock() instanceof BlockCeilingColumn));
  }

  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
    CeilingHung.takeDown(world, pos, false);
    super.breakBlock(world, pos, state);
  }

  @Override
  @Nonnull
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return Items.AIR;
  }

  @Override
  @Nonnull
  public ItemStack getPickBlock(@Nonnull IBlockState state, RayTraceResult target,
      @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player) {
    BlockPos head = CeilingHung.headBelow(world, pos);
    return head != null ? world.getBlockState(head).getBlock().getPickBlock(
        world.getBlockState(head), target, world, head, player) : ItemStack.EMPTY;
  }
}
