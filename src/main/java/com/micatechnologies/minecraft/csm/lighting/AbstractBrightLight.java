package com.micatechnologies.minecraft.csm.lighting;

import com.micatechnologies.minecraft.csm.creativetab.TabLighting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

public abstract class AbstractBrightLight extends Block
{

    public static final PropertyDirection FACING  = BlockDirectional.FACING;
    public static final PropertyBool      POWERED = PropertyBool.create( "powered" );

    public AbstractBrightLight() {
        super( Material.ROCK );
        setRegistryName( getBlockRegistryName() );
        setUnlocalizedName( getBlockRegistryName() );
        setSoundType( SoundType.GROUND );
        setHarvestLevel( "pickaxe", 1 );
        setHardness( 2F );
        setResistance( 10F );
        setLightLevel( 0F );
        setLightOpacity( 0 );
        setCreativeTab( TabLighting.tab );
        this.setDefaultState( this.blockState.getBaseState().withProperty( FACING, EnumFacing.NORTH ) );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    public boolean canConnectRedstone( IBlockState p_canConnectRedstone_1_,
                                       IBlockAccess p_canConnectRedstone_2_,
                                       BlockPos p_canConnectRedstone_3_,
                                       @Nullable EnumFacing p_canConnectRedstone_4_ )
    {
        return true;
    }

    @Override
    public boolean isFullCube( IBlockState state ) {
        return false;
    }

    @Override
    public void neighborChanged( IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos p_189540_5_ ) {
        boolean powered = world.isBlockPowered( pos );
        world.setBlockState( pos, state.withProperty( POWERED, powered ), 3 );
        // Powered, Add Light
        if ( powered ) {
            BlockPos doAddAt = null;
            for ( int findy = -1; findy >= -40; findy-- ) {
                BlockPos test = new BlockPos( pos.getX(), pos.getY() + findy, pos.getZ() );
                IBlockState bs = world.getBlockState( test );
                Block block = bs.getBlock();
                // found block that is not air
                if ( block.isAir( bs, world, test ) || block.isReplaceable( world, test ) ) {
                    // dont add light if block is right below street light
                    if ( findy < -1 ) {
                        doAddAt = test;
                    }
                }
                else {
                    break;
                }
            }
            // add if marked for add
            if ( doAddAt != null ) {
                world.setBlockState( doAddAt, Block.getBlockFromName( "csm:lightupair" ).getDefaultState(), 3 );
            }
        }
        // Not powered, remove light
        else {
            for ( int findy = -1; findy >= -40; findy-- ) {
                BlockPos test = new BlockPos( pos.getX(), pos.getY() + findy, pos.getZ() );
                IBlockState bs = world.getBlockState( test );
                // stop removing light once hit block
                if ( bs.getBlock() == Block.getBlockFromName( "csm:lightupair" ) ) {
                    world.setBlockToAir( test );
                }
            }
        }
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
    public IBlockState getStateFromMeta( int meta ) {
        return getDefaultState().withProperty( FACING, EnumFacing.getFront( meta & 7 ) )
                                .withProperty( POWERED, ( meta & 8 ) != 0 );
    }

    @Override
    public int getMetaFromState( IBlockState state ) {
        return state.getValue( FACING ).getIndex() + ( state.getValue( POWERED ) ? 8 : 0 );
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer( this, FACING, POWERED );
    }

    @Override
    public int getLightValue( IBlockState state, IBlockAccess world, BlockPos pos ) {
        return state.getValue( POWERED ) ? 15 : 0;
    }

    @Override
    public boolean isOpaqueCube( IBlockState state ) {
        return false;
    }

    abstract public String getBlockRegistryName();
}
