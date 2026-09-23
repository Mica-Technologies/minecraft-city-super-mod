package com.micatechnologies.minecraft.csm.lifesafety.fireprotection;

import com.micatechnologies.minecraft.csm.lifesafety.TileEntityFireAlarmControlPanel;
import com.micatechnologies.minecraft.csm.lifesafety.TileEntityFireAlarmSensor;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * A remote annunciator: the small panel by the front door that tells a firefighter what the main
 * panel says without walking to it. Its lamp lights while the panel is in alarm (the model reads
 * {@link #ALARM}); right-click reads out the panel's status and where the alarm came from.
 *
 * @since 2026.9
 */
public class BlockRemoteAnnunciator extends AbstractBlockPanelFollower {

  public BlockRemoteAnnunciator(String registryName, int[] box) {
    super(registryName, box);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    if (world.isRemote) {
      return true;
    }
    TileEntity te = world.getTileEntity(pos);
    BlockPos panelPos = te instanceof TileEntityFireAlarmSensor
        ? ((TileEntityFireAlarmSensor) te).getLinkedPanelPos(world) : null;
    if (panelPos == null) {
      player.sendMessage(new TextComponentTranslation(isInAlarm(world, pos)
          ? "csm.lifesafety.annunciator.unlinked_alarm"
          : "csm.lifesafety.annunciator.unlinked"));
      return true;
    }
    TileEntity panelTe = world.isBlockLoaded(panelPos) ? world.getTileEntity(panelPos) : null;
    if (!(panelTe instanceof TileEntityFireAlarmControlPanel)) {
      player.sendMessage(new TextComponentTranslation("csm.lifesafety.annunciator.no_panel",
          panelPos.getX(), panelPos.getY(), panelPos.getZ()));
      return true;
    }
    TileEntityFireAlarmControlPanel panel = (TileEntityFireAlarmControlPanel) panelTe;
    String key;
    if (!panel.getAlarmState()) {
      key = "csm.lifesafety.annunciator.normal";
    } else if (panel.getAudibleSilence()) {
      key = "csm.lifesafety.annunciator.silenced";
    } else if (panel.getDrill()) {
      key = "csm.lifesafety.annunciator.drill";
    } else {
      key = "csm.lifesafety.annunciator.alarm";
    }
    player.sendMessage(new TextComponentTranslation(key));
    BlockPos origin = panel.getAlarmOriginPos();
    if (panel.getAlarmState() && origin != null && !panel.getAlarmOriginName().isEmpty()) {
      Block device = Block.REGISTRY.getObject(new ResourceLocation(panel.getAlarmOriginName()));
      player.sendMessage(new TextComponentTranslation("csm.lifesafety.annunciator.origin",
          new TextComponentTranslation(device.getTranslationKey() + ".name"),
          origin.getX(), origin.getY(), origin.getZ()));
    }
    return true;
  }
}
