package com.micatechnologies.minecraft.csm.transit.station;

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
 * The fare line's service gate: a vanilla fence gate in every way -- the station railings join
 * it, a click opens it away from whoever clicks and closes it, redstone power opens it, and open
 * it is walked through -- drawn as a stainless leaf with a SERVICE GATE plate between a hinge post
 * and a latch post, swung a quarter about its hinge when open ({@code gen_transit_stations.py}).
 * Nobody checks a fare at it: which side is paid is the builder's business.
 *
 * @since 2026.9
 */
public class BlockStationGate extends BlockFenceGate implements IHasModel, ICsmBlock {

  private final String registryName;

  /**
   * Constructs a service gate.
   *
   * @param registryName its registry name
   */
  public BlockStationGate(String registryName) {
    super(BlockPlanks.EnumType.OAK);
    this.registryName = registryName;
    setTranslationKey(registryName);
    setRegistryName(CsmConstants.MOD_NAMESPACE, registryName);
    setSoundType(SoundType.METAL);
    setHardness(3.0F);
    setResistance(10.0F);
    setHarvestLevel("pickaxe", 1);
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
