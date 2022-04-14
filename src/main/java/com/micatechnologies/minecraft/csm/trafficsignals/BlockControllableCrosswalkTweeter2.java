package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableCrosswalkAccessory;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

@ElementsCitySuperMod.ModElement.Tag
public class BlockControllableCrosswalkTweeter2 extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:controllablecrosswalktweeter2" )
    public static final Block block = null;

    public BlockControllableCrosswalkTweeter2( ElementsCitySuperMod instance ) {
        super( instance, 2024 );
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
                                                    new ModelResourceLocation( "csm:controllablecrosswalktweeter2",
                                                                               "inventory" ) );
    }

    public static class BlockCustom extends AbstractBlockControllableCrosswalkAccessory
    {
        final int lenOfTweetSound           = 40;

        public BlockCustom() {
            super( Material.ROCK );
            setRegistryName( "controllablecrosswalktweeter2" );
            setUnlocalizedName( "controllablecrosswalktweeter2" );
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
        public int getLightValue( IBlockState state, IBlockAccess world, BlockPos pos ) {
            return 0;
        }

        @Override
        public void updateTick( World p_updateTick_1_,
                                BlockPos p_updateTick_2_,
                                IBlockState p_updateTick_3_,
                                Random p_updateTick_4_ )
        {
            int color = p_updateTick_3_.getValue( COLOR );
            if ( color == 2 ) {
                // Play cookoo sound
                p_updateTick_1_.playSound( null, p_updateTick_2_.getX(), p_updateTick_2_.getY(), p_updateTick_2_.getZ(),
                                           net.minecraft.util.SoundEvent.REGISTRY.getObject(
                                                   new ResourceLocation( "csm:crosswalk_cookoo_2" ) ),
                                           SoundCategory.NEUTRAL, ( float ) 1, ( float ) 1 );
            }
            p_updateTick_1_.scheduleUpdate( p_updateTick_2_, this, this.tickRate( p_updateTick_1_ ) );
        }

        @Override
        public void onBlockAdded( World p_onBlockAdded_1_, BlockPos p_onBlockAdded_2_, IBlockState p_onBlockAdded_3_ ) {
            p_onBlockAdded_1_.scheduleUpdate( p_onBlockAdded_2_, this, this.tickRate( p_onBlockAdded_1_ ) );
            super.onBlockAdded( p_onBlockAdded_1_, p_onBlockAdded_2_, p_onBlockAdded_3_ );
        }

        @Override
        public int tickRate( World p_tickRate_1_ ) {
            return lenOfTweetSound;
        }
    }
}
