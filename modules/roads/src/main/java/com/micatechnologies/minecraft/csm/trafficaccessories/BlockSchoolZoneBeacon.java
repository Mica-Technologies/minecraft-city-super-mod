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
 * School zone speed limit assembly: a fluorescent yellow-green SCHOOL SPEED LIMIT panel with
 * amber beacons that flash during the hours the zone is posted for.
 *
 * <p>Unlike the beacons in the signal system this one answers to nobody — it runs off the world
 * clock rather than a controller, which is how a real school zone assembly works. Right-click
 * opens its configuration: the posted speed, the panel size, whether it carries one beacon bar
 * or two, and the two windows it flashes in.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public class BlockSchoolZoneBeacon extends AbstractBlockRotatableNSEW
    implements ICsmTileEntityProvider {

  public BlockSchoolZoneBeacon() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "school_zone_beacon";
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntitySchoolZoneBeacon.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityschoolzonebeacon";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntitySchoolZoneBeacon();
  }

  @SideOnly(Side.CLIENT)
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing,
      float hitX, float hitY, float hitZ) {
    player.openGui(Csm.instance, 19, world, pos.getX(), pos.getY(), pos.getZ());
    return true;
  }

  /**
   * A thin slab against the cell's back face, matching the panel the renderer draws there. The
   * beacon bars and the larger panel scales reach well outside this, but the hitbox stays on the
   * block a player actually clicks to break — the same choice the pole-mount speed limit sign
   * makes.
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
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
