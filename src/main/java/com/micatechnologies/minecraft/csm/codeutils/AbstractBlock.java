package com.micatechnologies.minecraft.csm.codeutils;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * Abstract block class which provides common methods and properties for all blocks in this mod.
 *
 * @version 1.0
 * @see Block
 * @since 2023.3
 */
@MethodsReturnNonnullByDefault
public abstract class AbstractBlock extends Block
{

    /**
     * Render layer for the block.
     *
     * @since 1.0
     */
    private final BlockRenderLayer blockRenderLayer;

    /**
     * Block connects to redstone property.
     *
     * @since 1.0
     */
    private final boolean blockConnectsRedstone;

    /**
     * Constructs an AbstractBlock.
     *
     * @param material              The material of the block.
     * @param soundType             The sound type of the block.
     * @param harvestToolClass      The harvest tool class of the block.
     * @param harvestLevel          The harvest level of the block.
     * @param hardness              The block's hardness.
     * @param resistance            The block's resistance to explosions.
     * @param lightLevel            The block's light level.
     * @param lightOpacity          The block's light opacity.
     * @param blockConnectsRedstone The blocks connect to redstone property.
     * @param blockRenderLayer      The block's render layer.
     * @param creativeTab           The creative tab for the block.
     *
     * @since 1.0
     */
    public AbstractBlock( Material material,
                          SoundType soundType,
                          String harvestToolClass,
                          int harvestLevel,
                          float hardness,
                          float resistance,
                          float lightLevel,
                          int lightOpacity,
                          boolean blockConnectsRedstone,
                          BlockRenderLayer blockRenderLayer,
                          CreativeTabs creativeTab )
    {
        super( material );
        setUnlocalizedName( getBlockRegistryName() );
        setSoundType( soundType );
        setHarvestLevel( harvestToolClass, harvestLevel );
        setHardness( hardness );
        setResistance( resistance );
        setLightLevel( lightLevel );
        setLightOpacity( lightOpacity );
        setCreativeTab( creativeTab );
        this.blockConnectsRedstone = blockConnectsRedstone;
        this.blockRenderLayer = blockRenderLayer;
    }

    /**
     * Retrieves the registry name of the block.
     *
     * @return The registry name of the block.
     *
     * @since 1.0
     */
    public abstract String getBlockRegistryName();

    /**
     * Retrieves the bounding box of the block.
     *
     * @return The bounding box of the block.
     *
     * @since 1.0
     */
    public abstract AxisAlignedBB getBlockBoundingBox();

    /**
     * Retrieves whether the block is an opaque cube.
     *
     * @param state The block state.
     *
     * @return {@code true} if the block is an opaque cube, {@code false} otherwise.
     *
     * @since 1.0
     */
    public abstract boolean getBlockIsOpaqueCube( IBlockState state );

    /**
     * Retrieves whether the block is a full cube.
     *
     * @param state The block state.
     *
     * @return {@code true} if the block is a full cube, {@code false} otherwise.
     *
     * @since 1.0
     */
    public abstract boolean getBlockIsFullCube( IBlockState state );

    /**
     * Overridden method from {@link Block} which retrieves the bounding box of the block from
     * {@link #getBlockBoundingBox()}.
     *
     * @param state  the block state
     * @param source the block access
     * @param pos    the block position
     *
     * @return the bounding box of the block
     *
     * @since 1.0
     */
    @Override
    public AxisAlignedBB getBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {
        return getBlockBoundingBox();
    }

    /**
     * Overridden method from {@link Block} which determines if the block is an opaque cube. This method is overridden
     * and passed to a required abstract method to ensure all blocks are developed and implemented using a consistent
     * API.
     *
     * @param state the block state
     *
     * @return true if the block is an opaque cube, false otherwise
     *
     * @see Block#isOpaqueCube(IBlockState)
     * @since 1.0
     */
    @Override
    public boolean isOpaqueCube( IBlockState state ) {
        return getBlockIsOpaqueCube( state );
    }

    /**
     * Overridden method from {@link Block} which determines if the block is a full cube. This method is overridden and
     * passed to a required abstract method to ensure all blocks are developed and implemented using a consistent API.
     *
     * @param state the block state
     *
     * @return true if the block is a full cube, false otherwise
     *
     * @see Block#isFullCube(IBlockState)
     * @since 1.0
     */
    @Override
    public boolean isFullCube( IBlockState state ) {
        return getBlockIsFullCube( state );
    }

    /**
     * Overridden method from {@link Block} which determines if the block connects to redstone. This method is
     * overridden and passed a value set by the constructor to ensure all blocks are developed and implemented using a
     * consistent API.
     *
     * @param state  the block state
     * @param access the block access
     * @param pos    the block position
     * @param facing the block facing direction
     *
     * @return true if the block connects to redstone, false otherwise
     *
     * @see Block#canConnectRedstone(IBlockState, IBlockAccess, BlockPos, EnumFacing)
     * @since 1.0
     */
    @Override
    public boolean canConnectRedstone( IBlockState state,
                                       IBlockAccess access,
                                       BlockPos pos,
                                       @Nullable EnumFacing facing )
    {
        return blockConnectsRedstone;
    }

    /**
     * Overridden method from {@link Block} which retrieves the block's render layer. This method is overridden and
     * passed to a required abstract method to ensure all blocks are developed and implemented using a consistent API.
     *
     * @return the block's render layer
     *
     * @see Block#getBlockLayer()
     * @since 1.0
     */
    @Override
    @SideOnly( Side.CLIENT )
    public BlockRenderLayer getBlockLayer() {
        return blockRenderLayer;
    }
}
