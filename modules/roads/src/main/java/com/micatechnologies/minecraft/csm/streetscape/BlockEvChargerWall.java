package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTrafficPoleIgnored;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The wall-mount EV charger (#315), for garage walls and columns: one block standing in the cell
 * in front of the wall, its back against the wall. Placed against a wall it faces the player, so
 * it hangs on the face that was clicked.
 *
 * <p>Its status light is {@link EvChargerStatus}'s, as on the free-standing chargers: unpowered
 * available, powered charging, an empty-handed right-click sets or clears a fault. It does not
 * settle onto a surface below, as a wall device has none.</p>
 *
 * @since 2026.10
 */
public class BlockEvChargerWall extends AbstractBlockRotatableNSEW
    implements ICsmTrafficPoleIgnored {

  /** Carries the registry name to the superclass constructor, which asks for it first. */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB box;

  /**
   * @param registryName the registry name
   * @param box          the charger's box facing north, in block space
   */
  public BlockEvChargerWall(String registryName, AxisAlignedBB box) {
    super(initRegistryName(registryName), SoundType.METAL, "pickaxe", 1, 2.0F, 10.0F, 0F, 0);
    this.registryName = registryName;
    this.box = box;
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
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, EvChargerStatus.PROPERTY);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return EvChargerStatus.toMeta(state, super.getMetaFromState(state));
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3)
        .withProperty(EvChargerStatus.PROPERTY, EvChargerStatus.fromMeta(meta));
  }

  @Override
  public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
    super.onBlockAdded(world, pos, state);
    EvChargerStatus.refresh(world, pos, state);
  }

  @Override
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, blockIn, fromPos);
    EvChargerStatus.refresh(world, pos, world.getBlockState(pos));
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    EvChargerStatus.toggleFault(world, pos, state, player);
    return true;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return box;
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
    return true;
  }

  @Nonnull
  @Override
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }
}
