package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A tympanum ({@code cathedral_tympanum_<variant>}): the carved panel that fills the pointed arch
 * over a square-headed door, in the arch a {@link BlockCathedralDoorArch} would make -- four blocks
 * across over a pair, two over a single leaf, three high -- with the stone beside the arch. It
 * does not move.
 *
 * <p>One block per cell. The metadata holds the facing (the way to the inside, as a door's) and the
 * cell's column across the arch, 0 to 3 from the left seen from outside; the course (counted up
 * through the tympanum cells below) and whether it is a pair's arch (whether column 2 follows
 * column 1) are actual state. Each cell draws its piece of one carving, its texture window written
 * into the model.</p>
 *
 * @version 1.0
 * @since 2026.10
 */
public class BlockCathedralTympanum extends AbstractBlock {

  public static final PropertyDirection FACING = BlockHorizontal.FACING;
  public static final PropertyInteger COL = PropertyInteger.create("col", 0, 3);
  /** Actual state only. */
  public static final PropertyInteger COURSE = BlockCathedralDoorArch.COURSE;
  /** Actual state only. */
  public static final PropertyBool PAIRED = BlockCathedralDoorArch.PAIRED;

  private static final AxisAlignedBB PLANE_NORTH =
      new AxisAlignedBB(0, 0, 1 - BlockCathedralDoor.LEAF, 1, 1, 1);

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a {@link BlockCathedralTympanum}.
   *
   * @param registryName {@code cathedral_tympanum_<variant>}
   *
   * @since 1.0
   */
  public BlockCathedralTympanum(String registryName) {
    super(pendingMaterial(registryName), registryName.contains("bronze") ? SoundType.METAL
        : SoundType.WOOD, registryName.contains("bronze") ? "pickaxe" : "axe", 0, 3F, 15F, 0F, 0);
    this.registryName = registryName;
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(COL, 0).withProperty(COURSE, 0).withProperty(PAIRED, false));
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material pendingMaterial(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return registryName.contains("bronze") ? Material.IRON : Material.WOOD;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  protected ItemBlock createItemBlock() {
    return new ItemTympanum(this);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, COL, COURSE, PAIRED);
  }

  /** Bits 0-1 the facing (horizontal index: S 0, W 1, N 2, E 3), bits 2-3 the column. */
  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(COL, (meta >> 2) & 3);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | state.getValue(COL) << 2;
  }

  private boolean sameColumn(IBlockState other, IBlockState mine) {
    return other.getBlock() == this && other.getValue(FACING) == mine.getValue(FACING)
        && other.getValue(COL).equals(mine.getValue(COL));
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    int course = 0;
    while (course < 2 && sameColumn(worldIn.getBlockState(pos.down(course + 1)), state)) {
      course++;
    }
    int col = state.getValue(COL);
    boolean paired = col >= 2;
    if (!paired) {
      BlockPos third = pos.offset(state.getValue(FACING).rotateY(), 2 - col);
      IBlockState s = worldIn.getBlockState(third);
      paired = s.getBlock() == this && s.getValue(FACING) == state.getValue(FACING)
          && s.getValue(COL) == 2;
    }
    return state.withProperty(COURSE, course).withProperty(PAIRED, paired);
  }

  /**
   * Places a whole tympanum from its item, the clicked cell its left column: four blocks across if
   * there is room (sneak for two), three high.
   */
  boolean placeFromItem(World world, BlockPos pos, EntityPlayer player) {
    EnumFacing f = player.getHorizontalFacing();
    EnumFacing right = f.rotateY();
    int width = player.isSneaking() ? 2 : 4;
    while (width >= 2 && !room(world, pos, right, width)) {
      width -= 2;
    }
    if (width < 2) {
      return BlockCathedralDoor.refuse(world, player, "gui.csm.door.cathedral.no_tympanum_room");
    }
    for (int c = 0; c < width; c++) {
      for (int y = 0; y < 3; y++) {
        world.setBlockState(pos.offset(right, c).up(y),
            getDefaultState().withProperty(FACING, f).withProperty(COL, c), 3);
      }
    }
    return true;
  }

  private static boolean room(World world, BlockPos pos, EnumFacing right, int width) {
    for (int c = 0; c < width; c++) {
      for (int y = 0; y < 3; y++) {
        if (!BlockCathedralDoor.clear(world, pos.offset(right, c).up(y))) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BlockGarageDoor.turn(PLANE_NORTH, state.getValue(FACING));
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

  /** The item that places a whole tympanum rather than one cell. */
  static class ItemTympanum extends ItemBlock {

    ItemTympanum(Block block) {
      super(block);
    }

    @Override
    public boolean placeBlockAt(@Nonnull ItemStack stack, @Nonnull EntityPlayer player,
        @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EnumFacing side, float hitX,
        float hitY, float hitZ, @Nonnull IBlockState newState) {
      return ((BlockCathedralTympanum) block).placeFromItem(world, pos, player);
    }
  }
}
