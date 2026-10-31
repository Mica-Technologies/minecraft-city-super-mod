package com.micatechnologies.minecraft.csm.venues.golf;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import java.util.List;
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
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A low rail along any of a block's four sides, standing on the surface in the block above it:
 * a mini-golf hole's timber border ({@code minigolf_border}) and a golf cart path's concrete curb
 * ({@code golf_cart_path_curb}). Metadata is the sides that have a rail, one bit each (north 1,
 * east 2, south 4, west 8), so a straight edge, a corner, a dead end or a narrow channel is one
 * block; rails on neighbouring blocks meet end to end and a corner's two rails overlap into a
 * post. Placed by hand it takes the side the placer faces; a click with an empty hand on a side
 * toggles that side's rail.
 *
 * @since 2026.10
 */
public class BlockEdgeRail extends AbstractBlock {

  public static final PropertyBool NORTH = PropertyBool.create("north");
  public static final PropertyBool EAST = PropertyBool.create("east");
  public static final PropertyBool SOUTH = PropertyBool.create("south");
  public static final PropertyBool WEST = PropertyBool.create("west");
  private static final PropertyBool[] SIDES = {NORTH, EAST, SOUTH, WEST};
  private static final EnumFacing[] FACES = {EnumFacing.NORTH, EnumFacing.EAST,
      EnumFacing.SOUTH, EnumFacing.WEST};

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();
  private final String registryName;
  private final AxisAlignedBB[] rails;
  private final double height;

  /**
   * Constructs a rail.
   *
   * @param registryName its registry name
   * @param material     its material
   * @param sound        its sound
   * @param thickPx      a rail's thickness, pixels
   * @param heightPx     its height, pixels
   */
  public BlockEdgeRail(String registryName, Material material, SoundType sound, double thickPx,
      double heightPx) {
    super(stash(registryName, material), sound, material == Material.WOOD ? "axe" : "pickaxe", 0,
        1.0F, 2.0F, 0.0F, 0);
    this.registryName = registryName;
    double t = thickPx / 16.0;
    double h = heightPx / 16.0;
    this.height = h;
    this.rails = new AxisAlignedBB[]{
        new AxisAlignedBB(0, 0, 0, 1, h, t), new AxisAlignedBB(1 - t, 0, 0, 1, h, 1),
        new AxisAlignedBB(0, 0, 1 - t, 1, h, 1), new AxisAlignedBB(0, 0, 0, t, h, 1)};
    PENDING.remove();
    setDefaultState(blockState.getBaseState().withProperty(NORTH, true).withProperty(EAST, false)
        .withProperty(SOUTH, false).withProperty(WEST, false));
  }

  private static Material stash(String registryName, Material material) {
    PENDING.set(registryName);
    return material;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, NORTH, EAST, SOUTH, WEST);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    IBlockState s = getDefaultState();
    for (int i = 0; i < 4; i++) {
      s = s.withProperty(SIDES[i], (meta >> i & 1) != 0);
    }
    return s;
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    int meta = 0;
    for (int i = 0; i < 4; i++) {
      if (state.getValue(SIDES[i])) {
        meta |= 1 << i;
      }
    }
    return meta;
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 1;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing f = placer.getHorizontalFacing();
    IBlockState s = getStateFromMeta(0);
    for (int i = 0; i < 4; i++) {
      if (FACES[i] == f) {
        s = s.withProperty(SIDES[i], true);
      }
    }
    return s;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    EnumFacing f = side.getAxis() == EnumFacing.Axis.Y ? nearest(hitX, hitZ) : side;
    if (!world.isRemote) {
      for (int i = 0; i < 4; i++) {
        if (FACES[i] == f) {
          IBlockState n = state.cycleProperty(SIDES[i]);
          if (getMetaFromState(n) != 0) {
            world.setBlockState(pos, n, 3);
          }
        }
      }
    }
    return true;
  }

  /** The side of the block nearest a point on its top. */
  private static EnumFacing nearest(float x, float z) {
    float n = z;
    float s = 1 - z;
    float w = x;
    float e = 1 - x;
    float m = Math.min(Math.min(n, s), Math.min(w, e));
    return m == n ? EnumFacing.NORTH : m == s ? EnumFacing.SOUTH : m == w ? EnumFacing.WEST
        : EnumFacing.EAST;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    AxisAlignedBB u = null;
    for (int i = 0; i < 4; i++) {
      if (state.getValue(SIDES[i])) {
        u = u == null ? rails[i] : u.union(rails[i]);
      }
    }
    return u != null ? u : new AxisAlignedBB(0, 0, 0, 1, height, 1);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean actual) {
    for (int i = 0; i < 4; i++) {
      if (state.getValue(SIDES[i])) {
        addCollisionBoxToList(pos, entityBox, collidingBoxes, rails[i]);
      }
    }
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
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess world, @Nonnull IBlockState state,
      @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
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
