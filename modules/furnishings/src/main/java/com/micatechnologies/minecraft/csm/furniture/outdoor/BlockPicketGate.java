package com.micatechnologies.minecraft.csm.furniture.outdoor;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.CsmConstants;
import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.ICsmBlock;
import com.micatechnologies.minecraft.csm.codeutils.IHasModel;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The white picket fence's gate: a vanilla fence gate in every way -- fences join it, a click
 * opens it away from whoever clicks and closes it, redstone opens it, and open it is walked
 * through -- drawn as one picket leaf on its hinges, swung a quarter turn about them when open
 * ({@code gen_furniture_outdoor.py}). Registered as the mod's blocks are, under {@code csm:}.
 *
 * @since 2026.9
 */
public class BlockPicketGate extends BlockFenceGate implements IHasModel, ICsmBlock {

  private final String registryName;

  /**
   * Constructs a picket gate.
   *
   * @param registryName its registry name, ending in its paint
   */
  public BlockPicketGate(String registryName) {
    super(BlockPlanks.EnumType.BIRCH);
    this.registryName = registryName;
    setTranslationKey(registryName);
    setRegistryName(CsmConstants.MOD_NAMESPACE, registryName);
    setSoundType(SoundType.WOOD);
    setHardness(2.0F);
    setResistance(5.0F);
    setHarvestLevel("axe", 0);
    CsmRegistry.registerBlock(this);
    CsmRegistry.registerItem(
        new ItemBlock(this).setRegistryName(Objects.requireNonNull(getRegistryName())));
  }

  @Override
  public void registerModels() {
    Csm.proxy.setCustomModelResourceLocation(Item.getItemFromBlock(this), 0, "inventory");
  }

  @Override
  public String getBlockRegistryName() {
    return registryName;
  }

  /** Unused: a fence gate's own boxes are kept. */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return null;
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

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }
}
