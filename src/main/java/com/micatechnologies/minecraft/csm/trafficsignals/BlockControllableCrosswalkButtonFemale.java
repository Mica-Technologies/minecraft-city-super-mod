package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalRequester;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElementsCitySuperMod.ModElement.Tag
public class BlockControllableCrosswalkButtonFemale extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:controllablecrosswalkbuttonfemale" )
    public static final Block block = null;

    public BlockControllableCrosswalkButtonFemale( ElementsCitySuperMod instance ) {
        super( instance, 2018 );
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
                                                    new ModelResourceLocation( "csm:controllablecrosswalkbuttonfemale",
                                                                               "inventory" ) );
    }

    public static class BlockCustom extends AbstractBlockTrafficSignalRequester
    {

        public BlockCustom() {
            super( Material.ROCK );
            setRegistryName( "controllablecrosswalkbuttonfemale" );
            setUnlocalizedName( "controllablecrosswalkbuttonfemale" );
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
        public int getLightValue( IBlockState state, IBlockAccess world, BlockPos pos ) {
            return 0;
        }
    }
}
