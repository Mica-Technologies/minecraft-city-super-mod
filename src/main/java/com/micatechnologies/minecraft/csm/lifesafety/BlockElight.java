package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElementsCitySuperMod.ModElement.Tag
public class BlockElight extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:elight" )
    public static final Block block = null;

    public BlockElight( ElementsCitySuperMod instance ) {
        super( instance, 275 );
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
                                                    new ModelResourceLocation( "csm:elight", "inventory" ) );
    }

    public static class BlockCustom extends Block
    {
        public static final PropertyDirection FACING = BlockDirectional.FACING;
        public static final PropertyBool POWERED = PropertyBool.create( "powered" );
        private             boolean           red    = false;

        public BlockCustom() {
            super( Material.ROCK );
            setRegistryName( "elight" );
            setUnlocalizedName( "elight" );
            setSoundType( SoundType.STONE );
            setHarvestLevel( "pickaxe", 1 );
            setHardness( 2F );
            setResistance( 10F );
            setLightLevel( 0F );
            setLightOpacity( 0 );
            setCreativeTab( TabFireAlarms.tab );
            this.setDefaultState( this.blockState.getBaseState().withProperty( FACING, EnumFacing.NORTH ) );
        }

        @Override
        public IBlockState getStateFromMeta( int meta ) {
            return getDefaultState().withProperty( FACING, EnumFacing.getFront( meta & 7 ) )
                                    .withProperty( POWERED, ( meta & 8 ) != 0 );
        }

        @Override
        public int getMetaFromState( IBlockState state ) {
            return state.getValue( FACING ).getIndex() + ( state.getValue( POWERED ) ? 8 : 0 );
        }

        @Override
        public boolean isFullCube( IBlockState state ) {
            return false;
        }

        @Override
        public AxisAlignedBB getBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {
            switch ( ( EnumFacing ) state.getValue( BlockDirectional.FACING ) ) {
                case SOUTH:
                default:
                    return new AxisAlignedBB( 1D, 0.2D, 0.2D, 0D, 0.8D, 0D );
                case NORTH:
                    return new AxisAlignedBB( 0D, 0.2D, 0.8D, 1D, 0.8D, 1D );
                case WEST:
                    return new AxisAlignedBB( 0.8D, 0.2D, 1D, 1D, 0.8D, 0D );
                case EAST:
                    return new AxisAlignedBB( 0.2D, 0.2D, 0D, 0D, 0.8D, 1D );
                case UP:
                    return new AxisAlignedBB( 0D, 0.2D, 0.2D, 1D, 0D, 0.8D );
                case DOWN:
                    return new AxisAlignedBB( 0D, 0.8D, 0.8D, 1D, 1D, 0.2D );
            }
        }

        @Override
        public boolean isOpaqueCube( IBlockState state ) {
            return false;
        }

        @Override
        public void neighborChanged( IBlockState state,
                                     World world,
                                     BlockPos pos,
                                     Block blockIn,
                                     BlockPos p_189540_5_ )
        {
            int powered = world.isBlockIndirectlyGettingPowered( pos );
            world.setBlockState( pos, state.withProperty( POWERED, powered > 0 ), 3 );
        }

        @Override
        public int getWeakPower( IBlockState state, IBlockAccess baccess, BlockPos pos, EnumFacing side ) {
            return red ? 15 : 0;
        }

        @Override
        public int getStrongPower( IBlockState state, IBlockAccess baccess, BlockPos pos, EnumFacing side ) {
            return red ? 15 : 0;
        }

        @Override
        public void onBlockPlacedBy( World world,
                                     BlockPos pos,
                                     IBlockState state,
                                     EntityLivingBase placer,
                                     ItemStack stack )
        {
            world.setBlockState( pos, state.withProperty( FACING, getFacingFromEntity( pos, placer ) ), 2 );
        }

        public static EnumFacing getFacingFromEntity( BlockPos clickedBlock, EntityLivingBase entity ) {
            return EnumFacing.getFacingFromVector( ( float ) ( entity.posX - clickedBlock.getX() ),
                                                   ( float ) ( entity.posY - clickedBlock.getY() ),
                                                   ( float ) ( entity.posZ - clickedBlock.getZ() ) );
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer( this, FACING, POWERED );
        }

        @Override
        public int getLightValue( IBlockState state, IBlockAccess world, BlockPos pos ) {
            return state.getValue( POWERED ) == false ? 15 : 0;
        }

        @Override
        public boolean canConnectRedstone( IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side ) {
            return true;
        }
    }
}
