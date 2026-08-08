package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

/**
 * Hand-held item used to link fire alarm devices (pull stations, detectors, sounders) to a
 * fire alarm control panel. Click the panel first, then click each device to establish the link.
 *
 * @author Mica Technologies
 * @since 2026.4
 */

public class ItemFireAlarmLinker extends AbstractItem {

  /**
   * Where the selected panel is remembered on the stack. A long rather than three integers, since
   * {@link BlockPos} already packs and unpacks itself that way.
   */
  private static final String SELECTED_PANEL_KEY = "SelectedPanelPos";

  /**
   * The most recent selection made by anybody, kept only to answer the deprecated no-argument
   * {@link #getSelectedPanel()}. Not read by anything else in this class.
   */
  private BlockPos lastSelectedPanelPos = null;

  public ItemFireAlarmLinker() {
    super(0, 1);
  }

  /**
   * Gets the fire alarm control panel the given linker has selected, or null if it has none.
   * <p>
   * Exposed so that other mods can take part in linking. The linker itself only knows how to attach
   * CSM's own sounders and sensors to a panel, and teaching it about every block another mod might
   * want to link would mean CSM knowing about those mods. Letting the other mod read the selection
   * inverts that: it can handle its own blocks with this same tool, and CSM needs to know nothing
   * about it.
   * <p>
   * The selection lives on the stack, so two players wiring two buildings do not overwrite each
   * other, and a linker keeps its selection when it is put down, stored in a chest, or logged out
   * with. The item itself is a singleton and is the wrong place to have kept it.
   *
   * @param stack the linker being used; anything else, or an empty stack, selects nothing
   *
   * @return the selected fire alarm control panel position, or null
   *
   * @since 2026.08.08
   */
  public static BlockPos getSelectedPanel(ItemStack stack) {
    if (stack == null || !(stack.getItem() instanceof ItemFireAlarmLinker)) {
      return null;
    }
    NBTTagCompound tag = stack.getTagCompound();
    return tag == null || !tag.hasKey(SELECTED_PANEL_KEY, Constants.NBT.TAG_LONG)
        ? null
        : BlockPos.fromLong(tag.getLong(SELECTED_PANEL_KEY));
  }

  /**
   * Remembers a panel on the stack, or forgets it when given null.
   */
  private static void setSelectedPanel(ItemStack stack, BlockPos panelPos) {
    if (stack == null || stack.isEmpty()) {
      return;
    }
    NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
    if (panelPos == null) {
      tag.removeTag(SELECTED_PANEL_KEY);
    } else {
      tag.setLong(SELECTED_PANEL_KEY, panelPos.toLong());
    }
    stack.setTagCompound(tag);
  }

  /**
   * @return the panel most recently selected by any player holding a linker, or null
   *
   * @deprecated The selection belongs to the stack, not to the item: this answers with whoever
   *     clicked a panel last, which on a server is not necessarily the player asking. Kept working
   *     so that consumers built against 2026.08.07 do not break. Use
   *     {@link #getSelectedPanel(ItemStack)}, which every caller can reach -- the stack is in hand
   *     wherever this is asked.
   * @since 2026.08.07
   */
  @Deprecated
  public BlockPos getSelectedPanel() {
    return lastSelectedPanelPos;
  }

  @Override
  public EnumActionResult onItemUse(EntityPlayer player,
      World worldIn,
      BlockPos pos,
      EnumHand hand,
      EnumFacing facing,
      float hitX,
      float hitY,
      float hitZ) {
    IBlockState state = worldIn.getBlockState(pos);
    ItemStack heldStack = player.getHeldItem(hand);
    BlockPos alarmPanelPos = getSelectedPanel(heldStack);

    // Save panel location if click on panel
    if (state.getBlock() instanceof BlockFireAlarmControlPanel) {
      setSelectedPanel(heldStack, pos);
      lastSelectedPanelPos = pos;
      if (!worldIn.isRemote) {
        player.sendMessage(new TextComponentString("Linking to fire alarm control panel at " +
            "(" +
            pos.getX() +
            "," +
            pos.getY() +
            "," +
            pos.getZ() +
            ")"));
      }
      return EnumActionResult.SUCCESS;
    }

    // Link alarm to panel if panel selected and alarm clicked
    if (alarmPanelPos != null &&
        worldIn.getTileEntity(alarmPanelPos) instanceof TileEntityFireAlarmControlPanel) {
      TileEntityFireAlarmControlPanel fireAlarmControlPanel
          = (TileEntityFireAlarmControlPanel) worldIn.getTileEntity(alarmPanelPos);

      if (state.getBlock() instanceof AbstractBlockFireAlarmSounderVoiceEvac) {
        boolean didAdd = fireAlarmControlPanel.addLinkedAlarm(pos);
        if (didAdd && !worldIn.isRemote) {
          player.sendMessage(
              new TextComponentString("Successfully linked to voice evac circuit of fire " +
                  "alarm control panel at " +
                  "(" +
                  pos.getX() +
                  "," +
                  pos.getY() +
                  "," +
                  pos.getZ() +
                  ")"));
        }
        return EnumActionResult.SUCCESS;
      } else if (state.getBlock() instanceof AbstractBlockFireAlarmSounder) {
        boolean didAdd = fireAlarmControlPanel.addLinkedAlarm(pos);
        if (didAdd && !worldIn.isRemote) {
          player.sendMessage(
              new TextComponentString("Successfully linked to main circuit of fire " +
                  "alarm control panel at " +
                  "(" +
                  alarmPanelPos.getX() +
                  "," +
                  alarmPanelPos.getY() +
                  "," +
                  alarmPanelPos.getZ() +
                  ")"));
        }
        return EnumActionResult.SUCCESS;
      } else if (state.getBlock() instanceof AbstractBlockFireAlarmActivator) {
        TileEntity tileEntityAtClickedPos = worldIn.getTileEntity(pos);
        if (tileEntityAtClickedPos instanceof TileEntityFireAlarmSensor) {
          TileEntityFireAlarmSensor fireAlarmSensor =
              (TileEntityFireAlarmSensor) tileEntityAtClickedPos;
          boolean didLink = fireAlarmSensor.setLinkedPanelPos(alarmPanelPos, player);
          if (didLink && !worldIn.isRemote) {
            player.sendMessage(new TextComponentString("Successfully linked activator to " +
                "alarm control panel at " +
                "(" +
                alarmPanelPos.getX() +
                "," +
                alarmPanelPos.getY() +
                "," +
                alarmPanelPos.getZ() +
                ")"));
          }
        }

        return EnumActionResult.SUCCESS;
      }
    } else {
      if (!worldIn.isRemote) {
        player.sendMessage(new TextComponentString("No panel selected!"));
      }
    }
    return EnumActionResult.FAIL;
  }

  /**
   * Says which panel this particular linker is holding. Worth stating now that the selection is per
   * stack: with one shared selection there was nothing a player could usefully be told, since the
   * answer belonged to whoever clicked last rather than to the tool in their hand.
   */
  @Override
  public void addInformation(ItemStack itemstack, World world, List<String> list,
      ITooltipFlag flag) {
    super.addInformation(itemstack, world, list, flag);
    list.add("Link fire alarm appliances to a fire alarm control panel");
    BlockPos selected = getSelectedPanel(itemstack);
    list.add(selected == null
        ? "No panel selected"
        : "Panel selected at (" + selected.getX() + "," + selected.getY() + "," + selected.getZ()
            + ")");
  }

  /**
   * Retrieves the registry name of the item.
   *
   * @return The registry name of the item.
   *
   * @since 1.0
   */
  @Override
  public String getItemRegistryName() {
    return "firealarmlinker";
  }
}
