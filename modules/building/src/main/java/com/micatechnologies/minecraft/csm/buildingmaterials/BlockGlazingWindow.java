package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.BlockUtils;
import com.micatechnologies.minecraft.csm.codeutils.CsmExtendedBlockState;
import com.micatechnologies.minecraft.csm.codeutils.ICsmGlassFronted;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockPane;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A framed window that extends the Glazing set: clear glass in a frame of one finish
 * ({@code glass_window_<finish>}), or the same window turned 45 degrees across the cell for a
 * canted bay ({@code glass_window_<finish>_canted}). One class, constructed by registry name.
 *
 * <p><b>Joins, computed from the neighbours every time (nothing about them is stored):</b> a
 * window runs across its cell square to its facing. Toward each end of the run it meets the same
 * window (joined: no frame between plain windows, one shared mullion between sash windows), a
 * canted corner of the same finish and depth (joined; the corner draws the post), other glass or
 * another window (framed), a solid face (framed, and the frame lines the opening to the inner
 * face) or nothing (framed). It turns a corner, with one post, toward a window of the same finish
 * and depth that faces square to it (a bay's side window), or, when one end of its run is free,
 * toward a solid face behind it, or toward the facade where the bay's back opens into the room (a
 * 1-deep bay's side, back to the facade). Windows of the same
 * finish, facing and depth stacked one on another are one tall window: one sill at the foot, one
 * head at the top, the meeting rail halfway up ({@link WindowLayout}).</p>
 *
 * <p><b>Stored:</b> metadata {@code facing | depth << 2} (facing S0 W1 N2 E3, the outside, the way
 * the placer looked; depth 0 centred, 1 flush with the outside, 2 inset), and the tile entity's
 * {@code pt}, {@code op}, {@code sl}, {@code ar}, {@code hd}, {@code pn}
 * ({@link TileEntityGlazingWindow}). So
 * {@code server_set_blocks} with the meta and that NBT gives exactly the hand-placed window. 12
 * states a block; everything else reaches the model as one unlisted value ({@link #SHAPE}).</p>
 *
 * <p><b>Controls, an empty hand:</b></p>
 * <ul>
 *   <li>Right-click: the next pattern (plain, double-hung 1/1, double-hung 6/6, casement pair),
 *       for the whole stack.</li>
 *   <li>Right-click the bottom quarter of the stack's foot: open or close it, the whole stack.</li>
 *   <li>Right-click the top quarter of the stack's top: arched or square top.</li>
 *   <li>Sneak + right-click: the next depth (centred, flush, inset), for the whole stack.</li>
 *   <li>Sneak + right-click the bottom quarter of the foot: the next sill (none, stone, frame).</li>
 *   <li>Sneak + right-click the top quarter of the top: the next head (none, flat lintel,
 *       keystone lintel).</li>
 *   <li>Sneak + right-click from the room side (a face the window does not look out of), not on
 *       those quarters: this block alone becomes a solid spandrel panel, or glass again
 *       ({@code pn}). A panel is a stack of its own, so a bay's floor line can be a panel between
 *       two windows that keep their own sills and heads.</li>
 * </ul>
 * <p>An open window's collision is its closed glass: nothing passes through it either way.</p>
 *
 * @version 1.0
 * @since 2026.11
 */
public class BlockGlazingWindow extends AbstractBlock implements ICsmGlassFronted,
    ICsmTileEntityProvider {

  /**
   * Where the glass sits in the cell's depth.
   *
   * @since 1.0
   */
  public enum Depth implements IStringSerializable {
    CENTRED("centred"), FLUSH("flush"), INSET("inset");

    private final String name;

    Depth(String name) {
      this.name = name;
    }

    @Override
    @Nonnull
    public String getName() {
      return name;
    }
  }

  public static final PropertyDirection FACING = BlockHorizontal.FACING;
  public static final PropertyEnum<Depth> DEPTH = PropertyEnum.create("depth", Depth.class);

  /** The window's whole shape, {@link WindowLayout.Shape#pack()}, or null before it is known. */
  public static final IUnlistedProperty<Long> SHAPE = new IUnlistedProperty<Long>() {
    @Override
    public String getName() {
      return "shape";
    }

    @Override
    public boolean isValid(Long value) {
      return value != null;
    }

    @Override
    public Class<Long> getType() {
      return Long.class;
    }

    @Override
    public String valueToString(Long value) {
      return String.valueOf(value);
    }
  };

  /** The share of a block's height that is the bottom or top band a click there acts on. */
  private static final float BAND = 0.25f;

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final String finish;
  private final boolean canted;

  /**
   * Constructs a {@link BlockGlazingWindow}.
   *
   * @param registryName {@code glass_window_<finish>} or {@code glass_window_<finish>_canted}
   *
   * @since 1.0
   */
  public BlockGlazingWindow(String registryName) {
    super(pendingMaterial(registryName), SoundType.GLASS, "pickaxe", 0, 0.8F, 1.5F, 0F, 0);
    this.registryName = registryName;
    this.canted = registryName.endsWith("_canted");
    this.finish = registryName.substring("glass_window_".length()).replace("_canted", "");
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(DEPTH, Depth.CENTRED));
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material pendingMaterial(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return Material.GLASS;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  /** The frame finish, as in the registry name ({@code oak}, {@code darkoak}...). */
  public String getFinish() {
    return finish;
  }

  /** Whether this is the 45-degree canted corner. */
  public boolean isCanted() {
    return canted;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmExtendedBlockState(this, new IProperty[]{FACING, DEPTH},
        new IUnlistedProperty[]{SHAPE});
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    int depth = meta >> 2 & 3;
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(DEPTH, Depth.values()[depth < WindowLayout.DEPTHS ? depth : 0]);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | state.getValue(DEPTH).ordinal() << 2;
  }

  /**
   * Faces the way the placer looks: that is the outside. A canted corner faces the quarter the
   * placer looks into: looking north-west it faces north (its outsides are north and west).
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing f = placer.getHorizontalFacing();
    if (canted) {
      f = EnumFacing.byHorizontalIndex(WindowLayout.cantedFacing(placer.rotationYaw));
    }
    IBlockState state = getDefaultState().withProperty(FACING, f);
    BlockPos like = placementNeighbour(worldIn, pos, state);
    return like == null ? state
        : state.withProperty(DEPTH, worldIn.getBlockState(like).getValue(DEPTH));
  }

  /**
   * A window placed by hand against another of the same finish, facing the same way, takes that
   * window's settings: its depth here, and its pattern, open state, arch, sill and head in
   * {@link #onBlockPlacedBy}. Otherwise a window added under a 1/1 stack would become the stack's
   * foot and turn the whole stack plain, and one added beside a flush run would not join it until
   * it was sneak-clicked. Below and above are looked at first (the same stack), then either side.
   * Scripted placement ({@code setBlockState}) never runs this, so meta and NBT still say
   * everything.
   */
  @Nullable
  private BlockPos placementNeighbour(IBlockAccess world, BlockPos pos, IBlockState here) {
    EnumFacing f = here.getValue(FACING);
    for (EnumFacing d : new EnumFacing[]{EnumFacing.DOWN, EnumFacing.UP, f.rotateY(),
        f.rotateYCCW()}) {
      BlockPos p = pos.offset(d);
      IBlockState other = world.getBlockState(p);
      // a spandrel panel is not a window to copy: a window placed on one takes the next
      if (other.getBlock() == this && other.getValue(FACING) == f && !isPanel(world, p)) {
        return p;
      }
    }
    return null;
  }

  /** Whether the window at {@code pos} is a spandrel panel. */
  private static boolean isPanel(IBlockAccess world, BlockPos pos) {
    TileEntity te = BlockUtils.getTileEntitySafe(world, pos);
    return te instanceof TileEntityGlazingWindow && ((TileEntityGlazingWindow) te).isPanel();
  }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    if (world.isRemote) {
      return;
    }
    BlockPos like = placementNeighbour(world, pos, state);
    TileEntity from = like == null ? null : world.getTileEntity(like);
    TileEntity to = world.getTileEntity(pos);
    if (from instanceof TileEntityGlazingWindow && to instanceof TileEntityGlazingWindow) {
      TileEntityGlazingWindow a = (TileEntityGlazingWindow) from;
      TileEntityGlazingWindow b = (TileEntityGlazingWindow) to;
      b.setPattern(a.getPattern());
      b.setOpen(a.isOpen());
      b.setArch(a.isArch());
      b.setSill(a.getSill());
      b.setHead(a.getHead());
      b.markDirtySync(world, pos, state, true);
    }
  }

  // --- joins --------------------------------------------------------------------------------

  /** The world direction of a model-space direction (model north is the outside). */
  private static EnumFacing world(EnumFacing model, EnumFacing facing) {
    int turns = (facing.getHorizontalIndex() + 2) & 3;   // N 0, E 1, S 2, W 3
    EnumFacing out = model;
    for (int i = 0; i < turns; i++) {
      out = out.rotateY();
    }
    return out;
  }

  /**
   * Whether the window at {@code at} (state {@code other}) is in the same stack as this one at
   * {@code pos}: the same block, facing and depth, and both panels or both glass.
   */
  private boolean sameStack(IBlockAccess world, BlockPos pos, IBlockState here, BlockPos at,
      IBlockState other) {
    return other.getBlock() == this && other.getValue(FACING) == here.getValue(FACING)
        && other.getValue(DEPTH) == here.getValue(DEPTH)
        && isPanel(world, at) == isPanel(world, pos);
  }

  private int runEnd(IBlockAccess world, BlockPos pos, IBlockState here, EnumFacing toward) {
    BlockPos at = pos.offset(toward);
    IBlockState other = world.getBlockState(at);
    Block block = other.getBlock();
    if (block instanceof BlockGlazingWindow) {
      BlockGlazingWindow w = (BlockGlazingWindow) block;
      boolean sameDepth = other.getValue(DEPTH) == here.getValue(DEPTH);
      // a panel beside glass is framed against it, not joined: the panel would leave its end
      // open and the window would end in half a mullion
      if (w.finish.equals(finish) && sameDepth && isPanel(world, at) == isPanel(world, pos)) {
        if (w.canted) {
          return WindowLayout.END_JOIN;
        }
        EnumFacing a = here.getValue(FACING);
        EnumFacing b = other.getValue(FACING);
        if (a == b || a.getAxis() != b.getAxis()
            || here.getValue(DEPTH) == Depth.CENTRED) {
          return WindowLayout.END_JOIN;
        }
      }
      return WindowLayout.END_OTHER;
    }
    if (block instanceof BlockGlazingPane || block instanceof BlockGlazing
        || block instanceof BlockPane) {
      return WindowLayout.END_OTHER;
    }
    if (other.getBlockFaceShape(world, at, toward.getOpposite()) == BlockFaceShape.SOLID) {
      return WindowLayout.END_SOLID;
    }
    return WindowLayout.END_FREE;
  }

  /**
   * Whether the window turns toward {@code toward}. To a solid face only when one end of the run
   * is free ({@code free}, its direction, else null), and the face is there or, for a bay whose
   * back opens into the room, the facade carries on beside it: the block behind on the free side.
   */
  private int crossArm(IBlockAccess world, BlockPos pos, IBlockState here, EnumFacing toward,
      @Nullable EnumFacing free) {
    BlockPos at = pos.offset(toward);
    IBlockState other = world.getBlockState(at);
    if (other.getBlock() == this && other.getValue(DEPTH) == here.getValue(DEPTH)
        && other.getValue(FACING).getAxis() != here.getValue(FACING).getAxis()) {
      return WindowLayout.CROSS_JOIN;
    }
    if (free != null) {
      if (other.getBlockFaceShape(world, at, toward.getOpposite()) == BlockFaceShape.SOLID) {
        return WindowLayout.CROSS_SOLID;
      }
      BlockPos beside = at.offset(free);
      if (!other.isFullCube() && world.getBlockState(beside)
          .getBlockFaceShape(world, beside, free.getOpposite()) == BlockFaceShape.SOLID) {
        return WindowLayout.CROSS_SOLID;
      }
    }
    return WindowLayout.CROSS_NONE;
  }

  /**
   * The window's shape from its neighbours and, if {@code settings}, its stack and the stored
   * settings of the stack's foot and top.
   */
  WindowLayout.Shape shapeOf(IBlockAccess world, BlockPos pos, IBlockState state,
      boolean settings) {
    WindowLayout.Shape s = new WindowLayout.Shape();
    EnumFacing facing = state.getValue(FACING);
    s.facing = facing.getHorizontalIndex();
    s.depth = state.getValue(DEPTH).ordinal();
    s.canted = canted;
    if (!canted) {
      s.endW = runEnd(world, pos, state, world(EnumFacing.WEST, facing));
      s.endE = runEnd(world, pos, state, world(EnumFacing.EAST, facing));
      EnumFacing free = null;
      if ((s.endW == WindowLayout.END_FREE) != (s.endE == WindowLayout.END_FREE)) {
        free = world(s.endW == WindowLayout.END_FREE ? EnumFacing.WEST : EnumFacing.EAST, facing);
      }
      s.crossS = crossArm(world, pos, state, world(EnumFacing.SOUTH, facing), free);
      s.crossN = crossArm(world, pos, state, world(EnumFacing.NORTH, facing), free);
      // a turn to a wall goes behind; never both ways
      if (s.crossS == WindowLayout.CROSS_SOLID && s.crossN == WindowLayout.CROSS_SOLID) {
        s.crossN = WindowLayout.CROSS_NONE;
      }
    }
    if (!settings) {
      return s;
    }
    s.panel = isPanel(world, pos);
    int below = 0;
    while (below < WindowLayout.MAX_STACK - 1 && sameStack(world, pos, state,
        pos.down(below + 1), world.getBlockState(pos.down(below + 1)))) {
      below++;
    }
    int above = 0;
    while (below + above < WindowLayout.MAX_STACK - 1 && sameStack(world, pos, state,
        pos.up(above + 1), world.getBlockState(pos.up(above + 1)))) {
      above++;
    }
    s.height = below + above + 1;
    s.index = below;
    BlockPos foot = pos.down(below);
    BlockPos top = pos.up(above);
    TileEntity footTe = BlockUtils.getTileEntitySafe(world, foot);
    if (footTe instanceof TileEntityGlazingWindow) {
      TileEntityGlazingWindow t = (TileEntityGlazingWindow) footTe;
      s.pattern = t.getPattern();
      s.open = t.isOpen();
      s.sill = t.getSill();
    }
    TileEntity topTe = BlockUtils.getTileEntitySafe(world, top);
    if (topTe instanceof TileEntityGlazingWindow) {
      TileEntityGlazingWindow t = (TileEntityGlazingWindow) topTe;
      s.arch = t.isArch();
      s.head = t.getHead();
    }
    s.solidBelow = world.getBlockState(foot.down())
        .getBlockFaceShape(world, foot.down(), EnumFacing.UP) == BlockFaceShape.SOLID;
    s.solidAbove = world.getBlockState(top.up())
        .getBlockFaceShape(world, top.up(), EnumFacing.DOWN) == BlockFaceShape.SOLID;
    return s;
  }

  @Override
  @Nonnull
  public IBlockState getExtendedState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    if (!(state instanceof IExtendedBlockState)) {
      return state;
    }
    return ((IExtendedBlockState) state).withProperty(SHAPE,
        shapeOf(world, pos, state, true).pack());
  }

  // --- controls -----------------------------------------------------------------------------

  /** The stack this block is in, from its foot up. */
  private List<BlockPos> stack(World world, BlockPos pos, IBlockState state) {
    BlockPos foot = pos;
    int n = 0;
    while (n < WindowLayout.MAX_STACK - 1
        && sameStack(world, pos, state, foot.down(), world.getBlockState(foot.down()))) {
      foot = foot.down();
      n++;
    }
    List<BlockPos> out = new ArrayList<>();
    BlockPos p = foot;
    out.add(p);
    while (out.size() < WindowLayout.MAX_STACK
        && sameStack(world, pos, state, p.up(), world.getBlockState(p.up()))) {
      p = p.up();
      out.add(p);
    }
    return out;
  }

  @Nullable
  private static TileEntityGlazingWindow te(World world, BlockPos pos) {
    TileEntity te = world.getTileEntity(pos);
    return te instanceof TileEntityGlazingWindow ? (TileEntityGlazingWindow) te : null;
  }

  /**
   * Whether a click on {@code side} came from the room: the face opposite the one the window
   * looks out of (a canted corner, which looks out of its facing and the side anticlockwise of
   * it, has two). A straight window's ends are not the room side: at a bay's end one faces the
   * street.
   */
  private boolean roomSide(IBlockState state, EnumFacing side) {
    EnumFacing f = state.getValue(FACING);
    return side == f.getOpposite() || canted && side == f.rotateY();
  }

  private static void say(EntityPlayer player, String key) {
    player.sendStatusMessage(new TextComponentTranslation("csm.building.window." + key), true);
  }

  /**
   * The controls in the class comment: what a click does depends on sneaking and on whether it
   * lands in the bottom quarter of the stack's foot or the top quarter of its top.
   *
   * @since 1.0
   */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (world.isRemote) {
      return true;
    }
    List<BlockPos> stack = stack(world, pos, state);
    BlockPos foot = stack.get(0);
    BlockPos top = stack.get(stack.size() - 1);
    boolean bottomBand = pos.equals(foot) && hitY < BAND;
    boolean topBand = !bottomBand && pos.equals(top) && hitY > 1 - BAND;
    TileEntityGlazingWindow footTe = te(world, foot);
    TileEntityGlazingWindow topTe = te(world, top);
    if (footTe == null || topTe == null) {
      return true;
    }
    if (player.isSneaking()) {
      if (bottomBand) {
        footTe.setSill(footTe.getSill() + 1);
        footTe.markDirtySync(world, foot, world.getBlockState(foot), true);
        say(player, "sill." + footTe.getSill());
      } else if (topBand) {
        topTe.setHead(topTe.getHead() + 1);
        topTe.markDirtySync(world, top, world.getBlockState(top), true);
        say(player, "head." + topTe.getHead());
      } else if (roomSide(state, side)) {
        TileEntityGlazingWindow here = te(world, pos);
        if (here != null) {
          here.setPanel(!here.isPanel());
          if (here.isPanel()) {
            // a panel copied its window neighbour's trim when it was placed; as a stack of its
            // own it would draw that sill and head on the floor line
            here.setSill(WindowLayout.SILL_NONE);
            here.setHead(WindowLayout.HEAD_NONE);
            here.setArch(false);
            here.setOpen(false);
          }
          here.markDirtySync(world, pos, state, true);
          say(player, here.isPanel() ? "panel.on" : "panel.off");
        }
      } else {
        Depth next = Depth.values()[(state.getValue(DEPTH).ordinal() + 1) % WindowLayout.DEPTHS];
        for (BlockPos p : stack) {
          world.setBlockState(p, world.getBlockState(p).withProperty(DEPTH, next), 3);
        }
        say(player, "depth." + next.ordinal());
      }
      return true;
    }
    if (topBand) {
      topTe.setArch(!topTe.isArch());
      topTe.markDirtySync(world, top, world.getBlockState(top), true);
      say(player, topTe.isArch() ? "arch.on" : "arch.off");
      return true;
    }
    int pattern = footTe.getPattern();
    boolean open = footTe.isOpen();
    if (bottomBand) {
      if (pattern == WindowLayout.PLAIN) {
        say(player, "fixed");
        return true;
      }
      open = !open;
      say(player, open ? "open" : "closed");
    } else {
      pattern = (pattern + 1) % WindowLayout.PATTERNS;
      if (pattern == WindowLayout.PLAIN) {
        open = false;
      }
      say(player, "pattern." + pattern);
    }
    for (BlockPos p : stack) {
      TileEntityGlazingWindow t = te(world, p);
      if (t != null) {
        t.setPattern(pattern);
        t.setOpen(open);
        t.markDirtySync(world, p, world.getBlockState(p), true);
      }
    }
    return true;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
      ITooltipFlag flag) {
    tooltip.add(I18n.format("csm.building.window.tooltip.click"));
    tooltip.add(I18n.format("csm.building.window.tooltip.sneak"));
    tooltip.add(I18n.format("csm.building.window.tooltip.panel"));
  }

  // --- tile entity --------------------------------------------------------------------------

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World worldIn, int meta) {
    return new TileEntityGlazingWindow();
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityGlazingWindow.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityglazingwindow";
  }

  // --- shape --------------------------------------------------------------------------------

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, @Nonnull World worldIn,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn,
      boolean isActualState) {
    EnumFacing facing = state.getValue(FACING);
    for (float[] b : WindowLayout.collision(shapeOf(worldIn, pos, state, false))) {
      AxisAlignedBB box = new AxisAlignedBB(b[0] / 16.0, 0, b[1] / 16.0, b[2] / 16.0, 1,
          b[3] / 16.0);
      addCollisionBoxToList(pos, entityBox, collidingBoxes, BlockGarageDoor.turn(box, facing));
    }
  }

  /** The whole cell, so any part of the window, its trim included, can be clicked. */
  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return FULL_BLOCK_AABB;
  }

  @Override
  @Nonnull
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return face.getAxis() == EnumFacing.Axis.Y ? BlockFaceShape.CENTER_SMALL
        : BlockFaceShape.MIDDLE_POLE_THIN;
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
    return BlockRenderLayer.TRANSLUCENT;
  }
}
