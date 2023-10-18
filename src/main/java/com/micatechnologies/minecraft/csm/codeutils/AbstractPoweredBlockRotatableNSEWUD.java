package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * Abstract block class which provides the same common methods and properties as {@link AbstractBlockRotatableNSEWUD}
 * with the addition of a powered property.
 *
 * @version 1.0
 * @see Block
 * @see AbstractBlock
 * @see AbstractBlockRotatableNSEWUD
 * @since 2023.3
 */
public abstract class AbstractPoweredBlockRotatableNSEWUD extends AbstractBlock
{
    /**
     * The block facing direction property (directional)
     *
     * @since 1.0
     */
    public static final PropertyDirection FACING = BlockDirectional.FACING;

    /**
     * The block powered property (boolean)
     *
     * @since 1.0
     */
    public static final PropertyBool POWERED = PropertyBool.create( "powered" );

    /**
     * Constructs an {@link AbstractPoweredBlockRotatableNSEWUD} instance.
     *
     * @param material The material of the block.
     *
     * @since 1.0
     */
    public AbstractPoweredBlockRotatableNSEWUD( Material material )
    {
        this( material, true );
    }

    /**
     * Constructs an {@link AbstractPoweredBlockRotatableNSEWUD} instance.
     *
     * @param material        The material of the block.
     * @param setDefaultState Whether to set the default state of the block
     *
     * @since 1.0
     */
    public AbstractPoweredBlockRotatableNSEWUD( Material material, boolean setDefaultState )
    {
        super( material );
        if ( setDefaultState ) {
            this.setDefaultState( this.blockState.getBaseState().withProperty( FACING, EnumFacing.NORTH ) );
        }
    }

    /**
     * Constructs an {@link AbstractPoweredBlockRotatableNSEWUD} instance.
     *
     * @param material         The material of the block.
     * @param soundType        The sound type of the block.
     * @param harvestToolClass The harvest tool class of the block.
     * @param harvestLevel     The harvest level of the block.
     * @param hardness         The block's hardness.
     * @param resistance       The block's resistance to explosions.
     * @param lightLevel       The block's light level.
     * @param lightOpacity     The block's light opacity.
     *
     * @since 1.0
     */
    public AbstractPoweredBlockRotatableNSEWUD( Material material,
                                                SoundType soundType,
                                                String harvestToolClass,
                                                int harvestLevel,
                                                float hardness,
                                                float resistance,
                                                float lightLevel,
                                                int lightOpacity )
    {
        super( material, soundType, harvestToolClass, harvestLevel, hardness, resistance, lightLevel, lightOpacity );
        this.setDefaultState( this.blockState.getBaseState().withProperty( FACING, EnumFacing.NORTH ) );
    }

    /**
     * Constructs an {@link AbstractPoweredBlockRotatableNSEWUD} instance.
     *
     * @param material         The material of the block.
     * @param soundType        The sound type of the block.
     * @param harvestToolClass The harvest tool class of the block.
     * @param harvestLevel     The harvest level of the block.
     * @param hardness         The block's hardness.
     * @param resistance       The block's resistance to explosions.
     * @param lightLevel       The block's light level.
     * @param lightOpacity     The block's light opacity.
     * @param setDefaultState  Whether to set the default state of the block
     *
     * @since 1.0
     */
    public AbstractPoweredBlockRotatableNSEWUD( Material material,
                                                SoundType soundType,
                                                String harvestToolClass,
                                                int harvestLevel,
                                                float hardness,
                                                float resistance,
                                                float lightLevel,
                                                int lightOpacity,
                                                boolean setDefaultState )
    {
        super( material, soundType, harvestToolClass, harvestLevel, hardness, resistance, lightLevel, lightOpacity );
        if ( setDefaultState ) {
            this.setDefaultState( this.blockState.getBaseState().withProperty( FACING, EnumFacing.NORTH ) );
        }
    }

    /**
     * Creates a new {@link BlockStateContainer} for the block with the required property for rotation.
     *
     * @return a new {@link BlockStateContainer} for the block
     *
     * @see Block#createBlockState()
     * @since 1.0
     */
    @Override
    @Nonnull
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer( this, FACING, POWERED );
    }

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
    @Nonnull
    public IBlockState getStateFromMeta( int meta ) {
        int facingVal = meta & 7;
        boolean poweredVal = ( meta & 8 ) != 0;
        return getDefaultState().withProperty( FACING, EnumFacing.getFront( facingVal ) )
                                .withProperty( POWERED, poweredVal );
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
        return state.getValue( FACING ).getIndex() + ( state.getValue( POWERED ) ? 8 : 0 );
    }

    /**
     * Gets the {@link IBlockState} of the block to use for placement with the specified parameters.
     *
     * @param worldIn the world the block is being placed in
     * @param pos     the position the block is being place at
     * @param facing  the facing direction of the placement hit
     * @param hitX    the X coordinate of the placement hit
     * @param hitY    the Y coordinate of the placement hit
     * @param hitZ    the Z coordinate of the placement hit
     * @param meta    the meta value of the block state
     * @param placer  the placer of the block
     *
     * @return the {@link IBlockState} of the block to use for placement
     *
     * @since 1.0
     */
    @Override
    @Nonnull
    public IBlockState getStateForPlacement( World worldIn,
                                             BlockPos pos,
                                             EnumFacing facing,
                                             float hitX,
                                             float hitY,
                                             float hitZ,
                                             int meta,
                                             EntityLivingBase placer )
    {
        return this.getDefaultState().withProperty( FACING, EnumFacing.getDirectionFromEntityLiving( pos, placer ) );
    }

    /**
     * Overridden method from {@link Block} which retrieves the bounding box of the block from
     * {@link #getBlockBoundingBox(IBlockState, IBlockAccess, BlockPos)} and rotates as necessary.
     *
     * @param state  the block state
     * @param source the block access
     * @param pos    the block position
     *
     * @return the bounding box of the block, rotated as necessary
     *
     * @since 1.0
     */
    @Override
    @Nonnull
    public AxisAlignedBB getBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {
        return RotationUtils.rotateBoundingBoxByFacing( getBlockBoundingBox( state, source, pos ),
                                                        state.getValue( FACING ) );
    }

    /**
     * Overridden method from {@link Block} which is called when a neighbor block changes. This method sets the powered
     * property of the block based on whether the block is receiving power from a redstone source.
     *
     * @param state   the block state
     * @param world   the world the block is in
     * @param pos     the block position
     * @param blockIn the neighbor block
     * @param fromPos the neighbor block position
     */
    @Override
    public void neighborChanged( IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos )
    {
        int powered = world.isBlockIndirectlyGettingPowered( pos );
        world.setBlockState( pos, state.withProperty( POWERED, powered > 0 ), 3 );
    }

}