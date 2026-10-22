package com.micatechnologies.minecraft.csm.lifesafety.fireprotection;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmPipe;
import com.micatechnologies.minecraft.csm.lifesafety.AbstractBlockFireSprinkler;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A sprinkler head that joins an exposed pipe (#304): a <b>pendant</b> hung under the pipe in
 * the cell above it, or an <b>upright</b> standing on the pipe in the cell below it, its nipple
 * reaching to the pipe's underside or top whatever pipe it is (any {@link ICsmPipe}: HVAC's
 * exposed pipe, a free-standing standpipe pipe, a {@link BlockSprinklerDrop}). With no pipe there
 * a pendant sits flush against the ceiling on an escutcheon, an upright on its flange.
 *
 * <p>It is a sprinkler like any other ({@link AbstractBlockFireSprinkler}): it finds fire, floods
 * the cell under it and the fire, and drains on the panel's reset. Stored: the facing the
 * detector base keeps (not drawn: a head always hangs or stands upright). Actual state:
 * {@code reach}, how far the nipple reaches into the pipe's cell, 0 to 8 sixteenths, or 9 for
 * none.</p>
 *
 * @since 2026.10
 */
public class BlockSprinklerHead extends AbstractBlockFireSprinkler {

  public static final PropertyInteger REACH = PropertyInteger.create("reach", 0, 9);
  public static final int NONE = 9;

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();
  private static final AxisAlignedBB PENDANT = new AxisAlignedBB(5.5 / 16, 9.5 / 16, 5.5 / 16,
      10.5 / 16, 1, 10.5 / 16);
  private static final AxisAlignedBB UPRIGHT = new AxisAlignedBB(5.5 / 16, 0, 5.5 / 16,
      10.5 / 16, 6.5 / 16, 10.5 / 16);

  private final String registryName;
  private final boolean upright;

  public BlockSprinklerHead(String registryName, boolean upright) {
    this(stash(registryName), registryName, upright);
  }

  private BlockSprinklerHead(Void ignored, String registryName, boolean upright) {
    super();
    this.registryName = registryName;
    this.upright = upright;
    PENDING.remove();
    setDefaultState(getDefaultState().withProperty(REACH, NONE));
  }

  private static Void stash(String registryName) {
    PENDING.set(registryName);
    return null;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Nonnull
  @Override
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, REACH);
  }

  /** How far the nipple reaches into the pipe's cell, or {@link #NONE}. */
  private int reach(IBlockAccess world, BlockPos pos) {
    BlockPos at = upright ? pos.down() : pos.up();
    IBlockState other = world.getBlockState(at);
    if (!(other.getBlock() instanceof ICsmPipe)) {
      return NONE;
    }
    ICsmPipe pipe = (ICsmPipe) other.getBlock();
    if (upright) {
      int top = pipe.pipeTop(world, at, other);
      return top >= 8 && top <= 16 ? 16 - top : NONE;
    }
    int bottom = pipe.pipeBottom(world, at, other);
    return bottom >= 0 && bottom <= 8 ? bottom : NONE;
  }

  @Nonnull
  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos) {
    return state.withProperty(REACH, reach(world, pos));
  }

  /** The head's own box, never turned by the detector's facing. */
  @Override
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return upright ? UPRIGHT : PENDANT;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return upright ? UPRIGHT : PENDANT;
  }
}
