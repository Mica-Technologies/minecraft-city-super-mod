package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockDynamicGuideSign extends AbstractBlockRotatableNSEW
    implements ICsmTileEntityProvider {

  public BlockDynamicGuideSign() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "dynamic_guide_sign";
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityDynamicGuideSign.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitydynamicguidesign";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityDynamicGuideSign();
  }

  @SideOnly(Side.CLIENT)
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing,
      float hitX, float hitY, float hitZ) {
    player.openGui(Csm.instance, 14, world, pos.getX(), pos.getY(), pos.getZ());
    return true;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    // The sign panel is rendered as a 1.5-pixel-thick slab against one face of the block,
    // determined by BlockHorizontal.FACING after the renderer's rotation. Match the bbox to
    // that slab so the selection outline (and collision) reflects what the player sees.
    EnumFacing facing = state.getValue(BlockHorizontal.FACING);
    final double t = 1.5 / 16.0;
    switch (facing) {
      case SOUTH:
        return new AxisAlignedBB(0.0, 0.0, 1.0 - t, 1.0, 1.0, 1.0);
      case NORTH:
        return new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, t);
      case WEST:
        return new AxisAlignedBB(1.0 - t, 0.0, 0.0, 1.0, 1.0, 1.0);
      case EAST:
        return new AxisAlignedBB(0.0, 0.0, 0.0, t, 1.0, 1.0);
      default:
        return new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
    }
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
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      EnumFacing facing) {
    return false;
  }
}
