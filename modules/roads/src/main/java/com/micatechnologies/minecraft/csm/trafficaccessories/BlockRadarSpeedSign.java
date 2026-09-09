package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficsignals.ItemSensorZoneTool;
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
 * Radar speed feedback sign: the "YOUR SPEED 32" board that measures an approaching vehicle and
 * shows its speed back to it.
 *
 * <p>Right-click opens its configuration — the posted speed, the calibration, the panel size and
 * colour, and whether it carries a SPEED LIMIT header. Right-click <em>with the sensor zone
 * tool</em> instead hands the click to the tool, so the sign's detection zone is programmed the
 * same way a signal sensor's is.</p>
 *
 * <p>The block emits redstone while the reading is over the posted speed, which is the hook for
 * wiring one to a gate, a beacon or anything else a player wants to trip.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public class BlockRadarSpeedSign extends AbstractBlockRotatableNSEW
    implements ICsmTileEntityProvider {

  public static final int GUI_ID = 21;

  public BlockRadarSpeedSign() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "radar_speed_sign";
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityRadarSpeedSign.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityradarspeedsign";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityRadarSpeedSign();
  }

  /**
   * Opens the configuration GUI, unless the player is holding the sensor zone tool.
   *
   * <p>A block's {@code onBlockActivated} runs before the held item's {@code onItemUse}, so
   * without the exemption a sign could never be selected for zone programming — the GUI would
   * open over the top of it every time.</p>
   */
  @SideOnly(Side.CLIENT)
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing,
      float hitX, float hitY, float hitZ) {
    if (player.getHeldItem(hand).getItem() instanceof ItemSensorZoneTool) {
      return false;
    }
    player.openGui(Csm.instance, GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
    return true;
  }

  /**
   * A thin slab against the cell's back face, matching the panel the renderer draws there. The
   * larger panel scales reach outside this, but the hitbox stays on the block a player actually
   * clicks — the same choice the school zone assembly makes.
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
    return true;
  }

  @Override
  public boolean canProvidePower(IBlockState state) {
    return true;
  }

  /**
   * Full power out of every side while the measured speed is over the posted limit.
   *
   * <p>Read from the tile entity rather than from a block state property: the reading changes
   * several times a second while someone drives past, and a state property would mean a block
   * update on every one of those.</p>
   */
  @Override
  public int getWeakPower(IBlockState state, IBlockAccess access, BlockPos pos,
      EnumFacing side) {
    TileEntity tileEntity = access.getTileEntity(pos);
    if (tileEntity instanceof TileEntityRadarSpeedSign
        && ((TileEntityRadarSpeedSign) tileEntity).isOverLimit()) {
      return 15;
    }
    return 0;
  }
}
