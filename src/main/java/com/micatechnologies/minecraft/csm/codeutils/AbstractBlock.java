package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.CsmConstants;
import com.micatechnologies.minecraft.csm.CsmRegistry;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
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
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

/**
 * Abstract block class which provides common methods and properties for all blocks in this mod.
 *
 * @version 1.0
 * @see Block
 * @since 2023.3
 */
@MethodsReturnNonnullByDefault
public abstract class AbstractBlock extends Block implements IHasModel, ICsmBlock
{

    /**
     * Constructs an {@link AbstractBlock} instance.
     *
     * @param material The material of the block.
     *
     * @since 1.0
     */
    public AbstractBlock( Material material )
    {
        super( material );
        setUnlocalizedName( getBlockRegistryName() );
        setRegistryName( CsmConstants.MOD_NAMESPACE, getBlockRegistryName() );
        CsmRegistry.registerBlock( this );
        CsmRegistry.registerItem(
                new ItemBlock( this ).setRegistryName( Objects.requireNonNull( this.getRegistryName() ) ) );
    }

    /**
     * Constructs an {@link AbstractBlock} instance.
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
    public AbstractBlock( Material material,
                          SoundType soundType,
                          String harvestToolClass,
                          int harvestLevel,
                          float hardness,
                          float resistance,
                          float lightLevel,
                          int lightOpacity )
    {
        super( material );
        setUnlocalizedName( getBlockRegistryName() );
        setRegistryName( CsmConstants.MOD_NAMESPACE, getBlockRegistryName() );
        setSoundType( soundType );
        setHarvestLevel( harvestToolClass, harvestLevel );
        setHardness( hardness );
        setResistance( resistance );
        setLightLevel( lightLevel );
        setLightOpacity( lightOpacity );
        CsmRegistry.registerBlock( this );
        CsmRegistry.registerItem(
                new ItemBlock( this ).setRegistryName( Objects.requireNonNull( this.getRegistryName() ) ) );
    }

    /**
     * Overridden method from {@link Block} which retrieves the bounding box of the block from
     * {@link #getBlockBoundingBox(IBlockState, IBlockAccess, BlockPos)}.
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
        AxisAlignedBB blockBoundingBox = getBlockBoundingBox( state, source, pos );
        return blockBoundingBox == null ? SQUARE_BOUNDING_BOX : blockBoundingBox;
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
        return getBlockConnectsRedstone( state, access, pos, facing );
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
        return getBlockRenderLayer();
    }

    /**
     * Registers the block's model.
     *
     * @see IHasModel#registerModels()
     * @since 1.0
     */
    @Override
    public void registerModels() {
        Csm.proxy.setCustomModelResourceLocation( Item.getItemFromBlock( this ), 0, "inventory" );
    }

    /**
     * Indicates whether the block has a tile entity. This method returns {@code true} if the block implements the
     * {@link ICsmTileEntityProvider} interface.
     *
     * @param blockState the block state
     *
     * @return {@code true} if the block has a tile entity, {@code false} otherwise
     *
     * @since 1.0
     */
    @Override
    @ParametersAreNonnullByDefault
    public boolean hasTileEntity( IBlockState blockState ) {
        return this instanceof ICsmTileEntityProvider;
    }

    /**
     * Creates a new tile entity for the block. This method returns {@code null} if the block does not implement the
     * {@link ICsmTileEntityProvider} interface.
     *
     * @param worldIn the world
     * @param meta    the block metadata
     *
     * @return the new tile entity
     *
     * @since 1.0
     */
    @Nullable
    public TileEntity createNewTileEntity( @Nonnull World worldIn, int meta ) {
        TileEntity returnVal = null;
        if ( this instanceof ICsmTileEntityProvider ) {
            ICsmTileEntityProvider tileEntityProvider = ( ICsmTileEntityProvider ) this;
            Class< ? extends TileEntity > tileEntityClass = tileEntityProvider.getTileEntityClass();
            if ( tileEntityClass != null ) {
                try {
                    returnVal = tileEntityClass.newInstance();
                }
                catch ( Exception e ) {
                    Csm.getLogger().error( "Failed to create tile entity for block: " + this.getRegistryName(), e );
                }
            }
            else {
                Csm.getLogger().error( "Tile entity class is null for block: " + this.getRegistryName() );
            }
        }
        return returnVal;
    }

    /**
     * Overridden method from {@link Block} which handles the removal of the block. This method removes the tile entity
     * from the world if the block has a tile entity.
     *
     * @param worldIn the world
     * @param pos     the block position
     * @param state   the block state
     *
     * @since 1.0
     */
    @Override
    public void breakBlock( World worldIn, BlockPos pos, IBlockState state ) {
        if ( hasTileEntity( state ) ) {
            worldIn.removeTileEntity( pos );
        }
        super.breakBlock( worldIn, pos, state );
    }
}
