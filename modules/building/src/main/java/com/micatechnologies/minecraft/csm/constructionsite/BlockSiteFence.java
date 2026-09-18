package com.micatechnologies.minecraft.csm.constructionsite;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A fence that joins its neighbours the way a vanilla fence does, with a post at its centre and a
 * panel out to each side it connects on: the temporary site fence (plain, and with a privacy
 * screen), the silt fence, the permanent chain-link fence in two finishes, and the barbed-wire top
 * that goes on a chain-link fence.
 *
 * <p>They differ in their geometry, which lives in {@code dev-env-utils/scripts/gen_fencing.py},
 * in how tall they stand and what they join, so there is one class, constructed by registry name
 * like the other site blocks, and the name picks a {@link Kind}. The kind is handed across on the
 * thread, as {@link BlockSiteProp} does, because {@link AbstractBlock}'s constructor asks for the
 * registry name before this class's fields are set.</p>
 *
 * <h3>State</h3>
 *
 * <p>Nothing is stored. The four sides, whether the same family continues above and below, and
 * whether the post is a terminal -- an end, a corner or a junction, rather than a post in a
 * straight run -- are all actual state. Only a chain-link fence draws anything from the last
 * three: it stacks, so a tall fence is several blocks with one continuous post and mesh, the top
 * rail and post cap only on the top course, the tension wire only on the bottom one, and the
 * heavier terminal post wherever a run ends or turns.</p>
 *
 * <p>Fences join their own family -- both temporary fences, both chain-link finishes -- and the
 * site and chain-link fences also butt up against a solid face, as a vanilla fence does.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockSiteFence extends AbstractBlock {

  public static final PropertyBool NORTH = PropertyBool.create("north");
  public static final PropertyBool EAST = PropertyBool.create("east");
  public static final PropertyBool SOUTH = PropertyBool.create("south");
  public static final PropertyBool WEST = PropertyBool.create("west");
  /** The same family continues above or below. */
  public static final PropertyBool UP = PropertyBool.create("up");
  public static final PropertyBool DOWN = PropertyBool.create("down");
  /** The post ends, turns or branches a run, rather than standing in a straight one. */
  public static final PropertyBool TERMINAL = PropertyBool.create("terminal");

  /**
   * Steel that comes down by hand, for the same reason as the scaffold's: {@link Material#IRON}
   * drops nothing without a pickaxe.
   */
  private static final Material FENCE_STEEL = new Material(MapColor.IRON);

  /** Half the width of a panel's collision, and of its post's. */
  private static final double ARM = 1.0 / 16.0;
  private static final double POST = 1.5 / 16.0;

  private static final ThreadLocal<Kind> PENDING_KIND = new ThreadLocal<>();

  private final Kind kind;

  /**
   * The fences, in creative order.
   *
   * @since 1.0
   */
  public enum Kind {
    /** The panel fence around a building site, on concrete feet. */
    TEMPORARY("temp_fence", "temporary", true, true, 1.5, 28),
    /** The same, with green privacy screen on the mesh. */
    TEMPORARY_SCREENED("temp_fence_screened", "temporary", true, true, 1.5, 28),
    /** Black geotextile on wooden stakes, low along a site's edge. */
    SILT("silt_fence", "silt", false, false, 0.75, 14),
    /** Permanent galvanized chain-link, one block tall, stacking. */
    CHAIN_LINK("chainlink_fence", "chainlink", true, true, 1.5, 16),
    /** Permanent chain-link, black vinyl-coated. */
    CHAIN_LINK_BLACK("chainlink_fence_black", "chainlink", true, true, 1.5, 16),
    /** Barbed-wire arms and strands, on top of a chain-link fence. */
    BARBED_TOP("chainlink_barbed_top", "barbed", true, false, 1.0, 10);

    private final String registryName;
    private final String family;
    private final boolean steel;
    private final boolean joinsWalls;
    private final double collisionHeight;
    private final double selectionHeight;

    Kind(String registryName, String family, boolean steel, boolean joinsWalls,
        double collisionHeight, int selectionHeight16) {
      this.registryName = registryName;
      this.family = family;
      this.steel = steel;
      this.joinsWalls = joinsWalls;
      this.collisionHeight = collisionHeight;
      this.selectionHeight = selectionHeight16 / 16.0;
    }

    /**
     * The registry name.
     *
     * @return the registry name
     *
     * @since 1.0
     */
    public String getRegistryName() {
      return registryName;
    }

    /**
     * The kind with {@code registryName}.
     *
     * @param registryName a registry name
     *
     * @return the kind
     *
     * @throws IllegalArgumentException if no fence has that name
     * @since 1.0
     */
    public static Kind byRegistryName(String registryName) {
      for (Kind k : values()) {
        if (k.registryName.equals(registryName)) {
          return k;
        }
      }
      throw new IllegalArgumentException("No site fence named " + registryName);
    }
  }

  /**
   * Constructs a {@link BlockSiteFence}.
   *
   * @param registryName which fence, by the registry name of its {@link Kind}
   *
   * @since 1.0
   */
  public BlockSiteFence(String registryName) {
    this(Kind.byRegistryName(registryName));
  }

  private BlockSiteFence(Kind kind) {
    super(pendingMaterial(kind), kind.steel ? SoundType.METAL : SoundType.WOOD,
        kind.steel ? "pickaxe" : "axe", 0, kind.steel ? 1.5F : 0.8F, 6F, 0F, 0);
    this.kind = kind;
    PENDING_KIND.remove();
  }

  private static Material pendingMaterial(Kind kind) {
    PENDING_KIND.set(kind);
    return kind.steel ? FENCE_STEEL : Material.WOOD;
  }

  private Kind kind() {
    return kind != null ? kind : PENDING_KIND.get();
  }

  @Override
  public String getBlockRegistryName() {
    return kind().registryName;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, NORTH, EAST, SOUTH, WEST, UP, DOWN, TERMINAL);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState();
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return 0;
  }

  private boolean sameFamily(IBlockState other) {
    return other.getBlock() instanceof BlockSiteFence
        && ((BlockSiteFence) other.getBlock()).kind().family.equals(kind().family);
  }

  private boolean connects(IBlockAccess world, BlockPos pos, EnumFacing side) {
    BlockPos at = pos.offset(side);
    IBlockState other = world.getBlockState(at);
    if (sameFamily(other)) {
      return true;
    }
    return kind().joinsWalls
        && other.getBlockFaceShape(world, at, side.getOpposite()) == BlockFaceShape.SOLID;
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    boolean n = connects(worldIn, pos, EnumFacing.NORTH);
    boolean e = connects(worldIn, pos, EnumFacing.EAST);
    boolean s = connects(worldIn, pos, EnumFacing.SOUTH);
    boolean w = connects(worldIn, pos, EnumFacing.WEST);
    boolean straight = (n && s && !e && !w) || (e && w && !n && !s);
    return state.withProperty(NORTH, n).withProperty(EAST, e).withProperty(SOUTH, s)
        .withProperty(WEST, w)
        .withProperty(UP, sameFamily(worldIn.getBlockState(pos.up())))
        .withProperty(DOWN, sameFamily(worldIn.getBlockState(pos.down())))
        .withProperty(TERMINAL, !straight);
  }

  /**
   * The post and a panel out to each connected side, {@code height} tall.
   */
  private static AxisAlignedBB[] boxes(IBlockState actual, double height, double arm) {
    double lo = 0.5 - arm;
    double hi = 0.5 + arm;
    return new AxisAlignedBB[]{
        new AxisAlignedBB(0.5 - POST, 0, 0.5 - POST, 0.5 + POST, height, 0.5 + POST),
        actual.getValue(NORTH) ? new AxisAlignedBB(lo, 0, 0, hi, height, 0.5) : null,
        actual.getValue(SOUTH) ? new AxisAlignedBB(lo, 0, 0.5, hi, height, 1) : null,
        actual.getValue(WEST) ? new AxisAlignedBB(0, 0, lo, 0.5, height, hi) : null,
        actual.getValue(EAST) ? new AxisAlignedBB(0.5, 0, lo, 1, height, hi) : null};
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, @Nonnull World worldIn,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn,
      boolean isActualState) {
    IBlockState actual = isActualState ? state : state.getActualState(worldIn, pos);
    for (AxisAlignedBB box : boxes(actual, kind().collisionHeight, ARM)) {
      if (box != null) {
        addCollisionBoxToList(pos, entityBox, collidingBoxes, box);
      }
    }
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    IBlockState actual = state.getActualState(source, pos);
    AxisAlignedBB union = null;
    for (AxisAlignedBB box : boxes(actual, kind().selectionHeight, POST)) {
      if (box != null) {
        union = union == null ? box : union.union(box);
      }
    }
    return union;
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
