package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Block for overheight detection sensors. Two of these are placed on opposite sides of a road and
 * paired together by right-clicking one then the other. They form a detection barrier between them
 * that extends {@link TileEntityOverheightDetectionSensor#DETECTION_HEIGHT} blocks upward. Used
 * with the {@link TrafficSignalControllerMode#OVERHEIGHT_DETECTION OVERHEIGHT_DETECTION} controller
 * mode to activate beacons when an overheight entity is detected.
 *
 * <p>Pairing workflow: right-click the first sensor, then right-click the second. Both sensors
 * are linked bidirectionally. Right-click a paired sensor while sneaking to unpair.</p>
 *
 * @version 1.0
 * @since 2024.1.0
 */
public class BlockOverheightDetectionSensor extends AbstractBlockRotatableNSEW
    implements ICsmTileEntityProvider {

  /**
   * Per-player map tracking the first sensor selected for pairing.
   */
  private static final Map<UUID, BlockPos> pairingFirstSensor = new HashMap<>();

  public BlockOverheightDetectionSensor() {
    super(Material.ROCK);
  }

  @Override
  public String getBlockRegistryName() {
    return "overheight_detection_sensor";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return new AxisAlignedBB(0.354956, -1.000000, 0.062500, 0.645044, 1.851563, 1.000000);
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

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityOverheightDetectionSensor.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityoverheightdetectionsensor";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityOverheightDetectionSensor();
  }

  /**
   * Handles right-click interaction for pairing/unpairing sensors. Passes through to item
   * interaction (e.g. signal linker tool, sensor zone tool) when the player is holding one of
   * those tools.
   */
  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
      float hitX, float hitY, float hitZ) {
    if (worldIn.isRemote || hand != EnumHand.MAIN_HAND) {
      return true;
    }

    // Pass through to item interaction when holding a tool that needs to handle the click
    // (onBlockActivated runs before onItemUse in 1.12.2 when not sneaking)
    if (playerIn.getHeldItem(hand).getItem() instanceof ItemSignalLinkTool ||
        playerIn.getHeldItem(hand).getItem() instanceof ItemSensorZoneTool) {
      return false;
    }

    TileEntity te = worldIn.getTileEntity(pos);
    if (!(te instanceof TileEntityOverheightDetectionSensor)) {
      return false;
    }
    TileEntityOverheightDetectionSensor sensor = (TileEntityOverheightDetectionSensor) te;

    // Sneak-click to unpair
    if (playerIn.isSneaking()) {
      if (sensor.isPaired()) {
        // Unpair the partner as well
        TileEntity partnerTE = worldIn.getTileEntity(sensor.getPartnerPos());
        if (partnerTE instanceof TileEntityOverheightDetectionSensor) {
          ((TileEntityOverheightDetectionSensor) partnerTE).setPartnerPos(null);
        }
        sensor.setPartnerPos(null);
        playerIn.sendMessage(new TextComponentString("Overheight sensor unpaired."));
      } else {
        playerIn.sendMessage(new TextComponentString("Sensor is not paired."));
      }
      // Clear any pending pairing selection
      pairingFirstSensor.remove(playerIn.getUniqueID());
      return true;
    }

    // Normal click — pairing workflow
    UUID playerId = playerIn.getUniqueID();
    BlockPos firstPos = pairingFirstSensor.get(playerId);

    if (firstPos == null) {
      // First click — select this sensor
      pairingFirstSensor.put(playerId, pos);
      playerIn.sendMessage(new TextComponentString(
          "Overheight sensor selected at (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() +
              "). Right-click the partner sensor to complete pairing."));
    } else if (firstPos.equals(pos)) {
      // Clicked the same sensor again — deselect
      pairingFirstSensor.remove(playerId);
      playerIn.sendMessage(new TextComponentString("Pairing selection cleared."));
    } else {
      // Second click — pair the two sensors
      TileEntity firstTE = worldIn.getTileEntity(firstPos);
      if (!(firstTE instanceof TileEntityOverheightDetectionSensor)) {
        playerIn.sendMessage(new TextComponentString(
            "First sensor is no longer valid. Pairing cancelled."));
        pairingFirstSensor.remove(playerId);
        return true;
      }

      TileEntityOverheightDetectionSensor firstSensor =
          (TileEntityOverheightDetectionSensor) firstTE;

      // Unpair any previous partners
      if (firstSensor.isPaired()) {
        TileEntity oldPartner = worldIn.getTileEntity(firstSensor.getPartnerPos());
        if (oldPartner instanceof TileEntityOverheightDetectionSensor) {
          ((TileEntityOverheightDetectionSensor) oldPartner).setPartnerPos(null);
        }
      }
      if (sensor.isPaired()) {
        TileEntity oldPartner = worldIn.getTileEntity(sensor.getPartnerPos());
        if (oldPartner instanceof TileEntityOverheightDetectionSensor) {
          ((TileEntityOverheightDetectionSensor) oldPartner).setPartnerPos(null);
        }
      }

      // Set bidirectional pairing
      firstSensor.setPartnerPos(pos);
      sensor.setPartnerPos(firstPos);

      pairingFirstSensor.remove(playerId);
      playerIn.sendMessage(new TextComponentString(
          "Overheight sensors paired! Detection zone spans between (" +
              firstPos.getX() + ", " + firstPos.getY() + ", " + firstPos.getZ() + ") and (" +
              pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "), " +
              TileEntityOverheightDetectionSensor.DETECTION_HEIGHT + " blocks high."));
    }

    return true;
  }

  /**
   * When a sensor is broken, unpair it from its partner.
   */
  @Override
  public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
    TileEntity te = worldIn.getTileEntity(pos);
    if (te instanceof TileEntityOverheightDetectionSensor) {
      TileEntityOverheightDetectionSensor sensor = (TileEntityOverheightDetectionSensor) te;
      if (sensor.isPaired()) {
        TileEntity partnerTE = worldIn.getTileEntity(sensor.getPartnerPos());
        if (partnerTE instanceof TileEntityOverheightDetectionSensor) {
          ((TileEntityOverheightDetectionSensor) partnerTE).setPartnerPos(null);
        }
      }
    }
    super.breakBlock(worldIn, pos, state);
  }
}
