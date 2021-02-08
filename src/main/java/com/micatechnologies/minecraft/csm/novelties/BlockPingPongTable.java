package com.micatechnologies.minecraft.csm.novelties;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElementsCitySuperMod.ModElement.Tag
public class BlockPingPongTable extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:pingpongtable" )
    public static final Block block = null;

    public BlockPingPongTable( ElementsCitySuperMod instance ) {
        super( instance, 1116 );
    }

    @Override
    public void initElements() {
        elements.blocks.add( () -> new BlockCustom().setRegistryName( "pingpongtable" ) );
        elements.items.add( () -> new ItemBlock( block ).setRegistryName( block.getRegistryName() ) );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( Item.getItemFromBlock( block ), 0,
                                                    new ModelResourceLocation( "csm:pingpongtable", "inventory" ) );
    }

    public static class BlockCustom extends Block
    {
        public BlockCustom() {
            super( Material.ROCK );
            setUnlocalizedName( "pingpongtable" );
            setSoundType( SoundType.STONE );
            setHardness( 1F );
            setResistance( 10F );
            setLightLevel( 0F );
            setLightOpacity( 0 );
            setCreativeTab( TabNovelties.tab );
        }

        @Override
        public boolean isOpaqueCube( IBlockState state ) {
            return false;
        }

        @SideOnly( Side.CLIENT )
        @Override
        public BlockRenderLayer getBlockLayer() {
            return BlockRenderLayer.CUTOUT_MIPPED;
        }
    }
}
