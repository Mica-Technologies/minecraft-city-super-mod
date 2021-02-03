package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
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

import java.util.List;

@ElementsCitySuperMod.ModElement.Tag
public class ItemFireAlarmLinker extends ElementsCitySuperMod.ModElement
{
    public static final String itemRegistryName = "firealarmlinker";
    @GameRegistry.ObjectHolder( "csm:" + itemRegistryName )
    public static final Item   block            = null;

    public ItemFireAlarmLinker( ElementsCitySuperMod instance ) {
        super( instance, 2030 );
    }

    @Override
    public void initElements() {
        elements.items.add( ItemCustom::new );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( block, 0, new ModelResourceLocation( "csm:" + itemRegistryName,
                                                                                         "inventory" ) );
    }

    public static class ItemCustom extends Item
    {

        private BlockPos alarmPanelPos = null;

        public ItemCustom() {
            setMaxDamage( 0 );
            maxStackSize = 1;
            setUnlocalizedName( itemRegistryName );
            setRegistryName( itemRegistryName );
            setCreativeTab( TabFireAlarms.tab );
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
            if ( state.getBlock() instanceof BlockFireAlarmControlPanel.BlockCustom ) {
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
                        player.sendMessage( new TextComponentString(
                                "Successfully linked to voice evac circuit of fire " +
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
                        TileEntityFireAlarmSensor fireAlarmSensor
                                = ( TileEntityFireAlarmSensor ) tileEntityAtClickedPos;
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
        public float getDestroySpeed( ItemStack par1ItemStack, IBlockState par2Block ) {
            return 1F;
        }

        @Override
        public int getMaxItemUseDuration( ItemStack itemstack ) {
            return 0;
        }

        @Override
        public void addInformation( ItemStack itemstack, World world, List< String > list, ITooltipFlag flag ) {
            super.addInformation( itemstack, world, list, flag );
            list.add( "Link fire alarm appliances to a fire alarm control panel" );
        }

        @Override
        public int getItemEnchantability() {
            return 0;
        }
    }
}
