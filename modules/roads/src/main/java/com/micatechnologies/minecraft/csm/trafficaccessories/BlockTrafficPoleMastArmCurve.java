package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTrafficPoleIgnored;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
  private final MastArmCurveProfile profile;
  private final PropertyInteger shapeProperty;

  /**
   * Constructs a mast arm curve block.
   *
   * @param registryName the block's registry name
   * @param profile      the size of curve this block places
   */
  public BlockTrafficPoleMastArmCurve(String registryName, MastArmCurveProfile profile) {
    super(stash(registryName, profile), SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0);
    this.registryName = registryName;
    this.profile = profile;
    this.shapeProperty = shapeProperty(profile.getShapeCount());
    PENDING.remove();
  }

  private static Material stash(String registryName, MastArmCurveProfile profile) {
    PENDING.set(new Object[]{registryName, profile});
    return Material.ROCK;
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
   * @return the curve profile
   */
  public MastArmCurveProfile getProfile() {
    return profile;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    MastArmCurveProfile pending =
        profile != null ? profile : (MastArmCurveProfile) PENDING.get()[1];
    return new BlockStateContainer(this, FACING, shapeProperty(pending.getShapeCount()));
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
    return root.offset(facing, offset[0]).up(offset[1]);
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
    return pos.down(offset[1]).offset(facing, -offset[0]);
  }

  private int cellIndexAt(IBlockAccess world, BlockPos pos) {
    TileEntity tileEntity = world.getTileEntity(pos);
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
    for (int i = 1; i < profile.getCellCount(); i++) {
      BlockPos cell = cellPos(pos, facing, i);
      if (!world.getBlockState(cell).getBlock().isReplaceable(world, cell)) {
        refuse(world, pos, placer, cell);
        return;
      }
    }

    setCellIndex(world, pos, 0);
    for (int i = 1; i < profile.getCellCount(); i++) {
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
          String.format("Not enough room for a %dx%d mast arm curve -- blocked at %d, %d, %d",
              profile.getRun(), profile.getRise(), blockedBy.getX(), blockedBy.getY(),
              blockedBy.getZ())), true);
    }
  }

  private void setCellIndex(World world, BlockPos pos, int index) {
    TileEntity tileEntity = world.getTileEntity(pos);
    if (tileEntity instanceof TileEntityMastArmCurve) {
      ((TileEntityMastArmCurve) tileEntity).setCellIndex(index);
      ((TileEntityMastArmCurve) tileEntity).syncServerToClient(world);
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
   */
  @Override
  public @NotNull IBlockState getActualState(@NotNull IBlockState state,
      @NotNull IBlockAccess worldIn,
      @NotNull BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);
    Class<?>[] ignore = AbstractBlockTrafficPole.IGNORE_BLOCK;
    int mask = 0;
    if (AbstractBlockTrafficPole.isMountableAdjacent(worldIn, pos.down(), ignore)) {
      mask |= MastArmCurveProfile.MOUNT_DOWN;
    }
    if (AbstractBlockTrafficPole.isMountableAdjacent(worldIn, pos.up(), ignore)) {
      mask |= MastArmCurveProfile.MOUNT_UP;
    }
    if (AbstractBlockTrafficPole.isMountableAdjacent(worldIn, pos.offset(facing.rotateY()),
        ignore)) {
      mask |= MastArmCurveProfile.MOUNT_EAST;
    }
    if (AbstractBlockTrafficPole.isMountableAdjacent(worldIn, pos.offset(facing.rotateYCCW()),
        ignore)) {
      mask |= MastArmCurveProfile.MOUNT_WEST;
    }
    return state.withProperty(shapeProperty,
        MastArmCurveProfile.shapeIndex(cellIndexAt(worldIn, pos), mask));
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
