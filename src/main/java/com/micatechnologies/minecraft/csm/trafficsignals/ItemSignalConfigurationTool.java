package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import com.micatechnologies.minecraft.csm.tabs.CsmTabTrafficSignals;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensor;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ItemSignalConfigurationTool extends AbstractItem
{

    private final Map< UUID, ItemSignalConfigurationToolMode > modeMap = new HashMap<>();

    private final Map< UUID, BlockPos > posMap1 = new HashMap<>();
    private final Map< UUID, BlockPos > posMap2 = new HashMap<>();

    public ItemSignalConfigurationToolMode getMode( EntityPlayer player ) {
        return modeMap.getOrDefault( player.getUniqueID(), ItemSignalConfigurationToolMode.CYCLE_SIGNAL_COLORS );
    }

    public String switchToNextMode( EntityPlayer player ) {
        ItemSignalConfigurationToolMode newMode = modeMap.getOrDefault( player.getUniqueID(),
                                                                        ItemSignalConfigurationToolMode.CYCLE_SIGNAL_COLORS );
        int newModeOrdinal = newMode.ordinal() + 1;
        newModeOrdinal %= ItemSignalConfigurationToolMode.values().length;
        newMode = ItemSignalConfigurationToolMode.values()[ newModeOrdinal ];
        modeMap.put( player.getUniqueID(), newMode );
        return newMode.getFriendlyName();
    }

    @Override
    public EnumActionResult onItemUse( EntityPlayer player,
                                       World worldIn,
                                       BlockPos pos,
                                       EnumHand hand,
                                       EnumFacing facing,
                                       float hitX,
                                       float hitY,
                                       float hitZ )
    {
        if ( !worldIn.isRemote ) {
            IBlockState state = worldIn.getBlockState( pos );
            Block clickedBlock = state.getBlock();
            ItemSignalConfigurationToolMode mode = getMode( player );

            // If sneaking, change mode
            EnumActionResult expectedResult = EnumActionResult.PASS;
            EnumActionResult result = EnumActionResult.FAIL;
            if ( player.isSneaking() ) {
                // Switch to next mode
                switchToNextMode( player );

                // Notify player
                player.sendMessage(
                        new TextComponentString( "Mode changed to: " + getMode( player ).getFriendlyName() ) );

                // Mark result as success
                result = EnumActionResult.PASS;
            }
            // If in cycle signal colors mode, cycle signal color if clicked block is a signal
            else if ( mode == ItemSignalConfigurationToolMode.CYCLE_SIGNAL_COLORS ) {
                // Check if clicked block is a signal
                if ( clickedBlock instanceof AbstractBlockControllableSignal ) {
                    // Cycle signal color
                    worldIn.setBlockState( pos, state.cycleProperty( AbstractBlockControllableSignal.COLOR ) );

                    // Notify player
                    player.sendMessage( new TextComponentString( "Cycling traffic signal color/state!" ) );

                    // Mark result as success
                    result = EnumActionResult.PASS;
                }
            }
            // If in re-orient sensor mode, re-orient sensor if clicked block is a sensor
            else if ( mode == ItemSignalConfigurationToolMode.REORIENT_SENSOR ) {
                // Check if clicked block is a sensor
                if ( clickedBlock instanceof AbstractBlockTrafficSignalSensor ) {
                    // Re-orient sensor
                    worldIn.setBlockState( pos, state.withProperty( AbstractBlockTrafficSignalSensor.FACING,
                                                                    player.getHorizontalFacing().getOpposite() ) );

                    // Notify player
                    player.sendMessage( new TextComponentString( "Re-oriented traffic signal sensor!" ) );

                    // Mark result as success
                    result = EnumActionResult.PASS;
                }
            }
            // If in toggle controller nightly flash setting mode, toggle controller nightly flash setting if clicked
            // block is a controller
            else if ( mode == ItemSignalConfigurationToolMode.TOGGLE_CONTROLLER_NIGHTLY_FLASH_SETTING ) {
                TileEntity tileEntity = worldIn.getTileEntity( pos );
                if ( tileEntity instanceof TileEntityTrafficSignalController ) {
                    // Invert nighttime flash setting on controller
                    TileEntityTrafficSignalController tileEntityTrafficSignalController
                            = ( TileEntityTrafficSignalController ) tileEntity;
                    boolean newSetting = !tileEntityTrafficSignalController.getNightlyFallbackToFlashMode();
                    tileEntityTrafficSignalController.setNightlyFallbackToFlashMode( newSetting );

                    // Notify player
                    player.sendMessage( new TextComponentString(
                            "Set traffic signal controller nightly flash setting to " + newSetting ) );

                    // Mark result as success
                    result = EnumActionResult.PASS;
                }
            }
            // If in toggle controller power loss flash setting mode, toggle controller power loss flash setting if
            // clicked block is a controller
            else if ( mode == ItemSignalConfigurationToolMode.TOGGLE_CONTROLLER_POWER_LOSS_FLASH_SETTING ) {
                TileEntity tileEntity = worldIn.getTileEntity( pos );
                if ( tileEntity instanceof TileEntityTrafficSignalController ) {
                    // Invert power loss flash setting on controller
                    TileEntityTrafficSignalController tileEntityTrafficSignalController
                            = ( TileEntityTrafficSignalController ) tileEntity;
                    boolean newSetting = !tileEntityTrafficSignalController.getPowerLossFallbackToFlashMode();
                    tileEntityTrafficSignalController.setPowerLossFallbackToFlashMode( newSetting );

                    // Notify player
                    player.sendMessage( new TextComponentString(
                            "Set traffic signal controller power loss flash setting to " + newSetting ) );

                    // Mark result as success
                    result = EnumActionResult.PASS;
                }
            }
            // If in toggle controller overlap pedestrian signals setting mode, toggle controller overlap pedestrian
            // signals setting if clicked block is a controller
            else if ( mode == ItemSignalConfigurationToolMode.TOGGLE_CONTROLLER_OVERLAP_PEDESTRIAN_SIGNALS_SETTING ) {
                TileEntity tileEntity = worldIn.getTileEntity( pos );
                if ( tileEntity instanceof TileEntityTrafficSignalController ) {
                    // Invert overlap pedestrian signals setting on controller
                    TileEntityTrafficSignalController tileEntityTrafficSignalController
                            = ( TileEntityTrafficSignalController ) tileEntity;
                    boolean newSetting = !tileEntityTrafficSignalController.getOverlapPedestrianSignals();
                    tileEntityTrafficSignalController.setOverlapPedestrianSignals( newSetting );

                    // Notify player
                    player.sendMessage( new TextComponentString(
                            "Set traffic signal controller overlap pedestrian signals setting to " + newSetting ) );

                    // Mark result as success
                    result = EnumActionResult.PASS;
                }
            }
            // If in clear controller faults mode, clear controller faults if clicked block is a controller
            else if ( mode == ItemSignalConfigurationToolMode.CLEAR_CONTROLLER_FAULTS ) {
                TileEntity tileEntity = worldIn.getTileEntity( pos );
                if ( tileEntity instanceof TileEntityTrafficSignalController ) {
                    // Clear fault state from controller
                    TileEntityTrafficSignalController tileEntityTrafficSignalController
                            = ( TileEntityTrafficSignalController ) tileEntity;
                    boolean isInFaultState = tileEntityTrafficSignalController.isInFaultState();
                    String faultMessage = "";
                    if ( isInFaultState ) {
                        faultMessage = tileEntityTrafficSignalController.getCurrentFaultMessage();
                        tileEntityTrafficSignalController.clearFaultState();
                    }

                    // Notify player
                    // Controller in fault state
                    if ( isInFaultState ) {
                        player.sendMessage( new TextComponentString( "Controller fault state has been reset." ) );
                        player.sendMessage(
                                new TextComponentString( "Cleared controller fault message: " + faultMessage ) );
                    }
                    // Controller in non-fault state
                    else {
                        player.sendMessage(
                                new TextComponentString( "Controller is not in fault state. Unable to reset!" ) );
                    }

                    // Mark result as success
                    result = EnumActionResult.PASS;
                }
            }
            else if ( mode == ItemSignalConfigurationToolMode.CREATE_SIGNAL_OVERLAPS ) {
                BlockPos controllerPos = posMap1.getOrDefault( player.getUniqueID(), null );
                BlockPos overlapSourcePos = posMap2.getOrDefault( player.getUniqueID(), null );

                // Check if clicked block is a signal controller
                if ( clickedBlock instanceof BlockTrafficSignalController ) {
                    // Store controller position
                    posMap1.put( player.getUniqueID(), pos );

                    // Notify player
                    player.sendMessage( new TextComponentString( "Controller position set to " +
                                                                         pos +
                                                                         ". Click the overlap source signal " +
                                                                         "followed by the overlap target signal." ) );

                    // Mark result as success
                    result = EnumActionResult.PASS;
                }
                // Check if clicked block is a signal
                else if ( clickedBlock instanceof AbstractBlockControllableSignal ) {
                    // Check if controller position is set
                    if ( controllerPos == null ) {
                        // Notify player
                        player.sendMessage( new TextComponentString( "Controller position not set. Click a " +
                                                                             "controller to begin configuring an " +
                                                                             "overlap." ) );

                        // Mark result as success
                        result = EnumActionResult.PASS;
                    }
                    // Check if overlap source position is set
                    else if ( overlapSourcePos == null ) {
                        // Store overlap source position
                        posMap2.put( player.getUniqueID(), pos );

                        // Notify player
                        player.sendMessage( new TextComponentString( "Overlap source signal position set to " +
                                                                             pos +
                                                                             ". Click the overlap target signal." ) );

                        // Mark result as success
                        result = EnumActionResult.PASS;
                    }
                    // Check if overlap source position and target position are the same
                    else if ( overlapSourcePos.equals( pos ) ) {
                        // Notify player
                        player.sendMessage( new TextComponentString( "Overlap target signal position cannot " +
                                                                             "be the same as source signal " +
                                                                             "position!" ) );
                    }
                    // Otherwise, create overlap on selected controller using source and target signal positions
                    else {
                        // Get controller tile entity
                        TileEntity tileEntity = worldIn.getTileEntity( controllerPos );

                        // Double-check that tile entity is controller
                        if ( tileEntity instanceof TileEntityTrafficSignalController ) {
                            TileEntityTrafficSignalController tileEntityTrafficSignalController
                                    = ( TileEntityTrafficSignalController ) tileEntity;

                            // Create overlap
                            boolean created = tileEntityTrafficSignalController.createOverlap( overlapSourcePos, pos );

                            // Notify player whether overlap was created and mark result as success if so
                            if ( created ) {
                                // Reset pos2
                                posMap2.remove( player.getUniqueID() );

                                // Notify player
                                player.sendMessage( new TextComponentString( "Overlap created on controller at " +
                                                                                     controllerPos +
                                                                                     " from source signal at " +
                                                                                     overlapSourcePos +
                                                                                     " to target signal at " +
                                                                                     pos ) );

                                // Mark result as success
                                result = EnumActionResult.PASS;
                            }
                            else {
                                // Notify player
                                player.sendMessage( new TextComponentString( "Unable to create overlap on " +
                                                                                     "controller at " +
                                                                                     controllerPos +
                                                                                     " from source signal at " +
                                                                                     overlapSourcePos +
                                                                                     " to target signal at " +
                                                                                     pos +
                                                                                     ". Overlap already exists!" ) );
                            }

                        }
                    }
                }
                // If clicked block is not a signal or controller, reset pos1 and pos2
                else {
                    // Reset pos2
                    posMap2.remove( player.getUniqueID() );

                    // Notify player
                    player.sendMessage( new TextComponentString(
                            "Overlap source signal position " + "has been reset. (Clicked non-signal " + "block)" ) );

                    // Mark result as success
                    result = EnumActionResult.PASS;
                }
            }
            else {
                player.sendMessage( new TextComponentString( "Not sure how we got here, but something has clearly" +
                                                                     " gone very wrong. Please report this bug!" ) );
                expectedResult = EnumActionResult.FAIL;
            }

            // Check if result is expected, and if not, notify player
            if ( result != expectedResult ) {
                player.sendMessage( new TextComponentString( "Something went wrong. Please try again!" ) );
            }

            return result;
        }
        else {
            return EnumActionResult.SUCCESS;
        }
    }

    @Override
    public void addInformation( ItemStack itemstack, World world, List< String > list, ITooltipFlag flag ) {
        super.addInformation( itemstack, world, list, flag );
        list.add( "Configuration tool for changing signal colors, resetting controller faults, re-orienting " +
                          "sensors, and more..." );
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
        return "signalconfigurationtool";

    }
}
