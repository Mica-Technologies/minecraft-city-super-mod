package com.micatechnologies.minecraft.csm.codeutils;

import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;

/**
 * Abstract block class which provides the same common methods and properties as
 * {@link AbstractBlock} and creates a variant set including solid block, slab, stair, and fence.
 *
 * @version 1.0
 * @see Block
 * @see AbstractBlock
 * @since 2023.3
 */
public abstract class AbstractBlockSetBasic extends AbstractBlock implements IHasModel, ICsmBlock {

  private final BlockSetVariantFence fence;
  private final BlockSetVariantSlab slab;
  private final BlockSetVariantStairs stairs;

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
  public AbstractBlockSetBasic(Material material,
      SoundType soundType,
      String harvestToolClass,
      int harvestLevel,
      float hardness,
      float resistance,
      float lightLevel,
      int lightOpacity) {
    super(material, soundType, harvestToolClass, harvestLevel, hardness, resistance, lightLevel,
        lightOpacity);
    this.fence = new BlockSetVariantFence();
    this.slab = new BlockSetVariantSlab();
    this.stairs = new BlockSetVariantStairs();
  }

  /**
   * Overridden method from {@link Block} which sets the creative tab for the block and all blocks
   * in the variant set.
   *
   * @param tab The creative tab to set.
   *
   * @return The block instance.
   *
   * @since 1.0
   */
  @Nonnull
  @Override
  public Block setCreativeTab(CreativeTabs tab) {
    this.fence.setCreativeTab(tab);
    this.slab.setCreativeTab(tab);
    this.stairs.setCreativeTab(tab);
    return super.setCreativeTab(tab);
  }

  public class BlockSetVariantFence extends AbstractBlockFence {

    /**
     * Constructs a {@link BlockSetVariantFence} instance.
     *
     * @since 1.0
     */
    public BlockSetVariantFence() {
      super(AbstractBlockSetBasic.this.blockMaterial);
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
      return AbstractBlockSetBasic.this.getBlockRegistryName() + "_fence";
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
    public boolean getBlockIsOpaqueCube(IBlockState state) {
      return false;
    }
  }

  public class BlockSetVariantSlab extends AbstractBlockSlab {

    /**
     * Constructs a {@link BlockSetVariantSlab} instance.
     *
     * @since 1.0
     */
    public BlockSetVariantSlab() {
      super(AbstractBlockSetBasic.this.blockMaterial, AbstractBlockSetBasic.this.blockSoundType,
          AbstractBlockSetBasic.this.getHarvestTool(AbstractBlockSetBasic.this.getDefaultState()),
          AbstractBlockSetBasic.this.getHarvestLevel(AbstractBlockSetBasic.this.getDefaultState()),
          AbstractBlockSetBasic.this.blockHardness, AbstractBlockSetBasic.this.blockResistance,
          AbstractBlockSetBasic.this.lightValue, AbstractBlockSetBasic.this.lightOpacity);
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
      return AbstractBlockSetBasic.this.getBlockRegistryName() + "_slab";
    }
  }

  public class BlockSetVariantStairs extends AbstractBlockStairs {

    /**
     * Constructs a {@link BlockSetVariantStairs} instance.
     *
     * @since 1.0
     */
    public BlockSetVariantStairs() {
      super(AbstractBlockSetBasic.this, AbstractBlockSetBasic.this.blockSoundType,
          AbstractBlockSetBasic.this.getHarvestTool(AbstractBlockSetBasic.this.getDefaultState()),
          AbstractBlockSetBasic.this.getHarvestLevel(AbstractBlockSetBasic.this.getDefaultState()),
          AbstractBlockSetBasic.this.blockHardness, AbstractBlockSetBasic.this.blockResistance,
          AbstractBlockSetBasic.this.lightValue, AbstractBlockSetBasic.this.lightOpacity);
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
      return AbstractBlockSetBasic.this.getBlockRegistryName() + "_stairs";
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
    public boolean getBlockIsOpaqueCube(IBlockState state) {
      return false;
    }
  }
}
