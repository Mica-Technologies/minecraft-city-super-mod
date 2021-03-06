package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElementsCitySuperMod.ModElement.Tag
public class BlockPV extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:pv" )
    public static final Block block = null;

    public BlockPV( ElementsCitySuperMod instance ) {
        super( instance, 767 );
    }

    @Override
    public void initElements() {
        elements.blocks.add( () -> new BlockCustom().setRegistryName( "pv" ) );
        elements.items.add( () -> new ItemBlock( block ).setRegistryName( block.getRegistryName() ) );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( Item.getItemFromBlock( block ), 0,
                                                    new ModelResourceLocation( "csm:pv", "inventory" ) );
    }

    public static class BlockCustom extends Block
    {
        public static final PropertyDirection FACING = BlockDirectional.FACING;

        public BlockCustom() {
            super( Material.ROCK );
            setUnlocalizedName( "pv" );
            setSoundType( SoundType.STONE );
            setHarvestLevel( "pickaxe", 1 );
            setHardness( 2F );
            setResistance( 10F );
            setLightLevel( 0F );
            setLightOpacity( 255 );
            setCreativeTab( TabHVAC.tab );
            this.setDefaultState( this.blockState.getBaseState().withProperty( FACING, EnumFacing.NORTH ) );
        }

        @Override
        public IBlockState getStateFromMeta( int meta ) {
            return this.getDefaultState().withProperty( FACING, EnumFacing.getFront( meta ) );
        }

        @Override
        public int getMetaFromState( IBlockState state ) {
            return state.getValue( FACING ).getIndex();
        }

        @Override
        public IBlockState withRotation( IBlockState state, Rotation rot ) {
            return state.withProperty( FACING, rot.rotate( state.getValue( FACING ) ) );
        }

        @Override
        public IBlockState withMirror( IBlockState state, Mirror mirrorIn ) {
            return state.withRotation( mirrorIn.toRotation( state.getValue( FACING ) ) );
        }

        @Override
        public IBlockState getStateForPlacement( World worldIn,
                                                 BlockPos pos,
                                                 EnumFacing facing,
                                                 float hitX,
                                                 float hitY,
                                                 float hitZ,
                                                 int meta,
                                                 EntityLivingBase placer )
        {
            return this.getDefaultState()
                       .withProperty( FACING, EnumFacing.getDirectionFromEntityLiving( pos, placer ) );
        }

        @Override
        protected net.minecraft.block.state.BlockStateContainer createBlockState() {
            return new net.minecraft.block.state.BlockStateContainer( this, FACING );
        }
    }
}
