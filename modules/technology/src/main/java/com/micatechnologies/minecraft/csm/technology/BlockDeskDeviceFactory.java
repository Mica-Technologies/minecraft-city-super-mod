package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
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
 * Factory for decorative devices that sit on a desk or a shelf: a closed laptop, a keyboard, a
 * small desktop computer. They turn to face whoever places them and never stand on end, which
 * is why they are not {@code BlockRotatableNSEWUDFactory}: that one faces up when placed from
 * above, as anything set down on a desk is.
 *
 * <p>Instances differ only in registry name and box, so they need no class of their own.</p>
 *
 * @since 2026.9
 */
public class BlockDeskDeviceFactory extends AbstractBlockRotatableNSEW {

  /**
   * The registry name, for the {@code AbstractBlock} constructor, which asks for it before this
   * class's fields are set.
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB boundingBox;

  /**
   * @param registryName the block's registry name
   * @param box          the device's box facing north, in sixteenths: x0, y0, z0, x1, y1, z1
   */
  public BlockDeskDeviceFactory(String registryName, double[] box) {
    super(initRegistryName(registryName), SoundType.METAL, "pickaxe", 1, 1F, 10F, 0F, 0);
    this.registryName = registryName;
    this.boundingBox = new AxisAlignedBB(box[0] / 16, box[1] / 16, box[2] / 16, box[3] / 16,
        box[4] / 16, box[5] / 16);
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material initRegistryName(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return boundingBox;
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
