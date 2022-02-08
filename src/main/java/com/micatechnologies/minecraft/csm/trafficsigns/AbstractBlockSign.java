package com.micatechnologies.minecraft.csm.trafficsigns;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class AbstractBlockSign extends Block
{
    public static final PropertyDirection FACING = BlockDirectional.FACING;

    public AbstractBlockSign() {
        super( Material.ROCK );
        setUnlocalizedName( getBlockRegistryName() );
        setSoundType( SoundType.STONE );
        setHarvestLevel( "pickaxe", 1 );
        setHardness( 2F );
        setResistance( 10F );
        setLightLevel( 0F );
        setLightOpacity( 0 );
        setCreativeTab( TabRoadSignTab.tab );
        this.setDefaultState( this.blockState.getBaseState().withProperty( FACING, EnumFacing.NORTH ) );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    protected net.minecraft.block.state.BlockStateContainer createBlockState() {
        return new net.minecraft.block.state.BlockStateContainer( this, new IProperty[]{ FACING } );
    }

    @Override
    public IBlockState withRotation( IBlockState state, Rotation rot ) {
        return state.withProperty( FACING, rot.rotate( ( EnumFacing ) state.getValue( FACING ) ) );
    }

    @Override
    public IBlockState withMirror( IBlockState state, Mirror mirrorIn ) {
        return state.withRotation( mirrorIn.toRotation( ( EnumFacing ) state.getValue( FACING ) ) );
    }

    @Override
    public IBlockState getStateFromMeta( int meta ) {
        return this.getDefaultState().withProperty( FACING, EnumFacing.getFront( meta ) );
    }

    @Override
    public int getMetaFromState( IBlockState state ) {
        return ( ( EnumFacing ) state.getValue( FACING ) ).getIndex();
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
        return this.getDefaultState().withProperty( FACING, EnumFacing.getDirectionFromEntityLiving( pos, placer ) );
    }

    @Override
    public boolean isOpaqueCube( IBlockState state ) {
        return false;
    }

    abstract public String getBlockRegistryName();
}
