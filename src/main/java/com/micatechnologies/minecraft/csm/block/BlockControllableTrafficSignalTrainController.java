package com.micatechnologies.minecraft.csm.block;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.creativetab.TabTrafficSignals;
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
public class BlockControllableTrafficSignalTrainController extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:controllabletrafficsignaltraincontroller" )
    public static final Block block = null;

    public BlockControllableTrafficSignalTrainController( ElementsCitySuperMod instance ) {
        super( instance, 2025 );
    }

    @Override
    public void initElements() {
        elements.blocks.add( () -> new BlockCustom() );
        elements.items.add( () -> new ItemBlock( block ).setRegistryName( block.getRegistryName() ) );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( Item.getItemFromBlock( block ), 0, new ModelResourceLocation(
                "csm:controllabletrafficsignaltraincontroller", "inventory" ) );
    }

    public static class BlockCustom extends AbstractBlockControllableSignal
    {

        public BlockCustom() {
            super( Material.ROCK );
            setRegistryName( "controllabletrafficsignaltraincontroller" );
            setUnlocalizedName( "controllabletrafficsignaltraincontroller" );
            setSoundType( SoundType.GROUND );
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

        /**
         * @param p_getWeakPower_1_
         * @param p_getWeakPower_2_
         * @param p_getWeakPower_3_
         * @param p_getWeakPower_4_
         *
         * @deprecated
         */
        @Override
        public int getWeakPower( IBlockState p_getWeakPower_1_,
                                 IBlockAccess p_getWeakPower_2_,
                                 BlockPos p_getWeakPower_3_,
                                 EnumFacing p_getWeakPower_4_ )
        {
            return this.getStrongPower( p_getWeakPower_1_, p_getWeakPower_2_, p_getWeakPower_3_, p_getWeakPower_4_ );
        }

        /**
         * @param p_canProvidePower_1_
         *
         * @deprecated
         */
        @Override
        public boolean canProvidePower( IBlockState p_canProvidePower_1_ ) {
            return true;
        }

        /**
         * @param p_getStrongPower_1_
         * @param p_getStrongPower_2_
         * @param p_getStrongPower_3_
         * @param p_getStrongPower_4_
         *
         * @deprecated
         */
        @Override
        public int getStrongPower( IBlockState p_getStrongPower_1_,
                                   IBlockAccess p_getStrongPower_2_,
                                   BlockPos p_getStrongPower_3_,
                                   EnumFacing p_getStrongPower_4_ )
        {
            int powerLevel = 0;
            if ( p_getStrongPower_1_.getValue( COLOR ) == 2 ) {
                powerLevel = 15;
            }

            return powerLevel;
        }

        @Override
        public SIGNAL_SIDE getSignalSide() {
            return SIGNAL_SIDE.PROTECTED_AHEAD;
        }

    }
}
