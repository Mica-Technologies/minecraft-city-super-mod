package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.tabs.CsmTabRoadSigns;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@MethodsReturnNonnullByDefault
public abstract class AbstractBlockSign extends Block
{
    public static final PropertyDirection FACING = BlockHorizontal.FACING;

    public AbstractBlockSign() {
        super( Material.ROCK );
        setUnlocalizedName( getBlockRegistryName() );
        setSoundType( SoundType.STONE );
        setHarvestLevel( "pickaxe", 1 );
        setHardness( 2F );
        setResistance( 10F );
        setLightLevel( 0F );
        setLightOpacity( 0 );
        setCreativeTab( CsmTabRoadSigns.get() );
        this.setDefaultState( this.blockState.getBaseState().withProperty( FACING, EnumFacing.NORTH ) );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    protected net.minecraft.block.state.BlockStateContainer createBlockState() {
        return new net.minecraft.block.state.BlockStateContainer( this, FACING );
    }

    @Override
    public IBlockState getStateFromMeta( int meta ) {
        int facingVal = meta;
        // Convert old directional WEST to new horizontal WEST
        if ( facingVal == 4 ) {
            facingVal = 1;
        }
        // Convert old directional EAST to new horizontal EAST
        else if ( facingVal == 5 ) {
            facingVal = 3;
        }
        // Otherwise, fallback to SOUTH (0) if invalid
        else if ( facingVal < 0 || facingVal > 3 ) {
            facingVal = 0;
        }
        return getDefaultState().withProperty( FACING, EnumFacing.getHorizontal( facingVal ) );
    }

    @Override
    public int getMetaFromState( IBlockState state ) {
        return state.getValue( FACING ).getHorizontalIndex();
    }

    @Override
    public void onBlockPlacedBy( World world,
                                 BlockPos pos,
                                 IBlockState state,
                                 EntityLivingBase placer,
                                 ItemStack stack )
    {
        world.setBlockState( pos, state.withProperty( FACING, placer.getHorizontalFacing().getOpposite() ), 2 );
    }

    @Override
    public boolean isOpaqueCube( IBlockState state ) {
        return false;
    }

    abstract public String getBlockRegistryName();
}
