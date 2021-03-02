package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElementsCitySuperMod.ModElement.Tag
public class BlockControllableVerticalAngleUTurnSignal extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:controllableverticalangleuturnsignal" )
    public static final Block block = null;

    public BlockControllableVerticalAngleUTurnSignal( ElementsCitySuperMod instance ) {
        super( instance, 2011 );
    }

    @Override
    public void initElements() {
        elements.blocks.add( () -> new BlockCustom() );
        elements.items.add( () -> new ItemBlock( block ).setRegistryName( block.getRegistryName() ) );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( Item.getItemFromBlock( block ), 0,
                                                    new ModelResourceLocation( "csm:controllableverticalangleuturnsignal",
                                                                               "inventory" ) );
    }

    public static class BlockCustom extends AbstractBlockControllableSignal
    {
        public BlockCustom() {
            super( Material.ROCK );
            setRegistryName( "controllableverticalangleuturnsignal" );
            setUnlocalizedName( "controllableverticalangleuturnsignal" );
            setSoundType( SoundType.STONE );
            setHarvestLevel( "pickaxe", 1 );
            setHardness( 2F );
            setResistance( 10F );
            setLightLevel( 0F );
            setLightOpacity( 0 );
            setCreativeTab( TabTrafficSignals.tab );
            this.setDefaultState(
                    this.blockState.getBaseState().withProperty( FACING, EnumFacing.NORTH ).withProperty( COLOR, 3 ) );
        }

        @Override
        public SIGNAL_SIDE getSignalSide() {
            return SIGNAL_SIDE.LEFT;
        }

    }
}
