package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.codeutils.DirectionSixteen;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityCrosswalkSignalNew;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityCrosswalkSignalNewRenderer;
import javax.annotation.Nullable;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Abstract base class for the new custom-rendered crosswalk signal blocks. Provides tile entity
 * management, display list cleanup on break, and dynamic bounding box support.
 */
public abstract class AbstractBlockControllableCrosswalkSignalNew
        extends AbstractBlockControllableSignal implements ICsmTileEntityProvider {

    public AbstractBlockControllableCrosswalkSignalNew() {
        super( Material.ROCK );
    }

    // region Signal Properties

    @Override
    public SIGNAL_SIDE getSignalSide( World world, BlockPos blockPos ) {
        return SIGNAL_SIDE.PEDESTRIAN;
    }

    @Override
    public boolean doesFlash() {
        return true;
    }

    /**
     * Returns the display type for this crosswalk signal (SYMBOL for hand/man icons, TEXT for
     * DON'T WALK/WALK text).
     */
    public abstract CrosswalkDisplayType getDisplayType();

    // endregion

    // region Tile Entity

    @Override
    public Class<? extends TileEntity> getTileEntityClass() {
        return TileEntityCrosswalkSignalNew.class;
    }

    @Override
    public String getTileEntityName() {
        return "tileentitycrosswalksignalnew";
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity( World worldIn, int meta ) {
        return new TileEntityCrosswalkSignalNew();
    }

    private void ensureTileEntity( World worldIn, BlockPos pos ) {
        if ( !worldIn.isRemote && worldIn.getTileEntity( pos ) == null ) {
            worldIn.setTileEntity( pos, createNewTileEntity( worldIn, 0 ) );
        }
    }

    // endregion

    // region Bounding Box

    @Override
    public int getLightValue( IBlockState state, IBlockAccess world, BlockPos pos ) {
        TileEntity te = world.getTileEntity( pos );
        if ( te instanceof TileEntityCrosswalkSignalNew
                && ( (TileEntityCrosswalkSignalNew) te ).isPowerLossOff() ) {
            return 0;
        }
        return 15;
    }

    /**
     * Default bounding box. Subclasses may override with a static bounding box, or this could
     * be made dynamic based on the TE's mount type in the future.
     */
    @Override
    public AxisAlignedBB getBlockBoundingBox( IBlockState state, IBlockAccess source,
            BlockPos pos ) {
        return new AxisAlignedBB( 0.0, 0.0, 0.0, 1.0, 1.0, 0.625 );
    }

    @Nullable
    @Override
    public RayTraceResult collisionRayTrace( IBlockState state, World worldIn, BlockPos pos,
            Vec3d start, Vec3d end ) {
        AxisAlignedBB bb = getBoundingBox( state, worldIn, pos );
        AxisAlignedBB clamped = new AxisAlignedBB(
                Math.max( 0.0, bb.minX ), Math.max( 0.0, bb.minY ),
                Math.max( 0.0, bb.minZ ),
                Math.min( 1.0, bb.maxX ), Math.min( 1.0, bb.maxY ),
                Math.min( 1.0, bb.maxZ ) );
        return rayTrace( pos, start, end, clamped );
    }

    // endregion

    // region Tilt Direction

    public DirectionSixteen getTiltedFacing( IBlockAccess worldIn, BlockPos pos,
            EnumFacing facing4 ) {
        TileEntity tileEntity = worldIn.getTileEntity( pos );
        if ( tileEntity instanceof TileEntityCrosswalkSignalNew te ) {
            return AbstractBlockControllableSignalHead.getTiltedFacing( te.getBodyTilt(),
                    facing4 );
        }
        return AbstractBlockControllableSignalHead.getTiltedFacing(
                TrafficSignalBodyTilt.NONE, facing4 );
    }

    // endregion

    // region Block Events

    @Override
    public void onBlockPlacedBy( World worldIn, BlockPos pos, IBlockState state,
            EntityLivingBase placer, ItemStack stack ) {
        super.onBlockPlacedBy( worldIn, pos, state, placer, stack );
        if ( !worldIn.isRemote ) {
            TileEntity te = worldIn.getTileEntity( pos );
            if ( te instanceof TileEntityCrosswalkSignalNew ) {
                CrosswalkMountType detected = detectMountType( worldIn, pos, state );
                ( (TileEntityCrosswalkSignalNew) te ).setMountType( detected );
            }
        }
    }

    /**
     * Detects the appropriate mount type based on adjacent solid blocks. Checks behind, left,
     * and right relative to the signal's facing direction. Priority: REAR > LEFT > RIGHT > BASE.
     */
    private CrosswalkMountType detectMountType( World worldIn, BlockPos pos, IBlockState state ) {
        EnumFacing facing = state.getValue( FACING );

        // "Behind" = opposite of facing (the back of the signal housing)
        EnumFacing behind = facing.getOpposite();
        if ( isAttachableBlock( worldIn, pos.offset( behind ) ) ) {
            return CrosswalkMountType.REAR;
        }

        // "Left" = CW rotation of facing (viewer's left when looking at the signal face)
        EnumFacing left = facing.rotateY();
        if ( isAttachableBlock( worldIn, pos.offset( left ) ) ) {
            return CrosswalkMountType.LEFT;
        }

        // "Right" = CCW rotation of facing (viewer's right)
        EnumFacing right = facing.rotateYCCW();
        if ( isAttachableBlock( worldIn, pos.offset( right ) ) ) {
            return CrosswalkMountType.RIGHT;
        }

        return CrosswalkMountType.BASE;
    }

    /**
     * Checks whether a block at the given position is suitable for mounting (solid, opaque,
     * or a traffic pole).
     */
    private boolean isAttachableBlock( World worldIn, BlockPos pos ) {
        IBlockState adjState = worldIn.getBlockState( pos );
        return adjState.isFullCube() || adjState.isOpaqueCube()
                || adjState.getBlock() instanceof
                com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole
                || adjState.getBlock() instanceof
                com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPoleDiagonal;
    }

    @Override
    public void neighborChanged( IBlockState state, World worldIn, BlockPos pos,
            net.minecraft.block.Block blockIn, BlockPos fromPos ) {
        ensureTileEntity( worldIn, pos );
        super.neighborChanged( state, worldIn, pos, blockIn, fromPos );
    }

    @Override
    public boolean onBlockActivated( World p_180639_1_, BlockPos p_180639_2_,
            IBlockState p_180639_3_, EntityPlayer p_180639_4_, EnumHand p_180639_5_,
            EnumFacing p_180639_6_, float p_180639_7_, float p_180639_8_, float p_180639_9_ ) {
        ensureTileEntity( p_180639_1_, p_180639_2_ );
        return super.onBlockActivated( p_180639_1_, p_180639_2_, p_180639_3_, p_180639_4_,
                p_180639_5_, p_180639_6_, p_180639_7_, p_180639_8_, p_180639_9_ );
    }

    @Override
    public void breakBlock( World worldIn, BlockPos pos, IBlockState state ) {
        if ( worldIn.isRemote ) {
            TileEntitySpecialRenderer< ? > renderer =
                    TileEntityRendererDispatcher.instance.renderers.get(
                            TileEntityCrosswalkSignalNew.class );
            if ( renderer instanceof TileEntityCrosswalkSignalNewRenderer ) {
                ( (TileEntityCrosswalkSignalNewRenderer) renderer ).cleanupDisplayList( pos );
            }
        }
        super.breakBlock( worldIn, pos, state );
    }

    // endregion
}
