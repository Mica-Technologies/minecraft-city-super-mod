package com.micatechnologies.minecraft.csm.powergrid.sewer;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A sidewalk door (#310): a steel or aluminium door set flush in the pavement over a vault or a
 * utility tunnel, several blocks across, one block a cell. It is placed in the ground like the
 * {@link BlockAccessHatch}, its top the block's top, in a hole dug to its size.
 *
 * <p><b>Shape.</b> A door is {@code width} cells across (2 or 3) and {@code length} cells along
 * its hinges (1 to 4). Facing north (the model's), the placer stands to the north: the door
 * reaches from the cell clicked to the model's west (the placer's right) and south (away from
 * them). A double door's leaves are hinged on its west and east edges and meet over the middle;
 * a single door's one leaf is hinged on its west edge. Open, a leaf stands on its hinge as tall
 * as it is wide (half the door for a double door, all of it for a single one).</p>
 *
 * <p><b>What a cell stores.</b> Only its facing and {@link #OPEN}. Which part of the door it is
 * comes from its neighbours as actual state ({@link #LEFT}, {@link #RIGHT}, {@link #FRONT},
 * {@link #BACK}: the door carries on to the model's west, east, north, south), so the multipart
 * blockstate draws the frame and collar on the outside edges, the seam between the leaves and
 * the standing leaves on the hinge edges. Nothing ticks and there is no tile entity.</p>
 *
 * <p>A click opens or shuts the whole door; breaking one cell takes the door and drops one.</p>
 *
 * @since 2026.10
 */
public class BlockSidewalkDoor extends AbstractBlockRotatableNSEW {

  /** Whether the leaves are open. */
  public static final PropertyBool OPEN = PropertyBool.create("open");
  /** The door carries on to the model's west. */
  public static final PropertyBool LEFT = PropertyBool.create("left");
  /** The door carries on to the model's east. */
  public static final PropertyBool RIGHT = PropertyBool.create("right");
  /** The door carries on to the model's north, toward the placer. */
  public static final PropertyBool FRONT = PropertyBool.create("front");
  /** The door carries on to the model's south. */
  public static final PropertyBool BACK = PropertyBool.create("back");

  /** The most cells one door is searched for, beyond the largest door. */
  private static final int MAX_CELLS = 64;

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();
  /** Set while a door's cells are removed together, so they do not take each other again. */
  private static final ThreadLocal<Boolean> DEMOLISHING = ThreadLocal.withInitial(() -> false);

  /** The concrete collar and frame round the opening, in blocks; the generator's COLLAR. */
  private static final double C = 1.5 / 16;
  /** The collar on the model's north side; turned for the other sides. */
  private static final AxisAlignedBB COLLAR_NORTH = new AxisAlignedBB(0, 0, 0, 1, 1, C);
  /** A standing leaf, on the model's west edge, before its height is set. */
  private static final double LEAF_X0 = 0.4 / 16;
  private static final double LEAF_X1 = 2.4 / 16;

  private final String registryName;
  private final int width;
  private final int length;
  private final boolean single;

  /**
   * @param registryName its registry name
   * @param width        cells across the leaves, 2 or 3
   * @param length       cells along the hinges, 1 to 4
   * @param single       one leaf hinged on the west edge, rather than two meeting in the middle
   */
  public BlockSidewalkDoor(String registryName, int width, int length, boolean single) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 3F, 15F, 0F, 0, true);
    this.registryName = registryName;
    this.width = width;
    this.length = length;
    this.single = single;
    PENDING.remove();
    setDefaultState(getDefaultState().withProperty(OPEN, false).withProperty(LEFT, false)
        .withProperty(RIGHT, false).withProperty(FRONT, false).withProperty(BACK, false));
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, OPEN, LEFT, RIGHT, FRONT, BACK);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(OPEN, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(OPEN) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(OPEN, false);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);
    return state.withProperty(LEFT, joins(world, pos.offset(facing.rotateYCCW()), facing))
        .withProperty(RIGHT, joins(world, pos.offset(facing.rotateY()), facing))
        .withProperty(FRONT, joins(world, pos.offset(facing), facing))
        .withProperty(BACK, joins(world, pos.offset(facing.getOpposite()), facing));
  }

  private boolean joins(IBlockAccess world, BlockPos at, EnumFacing facing) {
    IBlockState other = world.getBlockState(at);
    return other.getBlock() == this && other.getValue(FACING) == facing;
  }

  /** The door's cells, from the cell clicked: west (the model's) and south of it. */
  private List<BlockPos> cellsFrom(BlockPos pos, EnumFacing facing) {
    List<BlockPos> cells = new ArrayList<>(width * length);
    EnumFacing across = facing.rotateYCCW();
    EnumFacing along = facing.getOpposite();
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < length; j++) {
        cells.add(pos.offset(across, i).offset(along, j));
      }
    }
    return cells;
  }

  /** The cells joined to {@code pos}: this block, facing the same way, side by side. */
  private Set<BlockPos> door(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    Set<BlockPos> found = new HashSet<>();
    ArrayDeque<BlockPos> todo = new ArrayDeque<>();
    found.add(pos);
    todo.add(pos);
    while (!todo.isEmpty() && found.size() < MAX_CELLS) {
      BlockPos at = todo.poll();
      for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
        BlockPos next = at.offset(side);
        if (!found.contains(next) && joins(world, next, facing)) {
          found.add(next);
          todo.add(next);
        }
      }
    }
    return found;
  }

  /** Places the rest of the door, or refuses it if a cell is taken. */
  @Override
  public void onBlockPlacedBy(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state, @Nonnull EntityLivingBase placer, @Nonnull ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    if (world.isRemote) {
      return;
    }
    List<BlockPos> cells = cellsFrom(pos, state.getValue(FACING));
    for (BlockPos cell : cells) {
      if (!cell.equals(pos) && !world.getBlockState(cell).getBlock().isReplaceable(world, cell)) {
        DEMOLISHING.set(true);
        try {
          world.setBlockToAir(pos);
        } finally {
          DEMOLISHING.set(false);
        }
        if (placer instanceof EntityPlayer) {
          EntityPlayer player = (EntityPlayer) placer;
          if (!player.capabilities.isCreativeMode) {
            player.inventory.addItemStackToInventory(new ItemStack(this));
          }
          player.sendStatusMessage(new TextComponentTranslation("csm.utilities.hatch.blocked",
              cell.getX(), cell.getY(), cell.getZ()), true);
        }
        return;
      }
    }
    IBlockState cellState = getDefaultState().withProperty(FACING, state.getValue(FACING));
    for (BlockPos cell : cells) {
      if (!cell.equals(pos)) {
        world.setBlockState(cell, cellState, 3);
      }
    }
  }

  /** Takes the rest of the door with it; only the cell broken drops the door. */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    if (!world.isRemote && !DEMOLISHING.get()) {
      Set<BlockPos> cells = door(world, pos, state.getValue(FACING));
      cells.remove(pos);
      DEMOLISHING.set(true);
      try {
        for (BlockPos cell : cells) {
          world.setBlockToAir(cell);
        }
      } finally {
        DEMOLISHING.set(false);
      }
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      boolean open = !state.getValue(OPEN);
      for (BlockPos cell : door(world, pos, state.getValue(FACING))) {
        world.setBlockState(cell, world.getBlockState(cell).withProperty(OPEN, open), 3);
      }
      world.playSound(null, pos, open ? SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN
          : SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 1.0F, open ? 0.7F : 0.8F);
    }
    return true;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return FULL_BLOCK_AABB;
  }

  /**
   * Shut, a cell is walked on. Open, it is the collar on the door's outside edges and, on a hinge
   * edge, the leaf standing on it, as tall as the leaf is wide.
   */
  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
      AxisAlignedBB entityBox, List<AxisAlignedBB> boxes, @Nullable Entity entity,
      boolean isActualState) {
    if (!state.getValue(OPEN)) {
      addCollisionBoxToList(pos, entityBox, boxes, FULL_BLOCK_AABB);
      return;
    }
    IBlockState actual = isActualState ? state : getActualState(state, world, pos);
    EnumFacing facing = state.getValue(FACING);
    // The collar's sides, each the north side turned, as the blockstate turns its model
    if (!actual.getValue(FRONT)) {
      addCollisionBoxToList(pos, entityBox, boxes, RotationUtils.rotateBoundingBoxByFacing(
          COLLAR_NORTH, facing));
    }
    if (!actual.getValue(RIGHT)) {
      addCollisionBoxToList(pos, entityBox, boxes, RotationUtils.rotateBoundingBoxByFacing(
          COLLAR_NORTH, facing.rotateY()));
    }
    if (!actual.getValue(BACK)) {
      addCollisionBoxToList(pos, entityBox, boxes, RotationUtils.rotateBoundingBoxByFacing(
          COLLAR_NORTH, facing.getOpposite()));
    }
    if (!actual.getValue(LEFT)) {
      addCollisionBoxToList(pos, entityBox, boxes, RotationUtils.rotateBoundingBoxByFacing(
          COLLAR_NORTH, facing.rotateYCCW()));
    }
    double leaf = single ? width : width / 2.0;
    AxisAlignedBB standing = new AxisAlignedBB(LEAF_X0, 1, 0, LEAF_X1, 1 + leaf, 1);
    if (!actual.getValue(LEFT)) {
      addCollisionBoxToList(pos, entityBox, boxes,
          RotationUtils.rotateBoundingBoxByFacing(standing, facing));
    }
    if (!single && !actual.getValue(RIGHT)) {
      addCollisionBoxToList(pos, entityBox, boxes,
          RotationUtils.rotateBoundingBoxByFacing(standing, facing.getOpposite()));
    }
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return face == EnumFacing.UP && !state.getValue(OPEN) ? BlockFaceShape.SOLID
        : BlockFaceShape.UNDEFINED;
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
