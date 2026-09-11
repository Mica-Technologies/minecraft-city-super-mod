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
    // Meta is four bits and only eight of the sixteen values are ours, so anything above seven
    // has to be absorbed rather than indexed with. Chunks written before a block moved to eight
    // facings, and any block whose meta once carried something else, both arrive here — and an
    // exception thrown while a chunk is loading takes the world with it, not just the block.
    DirectionEight[] directions = DirectionEight.values();
    if (meta < 0 || meta >= directions.length) {
      return getDefaultState();
    }
    return getDefaultState().withProperty(FACING, directions[meta]);
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
    // Stacked on the same kind of block, take its facing, so the two read as one assembly
    IBlockState belowState = worldIn.getBlockState(pos.down());
    if (belowState.getProperties().containsKey(FACING) && inheritsFacingFrom(belowState)) {
      return this.getDefaultState().withProperty(FACING, belowState.getValue(FACING));
    } else {
      // Otherwise, determine the direction based on placer's orientation
      DirectionEight direction = getDirectionFromPlacer(placer);
      return this.getDefaultState().withProperty(FACING, direction);
    }
  }

  /**
   * Whether this block, placed on top of {@code below}, takes its facing instead of the placer's.
   *
   * <p>This is for STACKING like on like — a sign on the post under it — where the two are one
   * assembly and turning the upper one by hand to match would be a chore. It used to answer yes
   * for any eight-way block at all, so a sign put up on a guardrail silently turned to face the
   * way the rail does. The default is now the same class only, and a family that stacks across
   * classes widens it.</p>
   *
   * @param below the state of the block directly below, which carries {@link #FACING}
   *
   * @return true to copy the facing of {@code below}
   *
   * @since 2026.9
   */
  protected boolean inheritsFacingFrom(IBlockState below) {
    return below.getBlock().getClass() == getClass();
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
