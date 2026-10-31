package com.micatechnologies.minecraft.csm.venues.skate;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A skate park element shaped in one block, its curve or slope drawn true (OBJ) and its
 * collision a staircase of boxes under it: a quarter pipe (a small one, or one of the three
 * blocks of a large one), a bowl corner, a kicker or bank. Steps are at most 4 px, so a player
 * walks up what a skater would ride; nothing is slippery or moves.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3; the way the riding surface
 * faces), bits 2-3 the piece where the block has more than one, stepped by a sneak-click with an
 * empty hand. The boxes per piece are {@code gen_venues_skate.py}'s, written into the tab
 * line.</p>
 *
 * @since 2026.10
 */
public class BlockSkateElement extends BlockVenue {

  private static final ThreadLocal<Integer> PENDING = new ThreadLocal<>();

  private PropertyInteger piece;
  private final AxisAlignedBB[][] boxes;
  private final AxisAlignedBB[] outline;

  /**
   * Constructs an element.
   *
   * @param registryName its registry name
   * @param boxes        per piece, its boxes in pixels, model frame: {x0, y0, z0, x1, y1, z1}
   *                     sextuples
   */
  public BlockSkateElement(String registryName, int[][] boxes) {
    super(stash(registryName, boxes.length), Material.ROCK, SoundType.STONE, 2.0F);
    PENDING.remove();
    this.boxes = new AxisAlignedBB[boxes.length][];
    this.outline = new AxisAlignedBB[boxes.length];
    for (int p = 0; p < boxes.length; p++) {
      int n = boxes[p].length / 6;
      this.boxes[p] = new AxisAlignedBB[n];
      AxisAlignedBB u = null;
      for (int i = 0; i < n; i++) {
        int o = i * 6;
        AxisAlignedBB b = new AxisAlignedBB(boxes[p][o] / 16.0, boxes[p][o + 1] / 16.0,
            boxes[p][o + 2] / 16.0, boxes[p][o + 3] / 16.0, boxes[p][o + 4] / 16.0,
            boxes[p][o + 5] / 16.0);
        this.boxes[p][i] = b;
        u = u == null ? b : u.union(b);
      }
      this.outline[p] = u != null ? u : FULL_BLOCK_AABB;
    }
    IBlockState s = blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH);
    if (piece != null) {
      s = s.withProperty(piece, 0);
    }
    setDefaultState(s);
  }

  private static String stash(String registryName, int pieces) {
    PENDING.set(pieces);
    return registryName;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    Integer n = PENDING.get();
    if (n != null && n > 1) {
      piece = PropertyInteger.create("piece", 0, n - 1);
      return new CsmBlockStateContainer(this, FACING, piece);
    }
    return new CsmBlockStateContainer(this, FACING);
  }

  private int pieceOf(IBlockState state) {
    return piece == null || state.getBlock() != this ? 0 : state.getValue(piece);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    IBlockState s = getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
    if (piece != null) {
      s = s.withProperty(piece, Math.min(boxes.length - 1, (meta >> 2) & 3));
    }
    return s;
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    int meta = state.getValue(FACING).getHorizontalIndex();
    return piece == null ? meta : meta | state.getValue(piece) << 2;
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return outline[pieceOf(state)];
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean actual) {
    EnumFacing f = state.getValue(FACING);
    for (AxisAlignedBB b : boxes[pieceOf(state)]) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes,
          RotationUtils.rotateBoundingBoxByFacing(b, f));
    }
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess world, @Nonnull IBlockState state,
      @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
    return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (piece == null || !player.isSneaking() || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      int p = (state.getValue(piece) + 1) % boxes.length;
      world.setBlockState(pos, state.withProperty(piece, p), 3);
      player.sendStatusMessage(new TextComponentTranslation(
          "csm.venues.skate." + getBlockRegistryName() + "." + p), true);
    }
    return true;
  }
}
