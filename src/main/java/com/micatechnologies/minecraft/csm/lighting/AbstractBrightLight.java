package com.micatechnologies.minecraft.csm.lighting;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

public abstract class AbstractBrightLight extends Block
{
    public static final  PropertyDirection FACING        = BlockHorizontal.FACING;
    public static final  PropertyInteger   STATE         = PropertyInteger.create( "state", 0, 3 );
    private static final int               STATE_RS_OFF  = 0;
    private static final int               STATE_RS_ON   = 1;
    private static final int               STATE_MAN_OFF = 2;
    private static final int               STATE_MAN_ON  = 3;

    public AbstractBrightLight() {
        super( Material.ROCK );
        setRegistryName( getBlockRegistryName() );
        setUnlocalizedName( getBlockRegistryName() );
        setSoundType( SoundType.STONE );
        setHarvestLevel( "pickaxe", 1 );
        setHardness( 2F );
        setResistance( 10F );
        setLightLevel( 0F );
        setLightOpacity( 0 );
        setCreativeTab( TabLighting.tab );
        this.setDefaultState( this.blockState.getBaseState()
                                             .withProperty( FACING, EnumFacing.NORTH )
                                             .withProperty( STATE, STATE_RS_OFF ) );
    }

    abstract public String getBlockRegistryName();

    @Override
    public IBlockState getStateFromMeta( int meta ) {
        int stateVal = meta % 4;
        int facingVal = ( meta - stateVal ) / 4;

        return getDefaultState().withProperty( FACING, EnumFacing.getHorizontal( facingVal ) )
                                .withProperty( STATE, stateVal );
    }

    @Override
    public int getMetaFromState( IBlockState state ) {
        int facingVal = state.getValue( FACING ).getHorizontalIndex() * 4;
        int stateVal = state.getValue( STATE );
        return facingVal + stateVal;
    }

    @Override
    public boolean isFullCube( IBlockState state ) {
        return false;
    }

    @Override
    public boolean isOpaqueCube( IBlockState state ) {
        return false;
    }

    @Override
    public void neighborChanged( IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos p_189540_5_ ) {
        // Check for redstone power and set state appropriately
        int currentState = state.getValue( STATE );
        boolean isPowered = world.isBlockPowered( pos );
        if ( currentState == STATE_RS_OFF && isPowered ) {
            // Need to turn on light
            world.setBlockState( pos, state.withProperty( STATE, STATE_RS_ON ), 3 );
            handleAirLightBlock( true, world, pos );
        }
        else if ( currentState == STATE_RS_ON && !isPowered ) {
            // Need to turn off light
            world.setBlockState( pos, state.withProperty( STATE, STATE_RS_OFF ), 3 );
            handleAirLightBlock( false, world, pos );
        }
    }

    private void handleAirLightBlock( boolean on, World world, BlockPos pos ) {
        // Add light
        if ( on ) {
            BlockPos doAddAt = null;
            for ( int findy = -1; findy >= -16; findy-- ) {
                BlockPos test = new BlockPos( pos.getX(), pos.getY() + findy, pos.getZ() );
                // found block that is not air
                if ( world.isAirBlock( test ) ) {
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
        // Remove light
        else {
            for ( int findy = -1; findy >= -16; findy-- ) {
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
    public void onBlockAdded( World p_onBlockAdded_1_, BlockPos p_onBlockAdded_2_, IBlockState p_onBlockAdded_3_ ) {
        p_onBlockAdded_1_.scheduleUpdate( p_onBlockAdded_2_, this, this.tickRate( p_onBlockAdded_1_ ) );
        super.onBlockAdded( p_onBlockAdded_1_, p_onBlockAdded_2_, p_onBlockAdded_3_ );
    }

    @Override
    public void breakBlock( World p_breakBlock_1_, BlockPos p_breakBlock_2_, IBlockState p_breakBlock_3_ ) {
        // Cleanup existing light block (if present)
        handleAirLightBlock( false, p_breakBlock_1_, p_breakBlock_2_ );

        super.breakBlock( p_breakBlock_1_, p_breakBlock_2_, p_breakBlock_3_ );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    public boolean onBlockActivated( World world,
                                     BlockPos pos,
                                     IBlockState state,
                                     EntityPlayer player,
                                     EnumHand hand,
                                     EnumFacing facing,
                                     float p_onBlockActivated_7_,
                                     float p_onBlockActivated_8_,
                                     float p_onBlockActivated_9_ )
    {
        int currentState = state.getValue( STATE );

        if ( currentState == STATE_RS_OFF || currentState == STATE_RS_ON ) {
            world.setBlockState( pos, state.withProperty( STATE, STATE_MAN_OFF ), 3 );
            handleAirLightBlock( false, world, pos );
            if ( player instanceof EntityPlayer && !world.isRemote ) {
                player.sendStatusMessage( new TextComponentString( "§6MANUAL Off" ), ( true ) );
            }
        }
        else if ( currentState == STATE_MAN_OFF ) {
            world.setBlockState( pos, state.withProperty( STATE, STATE_MAN_ON ), 3 );
            handleAirLightBlock( true, world, pos );
            if ( player instanceof EntityPlayer && !world.isRemote ) {
                player.sendStatusMessage( new TextComponentString( "§6MANUAL On" ), ( true ) );
            }
        }
        else {
            boolean isPowered = world.isBlockPowered( pos );
            int autoState = isPowered ? STATE_RS_ON : STATE_RS_OFF;

            world.setBlockState( pos, state.withProperty( STATE, autoState ), 3 );
            handleAirLightBlock( isPowered, world, pos );
            if ( player instanceof EntityPlayer && !world.isRemote ) {
                String autoLog = "§6AUTOMATIC (Has Power: " + isPowered + ")";
                player.sendStatusMessage( new TextComponentString( autoLog ), ( true ) );
            }
        }

        return true;
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
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer( this, FACING, STATE );
    }

    @Override
    public int getLightValue( IBlockState state, IBlockAccess world, BlockPos pos ) {
        boolean shouldLight = false;
        int stateValue = state.getValue( STATE );
        if ( stateValue == STATE_MAN_ON || stateValue == STATE_RS_ON ) {
            shouldLight = true;
        }
        return shouldLight ? 15 : 0;
    }

    @Override
    public boolean canConnectRedstone( IBlockState p_canConnectRedstone_1_,
                                       IBlockAccess p_canConnectRedstone_2_,
                                       BlockPos p_canConnectRedstone_3_,
                                       @Nullable EnumFacing p_canConnectRedstone_4_ )
    {
        return true;
    }
}
