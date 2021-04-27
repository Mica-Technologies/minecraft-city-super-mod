package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
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

import java.util.List;

@ElementsCitySuperMod.ModElement.Tag
public class ItemEWSignalLinker extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:ewsignallinker" )
    public static final Item block = null;

    public ItemEWSignalLinker( ElementsCitySuperMod instance ) {
        super( instance, 1999 );
    }

    @Override
    public void initElements() {
        elements.items.add( () -> new ItemCustom() );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( block, 0,
                                                    new ModelResourceLocation( "csm:ewsignallinker", "inventory" ) );
    }

    public static class ItemCustom extends Item
    {

        private BlockPos sensorPosClient  = null;
        private BlockPos sensorPosServer  = null;
        private BlockPos corner1PosClient = null;
        private BlockPos corner1PosServer = null;

        public ItemCustom() {
            setMaxDamage( 0 );
            maxStackSize = 1;
            setUnlocalizedName( "ewsignallinker" );
            setRegistryName( "ewsignallinker" );
            setCreativeTab( TabTrafficSignals.tab );
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
            if ( state.getBlock() instanceof AbstractBlockTrafficSignalSensor ) {
                if ( !worldIn.isRemote ) {
                    sensorPosClient = pos;
                    corner1PosClient = null;
                }
                else {
                    sensorPosServer = pos;
                    corner1PosServer = null;
                }
                if ( !worldIn.isRemote ) {
                    player.sendMessage( new TextComponentString( "Selected sensor at position: [" +
                                                                         pos.getX() +
                                                                         ", " +
                                                                         pos.getY() +
                                                                         ", " +
                                                                         pos.getZ() +
                                                                         "]. " +
                                                                         "Please select corner #1 of sensor search " +
                                                                         "box!" ) );
                }
                return EnumActionResult.SUCCESS;
            }
            else if ( !worldIn.isRemote && sensorPosClient != null && corner1PosClient == null ) {
                corner1PosClient = pos;
                player.sendMessage( new TextComponentString(
                        "Search box corner 1 set to: [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]." ) );
                return EnumActionResult.SUCCESS;
            }
            else if ( worldIn.isRemote && sensorPosServer != null && corner1PosServer == null ) {
                corner1PosServer = pos;
                return EnumActionResult.SUCCESS;
            }
            else if ( !worldIn.isRemote && sensorPosClient != null ) {
                player.sendMessage( new TextComponentString(
                        "Search box corner 2 set to: [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]." ) );

                try {
                    TileEntity tileEntity = worldIn.getTileEntity( sensorPosClient );
                    if ( tileEntity instanceof TileEntityTrafficSignalSensor ) {
                        TileEntityTrafficSignalSensor tileEntityTrafficSignalSensor
                                = ( TileEntityTrafficSignalSensor ) tileEntity;
                        boolean overwrote = tileEntityTrafficSignalSensor.setScanCorners( corner1PosClient, pos );
                        if ( overwrote ) {
                            player.sendMessage( new TextComponentString( "The selected search box corners have been " +
                                                                                 "applied to the desired sensor " +
                                                                                 "successfully! (Replaced previous " +
                                                                                 "search box)" ) );
                        }
                        else {
                            player.sendMessage( new TextComponentString( "The selected search box corners have been " +
                                                                                 "applied to the desired sensor " +
                                                                                 "successfully!" ) );
                        }
                    }
                    else {
                        player.sendMessage( new TextComponentString( "The selected traffic signal sensor did not " +
                                                                             "respond to programming. Please " +
                                                                             "replace the sensor and try again!" ) );
                    }
                }
                catch ( Exception e ) {
                    player.sendMessage( new TextComponentString( "An error occurred while programming the " +
                                                                         "selected traffic signal sensor. Please " +
                                                                         "replace the sensor and try again!" ) );
                }

                corner1PosClient = null;

                return EnumActionResult.SUCCESS;
            }
            else if ( worldIn.isRemote && sensorPosServer != null ) {

                try {
                    TileEntity tileEntity = worldIn.getTileEntity( sensorPosServer );
                    if ( tileEntity instanceof TileEntityTrafficSignalSensor ) {
                        TileEntityTrafficSignalSensor tileEntityTrafficSignalSensor
                                = ( TileEntityTrafficSignalSensor ) tileEntity;
                        tileEntityTrafficSignalSensor.setScanCorners( corner1PosServer, pos );
                    }
                }
                catch ( Exception ignored ) {
                }

                corner1PosServer = null;

                return EnumActionResult.SUCCESS;
            }
            else if ( !worldIn.isRemote ) {
                player.sendMessage( new TextComponentString( "Please select a sensor to begin configuration!" ) );
                return EnumActionResult.SUCCESS;
            }

            return EnumActionResult.SUCCESS;
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
            list.add( "Link traffic signals to the Secondary circuit of a signal controller." );
        }

        @Override
        public int getItemEnchantability() {
            return 0;
        }
    }
}
