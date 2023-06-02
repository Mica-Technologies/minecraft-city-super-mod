package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.CsmConstants;
import com.micatechnologies.minecraft.csm.CsmRegistry;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Abstract fence block class which provides common methods and properties for all blocks in this mod.
 *
 * @version 1.0
 * @see Block
 * @since 2023.3
 */
@MethodsReturnNonnullByDefault
public abstract class AbstractBlockFence extends BlockFence implements IHasModel, ICsmBlock
{

    /**
     * Constructs an {@link AbstractBlockFence} instance.
     *
     * @param material         The material of the fence block.
     * @param soundType        The sound type of the fence block.
     * @param harvestToolClass The harvest tool class of the fence block.
     * @param harvestLevel     The harvest level of the fence block.
     * @param hardness         The fence block's hardness.
     * @param resistance       The fence block's resistance to explosions.
     * @param lightLevel       The fence block's light level.
     * @param lightOpacity     The fence block's light opacity.
     *
     * @since 1.0
     */
    public AbstractBlockFence( Material material,
                               SoundType soundType,
                               String harvestToolClass,
                               int harvestLevel,
                               float hardness,
                               float resistance,
                               float lightLevel,
                               int lightOpacity )
    {
        super( material, material.getMaterialMapColor() );
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
     * Registers the fence block's model.
     *
     * @see IHasModel#registerModels()
     * @since 1.0
     */
    @Override
    public void registerModels() {
        Csm.proxy.setCustomModelResourceLocation( Item.getItemFromBlock( this ), 0, "inventory" );
    }

    /**
     * Implementation of the {@link ICsmBlock#getBlockBoundingBox()} method which returns {@code null}, as this class
     * uses the standard/default {@link BlockSlab} bounding boxes. This method is overridden to prevent the need to
     * implement the {@link ICsmBlock#getBlockBoundingBox()} method in subclasses.
     *
     * @return {@code null}
     *
     * @implNote The value returned by this method implementation is ignored.
     * @since 1.0
     */
    @Override
    public AxisAlignedBB getBlockBoundingBox() {
        return null;
    }

    /**
     * Implementation of the {@link ICsmBlock#getBlockIsFullCube(IBlockState)} method which returns {@code false}, as
     * this class uses the standard/default {@link BlockSlab} full cube value. This method is overridden to prevent the
     * need to implement the {@link ICsmBlock#getBlockIsFullCube(IBlockState)} method in subclasses.
     *
     * @param state the {@link IBlockState} to get the full cube value of
     *
     * @return {@code false}
     *
     * @implNote The value returned by this method implementation is ignored.
     * @since 1.0
     */
    @Override
    public boolean getBlockIsFullCube( IBlockState state ) {
        return false;
    }

    /**
     * Implementation of the {@link ICsmBlock#getBlockConnectsRedstone()} method which returns {@code false}, as this
     * class uses the standard/default {@link BlockSlab} redstone connection value. This method is overridden to prevent
     * the need to implement the {@link ICsmBlock#getBlockConnectsRedstone()} method in subclasses.
     *
     * @return {@code false}
     *
     * @implNote The value returned by this method implementation is ignored.
     * @since 1.0
     */
    @Override
    public boolean getBlockConnectsRedstone() {
        return false;
    }

    /**
     * Implementation of the {@link ICsmBlock#getBlockRenderLayer()} method which returns {@code null}, as this class
     * uses the standard/default {@link BlockSlab} render layer value. This method is overridden to prevent the need to
     * implement the {@link ICsmBlock#getBlockRenderLayer()} method in subclasses.
     *
     * @return {@code null}
     *
     * @implNote The value returned by this method implementation is ignored.
     * @since 1.0
     */
    @Nonnull
    @Override
    public BlockRenderLayer getBlockRenderLayer() {
        return null;
    }
}
