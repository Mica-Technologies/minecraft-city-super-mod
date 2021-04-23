package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
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
public class ItemNSSignalLinker extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:nssignallinker" )
    public static final Item block = null;

    public ItemNSSignalLinker( ElementsCitySuperMod instance ) {
        super( instance, 1998 );
    }

    @Override
    public void initElements() {
        elements.items.add( () -> new ItemCustom() );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( block, 0,
                                                    new ModelResourceLocation( "csm:nssignallinker", "inventory" ) );
    }

    public static class ItemCustom extends Item
    {

        private BlockPos signalControllerPos = null;
        private int      circuitLinkIndex    = 0;

        public ItemCustom() {
            setMaxDamage( 0 );
            maxStackSize = 1;
            setUnlocalizedName( "nssignallinker" );
            setRegistryName( "nssignallinker" );
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
            if ( state.getBlock() instanceof BlockTrafficSignalController.BlockCustom ) {
                signalControllerPos = pos;
                if ( !worldIn.isRemote ) {
                    player.sendMessage( new TextComponentString( "Linking to signal controller at " +
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
            else if ( signalControllerPos == null && state.getBlock() instanceof AbstractBlockControllableSignal ) {
                if ( !worldIn.isRemote ) {
                    player.sendMessage( new TextComponentString( "No signal controller has been selected." ) );
                }
                return EnumActionResult.SUCCESS;
            }
            else if ( signalControllerPos != null &&
                    state.getBlock() instanceof AbstractBlockControllableSignal &&
                    !player.isSneaking() ) {
                AbstractBlockControllableSignal clickedBlock = ( AbstractBlockControllableSignal ) state.getBlock();

                TileEntity tileEntity = worldIn.getTileEntity( signalControllerPos );
                if ( tileEntity instanceof TileEntityTrafficSignalController ) {
                    TileEntityTrafficSignalController tileEntityTrafficSignalController
                            = ( TileEntityTrafficSignalController ) tileEntity;
                    boolean linked = tileEntityTrafficSignalController.linkDevice( worldIn, pos,
                                                                                   clickedBlock.getSignalSide( null,
                                                                                                               null ),
                                                                                   circuitLinkIndex );
                    if ( !worldIn.isRemote &&
                            linked &&
                            ( clickedBlock instanceof BlockControllableCrosswalkLeftMount.BlockCustom ||
                                    clickedBlock instanceof BlockControllableCrosswalkRightMount.BlockCustom ) ) {
                        player.sendMessage( new TextComponentString( "Crosswalk light connected to circuit " +
                                                                             circuitLinkIndex +
                                                                             " of signal controller at " +
                                                                             "(" +
                                                                             signalControllerPos.getX() +
                                                                             "," +
                                                                             signalControllerPos.getY() +
                                                                             "," +
                                                                             signalControllerPos.getZ() +
                                                                             ")" ) );
                    }
                    else if ( !worldIn.isRemote &&
                            linked &&
                            clickedBlock instanceof BlockControllableTrafficSignalTrainController.BlockCustom ) {
                        player.sendMessage( new TextComponentString(
                                "Train locking rail controller connected to circuit " +
                                        circuitLinkIndex +
                                        " of signal controller at " +
                                        "(" +
                                        signalControllerPos.getX() +
                                        "," +
                                        signalControllerPos.getY() +
                                        "," +
                                        signalControllerPos.getZ() +
                                        ")" ) );
                    }
                    else if ( !worldIn.isRemote &&
                            linked &&
                            clickedBlock instanceof AbstractBlockControllableCrosswalkAccessory ) {
                        player.sendMessage( new TextComponentString( "Crosswalk accessory connected to circuit " +
                                                                             circuitLinkIndex +
                                                                             " of signal controller at " +
                                                                             "(" +
                                                                             signalControllerPos.getX() +
                                                                             "," +
                                                                             signalControllerPos.getY() +
                                                                             "," +
                                                                             signalControllerPos.getZ() +
                                                                             ")" ) );
                    }
                    else if ( !worldIn.isRemote && linked ) {
                        player.sendMessage( new TextComponentString( "Signal connected to circuit " +
                                                                             circuitLinkIndex +
                                                                             " of signal controller at " +
                                                                             "(" +
                                                                             signalControllerPos.getX() +
                                                                             "," +
                                                                             signalControllerPos.getY() +
                                                                             "," +
                                                                             signalControllerPos.getZ() +
                                                                             ")" ) );
                    }
                }
                else {
                    if ( !worldIn.isRemote ) {
                        player.sendMessage( new TextComponentString(
                                "Unable to link device! Lost connection to previously connected controller." ) );
                    }
                }

                return EnumActionResult.SUCCESS;
            }
            else if ( signalControllerPos != null &&
                    state.getBlock() instanceof AbstractBlockControllableSignal &&
                    player.isSneaking() ) {
                AbstractBlockControllableSignal clickedBlock = ( AbstractBlockControllableSignal ) state.getBlock();

                TileEntity tileEntity = worldIn.getTileEntity( signalControllerPos );
                if ( tileEntity instanceof TileEntityTrafficSignalController ) {
                    TileEntityTrafficSignalController tileEntityTrafficSignalController
                            = ( TileEntityTrafficSignalController ) tileEntity;
                    boolean removed = tileEntityTrafficSignalController.unlinkDevice( pos, worldIn );

                    // If unlinked, change device state to off
                    AbstractBlockControllableSignal.changeSignalColor( worldIn, pos,
                                                                       AbstractBlockControllableSignal.SIGNAL_OFF );

                    if ( !worldIn.isRemote &&
                            removed &&
                            ( clickedBlock instanceof BlockControllableCrosswalkLeftMount.BlockCustom ||
                                    clickedBlock instanceof BlockControllableCrosswalkRightMount.BlockCustom ) ) {
                        player.sendMessage( new TextComponentString( "Crosswalk light unlinked from signal controller" +
                                                                             " " +
                                                                             "at " +
                                                                             "(" +
                                                                             signalControllerPos.getX() +
                                                                             "," +
                                                                             signalControllerPos.getY() +
                                                                             "," +
                                                                             signalControllerPos.getZ() +
                                                                             ")" ) );
                    }
                    else if ( !worldIn.isRemote &&
                            removed &&
                            clickedBlock instanceof BlockControllableTrafficSignalTrainController.BlockCustom ) {
                        player.sendMessage( new TextComponentString(
                                "Train locking rail controller unlinked from signal controller at " +
                                        "(" +
                                        signalControllerPos.getX() +
                                        "," +
                                        signalControllerPos.getY() +
                                        "," +
                                        signalControllerPos.getZ() +
                                        ")" ) );
                    }
                    else if ( !worldIn.isRemote &&
                            removed &&
                            clickedBlock instanceof AbstractBlockControllableCrosswalkAccessory ) {
                        player.sendMessage( new TextComponentString(
                                "Crosswalk accessory unlinked from signal controller at " +
                                        "(" +
                                        signalControllerPos.getX() +
                                        "," +
                                        signalControllerPos.getY() +
                                        "," +
                                        signalControllerPos.getZ() +
                                        ")" ) );
                    }
                    else if ( !worldIn.isRemote && removed ) {
                        player.sendMessage( new TextComponentString( "Signal unlinked from signal controller at " +
                                                                             "(" +
                                                                             signalControllerPos.getX() +
                                                                             "," +
                                                                             signalControllerPos.getY() +
                                                                             "," +
                                                                             signalControllerPos.getZ() +
                                                                             ")" ) );
                    }
                }
                else {
                    if ( !worldIn.isRemote ) {
                        player.sendMessage( new TextComponentString(
                                "Unable to unlink device! Lost connection to previously " + "connected controller." ) );
                    }
                }

                return EnumActionResult.SUCCESS;
            }
            else {
                TileEntity tileEntity = null;
                try {
                    tileEntity = worldIn.getTileEntity( signalControllerPos );
                }
                catch ( Exception e ) {
                    if ( !worldIn.isRemote ) {
                        player.sendMessage( new TextComponentString(
                                "Cannot change circuit until a signal controller has been selected!" ) );
                    }
                }

                if ( tileEntity instanceof TileEntityTrafficSignalController ) {
                    TileEntityTrafficSignalController tileEntityTrafficSignalController
                            = ( TileEntityTrafficSignalController ) tileEntity;
                    if ( !worldIn.isRemote ) {
                        player.sendMessage( new TextComponentString( "Starting: " + circuitLinkIndex ) );
                    }

                    if ( circuitLinkIndex > tileEntityTrafficSignalController.getSignalCircuitCount() ) {
                        circuitLinkIndex = 0;
                    }
                    circuitLinkIndex++;

                    if ( !worldIn.isRemote ) {
                        player.sendMessage( new TextComponentString( "Ending: " + circuitLinkIndex ) );
                        if ( circuitLinkIndex > tileEntityTrafficSignalController.getSignalCircuitCount() ) {
                            player.sendMessage(
                                    new TextComponentString( "Linking to circuit #" + circuitLinkIndex + " (new)" ) );
                        }
                        else {
                            player.sendMessage( new TextComponentString( "Linking to circuit #" + circuitLinkIndex ) );
                        }
                        player.sendMessage( new TextComponentString( "Closing: " + circuitLinkIndex ) );
                    }
                }

                return EnumActionResult.PASS;
            }
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
            list.add( "Link traffic signals to the Primary circuit of a signal controller." );
        }

        @Override
        public int getItemEnchantability() {
            return 0;
        }
    }
}
