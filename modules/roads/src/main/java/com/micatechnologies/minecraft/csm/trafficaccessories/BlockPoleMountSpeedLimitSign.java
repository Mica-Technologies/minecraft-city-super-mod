package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
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

/**
 * Pole-mounted electronic speed limit sign — same display/data model as
 * {@link BlockOverheadSpeedLimitSign} (variable speed + housing color + full-screen
 * toggle) but rendered at the smaller panel size used by the portable trailer
 * variant, with a stubby mounting arm rather than a full overhead housing. Intended
 * to be placed next to a pole at the side of the road.
 */
public class BlockPoleMountSpeedLimitSign extends AbstractBlockRotatableNSEW
    implements ICsmTileEntityProvider {

  public BlockPoleMountSpeedLimitSign() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "polemount_speed_limit_sign";
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityPoleMountSpeedLimit.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitypolemountspeedlimit";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityPoleMountSpeedLimit();
  }

  @SideOnly(Side.CLIENT)
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing,
      float hitX, float hitY, float hitZ) {
    player.openGui(Csm.instance, 18, world, pos.getX(), pos.getY(), pos.getZ());
    return true;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    // Thin slab against the cell's back face — the panel itself only occupies
    // Z=13..16 model units (0.8125..1.0 block coords). The panel mounts directly to
    // whatever block sits behind, so no extra room is needed for bracket geometry.
    return new AxisAlignedBB(0.0, 0.0, 0.8125, 1.0, 1.0, 1.0);
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
