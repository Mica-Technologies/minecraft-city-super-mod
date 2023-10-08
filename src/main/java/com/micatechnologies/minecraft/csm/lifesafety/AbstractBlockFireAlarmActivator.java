package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.tabs.CsmTabLifeSafety;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

public abstract class AbstractBlockFireAlarmActivator extends AbstractBlockRotatableNSEWUD
        implements ICsmTileEntityProvider
{
    // TODO: Why is this here because it concerns me
    public static final PropertyDirection FACING = BlockDirectional.FACING;

    public AbstractBlockFireAlarmActivator() {
        super( Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0 );
    }

    @Override
    public void updateTick( World p_updateTick_1_,
                            BlockPos p_updateTick_2_,
                            IBlockState p_updateTick_3_,
                            Random p_updateTick_4_ )
    {
        try {
            onTick( p_updateTick_1_, p_updateTick_2_, p_updateTick_3_ );
        }
        catch ( Exception e ) {
            System.err.println( "An error occurred while ticking a fire alarm activator block: " );
            e.printStackTrace( System.err );
        }
        p_updateTick_1_.scheduleUpdate( p_updateTick_2_, this, this.tickRate( p_updateTick_1_ ) );
    }

    @Override
    public int tickRate( World p_tickRate_1_ ) {
        return getBlockTickRate();
    }

    @Override
    public void onBlockAdded( World p_onBlockAdded_1_, BlockPos p_onBlockAdded_2_, IBlockState p_onBlockAdded_3_ ) {
        p_onBlockAdded_1_.scheduleUpdate( p_onBlockAdded_2_, this, this.tickRate( p_onBlockAdded_1_ ) );
        super.onBlockAdded( p_onBlockAdded_1_, p_onBlockAdded_2_, p_onBlockAdded_3_ );
    }

    @Override
    public int getLightValue( IBlockState state, IBlockAccess world, BlockPos pos ) {
        return 0;
    }

    abstract public int getBlockTickRate();

    abstract public void onTick( World world, BlockPos blockPos, IBlockState blockState );

    public boolean activateLinkedPanel( World world, BlockPos blockPos, EntityPlayer p ) {
        boolean activated = false;
        TileEntity tileEntityAtPos = world.getTileEntity( blockPos );
        if ( tileEntityAtPos instanceof TileEntityFireAlarmSensor ) {
            TileEntityFireAlarmSensor tileEntityFireAlarmSensor = ( TileEntityFireAlarmSensor ) tileEntityAtPos;
            BlockPos linkedPanelPos = tileEntityFireAlarmSensor.getLinkedPanelPos( world );
            if ( linkedPanelPos != null ) {
                TileEntity tileEntityAtLinkedPanelPos = world.getTileEntity( linkedPanelPos );
                if ( tileEntityAtLinkedPanelPos instanceof TileEntityFireAlarmControlPanel ) {
                    TileEntityFireAlarmControlPanel fireAlarmControlPanel
                            = ( TileEntityFireAlarmControlPanel ) tileEntityAtLinkedPanelPos;
                    fireAlarmControlPanel.setAlarmState( true );
                    activated = true;
                }
            }
        }
        return activated;
    }

    /**
     * Retrieves the bounding box of the block.
     *
     * @param state  the block state
     * @param source the block access
     * @param pos    the block position
     *
     * @return The bounding box of the block.
     *
     * @since 1.0
     */
    @Override
    public AxisAlignedBB getBlockBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {
        return new AxisAlignedBB( 0D, 0D, 0.8D, 1D, 1D, 1D );
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

    /**
     * Gets the tile entity class for the block.
     *
     * @return the tile entity class for the block
     *
     * @since 1.0
     */
    @Override
    public Class< ? extends TileEntity > getTileEntityClass() {
        return TileEntityFireAlarmSensor.class;
    }

    /**
     * Gets the tile entity name for the block.
     *
     * @return the tile entity name for the block
     *
     * @since 1.0
     */
    @Override
    public String getTileEntityName() {
        return "tileentityfirealarmsensor";
    }

    /**
     * Gets a new tile entity for the block.
     *
     * @param worldIn the world
     * @param meta    the block metadata
     *
     * @return the new tile entity for the block
     *
     * @since 1.1
     */
    @Nullable
    @Override
    public TileEntity createNewTileEntity( World worldIn, int meta ) {
        return new TileEntityFireAlarmSensor();
    }
}