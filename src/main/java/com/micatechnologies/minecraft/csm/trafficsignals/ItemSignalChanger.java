package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
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

import java.util.List;

@ElementsCitySuperMod.ModElement.Tag
public class ItemSignalChanger extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:signalchanger" )
    public static final Item block = null;

    public ItemSignalChanger( ElementsCitySuperMod instance ) {
        super( instance, 2138 );
    }

    @Override
    public void initElements() {
        elements.items.add( () -> new ItemCustom() );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( block, 0, new ModelResourceLocation( "csm:wrench", "inventory" ) );
    }

    public static class ItemCustom extends Item
    {

        public ItemCustom() {
            setMaxDamage( 0 );
            maxStackSize = 1;
            setUnlocalizedName( "signalchanger" );
            setRegistryName( "signalchanger" );
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
            Block clickedBlock = state.getBlock();

            if ( clickedBlock instanceof AbstractBlockControllableSignal ) {
                worldIn.setBlockState( pos, state.cycleProperty( AbstractBlockControllableSignal.COLOR ) );

                if ( !worldIn.isRemote ) {
                    player.sendMessage( new TextComponentString( "Cycling traffic signal state!" ) );
                }
                return EnumActionResult.PASS;

            }
            else if ( clickedBlock instanceof AbstractBlockTrafficSignalSensor ) {
                worldIn.setBlockState( pos, state.withProperty( AbstractBlockTrafficSignalSensor.FACING,
                                                                player.getHorizontalFacing().getOpposite() ) );

                if ( !worldIn.isRemote ) {
                    player.sendMessage( new TextComponentString( "Re-oriented traffic signal sensor!" ) );
                }
                return EnumActionResult.PASS;

            }
            else if ( !player.isSneaking() && clickedBlock instanceof BlockTrafficSignalController.BlockCustom ) {
                TileEntity tileEntity = worldIn.getTileEntity( pos );
                if ( tileEntity instanceof TileEntityTrafficSignalController ) {
                    TileEntityTrafficSignalController tileEntityTrafficSignalController
                            = ( TileEntityTrafficSignalController ) tileEntity;
                    if ( player.isInLava() ) {
                        // Invert nighttime flash setting on controller
                        boolean newSetting = !tileEntityTrafficSignalController.getNightlyFallbackToFlashMode();
                        tileEntityTrafficSignalController.setNightlyFallbackToFlashMode( newSetting );
                        if ( !worldIn.isRemote ) {
                            player.sendMessage( new TextComponentString(
                                    "Set traffic signal controller nightly flash setting to " + newSetting ) );
                        }
                    }
                    else if ( player.isInWater() ) {
                        // Invert power loss flash setting on controller
                        boolean newSetting = !tileEntityTrafficSignalController.getPowerLossFallbackToFlashMode();
                        tileEntityTrafficSignalController.setPowerLossFallbackToFlashMode( newSetting );
                        if ( !worldIn.isRemote ) {
                            player.sendMessage( new TextComponentString(
                                    "Set traffic signal controller power loss flash setting to " + newSetting ) );
                        }
                    }
                    else {
                        // Invert overlap pedestrian signals setting on controller
                        boolean newSetting = !tileEntityTrafficSignalController.getOverlapPedestrianSignals();
                        tileEntityTrafficSignalController.setOverlapPedestrianSignals( newSetting );
                        if ( !worldIn.isRemote ) {
                            player.sendMessage( new TextComponentString(
                                    "Set traffic signal controller overlap pedestrian signals setting to " +
                                            newSetting ) );
                        }
                    }
                }
            }
            else if ( player.isSneaking() && clickedBlock instanceof BlockTrafficSignalController.BlockCustom ) {
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

                    // Display player output only if not remote
                    if ( !worldIn.isRemote ) {
                        // Display player output in fault state
                        if ( isInFaultState ) {
                            player.sendMessage( new TextComponentString( "Controller fault state has been reset." ) );
                            player.sendMessage(
                                    new TextComponentString( "Cleared controller fault message: " + faultMessage ) );
                        }
                        // Display player output in non-fault state
                        else {
                            player.sendMessage(
                                    new TextComponentString( "Controller is not in fault state. Unable to reset!" ) );
                        }
                    }
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
            list.add( "Change the color of a traffic signal manually. This does not permanently override signals " +
                              "that have been connected to a controller!" );
        }

        @Override
        public int getItemEnchantability() {
            return 0;
        }
    }
}
