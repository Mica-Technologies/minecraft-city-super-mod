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

        private BlockPos signalControllerPos = null;

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
            if ( state.getBlock() instanceof BlockTrafficSignalController.BlockCustom ) {
                if ( !worldIn.isRemote ) {
                    player.sendMessage( new TextComponentString( "This linker is not currently active. Stay tuned!" ) );
                }
                return EnumActionResult.SUCCESS;
            }
            else if ( signalControllerPos == null && state.getBlock() instanceof AbstractBlockControllableSignal ) {
                if ( !worldIn.isRemote ) {
                    player.sendMessage( new TextComponentString( "This linker is not currently active. Stay tuned!" ) );
                }
                return EnumActionResult.SUCCESS;
            }
            else if ( signalControllerPos != null && state.getBlock() instanceof AbstractBlockControllableSignal ) {
                if ( !worldIn.isRemote ) {
                    player.sendMessage( new TextComponentString( "This linker is not currently active. Stay tuned!" ) );
                }
                return EnumActionResult.SUCCESS;
            }
            return EnumActionResult.PASS;
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
