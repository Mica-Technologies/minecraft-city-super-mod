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
public abstract class AbstractBlockRotatableHZSixteen extends AbstractBlock {

  /**
   * The block facing direction property (horizontal sixteen directions)
   *
   * @since 1.0
   */
  public static final PropertyEnum<DirectionSixteen> FACING =
      PropertyEnum.create("facing", DirectionSixteen.class);

  /**
   * Constructs an {@link AbstractBlockRotatableHZSixteen} instance.
   *
   * @param material The material of the block.
   *
   * @since 1.0
   */
  public AbstractBlockRotatableHZSixteen(Material material) {
    this(material, true);
  }

  /**
   * Constructs an {@link AbstractBlockRotatableHZSixteen} instance.
   *
   * @param material        The material of the block.
   * @param setDefaultState Whether to set the default state of the block
   *
   * @since 1.0
   */
  public AbstractBlockRotatableHZSixteen(Material material, boolean setDefaultState) {
    super(material);
    if (setDefaultState) {
      this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, DirectionSixteen.N));
    }
  }

  /**
   * Constructs an {@link AbstractBlockRotatableHZSixteen} instance.
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
  public AbstractBlockRotatableHZSixteen(Material material, SoundType soundType,
      String harvestToolClass, int harvestLevel, float hardness, float resistance, float lightLevel,
      int lightOpacity) {
    this(material, soundType, harvestToolClass, harvestLevel, hardness, resistance, lightLevel,
        lightOpacity, true);
  }

  /**
   * Constructs an {@link AbstractBlockRotatableHZSixteen} instance.
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
  public AbstractBlockRotatableHZSixteen(Material material, SoundType soundType,
      String harvestToolClass, int harvestLevel, float hardness, float resistance, float lightLevel,
      int lightOpacity, boolean setDefaultState) {
    super(material, soundType, harvestToolClass, harvestLevel, hardness, resistance, lightLevel,
        lightOpacity);
    if (setDefaultState) {
      this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, DirectionSixteen.N));
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
    DirectionSixteen directionSixteen = DirectionSixteen.fromIndex(meta);
    return directionSixteen != null
        ? getDefaultState().withProperty(FACING, directionSixteen)
        : getDefaultState();
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
    if (belowState.getBlock() instanceof AbstractBlockRotatableHZSixteen
        && belowState.getProperties().containsKey(FACING)) {
      // If so, use the same direction
      return this.getDefaultState().withProperty(FACING, belowState.getValue(FACING));
    } else {
      // Otherwise, determine the direction based on placer's orientation
      DirectionSixteen direction = getDirectionFromPlacer(placer);
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
   * Gets the {@link DirectionSixteen} for block placement from the specified
   * {@link EntityLivingBase} (placer).
   *
   * @param placer the placer of the block
   *
   * @return the {@link DirectionSixteen} for block placement from the specified
   *     {@link EntityLivingBase} (placer)
   */
  private DirectionSixteen getDirectionFromPlacer(EntityLivingBase placer) {
    float yaw = placer.rotationYaw % 360;
    if (yaw < 0) {
      yaw += 360;
    }

    if (yaw < 11.25 || yaw >= 348.75) {
      return DirectionSixteen.N;
    } else if (yaw < 33.75) {
      return DirectionSixteen.NNE;
    } else if (yaw < 56.25) {
      return DirectionSixteen.NE;
    } else if (yaw < 78.75) {
      return DirectionSixteen.ENE;
    } else if (yaw < 101.25) {
      return DirectionSixteen.E;
    } else if (yaw < 123.75) {
      return DirectionSixteen.ESE;
    } else if (yaw < 146.25) {
      return DirectionSixteen.SE;
    } else if (yaw < 168.75) {
      return DirectionSixteen.SSE;
    } else if (yaw < 191.25) {
      return DirectionSixteen.S;
    } else if (yaw < 213.75) {
      return DirectionSixteen.SSW;
    } else if (yaw < 236.25) {
      return DirectionSixteen.SW;
    } else if (yaw < 258.75) {
      return DirectionSixteen.WSW;
    } else if (yaw < 281.25) {
      return DirectionSixteen.W;
    } else if (yaw < 303.75) {
      return DirectionSixteen.WNW;
    } else if (yaw < 326.25) {
      return DirectionSixteen.NW;
    } else {
      return DirectionSixteen.NNW;
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
    return RotationUtils.rotateBoundingBoxByFacing(getBlockBoundingBox(state, source, pos),
        state.getValue(FACING));
  }
}
