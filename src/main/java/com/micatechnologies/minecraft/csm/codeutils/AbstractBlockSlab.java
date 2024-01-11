package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.CsmConstants;
import com.micatechnologies.minecraft.csm.CsmRegistry;
import java.util.Objects;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSlab;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Abstract slab block class which provides common methods and properties for all blocks in this
 * mod.
 *
 * @version 1.0
 * @see Block
 * @since 2023.3
 */
@MethodsReturnNonnullByDefault
public abstract class AbstractBlockSlab extends BlockSlab implements IHasModel, ICsmBlock {

  /**
   * The property for the slab block's variant.
   *
   * @since 1.0
   */
  public static final PropertyEnum<Variant> VARIANT = PropertyEnum.create("variant", Variant.class);
  /**
   * The post-fix for the double slab variant. This is used to create the double slab variant's
   * registry name.
   *
   * @since 1.0
   */
  private static final String DOUBLE_VARIANT_POSTFIX = "_double";

  /**
   * Constructs an {@link AbstractBlockSlab} instance.
   *
   * @param material         The material of the slab block.
   * @param soundType        The sound type of the slab block.
   * @param harvestToolClass The harvest tool class of the slab block.
   * @param harvestLevel     The harvest level of the slab block.
   * @param hardness         The slab block's hardness.
   * @param resistance       The slab block's resistance to explosions.
   * @param lightLevel       The slab block's light level.
   * @param lightOpacity     The slab block's light opacity.
   *
   * @since 1.0
   */
  public AbstractBlockSlab(Material material,
      SoundType soundType,
      String harvestToolClass,
      int harvestLevel,
      float hardness,
      float resistance,
      float lightLevel,
      int lightOpacity) {
    super(material, material.getMaterialMapColor());
    AbstractBlockSlab.this.setTranslationKey(AbstractBlockSlab.this.getBlockRegistryName());
    AbstractBlockSlab.this.setRegistryName(CsmConstants.MOD_NAMESPACE,
        AbstractBlockSlab.this.getBlockRegistryName());
    AbstractBlockSlab.this.setSoundType(soundType);
    AbstractBlockSlab.this.setHarvestLevel(harvestToolClass, harvestLevel);
    AbstractBlockSlab.this.setHardness(hardness);
    AbstractBlockSlab.this.setResistance(resistance);
    AbstractBlockSlab.this.setLightLevel(lightLevel);
    AbstractBlockSlab.this.setLightOpacity(lightOpacity);
    CsmRegistry.registerBlock(AbstractBlockSlab.this);
    if (!AbstractBlockSlab.this.isDouble()) {
      final String doubleSlabRegistryName = AbstractBlockSlab.this.getBlockRegistryName() +
          DOUBLE_VARIANT_POSTFIX;
      Double doubleSlabVariant = new Double(material, soundType, harvestToolClass, harvestLevel,
          hardness,
          resistance, lightLevel, lightOpacity) {
        @Override
        public String getBlockRegistryName() {
          return doubleSlabRegistryName;
        }
      };
      CsmRegistry.registerItem(
          new ItemSlab(AbstractBlockSlab.this, AbstractBlockSlab.this,
              doubleSlabVariant).setRegistryName(
              Objects.requireNonNull(AbstractBlockSlab.this.getRegistryName())));

    }
    IBlockState iblockstate = AbstractBlockSlab.this.blockState.getBaseState()
        .withProperty(VARIANT, Variant.DEFAULT);

    if (!AbstractBlockSlab.this.isDouble()) {
      iblockstate.withProperty(HALF, BlockSlab.EnumBlockHalf.BOTTOM);
    }
    AbstractBlockSlab.this.setDefaultState(iblockstate);
    AbstractBlockSlab.this.useNeighborBrightness = !AbstractBlockSlab.this.isDouble();
  }

  /**
   * Registers the slab block's model.
   *
   * @see IHasModel#registerModels()
   * @since 1.0
   */
  @Override
  public void registerModels() {
    if (!AbstractBlockSlab.this.isDouble()) {
      Csm.proxy.setCustomModelResourceLocation(Item.getItemFromBlock(AbstractBlockSlab.this), 0,
          "inventory");
    }
  }

  /**
   * Overridden method from {@link BlockSlab} which returns if the face of the slab block should
   * block rendering.
   *
   * @param state the slab block state
   * @param world the slab block world
   * @param pos   the slab block position
   * @param face  the slab block face
   *
   * @return {@code true} if the face of the slab block should block rendering, {@code false}
   *     otherwise
   *
   * @see BlockSlab#doesSideBlockRendering(IBlockState, IBlockAccess, BlockPos, EnumFacing)
   * @since 1.0
   */
  @Override
  public boolean doesSideBlockRendering(IBlockState state, IBlockAccess world, BlockPos pos,
      EnumFacing face) {
    if (isDouble()) {
      return true;
    }
    return super.doesSideBlockRendering(state, world, pos, face);
  }

  /**
   * Overridden method from {@link BlockSlab} which gets the unlocalized name of the slab block.
   *
   * @param meta the slab block metadata
   *
   * @return the unlocalized name of the slab block
   *
   * @see BlockSlab#getTranslationKey(int)
   * @since 1.0
   */
  @Override
  public String getTranslationKey(int meta) {
    return super.getTranslationKey();
  }

  /**
   * Overridden method from {@link BlockSlab} which returns whether the slab block is double. This
   * method always returns {@code false} as this class represents a single slab block. The double
   * slab block is represented by the {@link AbstractBlockSlab.Double} class.
   *
   * @return {@code false}
   *
   * @see BlockSlab#isDouble()
   * @since 1.0
   */
  @Override
  public boolean isDouble() {
    return false;
  }

  /**
   * Overridden method from {@link BlockSlab} which gets the variant property of the slab block.
   *
   * @return the variant property of the slab block
   *
   * @see BlockSlab#getVariantProperty()
   * @since 1.0
   */
  @Override
  public IProperty<?> getVariantProperty() {
    return VARIANT;
  }

  /**
   * Overridden method from {@link BlockSlab} which gets the type of slab block from the item
   * stack.
   *
   * @param stack the item stack
   *
   * @return the type of slab block
   *
   * @see BlockSlab#getTypeForItem(ItemStack)
   * @since 1.0
   */
  @Override
  public Comparable<?> getTypeForItem(ItemStack stack) {
    return Variant.DEFAULT;
  }

  /**
   * Gets the {@link IBlockState} equivalent for this slab block using the specified {@code meta}
   * value.
   *
   * @param meta the value to get the equivalent {@link IBlockState} of
   *
   * @return the {@link IBlockState} equivalent for the specified {@code meta} value
   *
   * @see BlockSlab#getStateFromMeta(int)
   * @since 1.0
   */
  @Override
  public final IBlockState getStateFromMeta(final int meta) {
    IBlockState blockstate = AbstractBlockSlab.this.blockState.getBaseState()
        .withProperty(VARIANT, Variant.DEFAULT);

    if (!AbstractBlockSlab.this.isDouble()) {
      blockstate = blockstate.withProperty(HALF,
          ((meta & 8) != 0) ? EnumBlockHalf.TOP : EnumBlockHalf.BOTTOM);
    }

    return blockstate;
  }

  /**
   * Gets the equivalent {@link Integer} meta value for the specified {@link IBlockState} of this
   * slab block.
   *
   * @param state the {@link IBlockState} to get the equivalent {@link Integer} meta value for
   *
   * @return the equivalent {@link Integer} meta value for the specified {@link IBlockState}
   *
   * @see BlockSlab#getMetaFromState(IBlockState)
   * @since 1.0
   */
  @Override
  public final int getMetaFromState(final IBlockState state) {
    int meta = 0;

    if (!AbstractBlockSlab.this.isDouble() && state.getValue(HALF) == EnumBlockHalf.TOP) {
      meta |= 8;
    }

    return meta;
  }

  /**
   * Overridden method from {@link BlockSlab} which gets the item dropped from the slab block.
   *
   * @param state   the slab block state
   * @param rand    the random number generator
   * @param fortune the fortune level
   *
   * @return the item dropped from the slab block
   *
   * @see BlockSlab#getItemDropped(IBlockState, Random, int)
   * @since 1.0
   */
  @Override
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return Item.getItemFromBlock(CsmRegistry.getBlocksMap()
        .get(AbstractBlockSlab.this.getRegistryName() != null ?
            AbstractBlockSlab.this.getRegistryName().toString() :
            AbstractBlockSlab.this.getTranslationKey()));
  }

  /**
   * Overridden method from {@link BlockSlab} which gets the item for the slab block.
   *
   * @param worldIn the slab block world
   * @param pos     the slab block position
   * @param state   the slab block state
   *
   * @return the item for the slab block
   *
   * @see BlockSlab#getItem(World, BlockPos, IBlockState)
   * @since 1.0
   */
  @Override
  public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
    return new ItemStack(CsmRegistry.getBlocksMap()
        .get(AbstractBlockSlab.this.getRegistryName() != null ?
            AbstractBlockSlab.this.getRegistryName().toString() :
            AbstractBlockSlab.this.getTranslationKey()));
  }

  /**
   * Creates a new {@link BlockStateContainer} for the slab block with the required property for
   * rotation.
   *
   * @return a new {@link BlockStateContainer} for the slab block
   *
   * @see BlockSlab#createBlockState()
   * @since 1.0
   */
  @Override
  protected BlockStateContainer createBlockState() {
    if (!this.isDouble()) {
      return new BlockStateContainer(AbstractBlockSlab.this, VARIANT, HALF);
    }
    return new BlockStateContainer(AbstractBlockSlab.this, VARIANT);
  }

  /**
   * Implementation of the
   * {@link ICsmBlock#getBlockBoundingBox(IBlockState, IBlockAccess, BlockPos)} method which returns
   * {@code null}, as this class uses the standard/default {@link BlockSlab} bounding boxes. This
   * method is overridden to prevent the need to implement the
   * {@link ICsmBlock#getBlockBoundingBox(IBlockState, IBlockAccess, BlockPos)} method in
   * subclasses.
   *
   * @param state  the block state
   * @param source the block access
   * @param pos    the block position
   *
   * @return {@code null}
   *
   * @implNote The value returned by this method implementation is ignored.
   * @since 1.0
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return null;
  }

  /**
   * Implementation of the {@link ICsmBlock#getBlockIsOpaqueCube(IBlockState)} method which returns
   * {@code false}, as this class uses the standard/default {@link BlockSlab} opaque cube value.
   * This method is overridden to prevent the need to implement the
   * {@link ICsmBlock#getBlockIsOpaqueCube(IBlockState)} method in subclasses.
   *
   * @param state the {@link IBlockState} to get the opaque cube value of
   *
   * @return {@code false}
   *
   * @implNote The value returned by this method implementation is ignored.
   * @since 1.0
   */
  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  /**
   * Implementation of the {@link ICsmBlock#getBlockIsFullCube(IBlockState)} method which returns
   * {@code false}, as this class uses the standard/default {@link BlockSlab} full cube value. This
   * method is overridden to prevent the need to implement the
   * {@link ICsmBlock#getBlockIsFullCube(IBlockState)} method in subclasses.
   *
   * @param state the {@link IBlockState} to get the full cube value of
   *
   * @return {@code false}
   *
   * @implNote The value returned by this method implementation is ignored.
   * @since 1.0
   */
  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  /**
   * Implementation of the
   * {@link ICsmBlock#getBlockConnectsRedstone(IBlockState, IBlockAccess, BlockPos, EnumFacing)}
   * method which returns {@code false}, as this class uses the standard/default {@link BlockSlab}
   * redstone connection value. This method is overridden to prevent the need to implement the
   * {@link ICsmBlock#getBlockConnectsRedstone(IBlockState, IBlockAccess, BlockPos, EnumFacing)}
   * method in subclasses.
   *
   * @param state  the block state
   * @param access the block access
   * @param pos    the block position
   * @param facing the block facing direction
   *
   * @return {@code false}
   *
   * @implNote The value returned by this method implementation is ignored.
   * @since 1.0
   */
  @Override
  public boolean getBlockConnectsRedstone(IBlockState state,
      IBlockAccess access,
      BlockPos pos,
      @Nullable
      EnumFacing facing) {
    return false;
  }

  /**
   * Implementation of the {@link ICsmBlock#getBlockRenderLayer()} method which returns
   * {@code null}, as this class uses the standard/default {@link BlockSlab} render layer value.
   * This method is overridden to prevent the need to implement the
   * {@link ICsmBlock#getBlockRenderLayer()} method in subclasses.
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

  /**
   * Enum which represents the variant of the slab block.
   *
   * @version 1.0
   * @see IStringSerializable
   * @since 1.0
   */
  public enum Variant implements IStringSerializable {
    /**
     * The default variant of the slab block.
     *
     * @since 1.0
     */
    DEFAULT;

    /**
     * Gets the name of the variant.
     *
     * @return the name of the variant
     *
     * @see IStringSerializable#getName()
     * @since 1.0
     */
    @Override
    public String getName() {
      return "default";
    }

  }

  /**
   * Static class which represents a double slab block.
   *
   * @version 1.0
   * @see AbstractBlockSlab
   * @see BlockSlab
   * @since 1.0
   */
  public static abstract class Double extends AbstractBlockSlab {

    /**
     * Constructs an {@link AbstractBlockSlab.Double} instance.
     *
     * @param material         The material of the slab block.
     * @param soundType        The sound type of the slab block.
     * @param harvestToolClass The harvest tool class of the slab block.
     * @param harvestLevel     The harvest level of the slab block.
     * @param hardness         The slab block's hardness.
     * @param resistance       The slab block's resistance to explosions.
     * @param lightLevel       The slab block's light level.
     * @param lightOpacity     The slab block's light opacity.
     *
     * @since 1.0
     */
    public Double(Material material,
        SoundType soundType,
        String harvestToolClass,
        int harvestLevel,
        float hardness,
        float resistance,
        float lightLevel,
        int lightOpacity) {
      super(material, soundType, harvestToolClass, harvestLevel, hardness, resistance, lightLevel,
          lightOpacity);
    }

    /**
     * Overridden method from {@link BlockSlab} which returns whether the slab block is double. This
     * method always returns {@code true} as this class represents a double slab block.
     *
     * @return {@code true}
     *
     * @see BlockSlab#isDouble()
     * @since 1.0
     */
    @Override
    public boolean isDouble() {
      return true;
    }

  }
}
