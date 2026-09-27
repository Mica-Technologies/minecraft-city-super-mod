package com.micatechnologies.minecraft.csm.lighting;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmPostTopFixture;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * An aviation obstruction beacon for the top of a tall building, a tower or a mast: the red
 * flashing beacon (FAA L-864 style) and the white strobe (L-865 style).
 *
 * <p>The flash is drawn by {@link TileEntityObstructionBeaconRenderer}: the lens at full
 * brightness and a glow around it, only while the flash is on, timed from the wall clock so every
 * beacon in the world flashes in step. It was first the lens texture's animation, which a baked
 * model can only draw at the light around it: the red flash read as a slightly lighter lens and the
 * white strobe's single frame was not seen at all. The tile entity holds nothing and never ticks.
 * A lit beacon gives steady block light. The blockstate swaps the lens for its dark texture when
 * unlit.
 *
 * <p>A beacon is lit unless redstone powers it. A real obstruction light burns all the time (the
 * red one by night, from its photocell), so it is placed lit, and a signal is an off switch: a
 * daylight sensor beside a red beacon keeps it dark by day, as its photocell would. That rule
 * reads the power as it is, so the beacon keeps no memory of it and its metadata holds only the
 * mount and {@link #LIT}.
 *
 * <p>It stands on the block below ({@link Mount#FLOOR}: a roof, a parapet, a pole top, which is
 * why it is an {@link ICsmPostTopFixture}), or, placed against the side of a block, on a bracket
 * off that face. Its models come from {@code dev-env-utils/scripts/gen_obstruction_beacons.py}.
 *
 * @since 2026.9
 */
public class BlockObstructionBeacon extends AbstractBlock
    implements ICsmPostTopFixture, ICsmTileEntityProvider {

  /** Where the beacon is fixed: on the block below, or on a bracket off a wall facing that way. */
  public enum Mount implements IStringSerializable {
    FLOOR, NORTH, EAST, SOUTH, WEST;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase(java.util.Locale.ROOT);
    }
  }

  /** How the beacon is mounted. */
  public static final PropertyEnum<Mount> MOUNT = PropertyEnum.create("mount", Mount.class);
  /** Whether the beacon is flashing. */
  public static final PropertyBool LIT = PropertyBool.create("lit");

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB floorBox;
  private final AxisAlignedBB[] wallBoxes;
  private final int light;

  /**
   * Constructs an obstruction beacon.
   *
   * @param registryName its registry name
   * @param floorBox     its box standing on a block: x0, y0, z0, x1, y1, z1 in sixteenths
   * @param wallBox      its box on a wall bracket, facing north with the wall at z 16
   * @param light        the light it gives while lit, 0 to 15
   */
  public BlockObstructionBeacon(String registryName, double[] floorBox, double[] wallBox,
      int light) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 2.0F, 6.0F, 0.0F, 0);
    PENDING.remove();
    this.registryName = registryName;
    this.floorBox = box(floorBox);
    // north, east, south, west: the wall behind at +z, -x, -z, +x
    this.wallBoxes = new AxisAlignedBB[4];
    double[] b = wallBox;
    wallBoxes[0] = box(b);
    wallBoxes[1] = box(new double[]{16 - b[5], b[1], b[0], 16 - b[2], b[4], b[3]});
    wallBoxes[2] = box(new double[]{16 - b[3], b[1], 16 - b[5], 16 - b[0], b[4], 16 - b[2]});
    wallBoxes[3] = box(new double[]{b[2], b[1], 16 - b[3], b[5], b[4], 16 - b[0]});
    this.light = light;
    setDefaultState(blockState.getBaseState().withProperty(MOUNT, Mount.FLOOR)
        .withProperty(LIT, true));
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  private static AxisAlignedBB box(double[] b) {
    return new AxisAlignedBB(b[0] / 16.0, b[1] / 16.0, b[2] / 16.0, b[3] / 16.0, b[4] / 16.0,
        b[5] / 16.0);
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  /** Whether this is the white strobe (L-865) rather than the red flashing beacon (L-864). */
  public boolean isStrobe() {
    return getBlockRegistryName().endsWith("_white");
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityObstructionBeacon.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityobstructionbeacon";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityObstructionBeacon();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, MOUNT, LIT);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    Mount[] mounts = Mount.values();
    return getDefaultState().withProperty(MOUNT, mounts[Math.min(meta & 7, mounts.length - 1)])
        .withProperty(LIT, (meta & 8) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(MOUNT).ordinal() | (state.getValue(LIT) ? 8 : 0);
  }

  /** Against the side of a block it hangs on a bracket off that face; otherwise it stands. */
  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    Mount mount;
    switch (facing) {
      case NORTH:
        mount = Mount.NORTH;
        break;
      case EAST:
        mount = Mount.EAST;
        break;
      case SOUTH:
        mount = Mount.SOUTH;
        break;
      case WEST:
        mount = Mount.WEST;
        break;
      default:
        mount = Mount.FLOOR;
    }
    return getDefaultState().withProperty(MOUNT, mount)
        .withProperty(LIT, !world.isBlockPowered(pos));
  }

  @Override
  public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
    super.onBlockAdded(world, pos, state);
    follow(world, pos, state);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, block, fromPos);
    follow(world, pos, state);
  }

  /** Lit unless powered. */
  private void follow(World world, BlockPos pos, IBlockState state) {
    if (world.isRemote) {
      return;
    }
    boolean lit = !world.isBlockPowered(pos);
    if (state.getValue(LIT) != lit) {
      world.setBlockState(pos, state.withProperty(LIT, lit), 2);
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return state.getValue(LIT) ? light : 0;
  }

  @Override
  public int getLightValue(@Nonnull IBlockState state, IBlockAccess world,
      @Nonnull BlockPos pos) {
    return getLightValue(state);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    Mount mount = state.getValue(MOUNT);
    return mount == Mount.FLOOR ? floorBox : wallBoxes[mount.ordinal() - 1];
  }

  /** Which light it is and how it flashes, and that redstone switches it off. */
  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
      ITooltipFlag flag) {
    super.addInformation(stack, world, tooltip, flag);
    tooltip.add(I18n.format("csm.lighting." + getBlockRegistryName() + ".tooltip"));
    tooltip.add(I18n.format("csm.lighting.obstruction_beacon.redstone"));
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
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
    return true;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
