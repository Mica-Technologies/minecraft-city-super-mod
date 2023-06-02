package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract block class which provides the same common methods and properties as {@link AbstractBlock} and creates a
 * variant set including solid block, slab, stair, and fence.
 *
 * @version 1.0
 * @see Block
 * @see AbstractBlock
 * @since 2023.3
 */
public abstract class AbstractBlockSetBasic extends AbstractBlock implements IHasModel, ICsmBlock
{

    /**
     * List of all blocks in the variant set.
     */
    private final List< Block > variantSet = new ArrayList<>();

    /**
     * Constructs an {@link AbstractBlockSetBasic} instance.
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
    public AbstractBlockSetBasic( Material material,
                                  SoundType soundType,
                                  String harvestToolClass,
                                  int harvestLevel,
                                  float hardness,
                                  float resistance,
                                  float lightLevel,
                                  int lightOpacity )
    {
        super( material, soundType, harvestToolClass, harvestLevel, hardness, resistance, lightLevel, lightOpacity );
        final AbstractBlockSetBasic baseVariant = this;

        // Create fence variant
        final String fenceVariantName = this.getBlockRegistryName() + "_fence";
        variantSet.add(
                new AbstractBlockFence( material, soundType, harvestToolClass, harvestLevel, hardness, resistance,
                                        lightLevel, lightOpacity )
                {
                    /**
                     * Retrieves the registry name of the block.
                     *
                     * @return The registry name of the block.
                     *
                     * @since 1.0
                     */
                    @Override
                    public String getBlockRegistryName() {
                        return fenceVariantName;
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
                        return baseVariant.getBlockIsOpaqueCube( state );
                    }
                } );

        // Create slab variant
        final String slabVariantName = this.getBlockRegistryName() + "_slab";
        variantSet.add(
                new AbstractBlockSlab( material, soundType, harvestToolClass, harvestLevel, hardness, resistance,
                                       lightLevel, lightOpacity )
                {

                    /**
                     * Retrieves the registry name of the block.
                     *
                     * @return The registry name of the block.
                     *
                     * @since 1.0
                     */
                    @Override
                    public String getBlockRegistryName() {
                        return slabVariantName;
                    }
                } );

        // Create stair variant
        final String stairVariantName = this.getBlockRegistryName() + "_stairs";
        variantSet.add(
                new AbstractBlockStairs( baseVariant, soundType, harvestToolClass, harvestLevel, hardness, resistance,
                                         lightLevel, lightOpacity )
                {
                    /**
                     * Retrieves the registry name of the block.
                     *
                     * @return The registry name of the block.
                     *
                     * @since 1.0
                     */
                    @Override
                    public String getBlockRegistryName() {
                        return stairVariantName;
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
                        return baseVariant.getBlockIsOpaqueCube( state );
                    }
                } );
    }

    /**
     * Overridden method from {@link Block} which sets the creative tab for the block and all blocks in the variant
     * set.
     *
     * @param tab The creative tab to set.
     *
     * @return The block instance.
     *
     * @since 1.0
     */
    @Nonnull
    @Override
    public Block setCreativeTab( CreativeTabs tab ) {
        for ( Block block : variantSet ) {
            block.setCreativeTab( tab );
        }
        return super.setCreativeTab( tab );
    }
}
