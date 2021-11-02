package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
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
public class BlockControllableCrosswalkButtonMale extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:controllablecrosswalkbuttonmale" )
    public static final Block block = null;

    public BlockControllableCrosswalkButtonMale( ElementsCitySuperMod instance ) {
        super( instance, 2019 );
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
                                                    new ModelResourceLocation( "csm:controllablecrosswalkbuttonmale",
                                                                               "inventory" ) );
    }

    public static class BlockCustom extends AbstractBlockControllableCrosswalkAccessory
    {
        final int lenOfWalkSound           = 140;
        final int lenOfDontWalkSound       = 30;
        final int lenOfNotSafeToCrossSound = 140;

        public BlockCustom() {
            super( Material.ROCK );
            setRegistryName( "controllablecrosswalkbuttonmale" );
            setUnlocalizedName( "controllablecrosswalkbuttonmale" );
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

        private int lastColor = 0;

        @Override
        public AxisAlignedBB getBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {
            switch ( state.getValue( FACING ) ) {
                case SOUTH:
                default:
                    return new AxisAlignedBB( 1D, 0D, 0.2D, 0D, 1D, 0D );
                case NORTH:
                    return new AxisAlignedBB( 0D, 0D, 0.8D, 1D, 1D, 1D );
                case WEST:
                    return new AxisAlignedBB( 0.8D, 0D, 1D, 1D, 1D, 0D );
                case EAST:
                    return new AxisAlignedBB( 0.2D, 0D, 0D, 0D, 1D, 1D );
                case UP:
                    return new AxisAlignedBB( 0D, 0.2D, 0D, 1D, 0D, 1D );
                case DOWN:
                    return new AxisAlignedBB( 0D, 0.8D, 1D, 1D, 1D, 0D );
            }
        }

        @Override
        public boolean onBlockActivated( World p_onBlockActivated_1_,
                                         BlockPos p_onBlockActivated_2_,
                                         IBlockState p_onBlockActivated_3_,
                                         EntityPlayer p_onBlockActivated_4_,
                                         EnumHand p_onBlockActivated_5_,
                                         EnumFacing p_onBlockActivated_6_,
                                         float p_onBlockActivated_7_,
                                         float p_onBlockActivated_8_,
                                         float p_onBlockActivated_9_ )
        {
            if ( p_onBlockActivated_4_.inventory.getCurrentItem() != null &&
                            p_onBlockActivated_4_.inventory.getCurrentItem()
                                                           .getItem() instanceof ItemNSSignalLinker.ItemCustom ) {
                return super.onBlockActivated( p_onBlockActivated_1_, p_onBlockActivated_2_, p_onBlockActivated_3_,
                                               p_onBlockActivated_4_, p_onBlockActivated_5_, p_onBlockActivated_6_,
                                               p_onBlockActivated_7_, p_onBlockActivated_8_, p_onBlockActivated_9_ );
            }

            if ( p_onBlockActivated_3_.getValue( COLOR ) == 0 ) {
                p_onBlockActivated_1_.playSound( null, p_onBlockActivated_2_.getX(), p_onBlockActivated_2_.getY(),
                                                 p_onBlockActivated_2_.getZ(),
                                                 net.minecraft.util.SoundEvent.REGISTRY.getObject(
                                                         new ResourceLocation( "csm" + ":male_wait" ) ),
                                                 SoundCategory.NEUTRAL, ( float ) 1, ( float ) 1 );
            }

            return true;
        }

        @Override
        public void updateTick( World p_updateTick_1_,
                                BlockPos p_updateTick_2_,
                                IBlockState p_updateTick_3_,
                                Random p_updateTick_4_ )
        {
            int color = p_updateTick_3_.getValue( COLOR );
            if ( color == 0 ) {
                // Play beep
                p_updateTick_1_.playSound( null, p_updateTick_2_.getX(), p_updateTick_2_.getY(),
                                           p_updateTick_2_.getZ(),
                                           net.minecraft.util.SoundEvent.REGISTRY.getObject(new ResourceLocation( "csm:male_beep")),
                                           SoundCategory.NEUTRAL, (float) 1, (float) 1);

                p_updateTick_1_.scheduleUpdate( p_updateTick_2_, this, lenOfDontWalkSound );
            }
            else if ( color == 1 ) {
                // Play walk voice
                p_updateTick_1_.playSound( null, p_updateTick_2_.getX(), p_updateTick_2_.getY(), p_updateTick_2_.getZ(),
                                           net.minecraft.util.SoundEvent.REGISTRY.getObject(
                                                   new ResourceLocation( "csm:male_crosswalk_on" ) ),
                                           SoundCategory.NEUTRAL, ( float ) 1, ( float ) 1 );

                p_updateTick_1_.scheduleUpdate( p_updateTick_2_, this, lenOfWalkSound );
            }
            else {
                p_updateTick_1_.scheduleUpdate( p_updateTick_2_, this, this.tickRate( p_updateTick_3_ ) );
            }

            lastColor = color;
        }

        @Override
        public void onBlockAdded( World p_onBlockAdded_1_, BlockPos p_onBlockAdded_2_, IBlockState p_onBlockAdded_3_ ) {
            p_onBlockAdded_1_.scheduleUpdate( p_onBlockAdded_2_, this, this.tickRate( p_onBlockAdded_3_ ) );
            super.onBlockAdded( p_onBlockAdded_1_, p_onBlockAdded_2_, p_onBlockAdded_3_ );
        }

        public int tickRate( IBlockState p_tickRate_1_ ) {
            int len = lenOfWalkSound;
            if ( p_tickRate_1_.getValue( COLOR ) == 0 ) {
                len = lenOfDontWalkSound;
            }
            return len;
        }
    }
}
