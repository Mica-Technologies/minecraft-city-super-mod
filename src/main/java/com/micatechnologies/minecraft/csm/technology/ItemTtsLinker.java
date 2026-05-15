package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/**
 * Linker tool for binding speakers (Atlas, Bose, FourJay, JBL, Valcom, etc.) to a Redstone
 * TTS Module. Mirrors the HVAC linker UX so players already familiar with the HVAC system can
 * pick this up without re-learning it.
 *
 * <ul>
 *   <li><b>Right-click TTS module</b> — select it as the linking source</li>
 *   <li><b>Then right-click a speaker</b> — link the speaker to the selected module</li>
 *   <li><b>Sneak + right-click a speaker</b> — clear that speaker's link</li>
 *   <li><b>Sneak + right-click a TTS module</b> — clear all of its linked speakers</li>
 * </ul>
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class ItemTtsLinker extends AbstractItem {

  private BlockPos selectedTtsPos = null;

  public ItemTtsLinker() {
    super(0, 1);
  }

  @Override
  public String getItemRegistryName() {
    return "ttslinker";
  }

  @Override
  public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos,
      EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    TileEntity te = worldIn.getTileEntity(pos);

    // Sneak + click to clear links.
    if (player.isSneaking()) {
      if (te instanceof TileEntitySpeaker) {
        BlockPos linkedTo = ((TileEntitySpeaker) te).getLinkedTtsPos();
        if (linkedTo != null && worldIn.isBlockLoaded(linkedTo)) {
          TileEntity ttsTe = worldIn.getTileEntity(linkedTo);
          if (ttsTe instanceof TileEntityRedstoneTTS) {
            ((TileEntityRedstoneTTS) ttsTe).unlinkSpeaker(pos);
          }
        }
        ((TileEntitySpeaker) te).clearLink();
        if (!worldIn.isRemote) {
          player.sendMessage(new TextComponentString("§aSpeaker link cleared"));
        }
        return EnumActionResult.SUCCESS;
      }
      if (te instanceof TileEntityRedstoneTTS) {
        TileEntityRedstoneTTS module = (TileEntityRedstoneTTS) te;
        int n = module.getLinkedSpeakerCount();
        for (BlockPos sp : new ArrayList<>(module.getLinkedSpeakers())) {
          module.unlinkSpeaker(sp);
        }
        if (!worldIn.isRemote) {
          player.sendMessage(new TextComponentString(
              "§aCleared " + n + " linked speaker(s)"));
        }
        return EnumActionResult.SUCCESS;
      }
    }

    // Click TTS module → select as linking source.
    if (te instanceof TileEntityRedstoneTTS) {
      selectedTtsPos = pos;
      if (!worldIn.isRemote) {
        TileEntityRedstoneTTS module = (TileEntityRedstoneTTS) te;
        player.sendMessage(new TextComponentString(
            "§bSelected TTS module (" + module.getLinkedSpeakerCount()
                + " linked speakers). Right-click a speaker to link it."));
      }
      return EnumActionResult.SUCCESS;
    }

    // Click speaker → link to selected TTS module.
    if (te instanceof TileEntitySpeaker) {
      if (selectedTtsPos == null) {
        if (!worldIn.isRemote) {
          player.sendMessage(new TextComponentString(
              "§eRight-click a Redstone TTS Module first to start linking."));
        }
        return EnumActionResult.FAIL;
      }
      TileEntity sourceTe = worldIn.getTileEntity(selectedTtsPos);
      if (!(sourceTe instanceof TileEntityRedstoneTTS)) {
        if (!worldIn.isRemote) {
          player.sendMessage(new TextComponentString(
              "§cThe selected TTS module no longer exists. Click a module first."));
        }
        selectedTtsPos = null;
        return EnumActionResult.FAIL;
      }
      TileEntityRedstoneTTS module = (TileEntityRedstoneTTS) sourceTe;
      // If the speaker was already linked elsewhere, clear that first so it can re-link cleanly.
      TileEntitySpeaker speaker = (TileEntitySpeaker) te;
      BlockPos previousLink = speaker.getLinkedTtsPos();
      if (previousLink != null && !previousLink.equals(selectedTtsPos)) {
        if (worldIn.isBlockLoaded(previousLink)) {
          TileEntity prevTe = worldIn.getTileEntity(previousLink);
          if (prevTe instanceof TileEntityRedstoneTTS) {
            ((TileEntityRedstoneTTS) prevTe).unlinkSpeaker(pos);
          }
        }
        speaker.clearLink();
      }
      boolean success = module.linkSpeaker(pos);
      if (!worldIn.isRemote) {
        if (success) {
          player.sendMessage(new TextComponentString(
              "§aLinked speaker to module (" + module.getLinkedSpeakerCount()
                  + " total)"));
        } else {
          player.sendMessage(new TextComponentString(
              "§cSpeaker is already linked to this module"));
        }
      }
      return EnumActionResult.SUCCESS;
    }

    if (!worldIn.isRemote) {
      if (selectedTtsPos != null) {
        player.sendMessage(new TextComponentString(
            "§eRight-click a speaker to link it to the selected TTS module."));
      } else {
        player.sendMessage(new TextComponentString(
            "§eRight-click a Redstone TTS Module to start linking."));
      }
    }
    return EnumActionResult.FAIL;
  }

  @Override
  public void addInformation(ItemStack itemstack, World world, List<String> list,
      ITooltipFlag flag) {
    super.addInformation(itemstack, world, list, flag);
    list.add("Link speakers to a Redstone TTS Module");
    list.add("§7Right-click TTS module → right-click each speaker");
    list.add("§7Sneak + right-click to clear links");
  }
}
