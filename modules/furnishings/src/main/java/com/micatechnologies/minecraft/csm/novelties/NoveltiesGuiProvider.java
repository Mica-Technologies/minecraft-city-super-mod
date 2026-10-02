package com.micatechnologies.minecraft.csm.novelties;

import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import com.micatechnologies.minecraft.csm.furniture.appliance.ContainerAppliance;
import com.micatechnologies.minecraft.csm.furniture.appliance.GuiAppliance;
import com.micatechnologies.minecraft.csm.furniture.appliance.IAppliance;
import com.micatechnologies.minecraft.csm.furniture.appliance.TileEntityAppliance;
import com.micatechnologies.minecraft.csm.furniture.office.BlockCubiclePanelNamed;
import com.micatechnologies.minecraft.csm.furniture.office.CubicleNamePlateGui;
import com.micatechnologies.minecraft.csm.furniture.office.TileEntityCubicleNamePlate;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialStorage;
import com.micatechnologies.minecraft.csm.furniture.residential.ContainerResidentialStorage;
import com.micatechnologies.minecraft.csm.furniture.residential.GuiResidentialStorage;
import com.micatechnologies.minecraft.csm.furniture.residential.IResidentialStorage;
import com.micatechnologies.minecraft.csm.furniture.residential.TileEntityResidentialStorage;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Supplies the Furniture &amp; Novelties module's GUI screens: the multi-game arcade cabinet, and
 * the storage of the Residential tab's TV stands, sideboards, kitchen cabinets, refrigerators,
 * freezers and cookie jar, and the working appliances' screen (both with a server-side
 * container), and the editor for a cubicle panel's name plate.
 *
 * @version 1.2
 * @since 2026.9
 */
public class NoveltiesGuiProvider implements ICsmGuiProvider {

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Nullable
  @Override
  public Object getClientGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
    TileEntity tileEntity = world.getTileEntity(pos);
    Object returnValue = null;
    if (id == BlockArcadeMultiGame.GUI_ID && tileEntity instanceof TileEntityArcadeCabinet) {
      // The GUI opens on its own game-select screen and instantiates a game only once the player
      // picks one, so nothing outside this client-side branch ever names a game class.
      returnValue = new ArcadeGui((TileEntityArcadeCabinet) tileEntity);
    } else if (id == BlockResidentialStorage.GUI_ID) {
      ContainerResidentialStorage container = storageContainer(player, world, pos, false);
      if (container != null) {
        returnValue = new GuiResidentialStorage(player.inventory, container,
            world.getBlockState(pos).getBlock().getLocalizedName());
      }
    } else if (id == IAppliance.GUI_ID) {
      ContainerAppliance container = applianceContainer(player, world, pos, false);
      if (container != null) {
        returnValue = new GuiAppliance(player.inventory, container,
            world.getBlockState(pos).getBlock().getLocalizedName());
      }
    } else if (id == BlockCubiclePanelNamed.GUI_ID
        && tileEntity instanceof TileEntityCubicleNamePlate) {
      Block block = world.getBlockState(pos).getBlock();
      if (block instanceof BlockCubiclePanelNamed) {
        returnValue = new CubicleNamePlateGui((TileEntityCubicleNamePlate) tileEntity,
            ((BlockCubiclePanelNamed) block).getStyle(), block.getLocalizedName());
      }
    }
    return returnValue;
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.1
   */
  @Nullable
  @Override
  public Object getServerGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
    if (id == BlockResidentialStorage.GUI_ID) {
      return storageContainer(player, world, pos, true);
    }
    return id == IAppliance.GUI_ID ? applianceContainer(player, world, pos, true) : null;
  }

  /** A working appliance's container, or null if the appliance is not there. */
  @Nullable
  private static ContainerAppliance applianceContainer(EntityPlayer player, World world,
      BlockPos pos, boolean server) {
    TileEntity te = world.getTileEntity(pos);
    Block block = world.getBlockState(pos).getBlock();
    if (!(te instanceof TileEntityAppliance) || !(block instanceof IAppliance)) {
      return null;
    }
    return new ContainerAppliance(player.inventory, (TileEntityAppliance) te,
        ((IAppliance) block).getApplianceSpec(), server);
  }

  /** A Residential storage block's container, or null if the block is not there. */
  @Nullable
  private static ContainerResidentialStorage storageContainer(EntityPlayer player, World world,
      BlockPos pos, boolean server) {
    TileEntity te = world.getTileEntity(pos);
    Block block = world.getBlockState(pos).getBlock();
    if (!(te instanceof TileEntityResidentialStorage)
        || !(block instanceof IResidentialStorage)) {
      return null;
    }
    return new ContainerResidentialStorage(player.inventory, (TileEntityResidentialStorage) te,
        ((IResidentialStorage) block).getSlots(), server);
  }
}
