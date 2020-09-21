package com.micatechnologies.minecraft.csm.item;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.block.*;
import com.micatechnologies.minecraft.csm.creativetab.TabTrafficSignals;
import com.micatechnologies.minecraft.csm.tiles.TileEntityTrafficSignalController;
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
        ModelLoader.setCustomModelResourceLocation( block, 0,
                                                    new ModelResourceLocation( "csm:wrench", "inventory" ) );
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
                    player.sendMessage(
                            new TextComponentString( "Cycling traffic signal state!" ) );
                }
                return EnumActionResult.PASS;

            }
            return EnumActionResult.FAIL;
        }

        @Override
        public int getItemEnchantability() {
            return 0;
        }

        @Override
        public int getMaxItemUseDuration( ItemStack itemstack ) {
            return 0;
        }

        @Override
        public float getDestroySpeed( ItemStack par1ItemStack, IBlockState par2Block ) {
            return 1F;
        }

        @Override
        public void addInformation( ItemStack itemstack, World world, List< String > list, ITooltipFlag flag ) {
            super.addInformation( itemstack, world, list, flag );
            list.add( "Change the color of a traffic signal manually. This does not permanently override signals " +
                              "that have been connected to a controller!" );
        }
    }
}
