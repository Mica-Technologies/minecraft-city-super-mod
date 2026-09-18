package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A garage door opener: the motor hung from the ceiling, with its rail running forward to the wall
 * over the door. Right-click it, or give it a redstone signal, and it opens or closes the door the
 * rail leads to.
 *
 * <p>Place it looking at the door, in the row just above the door's top (where a sectional door's
 * ceiling track runs), and hang it from the ceiling with {@link BlockGarageDoorHanger}s if the
 * ceiling is higher than that. The rail reaches as far forward as there is air, up to
 * {@link #MAX_LENGTH} blocks, and ends in a bracket on whatever it meets: its length is actual
 * state, so there is nothing to set up and nothing to tick. The door is found below the rail's
 * end: the first garage door in the three blocks under the wall it meets.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockGarageDoorOpener extends AbstractBlock {

  public static final PropertyDirection FACING = BlockHorizontal.FACING;
  /** SHARED with gen_garage_doors.MAX_RAIL. */
  public static final int MAX_LENGTH = 10;
  public static final PropertyInteger LENGTH = PropertyInteger.create("length", 0, MAX_LENGTH);
  public static final PropertyBool TOP = PropertyBool.create("top");

  /** The motor housing, with the door to the north. */
  private static final AxisAlignedBB MOTOR_NORTH =
      new AxisAlignedBB(3 / 16.0, 4 / 16.0, 3 / 16.0, 13 / 16.0, 11 / 16.0, 15 / 16.0);

  /** Which openers were last seen powered, per world, to act only on a rising edge. */
  private static final Map<World, Set<BlockPos>> POWERED =
      Collections.synchronizedMap(new WeakHashMap<>());

  /**
   * Constructs a {@link BlockGarageDoorOpener}.
   *
   * @since 1.0
   */
  public BlockGarageDoorOpener() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 0, 1F, 5F, 0F, 0);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
  }

  @Override
  public String getBlockRegistryName() {
    return "garage_door_opener";
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, LENGTH, TOP);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex();
  }

  /**
   * Faces the way the player looks: toward the door.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing());
  }

  /** How many blocks of air lie between the opener and the wall its rail runs to. */
  private static int length(IBlockAccess world, BlockPos pos, EnumFacing f) {
    int n = 0;
    while (n < MAX_LENGTH && world.isAirBlock(pos.offset(f, n + 1))) {
      n++;
    }
    return n;
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    return state.withProperty(LENGTH, length(worldIn, pos, state.getValue(FACING)))
        .withProperty(TOP, !(worldIn.getBlockState(pos.up()).getBlock()
            instanceof BlockGarageDoorHanger));
  }

  /**
   * Gives a command to the door the rail leads to, if there is one: its own button and redstone
   * toggle it, a linked wall control may open, close or stop it.
   *
   * @param world   the world
   * @param pos     the opener
   * @param state   its state
   * @param command what to do
   *
   * @return whether a door was found
   */
  boolean operate(World world, BlockPos pos, IBlockState state, BlockGarageDoor.Command command) {
    EnumFacing f = state.getValue(FACING);
    BlockPos wall = pos.offset(f, length(world, pos, f) + 1);
    for (int i = 0; i <= 3; i++) {
      BlockPos p = wall.down(i);
      IBlockState s = world.getBlockState(p);
      if (s.getBlock() instanceof BlockGarageDoor) {
        ((BlockGarageDoor) s.getBlock()).command(world, p, command);
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    if (playerIn.isSneaking()) {
      // The second half of linking a wall control to this door; see GarageDoorLinks.
      return hand == EnumHand.MAIN_HAND && GarageDoorLinks.finish(playerIn, worldIn, pos);
    }
    if (!worldIn.isRemote) {
      operate(worldIn, pos, state, BlockGarageDoor.Command.TOGGLE);
    }
    return true;
  }

  /**
   * A redstone signal arriving -- the rising edge only -- works the door, as a wall button wired
   * to a real opener does.
   *
   * @since 1.0
   */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    if (worldIn.isRemote) {
      return;
    }
    boolean powered = worldIn.isBlockPowered(pos);
    Set<BlockPos> seen = POWERED.computeIfAbsent(worldIn, w -> new HashSet<>());
    boolean was = seen.contains(pos);
    if (powered && !was) {
      seen.add(pos.toImmutable());
      operate(worldIn, pos, state, BlockGarageDoor.Command.TOGGLE);
    } else if (!powered && was) {
      seen.remove(pos);
    }
  }

  // --- shape ------------------------------------------------------------------------------------

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BlockGarageDoor.turn(MOTOR_NORTH, state.getValue(FACING));
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, @Nonnull World worldIn,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn,
      boolean isActualState) {
    addCollisionBoxToList(pos, entityBox, collidingBoxes,
        BlockGarageDoor.turn(MOTOR_NORTH, state.getValue(FACING)));
  }

  @Override
  @Nonnull
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
