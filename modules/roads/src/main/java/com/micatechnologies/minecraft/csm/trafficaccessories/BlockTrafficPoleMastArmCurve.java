package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole;
import com.micatechnologies.minecraft.csm.codeutils.BlockUtils;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.CsmPoleFit;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTrafficPoleIgnored;
import com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.BlockSpanWireHangerMount;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * A realistically scaled curved mast arm upsweep -- the section that carries a signal arm from
 * the pole up to the horizontal run over the roadway.
 *
 * <p>CSM already had {@code trafficpoleverticalcurveconnector} for this, and it is far too small
 * for the job. That was not a choice: its model is a Forge JSON element model, and it is already
 * pinned against that format's hard {@code -16..32} coordinate limit, so it could not be made
 * bigger. These are OBJ, which has no such limit.
 *
 * <p><b>One curve is several blocks.</b> Placing one lays a connected staircase of cells --
 * between four and ten of them, depending on the {@link MastArmCurveProfile} -- each drawing its
 * own slice of one continuous sweep. That costs placement and demolition logic that a single
 * block carrying one huge model would not, and buys three things that version could not have
 * had: the arm does not vanish when the origin block's chunk section is culled, signal heads and
 * signs can mount anywhere along the curve because there is a real block there, and the
 * auto-connecting pole system -- which works on block adjacency -- can eventually see it.
 *
 * <p>Every cell also grows mount hardware -- a band clamped round the tube, a bracket and a face
 * plate -- toward any of its four sides that has something mountable against it, the same way a
 * pole does. On a pole that is one model reused everywhere, because a pole is a straight tube
 * centred in its block; a curve cell's tube is neither centred nor level and its pose differs in
 * every cell, so the hardware is generated per cell and per direction.
 *
 * <p>Break any cell and the whole curve goes, dropping one item. Which cell was hit does not
 * matter: a cell knows its index, and index plus facing locates the root.
 *
 * <p><b>Placement follows the pole family's rule</b>, inherited unchanged from
 * {@link AbstractBlockRotatableNSEW}: the arm reaches away from the face you clicked. Click the
 * side of a pole and the arm sweeps out from it, which is both how every other CSM pole
 * accessory behaves and the orientation the pole-end boot is built for -- the arm is saddle-cut
 * against the pole's own cylinder, so it only lands correctly when the pole really is behind it.
 * An earlier version pointed the arm the way the player was looking
 * instead; that reads well when placing on top of a pole and makes the realistic side mount
 * impossible to aim, so it was dropped.
 */
public class BlockTrafficPoleMastArmCurve extends AbstractBlockRotatableNSEW
    implements ICsmTileEntityProvider, ICsmNoSnowAccumulation, ICsmTrafficPoleIgnored {

  /**
   * Carries the constructor arguments past {@code super()}. {@link #getBlockRegistryName()} and
   * {@link #createBlockState()} are both called from the superclass constructor, before this
   * class's fields exist. Same trick, and same reason, as
   * {@link BlockTrafficAccessoryNSEWUD}.
   */
  private static final ThreadLocal<Object[]> PENDING = new ThreadLocal<>();

  /**
   * One {@link PropertyInteger} per distinct shape count, shared by every block that needs it.
   * The property object's identity has to be the same in {@link #createBlockState()} and in the
   * field below, or the state container and the lookups would disagree.
   */
  private static final Map<Integer, PropertyInteger> SHAPE_PROPERTIES = new HashMap<>();

  /**
   * Set while a curve is tearing itself down, so that removing a sibling cell does not start a
   * fresh demolition of the same curve from that sibling. Thread-local because world edits
   * happen on the server thread and each demolition is one call stack.
   */
  private static final ThreadLocal<Boolean> DEMOLISHING = ThreadLocal.withInitial(() -> false);

  private final String registryName;
  private final ICurvedArmProfile profile;
  @Nullable
  private final MastArmCurveProfile mastArmProfile;
  private final PropertyInteger shapeProperty;

  /**
   * Constructs a mast arm curve block.
   *
   * @param registryName the block's registry name
   * @param profile      the size of curve this block places
   */
  public BlockTrafficPoleMastArmCurve(String registryName, MastArmCurveProfile profile) {
    this(registryName, curvedArm(profile), profile);
  }

  /**
   * Constructs a luminaire arm block: a Caltrans davit arm or a truss arm, single or double
   * (GitHub #99). The same multi-block machinery as the mast arm curves, from
   * gen_luminaire_arms.py's profiles.
   *
   * @param registryName the block's registry name
   * @param profile      the arm this block places
   */
  public BlockTrafficPoleMastArmCurve(String registryName, LuminaireArmProfile profile) {
    this(registryName, profile, null);
  }

  /**
   * Constructs a truss signal mast arm block: a two-chord signal arm, level, sloped or diagonal
   * across a corner (GitHub #99). The same multi-block machinery again, from
   * gen_truss_signal_arms.py's profiles.
   *
   * @param registryName the block's registry name
   * @param profile      the arm this block places
   */
  public BlockTrafficPoleMastArmCurve(String registryName, TrussSignalArmProfile profile) {
    this(registryName, profile, null);
  }

  /**
   * Constructs a New York City signal mast arm block: a tapered pipe under two guy rods, regular
   * or diagonal across a corner (GitHub #99). The same multi-block machinery again, from
   * gen_truss_signal_arms.py's second family of profiles.
   *
   * @param registryName the block's registry name
   * @param profile      the arm this block places
   */
  public BlockTrafficPoleMastArmCurve(String registryName, NycSignalArmProfile profile) {
    this(registryName, profile, null);
  }

  private BlockTrafficPoleMastArmCurve(String registryName, ICurvedArmProfile profile,
      @Nullable MastArmCurveProfile mastArmProfile) {
    super(stash(registryName, profile), SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0);
    this.registryName = registryName;
    this.profile = profile;
    this.mastArmProfile = mastArmProfile;
    this.shapeProperty = shapeProperty(profile.getShapeCount());
    PENDING.remove();
  }

  private static Material stash(String registryName, ICurvedArmProfile profile) {
    PENDING.set(new Object[]{registryName, profile});
    return Material.ROCK;
  }

  /**
   * A mast arm curve profile seen as an {@link ICurvedArmProfile}: its root is its only pole
   * joint, and its shape packs the mount mask as it always has.
   */
  private static ICurvedArmProfile curvedArm(MastArmCurveProfile p) {
    return new ICurvedArmProfile() {
      @Override
      public int getCellCount() {
        return p.getCellCount();
      }

      @Override
      public int[] getCellOffset(int index) {
        return p.getCellOffset(index);
      }

      @Override
      public double[] getCellBox(int index) {
        return p.getCellBox(index);
      }

      @Override
      public int getShapeCount() {
        return p.getShapeCount();
      }

      @Override
      public int poleSide(int cell) {
        return cell == 0 ? 1 : 0;
      }

      @Override
      public int shapeIndex(int cell, int mountMask, CsmPoleFit fit) {
        return p.shapeIndex(cell, mountMask, fit);
      }

      private final float[][][] rails = new float[p.getCellCount()][][];

      @Override
      public float[][] getCellRails(int cell) {
        if (rails[cell] == null) {
          rails[cell] = mastArmRails(p, cell);
        }
        return rails[cell];
      }

      @Override
      public String describe() {
        return String.format("%dx%d mast arm curve", p.getRun(), p.getRise());
      }
    };
  }

  /**
   * A mast arm curve cell's stretch of the tube, as two chords of the generator's parabola
   * ({@code centre} and {@code radius} in gen_mast_arm_curves.py: 8 + 16 rise (2t/run - t^2/run^2),
   * tapering 4.6 to 4.0), in the cell's own frame. Two chords a block keep the line within a
   * fraction of a pixel of the curve, which is all a hanger's clamp needs.
   */
  private static float[][] mastArmRails(MastArmCurveProfile p, int cell) {
    int[] offset = p.getCellOffset(cell);
    float run = p.getRun();
    float[][] out = new float[2][];
    for (int half = 0; half < 2; half++) {
      float t0 = Math.min(run, offset[0] + 0.5F * half);
      float t1 = Math.min(run, t0 + 0.5F);
      float tm = (t0 + t1) / 2.0F;
      out[half] = new float[]{
          8.0F, mastArmY(p, t0) - 16.0F * offset[1], 16.0F - 16.0F * (t0 - offset[0]),
          8.0F, mastArmY(p, t1) - 16.0F * offset[1], 16.0F - 16.0F * (t1 - offset[0]),
          4.6F + (4.0F - 4.6F) * tm / run};
    }
    return out;
  }

  private static float mastArmY(MastArmCurveProfile p, float t) {
    float run = p.getRun();
    return 8.0F + 16.0F * p.getRise() * (2.0F * t / run - t * t / (run * run));
  }

  /**
   * The pipes passing through the cell at {@code pos}, in world space relative to that cell's
   * lower north-west corner: each {@code {x0, y0, z0, x1, y1, z1, radius}} in sixteenths of a
   * block, turned to the arm's facing.
   *
   * @param world the world
   * @param pos   the cell's position
   * @param state the cell's block state
   *
   * @return the segments, or {@code null} if this arm does not describe its pipes
   */
  @Nullable
  public float[][] getWorldRails(IBlockAccess world, BlockPos pos, IBlockState state) {
    float[][] rails = profile.getCellRails(cellIndexAt(world, pos));
    if (rails == null) {
      return null;
    }
    EnumFacing facing = state.getValue(FACING);
    if (facing == EnumFacing.NORTH) {
      return rails;
    }
    float[][] out = new float[rails.length][];
    for (int i = 0; i < rails.length; i++) {
      float[] r = rails[i];
      float[] a = turned(r[0], r[2], facing);
      float[] b = turned(r[3], r[5], facing);
      out[i] = new float[]{a[0], r[1], a[1], b[0], r[4], b[1], r[6]};
    }
    return out;
  }

  /**
   * A point of the unrotated (north) model turned about the block's centre to a facing, the way
   * the blockstate's {@code y} rotation turns the model.
   */
  private static float[] turned(float x, float z, EnumFacing facing) {
    float dx = x - 8.0F;
    float dz = z - 8.0F;
    switch (facing) {
      case EAST:
        return new float[]{8.0F - dz, 8.0F + dx};
      case SOUTH:
        return new float[]{8.0F - dx, 8.0F - dz};
      case WEST:
        return new float[]{8.0F + dz, 8.0F - dx};
      default:
        return new float[]{x, z};
    }
  }

  private static synchronized PropertyInteger shapeProperty(int shapeCount) {
    return SHAPE_PROPERTIES.computeIfAbsent(shapeCount,
        count -> PropertyInteger.create("shape", 0, count - 1));
  }

  @Override
  public String getBlockRegistryName() {
    if (registryName != null) {
      return registryName;
    }
    return (String) PENDING.get()[0];
  }

  /**
   * Gets the size of curve this block places.
   *
   * @return the curve profile, or {@code null} for a luminaire arm
   */
  @Nullable
  public MastArmCurveProfile getProfile() {
    return mastArmProfile;
  }

  /**
   * Gets the cells, hitboxes and shapes of the arm this block places.
   *
   * @return the arm's profile
   */
  public ICurvedArmProfile getArmProfile() {
    return profile;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    ICurvedArmProfile pending =
        profile != null ? profile : (ICurvedArmProfile) PENDING.get()[1];
    return new CsmBlockStateContainer(this, FACING, shapeProperty(pending.getShapeCount()));
  }

  /**
   * The world position of one cell of a curve whose root is at {@code root}.
   *
   * @param root   the curve's root position, at the pole
   * @param facing the direction the arm reaches
   * @param index  the cell index
   *
   * @return the cell's world position
   */
  private BlockPos cellPos(BlockPos root, EnumFacing facing, int index) {
    int[] offset = profile.getCellOffset(index);
    BlockPos pos = root.offset(facing, offset[0]).up(offset[1]);
    return offset.length > 2 ? pos.offset(facing.rotateY(), offset[2]) : pos;
  }

  /**
   * The root position of the curve that the cell at {@code pos} belongs to. The inverse of
   * {@link #cellPos}, which is why no cell needs to store the root: index and facing are enough.
   *
   * @param pos    the cell's world position
   * @param facing the direction the arm reaches
   * @param index  the cell index
   *
   * @return the curve's root position
   */
  private BlockPos rootPos(BlockPos pos, EnumFacing facing, int index) {
    int[] offset = profile.getCellOffset(index);
    BlockPos root = pos.down(offset[1]).offset(facing, -offset[0]);
    return offset.length > 2 ? root.offset(facing.rotateY(), -offset[2]) : root;
  }

  /**
   * Whether cell 0 sits at the position the arm is placed at. It does for every arm but the
   * diagonal truss arms, whose first cell is diagonal to the pole and so one step aside.
   */
  private boolean rootIsPlacedCell() {
    int[] offset = profile.getCellOffset(0);
    return offset[0] == 0 && offset[1] == 0 && (offset.length < 3 || offset[2] == 0);
  }

  private int cellIndexAt(IBlockAccess world, BlockPos pos) {
    TileEntity tileEntity = BlockUtils.getTileEntitySafe(world, pos);
    if (tileEntity instanceof TileEntityMastArmCurve) {
      return MathHelper.clamp(((TileEntityMastArmCurve) tileEntity).getCellIndex(), 0,
          profile.getCellCount() - 1);
    }
    return 0;
  }

  /**
   * Lays the rest of the curve out from the block that was just placed.
   *
   * <p>Either the whole curve fits or none of it is placed. A partly built curve would be worse
   * than nothing: it would draw a tube that stops in mid-air, and breaking it would leave orphan
   * cells whose siblings no longer exist.
   */
  @Override
  public void onBlockPlacedBy(@NotNull World world,
      @NotNull BlockPos pos,
      @NotNull IBlockState state,
      @NotNull EntityLivingBase placer,
      @NotNull ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    if (world.isRemote) {
      return;
    }

    EnumFacing facing = state.getValue(FACING);
    boolean rootHere = rootIsPlacedCell();
    for (int i = rootHere ? 1 : 0; i < profile.getCellCount(); i++) {
      BlockPos cell = cellPos(pos, facing, i);
      if (!world.getBlockState(cell).getBlock().isReplaceable(world, cell)) {
        refuse(world, pos, placer, cell);
        return;
      }
    }

    if (rootHere) {
      setCellIndex(world, pos, 0);
    } else {
      // The block just placed only marked the spot: the arm's first cell is beside it. Remove it
      // without starting a demolition of the arm that is about to be laid from here.
      DEMOLISHING.set(true);
      try {
        world.setBlockToAir(pos);
      } finally {
        DEMOLISHING.set(false);
      }
    }
    for (int i = rootHere ? 1 : 0; i < profile.getCellCount(); i++) {
      BlockPos cell = cellPos(pos, facing, i);
      world.setBlockState(cell, getDefaultState().withProperty(FACING, facing), 3);
      setCellIndex(world, cell, i);
    }
  }

  private void refuse(World world, BlockPos pos, EntityLivingBase placer, BlockPos blockedBy) {
    DEMOLISHING.set(true);
    try {
      world.setBlockToAir(pos);
    } finally {
      DEMOLISHING.set(false);
    }
    if (placer instanceof EntityPlayer) {
      EntityPlayer player = (EntityPlayer) placer;
      if (!player.capabilities.isCreativeMode) {
        // The stack was already spent placing the root, and the root is now gone.
        player.inventory.addItemStackToInventory(new ItemStack(this));
      }
      player.sendStatusMessage(new TextComponentString(
          String.format("Not enough room for a %s -- blocked at %d, %d, %d",
              profile.describe(), blockedBy.getX(), blockedBy.getY(), blockedBy.getZ())), true);
    }
  }

  private void setCellIndex(World world, BlockPos pos, int index) {
    TileEntity tileEntity = world.getTileEntity(pos);
    if (tileEntity instanceof TileEntityMastArmCurve) {
      ((TileEntityMastArmCurve) tileEntity).setCellIndex(index);
      ((TileEntityMastArmCurve) tileEntity).markDirtyQueueSync(world, pos);
    }
  }

  /**
   * Takes the rest of the curve with whichever cell was broken.
   *
   * <p>Only one item drops, from the cell the player actually broke -- the siblings are removed
   * with {@code setBlockToAir}, which does not drop. So a curve costs one item however it is
   * taken apart.
   */
  @Override
  public void breakBlock(@NotNull World world, @NotNull BlockPos pos, @NotNull IBlockState state) {
    if (!world.isRemote && !DEMOLISHING.get()) {
      int index = cellIndexAt(world, pos);
      EnumFacing facing = state.getValue(FACING);
      BlockPos root = rootPos(pos, facing, index);
      DEMOLISHING.set(true);
      try {
        for (int i = 0; i < profile.getCellCount(); i++) {
          if (i == index) {
            continue;
          }
          BlockPos cell = cellPos(root, facing, i);
          if (world.getBlockState(cell).getBlock() == this) {
            world.setBlockToAir(cell);
          }
        }
      } finally {
        DEMOLISHING.set(false);
      }
    }
    super.breakBlock(world, pos, state);
  }

  /**
   * Resolves which cell this block is and which of its four faces have something worth mounting
   * to, and packs both into the single {@code shape} property the blockstate keys off.
   *
   * <p>The four directions are read in MODEL space, because that is the frame the stub models
   * were generated in: the curve is drawn facing north with the arm running toward -Z, so model
   * east is {@code facing.rotateY()} in the world and model west is {@code facing.rotateYCCW()}.
   * Along the arm there is no stub -- that is where the arm itself goes.
   *
   * <p>The mountability test is {@link AbstractBlockTrafficPole}'s own, so a curve ignores exactly
   * what a pole ignores. That list already contains this class, which is what stops one cell of a
   * curve from sprouting hardware into the next cell of the same curve.
   *
   * <p>The root cell also reads the width of the pole behind it. The arm's tube is wider than
   * the thin and pedestal poles, so the saddle-cut boot it wears on the 12-across pole family
   * has nothing to land on there; the generator emits a bolted bracket for those poles instead,
   * and {@link CsmPoleFit#behind} picks it from {@link AbstractBlockTrafficPole#getPoleRadius()}.
   * Anything that is not a pole gets the large fit: the saddle cut against a full block simply
   * disappears inside it. The root packs the fit into its own {@code shape} property rather than
   * carrying {@link CsmPoleFit#PROPERTY}, because its model already depends on the cell and the
   * mount mask and one property is all the blockstate can key a model off.
   */
  @Override
  public @NotNull IBlockState getActualState(@NotNull IBlockState state,
      @NotNull IBlockAccess worldIn,
      @NotNull BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);
    EnumFacing right = facing.rotateY();
    Class<?>[] ignore = AbstractBlockTrafficPole.IGNORE_BLOCK;
    int cell = cellIndexAt(worldIn, pos);
    // Only the sides this cell can grow hardware on are tested: the mast arm curves' four, or
    // whatever a truss arm's cell has free (a side that is another cell of the arm never is).
    int directions = profile.getMountDirections(cell);
    int mask = 0;
    for (int bit = 0; bit < ICurvedArmProfile.MOUNT_DIRECTION_COUNT; bit++) {
      if ((directions & (1 << bit)) == 0) {
        continue;
      }
      int[] to = ICurvedArmProfile.MOUNT_DIRECTION_OFFSETS[bit];
      BlockPos neighbour = pos.offset(facing, to[0]).up(to[1]).offset(right, to[2]);
      if (AbstractBlockTrafficPole.isMountableAdjacent(worldIn, neighbour, ignore)
          || (bit == 0 && isHangingWireMount(worldIn, neighbour))) {
        mask |= 1 << bit;
      }
    }
    // A cell holding a pole joint reads the pole's width: the root, behind it; a double luminaire
    // arm's second root, ahead of it, past the pole; a diagonal truss arm's, diagonally behind.
    int[] poleAt = profile.getPoleOffset(cell);
    CsmPoleFit fit = CsmPoleFit.LARGE;
    if (poleAt != null) {
      Block pole = worldIn.getBlockState(pos.offset(facing, poleAt[0]).offset(right, poleAt[1]))
          .getBlock();
      if (pole instanceof AbstractBlockTrafficPole) {
        fit = CsmPoleFit.forPoleRadius(((AbstractBlockTrafficPole) pole).getPoleRadius());
      }
    }
    return state.withProperty(shapeProperty, profile.shapeIndex(cell, mask, fit));
  }

  /**
   * Whether the block under a cell is a wire mount used on its own: the bracket a signal head
   * hangs from, placed under an arm instead of on a span. The arm then grows its hardware down
   * to it, which is how a head is hung from the tip of a truss arm.
   *
   * <p>Poles ignore every accessory of this family, wire mounts included, so the ordinary test
   * says no. A mount that is linked into a span is left alone: its bracket is hidden and the
   * span's own hardware carries it.
   */
  private static boolean isHangingWireMount(IBlockAccess world, BlockPos pos) {
    IBlockState below = world.getBlockState(pos);
    if (!(below.getBlock() instanceof BlockSpanWireHangerMount)
        || !below.getPropertyKeys().contains(BlockSpanWireHangerMount.LINKED)) {
      return false;
    }
    return !below.getActualState(world, pos).getValue(BlockSpanWireHangerMount.LINKED);
  }

  /**
   * The tube's measured extent inside this particular cell, for the unrotated orientation --
   * {@link AbstractBlockRotatableNSEW#getBoundingBox} applies the facing rotation itself, so
   * rotating here as well would turn it twice.
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    double[] box = profile.getCellBox(cellIndexAt(source, pos));
    return new AxisAlignedBB(box[0], box[1], box[2], box[3], box[4], box[5]);
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

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityMastArmCurve.class;
  }

  @Override
  public String getTileEntityName() {
    return "mast_arm_curve";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@NotNull World worldIn, int meta) {
    return new TileEntityMastArmCurve();
  }
}
