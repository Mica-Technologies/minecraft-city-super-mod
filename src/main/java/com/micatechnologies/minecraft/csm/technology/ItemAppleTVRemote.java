package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.tabs.CsmTabTechnology;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

@ElementsCitySuperMod.ModElement.Tag
public class ItemAppleTVRemote extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:appletvremote" )
    public static final Item block = null;

    public ItemAppleTVRemote( ElementsCitySuperMod instance ) {
        super( instance, 239 );
    }

    @Override
    public void initElements() {
        elements.items.add( () -> new ItemCustom() );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( block, 0,
                                                    new ModelResourceLocation( "csm:appletvremote", "inventory" ) );
    }

    public static class ItemCustom extends Item
    {
        public ItemCustom() {
            setMaxDamage( 0 );
            maxStackSize = 64;
            setUnlocalizedName( "appletvremote" );
            setRegistryName( "appletvremote" );
            setCreativeTab( CsmTabTechnology.get() );
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
            list.add( "This remote does nothing and is only for looks!" );
        }

        @Override
        public int getItemEnchantability() {
            return 0;
        }
    }
}
