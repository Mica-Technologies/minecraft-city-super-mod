package com.micatechnologies.minecraft.csm.healthcare.wards;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * One block of a cubicle curtain ({@code cubicle_curtain}, in the hidden tab): the curtain is a
 * column of these under each {@link BlockCurtainTrack} cell, filled by the track. The top panel
 * of a column carries the white mesh band over its cloth, the bottom one the hem; panels side by
 * side join into one curtain, straight or round a corner. Nothing collides: people walk through
 * a curtain.
 *
 * <p><b>Metadata</b>: bits 0-1 the cloth ({@link Pattern}: 0 mint, 1 blue check, 2 beige,
 * 3 white); bits 2-3 how it is drawn ({@link Drawn}: 0 closed, 1 open -- the cloth has gone to
 * the end of the run, nothing is drawn here -- 2 bunched, the cloth gathered here). Its shape,
 * top and bottom are worked out from the neighbours.</p>
 *
 * <p>A click opens or closes the whole curtain, bunched at the run's end nearest the click; a
 * sneak-click changes the whole curtain's cloth. See {@link CubicleCurtains}.</p>
 *
 * @since 2026.10
 */
public class BlockCurtainPanel extends AbstractBlock {

  /** The cloths. Stored in metadata by ordinal: append only. */
  public enum Pattern implements IStringSerializable {
    MINT, BLUE_CHECK, BEIGE, WHITE;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase();
    }

    /**
     * The pattern named {@code name}, or null.
     *
     * @param name a pattern's name
     *
     * @return the pattern
     */
    @Nullable
    public static Pattern byName(String name) {
      for (Pattern p : values()) {
        if (p.getName().equals(name)) {
          return p;
        }
      }
      return null;
    }
  }

  /** The panel's shape: straight along an axis, or a corner joining two sides. */
  public enum Shape implements IStringSerializable {
    X, Z, NE, NW, SE, SW;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase();
    }
  }

  /** How the panel is drawn. Stored by ordinal: append only. */
  public enum Drawn implements IStringSerializable {
    CLOSED, OPEN, BUNCHED;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase();
    }
  }

  public static final PropertyEnum<Pattern> PATTERN = PropertyEnum.create("pattern",
      Pattern.class);
  public static final PropertyEnum<Drawn> DRAWN = PropertyEnum.create("drawn", Drawn.class);
  public static final PropertyEnum<Shape> SHAPE = PropertyEnum.create("shape", Shape.class);
  public static final PropertyBool TOP = PropertyBool.create("top");
  public static final PropertyBool BOTTOM = PropertyBool.create("bottom");

  private static final AxisAlignedBB BOX_X = new AxisAlignedBB(0, 0, 6 / 16.0, 1, 1, 10 / 16.0);
  private static final AxisAlignedBB BOX_Z = new AxisAlignedBB(6 / 16.0, 0, 0, 10 / 16.0, 1, 1);
  private static final AxisAlignedBB BOX_CORNER = new AxisAlignedBB(0, 0, 0, 1, 1, 1);
  private static final AxisAlignedBB BOX_BUNCH = new AxisAlignedBB(5 / 16.0, 0, 5 / 16.0,
      11 / 16.0, 1, 11 / 16.0);

  /** How far up a lone panel looks for the track that gives it its line. */
  private static final int TRACK_SEARCH = 16;

  private static BlockCurtainPanel instance;

  public BlockCurtainPanel() {
    super(HealthcareMaterial.CLOTH.getMaterial(), HealthcareMaterial.CLOTH.getSound(), "axe", 0,
        0.5F, 1.0F, 0.0F, 0);
    setDefaultState(blockState.getBaseState().withProperty(PATTERN, Pattern.MINT)
        .withProperty(DRAWN, Drawn.CLOSED).withProperty(SHAPE, Shape.X)
        .withProperty(TOP, true).withProperty(BOTTOM, true));
    instance = this;
  }

  /**
   * The panel block.
   *
   * @return the registered panel block
   */
  public static BlockCurtainPanel get() {
    return instance;
  }

  @Override
  public String getBlockRegistryName() {
    return "cubicle_curtain";
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, PATTERN, DRAWN, SHAPE, TOP, BOTTOM);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    int drawn = meta >> 2 & 3;
    return getDefaultState().withProperty(PATTERN, Pattern.values()[meta & 3])
        .withProperty(DRAWN, drawn < 3 ? Drawn.values()[drawn] : Drawn.CLOSED);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(PATTERN).ordinal() | state.getValue(DRAWN).ordinal() << 2;
  }

  /**
   * Whether the block at {@code pos} is a curtain panel.
   *
   * @param world the world
   * @param pos   a position
   *
   * @return true for a panel
   */
  public static boolean isPanel(IBlockAccess world, BlockPos pos) {
    return world.getBlockState(pos).getBlock() instanceof BlockCurtainPanel;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos) {
    boolean n = isPanel(world, pos.north());
    boolean e = isPanel(world, pos.east());
    boolean s = isPanel(world, pos.south());
    boolean w = isPanel(world, pos.west());
    Shape shape;
    if (e && w || (e || w) && !n && !s) {
      shape = Shape.X;
    } else if (n && s || (n || s) && !e && !w) {
      shape = Shape.Z;
    } else if (n || s) {
      shape = n ? (e ? Shape.NE : Shape.NW) : (e ? Shape.SE : Shape.SW);
    } else {
      shape = loneShape(world, pos);
    }
    return state.withProperty(SHAPE, shape)
        .withProperty(TOP, !isPanel(world, pos.up()))
        .withProperty(BOTTOM, !isPanel(world, pos.down()));
  }

  /** A panel with no panel beside it hangs along the line of the track above it. */
  private static Shape loneShape(IBlockAccess world, BlockPos pos) {
    BlockPos p = pos.up();
    for (int i = 0; i < TRACK_SEARCH; i++, p = p.up()) {
      Block b = world.getBlockState(p).getBlock();
      if (b instanceof BlockCurtainTrack) {
        return ((BlockCurtainTrack) b).lineAxis(world, p) == EnumFacing.Axis.Z ? Shape.Z
            : Shape.X;
      }
      if (!(b instanceof BlockCurtainPanel)) {
        break;
      }
    }
    return Shape.X;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!world.isRemote) {
      if (player.isSneaking()) {
        CubicleCurtains.cyclePattern(world, pos);
      } else {
        CubicleCurtains.toggle(world, pos);
      }
    }
    return true;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    IBlockState actual = state.getBlock() == this && source.getBlockState(pos).getBlock() == this
        ? state.getActualState(source, pos) : state;
    if (actual.getValue(DRAWN) != Drawn.CLOSED) {
      return BOX_BUNCH;
    }
    switch (actual.getValue(SHAPE)) {
      case X:
        return BOX_X;
      case Z:
        return BOX_Z;
      default:
        return BOX_CORNER;
    }
  }

  @Nullable
  @Override
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
  }

  @Override
  public boolean isPassable(IBlockAccess world, BlockPos pos) {
    return true;
  }

  @Override
  @Nonnull
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return Items.AIR;
  }

  @Override
  @Nonnull
  public ItemStack getPickBlock(@Nonnull IBlockState state, RayTraceResult target,
      @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player) {
    Block track = CubicleCurtains.trackBlock();
    return track == null ? ItemStack.EMPTY : new ItemStack(track);
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
