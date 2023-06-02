package com.micatechnologies.minecraft.csm.trafficsigns;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElementsCitySuperMod.ModElement.Tag
public class BlockSign extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:sign" )
    public static final Block block = null;

    public BlockSign( ElementsCitySuperMod instance ) {
        super( instance, 247 );
    }

    @Override
    public void initElements() {
        elements.blocks.add( () -> new BlockCustom().setRegistryName( "sign" ) );
        elements.items.add( () -> new ItemBlock( block ).setRegistryName( block.getRegistryName() ) );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( Item.getItemFromBlock( block ), 0,
                                                    new ModelResourceLocation( "csm:sign", "inventory" ) );
    }

    public static class BlockCustom extends AbstractBlockSign
    {
        @Override
        public String getBlockRegistryName() {
            return "sign";
        }
    }
}
