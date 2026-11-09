package com.micatechnologies.minecraft.csm.lighting;

import com.micatechnologies.minecraft.csm.codeutils.ICsmPostTopFixture;
import com.micatechnologies.minecraft.csm.codeutils.light.CsmLightupAir;
import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A floodlight (GitHub #99): a bright light, with the 4-state STATE and redstone control every
 * {@link AbstractBrightLight} has, whose light goes where it is aimed rather than straight down.
 *
 * <p><b>Placeable on any face.</b> Metadata holds the facing (4) and STATE (4) and nothing more,
 * so the six ways a flood can be mounted are separate blocks, as vanilla's standing and wall signs
 * are: the visible block is the one on a wall, a pole or an arm end ({@link Surface#WALL}); placed
 * on top of a block it puts its {@code _g} companion down instead (a ground uplight, or on a
 * slipfitter a pole top), under one its {@code _c} (hung from a ceiling), and against the front of
 * a RealGrid crossarm its {@code _x} (on a bracket back to the arm, {@link RealGridCompat}). A
 * companion is registered hidden, carries the visible block's name, drops and is picked as the
 * visible block, and behaves the same in every other way.
 *
 * <p><b>The light.</b> Lit, a flood casts a beam along its aim ({@link Surface#pitchDegrees},
 * turned with the facing) and Core places light-up air where it lands
 * ({@link CsmLightupAir#projectBeam}): only when its state changes, healed by the bright lights'
 * random tick and {@code /csmlighting relight}, never on a tick of its own. The beam keeps
 * {@link #BEAM_COUNT} lights {@link #BEAM_SPACING} blocks apart, from the far end back
 * (LIGHTING_SYSTEM.md, "Floodlights", has the measurement that chose them).
 *
 * @since 2026.11
 */
public class BlockFloodlight extends AbstractBrightLight {

  /** Light-up air a beam keeps lit: the lit spot and the row behind it along the beam. */
  public static final int BEAM_COUNT = 3;

  /** Blocks between a beam's light-up air, along the beam. */
  public static final double BEAM_SPACING = 4.0;

  /** Where a flood is mounted, and so how its block is aimed. */
  public enum Surface {
    /** On a wall, a pole's side or an arm's end: aimed out from it and 35 degrees down. */
    WALL("", 35),
    /** On top of a block, knuckle or yoke: a ground uplight aimed 50 degrees up. */
    GROUND("_g", -50),
    /** On a pole-top tenon, on its slipfitter: aimed out and 35 degrees down. */
    POLE_TOP("_g", 35),
    /** Hung from the underside of a block: aimed 50 degrees down. */
    CEILING("_c", 50),
    /** On the front of a RealGrid crossarm, on a bracket: as on a wall. */
    CROSSARM("_x", 35);

    private final String suffix;
    private final double pitchDegrees;

    Surface(String suffix, double pitchDegrees) {
      this.suffix = suffix;
      this.pitchDegrees = pitchDegrees;
    }

    /** The registry name suffix of a flood's block for this surface. */
    public String suffix() {
      return suffix;
    }

    /** The aim below level, in degrees; negative is above. */
    public double pitchDegrees() {
      return pitchDegrees;
    }
  }

  /**
   * The registry name on its way to the superclass constructor, which asks for it before this
   * class's fields are set ({@link BlockBrightLightFactory} does the same).
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final String mainName;
  private final Surface surface;
  private final AxisAlignedBB boundingBox;
  private final int beamRange;

  /**
   * A flood's block.
   *
   * @param registryName this block's registry name: the visible block's, plus the surface's
   *                     suffix for a companion
   * @param surface      where this block is mounted
   * @param box          its bounding box, facing north
   * @param beamRange    how far its beam reaches, in blocks
   */
  public BlockFloodlight(String registryName, Surface surface, AxisAlignedBB box, int beamRange) {
    this(pending(registryName), surface, box, beamRange, true);
  }

  private BlockFloodlight(String registryName, Surface surface, AxisAlignedBB box, int beamRange,
      boolean ignored) {
    if (!registryName.endsWith(surface.suffix())) {
      throw new IllegalArgumentException(registryName + " is not a " + surface + " block");
    }
    this.registryName = registryName;
    this.mainName = registryName.substring(0, registryName.length() - surface.suffix().length());
    this.surface = surface;
    this.boundingBox = box;
    this.beamRange = beamRange;
  }

  private static String pending(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return name;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return boundingBox;
  }

  @Override
  public int getBrightLightXOffset() {
    return 0;
  }

  /** Where this block is mounted. */
  public Surface getSurface() {
    return surface;
  }

  /** How far this flood's beam reaches, in blocks. */
  public int getBeamRange() {
    return beamRange;
  }

  /**
   * The aim of a flood mounted on {@code surface} facing {@code facing}: out along the facing,
   * tilted by the surface's pitch.
   *
   * @return {@code {dx, dy, dz}}, a unit vector
   */
  public static double[] aim(Surface surface, EnumFacing facing) {
    double pitch = Math.toRadians(surface.pitchDegrees());
    double h = Math.cos(pitch);
    return new double[]{facing.getXOffset() * h, -Math.sin(pitch), facing.getZOffset() * h};
  }

  private double[] aimOf(IBlockAccess world, BlockPos pos, @Nullable IBlockState state) {
    IBlockState s = state != null ? state : world.getBlockState(pos);
    EnumFacing facing = s.getBlock() == this ? s.getValue(FACING) : EnumFacing.NORTH;
    return aim(surface, facing);
  }

  @Override
  protected boolean handleAirLightBlock(boolean on, World world, BlockPos pos,
      @Nullable IBlockState state) {
    double[] a = aimOf(world, pos, state);
    return CsmLightupAir.projectBeam(on, world, pos, a[0], a[1], a[2], beamRange, BEAM_COUNT,
        BEAM_SPACING) > 0 && on;
  }

  @Override
  public boolean claimsLightupAir(World world, BlockPos pos, IBlockState state, BlockPos air) {
    if (!isLit(state)) {
      return false;
    }
    double[] a = aimOf(world, pos, state);
    List<BlockPos> path = CsmLightupAir.beamPath(world, pos, a[0], a[1], a[2], beamRange);
    return path != null && CsmLightupAir.beamLights(world, path, pos, a[1], BEAM_COUNT,
        BEAM_SPACING).contains(air);
  }

  // ------------------------------------------------------------------ the companions

  /** The block for {@code surface} of the flood this block belongs to, if it has one. */
  @Nullable
  private BlockFloodlight sibling(Surface s) {
    Block b = Block.getBlockFromName("csm:" + mainName + s.suffix());
    return b instanceof BlockFloodlight ? (BlockFloodlight) b : null;
  }

  /**
   * Placed from the visible block's item: the block for the face clicked. A side face is a wall
   * (or, against the front of a RealGrid crossarm, the crossarm bracket), aimed away from it; the
   * top of a block the ground companion and the underside the ceiling one, both aimed the way the
   * placer looks.
   */
  @Nonnull
  @Override
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side, float hitX,
      float hitY, float hitZ, int meta, EntityLivingBase placer) {
    BlockFloodlight target = this;
    EnumFacing facing;
    if (side.getAxis().isHorizontal()) {
      facing = side;
      if (RealGridCompat.isCrossarmFront(world.getBlockState(pos.offset(side.getOpposite())),
          side)) {
        BlockFloodlight x = sibling(Surface.CROSSARM);
        target = x != null ? x : this;
      }
    } else {
      facing = placer.getHorizontalFacing();
      BlockFloodlight other = sibling(side == EnumFacing.UP ? Surface.GROUND : Surface.CEILING);
      if (other == null && side == EnumFacing.UP) {
        other = sibling(Surface.POLE_TOP);
      }
      target = other != null ? other : this;
    }
    return target.getDefaultState().withProperty(FACING, facing);
  }

  /** The visible block, whose item every companion drops and is picked as. */
  private Block mainBlock() {
    if (surface == Surface.WALL) {
      return this;
    }
    Block b = Block.getBlockFromName("csm:" + mainName);
    return b != null ? b : this;
  }

  @Nonnull
  @Override
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return Item.getItemFromBlock(mainBlock());
  }

  @Nonnull
  @Override
  public ItemStack getPickBlock(@Nonnull IBlockState state, RayTraceResult target,
      @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player) {
    return new ItemStack(mainBlock());
  }

  /**
   * A flood on a pole-top tenon, on its slipfitter: a concrete pole under it shows its tenon.
   *
   * @see ICsmPostTopFixture
   */
  public static class PostTop extends BlockFloodlight implements ICsmPostTopFixture {

    public PostTop(String registryName, AxisAlignedBB box, int beamRange) {
      super(registryName, Surface.POLE_TOP, box, beamRange);
    }
  }
}
