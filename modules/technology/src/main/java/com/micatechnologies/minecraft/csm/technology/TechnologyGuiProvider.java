package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import com.micatechnologies.minecraft.csm.technology.radio.BlockRadio;
import com.micatechnologies.minecraft.csm.technology.radio.ItemBlockRadio;
import com.micatechnologies.minecraft.csm.technology.radio.RadioGui;
import com.micatechnologies.minecraft.csm.technology.radio.TileEntityRadio;
import com.micatechnologies.minecraft.csm.technology.school.BellControllerGui;
import com.micatechnologies.minecraft.csm.technology.school.BlockBellController;
import com.micatechnologies.minecraft.csm.technology.school.TileEntityBellController;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Supplies the technology package's GUI screens: the computer, the bell schedule controller and
 * the handheld radios (held or standing).
 *
 * <p>GUI id 0 — the Redstone TTS Module — is not here. That block ships in the Text to Speech
 * module, which registers its own provider for that id.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class TechnologyGuiProvider implements ICsmGuiProvider {

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Nullable
  @Override
  public Object getClientGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
    if (id == BlockRadio.GUI_ID_HELD) {
      // A held radio's screen: the position's x is the hand, and there is no block to look at.
      EnumHand hand = pos.getX() == EnumHand.OFF_HAND.ordinal()
          ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
      ItemStack stack = player.getHeldItem(hand);
      if (stack.isEmpty() || !(stack.getItem() instanceof ItemBlockRadio)) {
        return null;
      }
      return new RadioGui(((ItemBlockRadio) stack.getItem()).getRadioModel(),
          ItemBlockRadio.getSettings(stack), hand);
    }
    TileEntity tileEntity = world.getTileEntity(pos);
    if (id == BlockRadio.GUI_ID_PLACED) {
      Block block = world.getBlockState(pos).getBlock();
      if (block instanceof BlockRadio && tileEntity instanceof TileEntityRadio) {
        return new RadioGui(((BlockRadio) block).getRadioModel(),
            ((TileEntityRadio) tileEntity).getSettings(), pos);
      }
      return null;
    }
    Object returnValue = null;
    if (id == 15 && tileEntity instanceof TileEntityComputer) {
      returnValue = new ComputerGui((TileEntityComputer) tileEntity);
    } else if (id == BlockBellController.GUI_ID
        && tileEntity instanceof TileEntityBellController) {
      returnValue = new BellControllerGui((TileEntityBellController) tileEntity);
    }
    return returnValue;
  }
}
