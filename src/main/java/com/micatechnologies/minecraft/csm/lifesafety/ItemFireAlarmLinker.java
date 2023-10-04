package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import net.minecraft.block.state.IBlockState;
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

import java.util.List;

public class ItemFireAlarmLinker extends AbstractItem
{

    private BlockPos alarmPanelPos = null;

    public ItemFireAlarmLinker() {
        super( 0, 1 );
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
        IBlockState state = worldIn.getBlockState( pos );

        // Save panel location if click on panel
        if ( state.getBlock() instanceof BlockFireAlarmControlPanel ) {
            alarmPanelPos = pos;
            if ( !worldIn.isRemote ) {
                player.sendMessage( new TextComponentString( "Linking to fire alarm control panel at " +
                                                                     "(" +
                                                                     pos.getX() +
                                                                     "," +
                                                                     pos.getY() +
                                                                     "," +
                                                                     pos.getZ() +
                                                                     ")" ) );
            }
            return EnumActionResult.SUCCESS;
        }

        // Link alarm to panel if panel selected and alarm clicked
        if ( alarmPanelPos != null &&
                worldIn.getTileEntity( alarmPanelPos ) instanceof TileEntityFireAlarmControlPanel ) {
            TileEntityFireAlarmControlPanel fireAlarmControlPanel
                    = ( TileEntityFireAlarmControlPanel ) worldIn.getTileEntity( alarmPanelPos );

            if ( state.getBlock() instanceof AbstractBlockFireAlarmSounderVoiceEvac ) {
                boolean didAdd = fireAlarmControlPanel.addLinkedAlarm( pos );
                if ( didAdd && !worldIn.isRemote ) {
                    player.sendMessage( new TextComponentString( "Successfully linked to voice evac circuit of fire " +
                                                                         "alarm control panel at " +
                                                                         "(" +
                                                                         pos.getX() +
                                                                         "," +
                                                                         pos.getY() +
                                                                         "," +
                                                                         pos.getZ() +
                                                                         ")" ) );
                }
                return EnumActionResult.SUCCESS;
            }
            else if ( state.getBlock() instanceof AbstractBlockFireAlarmSounder ) {
                boolean didAdd = fireAlarmControlPanel.addLinkedAlarm( pos );
                if ( didAdd && !worldIn.isRemote ) {
                    player.sendMessage( new TextComponentString( "Successfully linked to main circuit of fire " +
                                                                         "alarm control panel at " +
                                                                         "(" +
                                                                         alarmPanelPos.getX() +
                                                                         "," +
                                                                         alarmPanelPos.getY() +
                                                                         "," +
                                                                         alarmPanelPos.getZ() +
                                                                         ")" ) );
                }
                return EnumActionResult.SUCCESS;
            }
            else if ( state.getBlock() instanceof AbstractBlockFireAlarmActivator ) {
                TileEntity tileEntityAtClickedPos = worldIn.getTileEntity( pos );
                if ( tileEntityAtClickedPos instanceof TileEntityFireAlarmSensor ) {
                    TileEntityFireAlarmSensor fireAlarmSensor = ( TileEntityFireAlarmSensor ) tileEntityAtClickedPos;
                    boolean didLink = fireAlarmSensor.setLinkedPanelPos( alarmPanelPos, player );
                    if ( didLink && !worldIn.isRemote ) {
                        player.sendMessage( new TextComponentString( "Successfully linked activator to " +
                                                                             "alarm control panel at " +
                                                                             "(" +
                                                                             alarmPanelPos.getX() +
                                                                             "," +
                                                                             alarmPanelPos.getY() +
                                                                             "," +
                                                                             alarmPanelPos.getZ() +
                                                                             ")" ) );
                    }
                }

                return EnumActionResult.SUCCESS;
            }
        }
        else {
            if ( !worldIn.isRemote ) {
                player.sendMessage( new TextComponentString( "No panel selected!" ) );
            }
        }
        return EnumActionResult.FAIL;
    }

    @Override
    public void addInformation( ItemStack itemstack, World world, List< String > list, ITooltipFlag flag ) {
        super.addInformation( itemstack, world, list, flag );
        list.add( "Link fire alarm appliances to a fire alarm control panel" );
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
