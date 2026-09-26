package com.micatechnologies.minecraft.csm.powergrid.gas;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.powergrid.water.BlockPipeFitting;
import com.micatechnologies.minecraft.csm.powergrid.water.BlockWaterPipe;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A block of a gas regulator station's steel skid: a diamond-plate deck on a perimeter of steel
 * beam, a block tall, laid side by side to any size. It is best set into the ground so its deck
 * sits a little above grade, with the station's pipe, valves, regulators and meter a block above
 * it at their working height, and the line heater and odorant tank standing on it.
 *
 * <p>Nothing is stored. Whether the skid carries on to each side is actual state, and the
 * perimeter beam is drawn only where it stops, so a skid of any size reads as one. Where a
 * straight run of pipe, or an inline fitting, is directly above, {@link #SUPPORT} draws a pipe
 * stand and saddle up to it: under every fitting, and under every other block of a straight run
 * (by position), which is how often a real station's pipe is carried. Forty-eight states, one
 * model location.</p>
 *
 * @since 2026.9
 */
public class BlockEquipmentSkid extends AbstractBlock {

  /** Which way a pipe stand's saddle runs: none, or along the pipe above. */
  public enum Support implements IStringSerializable {
    NONE, X, Z;

    @Override
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  public static final PropertyBool NORTH = PropertyBool.create("north");
  public static final PropertyBool SOUTH = PropertyBool.create("south");
  public static final PropertyBool EAST = PropertyBool.create("east");
  public static final PropertyBool WEST = PropertyBool.create("west");
  public static final PropertyEnum<Support> SUPPORT = PropertyEnum.create("support",
      Support.class);

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  public BlockEquipmentSkid(String registryName) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 3F, 10F, 0F, 0);
    this.registryName = registryName;
    setDefaultState(getDefaultState().withProperty(NORTH, false).withProperty(SOUTH, false)
        .withProperty(EAST, false).withProperty(WEST, false).withProperty(SUPPORT, Support.NONE));
    PENDING.remove();
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
    return new CsmBlockStateContainer(this, NORTH, SOUTH, EAST, WEST, SUPPORT);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state.withProperty(NORTH, world.getBlockState(pos.north()).getBlock() == this)
        .withProperty(SOUTH, world.getBlockState(pos.south()).getBlock() == this)
        .withProperty(EAST, world.getBlockState(pos.east()).getBlock() == this)
        .withProperty(WEST, world.getBlockState(pos.west()).getBlock() == this)
        .withProperty(SUPPORT, support(world, pos));
  }

  /** The pipe stand under whatever is above: a fitting, or every other block of a run. */
  private static Support support(IBlockAccess world, BlockPos pos) {
    BlockPos up = pos.up();
    IBlockState above = world.getBlockState(up);
    if (above.getBlock() instanceof BlockPipeFitting) {
      return axis(above.getValue(AbstractBlockRotatableNSEW.FACING).getAxis());
    }
    if (!(above.getBlock() instanceof BlockWaterPipe) || ((pos.getX() + pos.getZ()) & 1) != 0) {
      return Support.NONE;
    }
    boolean[] arms = ((BlockWaterPipe) above.getBlock()).arms(world, up);
    if (!BlockWaterPipe.straight(arms) || arms[EnumFacing.UP.getIndex()]) {
      return Support.NONE;
    }
    return arms[EnumFacing.EAST.getIndex()] ? Support.X : Support.Z;
  }

  private static Support axis(EnumFacing.Axis axis) {
    return axis == EnumFacing.Axis.X ? Support.X : Support.Z;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState();
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return 0;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return FULL_BLOCK_AABB;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return face == EnumFacing.UP ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
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
    return BlockRenderLayer.SOLID;
  }
}
