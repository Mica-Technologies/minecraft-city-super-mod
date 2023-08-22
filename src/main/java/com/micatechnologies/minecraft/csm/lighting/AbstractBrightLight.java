package com.micatechnologies.minecraft.csm.lighting;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.tabs.CsmTabLighting;
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
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class AbstractBrightLight extends AbstractBlockRotatableNSEW
{
    public static final  PropertyInteger STATE         = PropertyInteger.create( "state", 0, 3 );
    private static final int             STATE_RS_OFF  = 0;
    private static final int             STATE_RS_ON   = 1;
    private static final int             STATE_MAN_OFF = 2;
    private static final int             STATE_MAN_ON  = 3;

    public AbstractBrightLight() {
        super( Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, false );
        this.setDefaultState( this.blockState.getBaseState()
                                             .withProperty( FACING, EnumFacing.NORTH )
                                             .withProperty( STATE, STATE_RS_OFF ) );
    }

    /**
     * Retrieves whether the block is an opaque cube.
     *
     * @param state The block state.
     *
     * @return {@code true} if the block is an opaque cube, {@code false} otherwise.
     *
     * @since 1.0
     */
    @Override
    public boolean getBlockIsOpaqueCube( IBlockState state ) {
        return false;
    }

    /**
     * Retrieves whether the block is a full cube.
     *
     * @param state The block state.
     *
     * @return {@code true} if the block is a full cube, {@code false} otherwise.
     *
     * @since 1.0
     */
    @Override
    public boolean getBlockIsFullCube( IBlockState state ) {
        return false;
    }

    /**
     * Retrieves whether the block connects to redstone.
     *
     * @param state  the block state
     * @param access the block access
     * @param pos    the block position
     * @param facing the block facing direction
     *
     * @return {@code true} if the block connects to redstone, {@code false} otherwise.
     *
     * @since 1.0
     */
    @Override
    public boolean getBlockConnectsRedstone( IBlockState state,
                                             IBlockAccess access,
                                             BlockPos pos,
                                             @Nullable EnumFacing facing )
    {
        return true;
    }

    /**
     * Retrieves the block's render layer.
     *
     * @return The block's render layer.
     *
     * @since 1.0
     */
    @Nonnull
    @Override
    public BlockRenderLayer getBlockRenderLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    abstract public int getBrightLightXOffset();

    /**
     * Gets the {@link IBlockState} equivalent for this block using the specified {@code meta} value.
     *
     * @param meta the value to get the equivalent {@link IBlockState} of
     *
     * @return the {@link IBlockState} equivalent for the specified {@code meta} value
     *
     * @see Block#getStateFromMeta(int)
     * @since 1.0
     */
    @Override
    public IBlockState getStateFromMeta( int meta ) {
        int stateVal = meta % 4;
        int facingVal = ( meta - stateVal ) / 4;

        return getDefaultState().withProperty( FACING, EnumFacing.getHorizontal( facingVal ) )
                                .withProperty( STATE, stateVal );
    }

    /**
     * Gets the equivalent {@link Integer} meta value for the specified {@link IBlockState} of this block.
     *
     * @param state the {@link IBlockState} to get the equivalent {@link Integer} meta value for
     *
     * @return the equivalent {@link Integer} meta value for the specified {@link IBlockState}
     *
     * @see Block#getMetaFromState(IBlockState)
     * @since 1.0
     */
    @Override
    public int getMetaFromState( IBlockState state ) {
        int facingVal = state.getValue( FACING ).getHorizontalIndex() * 4;
        int stateVal = state.getValue( STATE );
        return facingVal + stateVal;
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
                final int xWithOffset = pos.getX() + getBrightLightXOffset();
                final int yWithOffset = pos.getY() + findy;
                BlockPos test = new BlockPos( xWithOffset, yWithOffset, pos.getZ() );
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
                final int xWithOffset = pos.getX() + getBrightLightXOffset();
                final int yWithOffset = pos.getY() + findy;
                BlockPos test = new BlockPos( xWithOffset, yWithOffset, pos.getZ() );
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

    /**
     * Creates a new {@link BlockStateContainer} for the block with the required properties for rotation and state.
     *
     * @return a new {@link BlockStateContainer} for the block
     *
     * @see Block#createBlockState()
     * @since 1.0
     */
    @Override
    @Nonnull
    protected net.minecraft.block.state.BlockStateContainer createBlockState() {
        return new net.minecraft.block.state.BlockStateContainer( this, FACING, STATE );
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
}
