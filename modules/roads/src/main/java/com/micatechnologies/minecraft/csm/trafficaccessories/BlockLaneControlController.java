package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficsignals.ItemSignalLinkTool;
import javax.annotation.Nullable;
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
 * The cabinet that runs reversible lanes: groups of lane control signals, each showing an aspect
 * per time-of-day slot.
 *
 * <p>Its own block rather than a mode on the signal controller, because a reversible lane is a
 * clock decision and shares no machinery with phases, rings and barriers. It borrows the shape of
 * the signal controller — a linked-device list kept as positions, the same
 * {@code ItemSignalLinkTool}, a configuration GUI — and none of its code.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public class BlockLaneControlController extends AbstractBlockRotatableNSEW
    implements ICsmTileEntityProvider {

  public static final int GUI_ID = 24;

  public BlockLaneControlController() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "lane_control_controller";
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityLaneControlController.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitylanecontrolcontroller";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityLaneControlController();
  }

  /** A full cabinet, like the signal controller it stands beside. */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return FULL_BLOCK_AABB;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return true;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return true;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      EnumFacing facing) {
    return false;
  }

  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }

  /**
   * Opens the configuration GUI, unless the player is holding the signal link tool.
   *
   * <p>A block's {@code onBlockActivated} runs before the held item's {@code onItemUse}, so
   * without the exemption the controller could never be selected for linking — the GUI would
   * open over the top of it every time.</p>
   */
  @SideOnly(Side.CLIENT)
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing,
      float hitX, float hitY, float hitZ) {
    if (player.getHeldItem(hand).getItem() instanceof ItemSignalLinkTool) {
      return false;
    }
    player.openGui(Csm.instance, GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
    return true;
  }
}
