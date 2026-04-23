package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkMountType;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockBlankoutBox extends AbstractBlockRotatableNSEW
        implements ICsmTileEntityProvider {

    public static final PropertyInteger STATE = PropertyInteger.create( "state", 0, 2 );
    public static final int STATE_ON = 0;
    public static final int STATE_OFF = 1;
    public static final int STATE_FLASH = 2;

    public BlockBlankoutBox() {
        super( Material.ROCK, SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 0, false );
        this.setDefaultState( this.blockState.getBaseState()
                .withProperty( FACING, EnumFacing.NORTH )
                .withProperty( STATE, STATE_OFF ) );
    }

    @Override
    public String getBlockRegistryName() {
        return "blankout_box";
    }

    // region Block State

    @Override
    @Nonnull
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer( this, FACING, STATE );
    }

    @Override
    @Nonnull
    public IBlockState getStateFromMeta( int meta ) {
        int stateVal = meta % 3;
        int facingVal = ( meta - stateVal ) / 3;
        if ( facingVal < 0 || facingVal > 3 ) facingVal = 0;
        return getDefaultState()
                .withProperty( FACING, EnumFacing.byHorizontalIndex( facingVal ) )
                .withProperty( STATE, stateVal );
    }

    @Override
    public int getMetaFromState( IBlockState state ) {
        return state.getValue( FACING ).getHorizontalIndex() * 3 + state.getValue( STATE );
    }

    @Override
    @Nonnull
    public IBlockState getStateForPlacement( World worldIn, BlockPos pos, EnumFacing facing,
            float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer ) {
        return this.getDefaultState()
                .withProperty( FACING, placer.getHorizontalFacing().getOpposite() )
                .withProperty( STATE, STATE_OFF );
    }

    // endregion

    // region Tile Entity

    @Override
    public Class<? extends TileEntity> getTileEntityClass() {
        return TileEntityBlankoutBox.class;
    }

    @Override
    public String getTileEntityName() {
        return "tileentityblankoutbox";
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity( World worldIn, int meta ) {
        return new TileEntityBlankoutBox();
    }

    private void ensureTileEntity( World worldIn, BlockPos pos ) {
        if ( !worldIn.isRemote && worldIn.getTileEntity( pos ) == null ) {
            worldIn.setTileEntity( pos, createNewTileEntity( worldIn, 0 ) );
        }
    }

    // endregion

    // region Block Properties

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

    @Override
    public boolean getBlockIsOpaqueCube( IBlockState state ) {
        return false;
    }

    @Override
    public boolean getBlockIsFullCube( IBlockState state ) {
        return false;
    }

    @Override
    public boolean getBlockConnectsRedstone( IBlockState state, IBlockAccess access,
            BlockPos pos, @Nullable EnumFacing facing ) {
        return false;
    }

    @Nonnull
    @Override
    public BlockRenderLayer getBlockRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public int getLightValue( IBlockState state, IBlockAccess world, BlockPos pos ) {
        return 0;
    }

    // endregion

    // region Block Events

    @Override
    public void onBlockPlacedBy( World worldIn, BlockPos pos, IBlockState state,
            EntityLivingBase placer, ItemStack stack ) {
        super.onBlockPlacedBy( worldIn, pos, state, placer, stack );
        if ( !worldIn.isRemote ) {
            TileEntity te = worldIn.getTileEntity( pos );
            if ( te instanceof TileEntityBlankoutBox ) {
                CrosswalkMountType detected = detectMountType( worldIn, pos, state );
                ( (TileEntityBlankoutBox) te ).setMountType( detected );
            }
        }
    }

    private CrosswalkMountType detectMountType( World worldIn, BlockPos pos,
            IBlockState state ) {
        EnumFacing facing = state.getValue( FACING );

        EnumFacing behind = facing.getOpposite();
        if ( isAttachableBlock( worldIn, pos.offset( behind ) ) ) {
            return CrosswalkMountType.REAR;
        }

        EnumFacing left = facing.rotateY();
        if ( isAttachableBlock( worldIn, pos.offset( left ) ) ) {
            return CrosswalkMountType.LEFT;
        }

        EnumFacing right = facing.rotateYCCW();
        if ( isAttachableBlock( worldIn, pos.offset( right ) ) ) {
            return CrosswalkMountType.RIGHT;
        }

        return CrosswalkMountType.BASE;
    }

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
    public boolean onBlockActivated( World worldIn, BlockPos pos, IBlockState state,
            EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY,
            float hitZ ) {
        ensureTileEntity( worldIn, pos );
        return super.onBlockActivated( worldIn, pos, state, player, hand, facing, hitX, hitY,
                hitZ );
    }

    @Override
    public void breakBlock( World worldIn, BlockPos pos, IBlockState state ) {
        if ( worldIn.isRemote ) {
            TileEntitySpecialRenderer<?> renderer =
                    TileEntityRendererDispatcher.instance.renderers.get(
                            TileEntityBlankoutBox.class );
            if ( renderer instanceof TileEntityBlankoutBoxRenderer ) {
                ( (TileEntityBlankoutBoxRenderer) renderer ).cleanupDisplayList( pos );
            }
        }
        super.breakBlock( worldIn, pos, state );
    }

    // endregion
}
