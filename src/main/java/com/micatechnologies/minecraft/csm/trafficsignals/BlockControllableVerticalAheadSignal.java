package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@ElementsCitySuperMod.ModElement.Tag
public class BlockControllableVerticalAheadSignal extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:controllableverticalaheadsignal" )
    public static final Block block = null;

    public BlockControllableVerticalAheadSignal( ElementsCitySuperMod instance ) {
        super( instance, 2016 );
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
                                                    new ModelResourceLocation( "csm:controllableverticalaheadsignal",
                                                                               "inventory" ) );
    }

    public static class BlockCustom extends AbstractBlockControllableSignal
    {
        public BlockCustom() {
            super( Material.ROCK );
            setRegistryName( "controllableverticalaheadsignal" );
            setUnlocalizedName( "controllableverticalaheadsignal" );
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
        public SIGNAL_SIDE getSignalSide( World world, BlockPos blockPos ) {
            return SIGNAL_SIDE.THROUGH;
        }

        @Override
        public boolean doesFlash() {
            return true;
        }

    }
}
