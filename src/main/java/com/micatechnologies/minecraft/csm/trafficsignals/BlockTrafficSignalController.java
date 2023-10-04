package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
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

public class BlockTrafficSignalController extends AbstractBlock implements ICsmTileEntityProvider
{

    public static final PropertyBool POWERED = PropertyBool.create( "powered" );

    public BlockTrafficSignalController() {
        super( Material.ROCK );
        setTickRandomly( true );
    }

    @Override
    public void neighborChanged( IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos p_189540_5_ )
    {
        int powered = world.isBlockIndirectlyGettingPowered( pos );
        world.setBlockState( pos, state.withProperty( POWERED, powered > 0 ), 3 );
    }

    public static EnumFacing getFacingFromEntity( BlockPos clickedBlock, EntityLivingBase entity ) {
        return EnumFacing.getFacingFromVector( ( float ) ( entity.posX - clickedBlock.getX() ),
                                               ( float ) ( entity.posY - clickedBlock.getY() ),
                                               ( float ) ( entity.posZ - clickedBlock.getZ() ) );
    }

    @Override
    public IBlockState getStateFromMeta( int meta ) {
        return getDefaultState().withProperty( POWERED, meta == 1 );
    }

    @Override
    public int getMetaFromState( IBlockState state ) {
        return state.getValue( POWERED ) ? 1 : 0;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer( this, POWERED );
    }

    @Override
    public void breakBlock( World p_180663_1_, BlockPos p_180663_2_, IBlockState p_180663_3_ ) {
        TileEntity tileEntity = p_180663_1_.getTileEntity( p_180663_2_ );
        if ( tileEntity instanceof TileEntityTrafficSignalController ) {
            TileEntityTrafficSignalController controller = ( TileEntityTrafficSignalController ) tileEntity;
            controller.forciblyPowerOff();
        }

        p_180663_1_.removeTileEntity( p_180663_2_ );
        super.breakBlock( p_180663_1_, p_180663_2_, p_180663_3_ );
    }

    @Override
    public void onBlockPlacedBy( World world,
                                 BlockPos pos,
                                 IBlockState state,
                                 EntityLivingBase placer,
                                 ItemStack stack )
    {
        int powered = world.isBlockIndirectlyGettingPowered( pos );
        world.setBlockState( pos, state.withProperty( POWERED, powered > 0 ), 2 );
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
                ( p_onBlockActivated_4_.inventory.getCurrentItem().getItem() instanceof ItemEWSignalLinker ||
                        p_onBlockActivated_4_.inventory.getCurrentItem().getItem() instanceof ItemNSSignalLinker ||
                        p_onBlockActivated_4_.inventory.getCurrentItem()
                                                       .getItem() instanceof ItemSignalConfigurationTool ) ) {
            return super.onBlockActivated( p_onBlockActivated_1_, p_onBlockActivated_2_, p_onBlockActivated_3_,
                                           p_onBlockActivated_4_, p_onBlockActivated_5_, p_onBlockActivated_6_,
                                           p_onBlockActivated_7_, p_onBlockActivated_8_, p_onBlockActivated_9_ );
        }

        TileEntity tileEntity = p_onBlockActivated_1_.getTileEntity( p_onBlockActivated_2_ );

        // Check if controller tile entity still present/valid
        boolean valid = true;
        try {
            tileEntity = p_onBlockActivated_1_.getTileEntity( p_onBlockActivated_2_ );
            if ( !( tileEntity instanceof TileEntityTrafficSignalController ) ) {
                valid = false;
                if ( !p_onBlockActivated_1_.isRemote ) {
                    p_onBlockActivated_4_.sendMessage( new TextComponentString(
                            "Controller tile entity is not an instance of traffic signal " +
                                    "controller tile entity. Cannot operate. Will attempt to replace..." ) );
                }
            }
        }
        catch ( Exception e ) {
            valid = false;
            if ( !p_onBlockActivated_1_.isRemote ) {
                p_onBlockActivated_4_.sendMessage( new TextComponentString(
                        "Controller tile entity has failed. Cannot operate. Will attempt to replace..." ) );
            }
        }

        // If controller tile entity invalid, try to recover.
        if ( !valid ) {
            try {
                p_onBlockActivated_1_.setTileEntity( p_onBlockActivated_2_, new TileEntityTrafficSignalController() );
                valid = true;
                tileEntity = p_onBlockActivated_1_.getTileEntity( p_onBlockActivated_2_ );
                if ( !p_onBlockActivated_1_.isRemote ) {
                    p_onBlockActivated_4_.sendMessage( new TextComponentString(
                            "Broken controller tile entity has been replaced. Signals may need to be re-linked." ) );
                }
            }
            catch ( Exception e ) {
                if ( !p_onBlockActivated_1_.isRemote ) {
                    p_onBlockActivated_4_.sendMessage( new TextComponentString(
                            "Unable to replace broken controller tile entity. Replace this block." ) );
                }
            }
        }

        // Process click if controller tile entity is (now) valid
        if ( valid ) {

            // Increment cycle index or display fault message if sneaking
            if ( p_onBlockActivated_4_.isSneaking() ) {
                if ( tileEntity instanceof TileEntityTrafficSignalController ) {
                    // Get current mode and fault information from controller
                    TileEntityTrafficSignalController tileEntityTrafficSignalController
                            = ( TileEntityTrafficSignalController ) tileEntity;
                    String modeName = tileEntityTrafficSignalController.switchMode();
                    boolean isInFaultState = tileEntityTrafficSignalController.isInFaultState();
                    String faultMessage = tileEntityTrafficSignalController.getCurrentFaultMessage();

                    // Display player output only if not remote
                    if ( !p_onBlockActivated_1_.isRemote ) {
                        // Display player output in fault state
                        if ( isInFaultState ) {
                            p_onBlockActivated_4_.sendMessage( new TextComponentString(
                                    "Controller has encountered a fault! To reset the fault, please " +
                                            "click with the signal changer tool." ) );
                            p_onBlockActivated_4_.sendMessage(
                                    new TextComponentString( "Controller fault message: " + faultMessage ) );
                        }
                        // Display player output in non-fault state
                        else {
                            p_onBlockActivated_4_.sendMessage(
                                    new TextComponentString( "Controller has switched to " + modeName + " mode!" ) );
                        }
                    }
                }
            }
        }
        else {

            // Output error message if controller tile entity is invalid
            if ( !p_onBlockActivated_1_.isRemote ) {
                p_onBlockActivated_4_.sendMessage(
                        new TextComponentString( "Unable to process! An invalid TE condition was encountered." ) );
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
        p_addInformation_3_.add( I18n.format( "csm.signalcontroller" ) );
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
        return "signalcontroller";
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
        return SQUARE_BOUNDING_BOX;
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
        return true;
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
        return true;
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
        return BlockRenderLayer.SOLID;
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
        return TileEntityTrafficSignalController.class;
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
        return "tileentitytrafficsignalcontroller";
    }
}
