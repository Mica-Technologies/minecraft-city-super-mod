package com.micatechnologies.minecraft.csm.lighting;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Factory for the stackable chain that extends a decorative pendant's drop. Purely structural --
 * it carries no lighting logic, so it deliberately does NOT extend {@link AbstractBrightLight}: a
 * chain link that emitted light, answered redstone and toggled on right-click would be wrong in
 * every one of those ways.
 * <p>
 * One instance per metal finish, matching the pendant finishes, so a bronze pendant is not left
 * hanging from a black chain.
 *
 * @since 2026.8
 */
public class BlockDecorativeChainFactory extends AbstractBlock {

  /**
   * ThreadLocal used to pass the registry name to the superclass constructor. The AbstractBlock
   * constructor calls getBlockRegistryName() before subclass fields are initialized, so we store
   * the name here before calling super() and read it in getBlockRegistryName().
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  /**
   * The links overhang the block slightly at both ends so a stack reads as one continuous chain
   * rather than four separate rings per block.
   */
  private static final AxisAlignedBB CHAIN_BOX =
      new AxisAlignedBB(0.340371, 0.0, 0.340371, 0.659629, 1.0, 0.659629);

  private final String registryName;

  public BlockDecorativeChainFactory(String registryName) {
    this(initRegistryName(registryName), registryName);
  }

  private BlockDecorativeChainFactory(Void ignored, String registryName) {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 0);
    this.registryName = registryName;
  }

  private static Void initRegistryName(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return null;
  }

  @Override
  public String getBlockRegistryName() {
    if (registryName != null) {
      return registryName;
    }
    return PENDING_REGISTRY_NAME.get();
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return CHAIN_BOX;
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
  public boolean getBlockConnectsRedstone(IBlockState state,
      IBlockAccess access,
      BlockPos pos,
      @Nullable
      EnumFacing facing) {
    return false;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }
}
