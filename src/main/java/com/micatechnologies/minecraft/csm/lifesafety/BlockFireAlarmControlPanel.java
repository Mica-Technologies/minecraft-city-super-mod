package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class BlockFireAlarmControlPanel extends AbstractBlockRotatableNSEW implements ICsmTileEntityProvider
{

    public BlockFireAlarmControlPanel() {
        super( Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0 );
    }

    @Override
    public boolean onBlockActivated( World p_onBlockActivated_1_,
                                     BlockPos p_onBlockActivated_2_,
                                     IBlockState p_onBlockActivated_3_,
                                     EntityPlayer p_onBlockActivated_4_,
                                     EnumHand p_onBlockActivated_5_,
                                     EnumFacing p_onBlockActivated_6_,
                                     float p_onBlockActivated_7_,
                                     float p_onBlockActivated_8_,
                                     float p_onBlockActivated_9_ )
    {
        if ( p_onBlockActivated_4_.inventory.getCurrentItem() != null &&
                ( p_onBlockActivated_4_.inventory.getCurrentItem().getItem() instanceof ItemFireAlarmLinker ) ) {
            return super.onBlockActivated( p_onBlockActivated_1_, p_onBlockActivated_2_, p_onBlockActivated_3_,
                                           p_onBlockActivated_4_, p_onBlockActivated_5_, p_onBlockActivated_6_,
                                           p_onBlockActivated_7_, p_onBlockActivated_8_, p_onBlockActivated_9_ );
        }

        // Check if panel tile entity still present/valid
        boolean valid = true;
        try {
            TileEntity tileEntity = p_onBlockActivated_1_.getTileEntity( p_onBlockActivated_2_ );
            if ( !( tileEntity instanceof TileEntityFireAlarmControlPanel ) ) {
                valid = false;
                if ( !p_onBlockActivated_1_.isRemote ) {
                    p_onBlockActivated_4_.sendMessage( new TextComponentString(
                            "Alarm panel tile entity is an instance of an improper " +
                                    "super class. Cannot operate. Will attempt to replace..." ) );
                }
            }
        }
        catch ( Exception e ) {
            valid = false;
            if ( !p_onBlockActivated_1_.isRemote ) {
                p_onBlockActivated_4_.sendMessage(
                        new TextComponentString( "Alarm panel tile entity has failed. Replace this panel!" ) );
            }
        }

        // If panel tile entity invalid, try to recover.
        if ( !valid ) {
            try {
                p_onBlockActivated_1_.setTileEntity( p_onBlockActivated_2_, new TileEntityFireAlarmControlPanel() );
                valid = true;
                if ( !p_onBlockActivated_1_.isRemote ) {
                    p_onBlockActivated_4_.sendMessage( new TextComponentString(
                            "Broken alarm panel tile entity has been replaced. Appliances may need to be re-linked." ) );
                }
            }
            catch ( Exception e ) {
                if ( !p_onBlockActivated_1_.isRemote ) {
                    p_onBlockActivated_4_.sendMessage( new TextComponentString(
                            "Unable to replace broken alarm panel tile entity. Replace this panel!" ) );
                }
            }
        }

        if ( valid ) {
            TileEntity tileEntity = p_onBlockActivated_1_.getTileEntity( p_onBlockActivated_2_ );
            if ( tileEntity instanceof TileEntityFireAlarmControlPanel ) {
                TileEntityFireAlarmControlPanel tileEntityFireAlarmControlPanel
                        = ( TileEntityFireAlarmControlPanel ) tileEntity;

                boolean alarmState = tileEntityFireAlarmControlPanel.getAlarmState();
                if ( alarmState ) {
                    tileEntityFireAlarmControlPanel.setAlarmState( false );
                    if ( !p_onBlockActivated_1_.isRemote ) {
                        p_onBlockActivated_4_.sendMessage(
                                new TextComponentString( "Panel alarm status has been reset!" ) );
                    }
                }

                if ( p_onBlockActivated_4_.isSneaking() ) {
                    tileEntityFireAlarmControlPanel.switchSound();
                    if ( !p_onBlockActivated_1_.isRemote ) {
                        p_onBlockActivated_4_.sendMessage( new TextComponentString( "Switching alarm panel sound to " +
                                                                                            tileEntityFireAlarmControlPanel.getCurrentSoundName() ) );
                    }
                }
            }
        }

        return true;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void addInformation( ItemStack p_addInformation_1_,
                                World p_addInformation_2_,
                                List< String > p_addInformation_3_,
                                ITooltipFlag p_addInformation_4_ )
    {
        super.addInformation( p_addInformation_1_, p_addInformation_2_, p_addInformation_3_, p_addInformation_4_ );
        p_addInformation_3_.add( I18n.format( "csm.firealarmcontrolpanel.note" ) );
    }

    @Override
    public void neighborChanged( IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos p_189540_5_ )
    {
        // Check for redstone power and set alarm storm state
        boolean isPowered = world.isBlockPowered( pos );
        TileEntity tileEntity = world.getTileEntity( pos );
        if ( tileEntity instanceof TileEntityFireAlarmControlPanel ) {
            TileEntityFireAlarmControlPanel tileEntityFireAlarmControlPanel
                    = ( TileEntityFireAlarmControlPanel ) tileEntity;
            tileEntityFireAlarmControlPanel.setAlarmStormState( isPowered );
        }
    }

    /**
     * Retrieves the registry name of the block.
     *
     * @return The registry name of the block.
     *
     * @since 1.0
     */
    @Override
    public String getBlockRegistryName() {
        return "firealarmcontrolpanel";
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
     * Gets a new tile entity for the block.
     *
     * @return the new tile entity for the block
     *
     * @since 1.1
     */
    @Override
    public TileEntity getNewTileEntity() {
        return new TileEntityFireAlarmControlPanel();
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
        return TileEntityFireAlarmControlPanel.class;
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
        return "tileentityfirealarmcontrolpanel";
    }

}

