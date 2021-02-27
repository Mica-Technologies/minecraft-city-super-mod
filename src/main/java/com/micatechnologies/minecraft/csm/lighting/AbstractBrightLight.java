package com.micatechnologies.minecraft.csm.lighting;

import com.micatechnologies.minecraft.csm.codeutils.TickTimeConverter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

public abstract class AbstractBrightLight extends Block
{
    private static final int STATE_RS_OFF   = 0;
    private static final int STATE_RS_ON    = 1;
    private static final int STATE_AUTO_OFF = 2;
    private static final int STATE_AUTO_ON  = 3;

    private static final int LIGHT_LEVEL_TURN_ON = 10;

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyInteger   STATE  = PropertyInteger.create( "state", 0, 3 );

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
        int currentState = state.getValue( STATE );
        // Check if in redstone mode
        if ( currentState == STATE_RS_OFF || currentState == STATE_RS_ON ) {
            // Check for redstone power and set state appropriately
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
    }

    private void handleAirLightBlock( boolean on, World world, BlockPos pos ) {
        // Add light
        if ( on ) {
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
        // Remove light
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
        if ( blockHasAutomaticFunctionality() ) {
            // Cycle to on if off
            if ( currentState == STATE_RS_OFF ) {
                world.setBlockState( pos, state.withProperty( STATE, STATE_RS_ON ), 3 );
                handleAirLightBlock( true, world, pos );

                if ( !world.isRemote ) {
                    player.sendMessage( new TextComponentString( "Changed light to: ON (Manual/Redstone Control)" ) );
                }
            }
            // Cycle to auto if on
            else if ( currentState == STATE_RS_ON ) {
                world.setBlockState( pos, state.withProperty( STATE, STATE_AUTO_OFF ), 3 );
                handleAirLightBlock( false, world, pos );
                if ( !world.isRemote ) {
                    player.sendMessage(
                            new TextComponentString( "Changed light to: AUTO (Light Sensor Control)" ) );
                }
            }
            // Cycle to off if auto
            else if ( currentState == STATE_AUTO_OFF || currentState == STATE_AUTO_ON ) {
                world.setBlockState( pos, state.withProperty( STATE, STATE_RS_OFF ), 3 );
                handleAirLightBlock( false, world, pos );
                if ( !world.isRemote ) {
                    player.sendMessage( new TextComponentString( "Changed light to: OFF (Manual/Redstone Control)" ) );
                }
            }
        }
        else {
            // Show warning if light is powered by redstone
            if ( !world.isRemote && world.isBlockPowered( pos ) ) {
                player.sendMessage( new TextComponentString( "Warning: The clicked light is connected to redstone " +
                                                                     "power and cannot be properly controlled manually!" ) );
            }

            // Cycle to on if off
            if ( currentState == STATE_RS_OFF ) {
                world.setBlockState( pos, state.withProperty( STATE, STATE_RS_ON ), 3 );
                handleAirLightBlock( true, world, pos );

                if ( !world.isRemote ) {
                    player.sendMessage( new TextComponentString( "Changed light to: ON (Manual/Redstone Control)" ) );
                }
            }
            // Cycle to off if on
            else if ( currentState == STATE_RS_ON ) {
                world.setBlockState( pos, state.withProperty( STATE, STATE_RS_OFF ), 3 );
                handleAirLightBlock( false, world, pos );
                if ( !world.isRemote ) {
                    player.sendMessage( new TextComponentString( "Changed light to: OFF (Manual/Redstone Control)" ) );
                }
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
    public void onBlockAdded( World p_onBlockAdded_1_, BlockPos p_onBlockAdded_2_, IBlockState p_onBlockAdded_3_ ) {
        p_onBlockAdded_1_.scheduleUpdate( p_onBlockAdded_2_, this, this.tickRate( p_onBlockAdded_1_ ) );
        super.onBlockAdded( p_onBlockAdded_1_, p_onBlockAdded_2_, p_onBlockAdded_3_ );
    }

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
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer( this, FACING, STATE );
    }

    @Override
    public int getLightValue( IBlockState state, IBlockAccess world, BlockPos pos ) {
        int currentState = state.getValue( STATE );
        int lightLevel = 0;
        if ( currentState == STATE_RS_ON || currentState == STATE_AUTO_ON ) {
            lightLevel = 15;
        }
        return lightLevel;
    }

    @Override
    public boolean isOpaqueCube( IBlockState state ) {
        return false;
    }

    @Override
    public void updateTick( World world, BlockPos pos, IBlockState state, Random random )
    {
        int currentState = state.getValue( STATE );
        // Check if in automatic mode
        if ( currentState == STATE_AUTO_OFF || currentState == STATE_AUTO_ON ) {
            // Check ambient light level and set state appropriately
            int ambientLightLevel = 15 - world.getSkylightSubtracted();
            if ( currentState == STATE_AUTO_OFF && ambientLightLevel <= LIGHT_LEVEL_TURN_ON ) {
                // Need to turn on light
                world.setBlockState( pos, state.withProperty( STATE, STATE_AUTO_ON ), 3 );
                handleAirLightBlock( true, world, pos );
            }
            else if ( currentState == STATE_AUTO_ON && ambientLightLevel > LIGHT_LEVEL_TURN_ON ) {
                // Need to turn off light
                world.setBlockState( pos, state.withProperty( STATE, STATE_AUTO_OFF ), 3 );
                handleAirLightBlock( false, world, pos );
            }
        }
        else if ( currentState == STATE_RS_OFF || currentState == STATE_RS_ON ) {
            // Check for redstone power and set state appropriately
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
        super.updateTick( world, pos, state, random );
        world.scheduleUpdate( pos, this, this.tickRate( world ) );
    }

    @Override
    public int tickRate( World p_tickRate_1_ ) {
        return TickTimeConverter.getTicksFromSeconds( 2 );
    }

    @Override
    public void breakBlock( World p_breakBlock_1_, BlockPos p_breakBlock_2_, IBlockState p_breakBlock_3_ ) {
        // Cleanup existing light block (if present)
        handleAirLightBlock( false, p_breakBlock_1_, p_breakBlock_2_ );

        super.breakBlock( p_breakBlock_1_, p_breakBlock_2_, p_breakBlock_3_ );
    }

    abstract public String getBlockRegistryName();

    abstract public boolean blockHasAutomaticFunctionality();
}
