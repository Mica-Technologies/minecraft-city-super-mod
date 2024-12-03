package com.micatechnologies.minecraft.csm.codeutils;

import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Abstract block class which provides the same common methods and properties as
 * {@link AbstractBlock} and adds horizontal rotation functionality with eight directions (North,
 * Northeast, East, Southeast, South, Southwest, West, Northwest).
 *
 * @version 1.0
 * @see Block
 * @see AbstractBlock
 * @since 2023.3
 */
public abstract class AbstractBlockRotatableHZEight extends AbstractBlock {

  /**
   * The block facing direction property (horizontal eight directions)
   *
   * @since 1.0
   */
  public static final PropertyEnum<DirectionEight> FACING =
      PropertyEnum.create("facing", DirectionEight.class);

  /**
   * Constructs an {@link AbstractBlockRotatableHZEight} instance.
   *
   * @param material The material of the block.
   *
   * @since 1.0
   */
  public AbstractBlockRotatableHZEight(Material material) {
    this(material, true);
  }

  /**
   * Constructs an {@link AbstractBlockRotatableHZEight} instance.
   *
   * @param material        The material of the block.
   * @param setDefaultState Whether to set the default state of the block
   *
   * @since 1.0
   */
  public AbstractBlockRotatableHZEight(Material material, boolean setDefaultState) {
    super(material);
    if (setDefaultState) {
      this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, DirectionEight.N));
    }
  }

  /**
   * Constructs an {@link AbstractBlockRotatableHZEight} instance.
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
  public AbstractBlockRotatableHZEight(Material material, SoundType soundType,
      String harvestToolClass, int harvestLevel, float hardness, float resistance, float lightLevel,
      int lightOpacity) {
    this(material, soundType, harvestToolClass, harvestLevel, hardness, resistance, lightLevel,
        lightOpacity, true);
  }

  /**
   * Constructs an {@link AbstractBlockRotatableHZEight} instance.
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
  public AbstractBlockRotatableHZEight(Material material, SoundType soundType,
      String harvestToolClass, int harvestLevel, float hardness, float resistance, float lightLevel,
      int lightOpacity, boolean setDefaultState) {
    super(material, soundType, harvestToolClass, harvestLevel, hardness, resistance, lightLevel,
        lightOpacity);
    if (setDefaultState) {
      this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, DirectionEight.N));
    }
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
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, DirectionEight.values()[meta]);
  }

  /**
   * Gets the equivalent {@link Integer} meta value for the specified {@link IBlockState} of this
   * block.
   *
   * @param state the {@link IBlockState} to get the equivalent {@link Integer} meta value for
   *
   * @return the equivalent {@link Integer} meta value for the specified {@link IBlockState}
   *
   * @see Block#getMetaFromState(IBlockState)
   * @since 1.0
   */
  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getIndex();
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
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    // Check if the block below is the same type and has a FACING property
    IBlockState belowState = worldIn.getBlockState(pos.down());
    if (belowState.getBlock() instanceof AbstractBlockRotatableHZEight && belowState.getProperties()
        .containsKey(FACING)) {
      // If so, use the same direction
      return this.getDefaultState().withProperty(FACING, belowState.getValue(FACING));
    } else {
      // Otherwise, determine the direction based on placer's orientation
      DirectionEight direction = getDirectionFromPlacer(placer);
      return this.getDefaultState().withProperty(FACING, direction);
    }
  }

  /**
   * Creates a new {@link BlockStateContainer} for the block with the required property for
   * rotation.
   *
   * @return a new {@link BlockStateContainer} for the block
   *
   * @see Block#createBlockState()
   * @since 1.0
   */
  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING);
  }

  /**
   * Gets the {@link DirectionEight} for block placement from the specified {@link EntityLivingBase}
   * (placer).
   *
   * @param placer the placer of the block
   *
   * @return the {@link DirectionEight} for block placement from the specified
   *     {@link EntityLivingBase} (placer)
   */
  private DirectionEight getDirectionFromPlacer(EntityLivingBase placer) {
    // Normalize the yaw angle to a value between 0 and 360
    float yaw = placer.rotationYaw % 360;
    if (yaw < 0) {
      yaw += 360;
    }

    // Adjust the direction based on the yaw to correct flipped placement
    if (yaw < 22.5 || yaw >= 337.5) {
      return DirectionEight.N; // Placer is facing South, so place North
    } else if (yaw < 67.5) {
      return DirectionEight.NE; // Placer is facing Southwest, so place Northeast
    } else if (yaw < 112.5) {
      return DirectionEight.E; // Placer is facing West, so place East
    } else if (yaw < 157.5) {
      return DirectionEight.SE; // Placer is facing Northwest, so place Southeast
    } else if (yaw < 202.5) {
      return DirectionEight.S; // Placer is facing North, so place South
    } else if (yaw < 247.5) {
      return DirectionEight.SW; // Placer is facing Northeast, so place Southwest
    } else if (yaw < 292.5) {
      return DirectionEight.W; // Placer is facing East, so place West
    } else {
      return DirectionEight.NW; // Placer is facing Southeast, so place Northwest
    }
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
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {

    // Rotate the bounding box based on the facing direction if FACING property is present
    if (state.getBlock() instanceof AbstractBlockRotatableHZEight) {
      // Retrieve the actual state
      IBlockState actualState = source.getBlockState(pos).getActualState(source, pos);

      // Rotate the bounding box based on the facing direction if FACING property is present
      if (actualState.getProperties().containsKey(FACING)) {
        return RotationUtils.rotateBoundingBoxByFacing(
            getBlockBoundingBox(actualState, source, pos),
            actualState.getValue(FACING));
      }
    }

    // Default to a square bounding box
    return SQUARE_BOUNDING_BOX;
  }
}
