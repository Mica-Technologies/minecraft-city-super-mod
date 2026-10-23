package com.micatechnologies.minecraft.csm.codeutils.wayfinding;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.BlockUtils;
import com.micatechnologies.minecraft.csm.codeutils.CsmExtendedBlockState;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * A wayfinding panel: a sign of any size up to {@value WayfindingPanels#MAX_WIDTH} x
 * {@value WayfindingPanels#MAX_HEIGHT} blocks, built from cells placed side by side and stacked,
 * all facing the same way (towards the reader: S 0 W 1 N 2 E 3 in metadata). Transit's airport
 * panel, Healthcare's hospital panel and Building's generic panel are this block with their own
 * default sign, presets and tab; their tile entity NBT is one ({@link WayfindingSign}).
 *
 * <p><b>Baked, not rendered.</b> The block's own model is empty; {@link WayfindingBakedModel}
 * draws each cell's share of the slab, its frame, the sign and the rods or posts, clipped to the
 * cell, from the cell's {@link WayfindingFace} (an unlisted property). The cell's place in its
 * panel is worked out on the server when a cell is added or removed and kept in its tile entity,
 * so the model needs nothing but its own cell and what is right above, below and behind the
 * panel. Nothing runs per frame and nothing ticks. A lit panel gives a little light (8).</p>
 *
 * <p>An empty-handed click on any cell opens the editor ({@link WayfindingPanelGui}); a
 * sneak-click turns the arrow to the next direction.</p>
 *
 * @since 2026.10
 */
public abstract class AbstractBlockWayfindingPanel extends AbstractBlockRotatableNSEW
    implements ICsmTileEntityProvider {

  /** The editor's GUI id, unique across the mod. */
  public static final int GUI_ID = 46;
  /** The light a lit panel gives. */
  public static final int LIGHT = 8;

  /** What the cell draws, from its tile entity and the world round the panel. */
  public static final IUnlistedProperty<WayfindingFace> FACE =
      new IUnlistedProperty<WayfindingFace>() {
        @Override
        public String getName() {
          return "face";
        }

        @Override
        public boolean isValid(WayfindingFace value) {
          return value != null;
        }

        @Override
        public Class<WayfindingFace> getType() {
          return WayfindingFace.class;
        }

        @Override
        public String valueToString(WayfindingFace value) {
          return value.sign.getLine(0);
        }
      };

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();
  private static final AxisAlignedBB DEFAULT_BOX =
      new AxisAlignedBB(0, 0, 6 / 16.0, 1, 1, 10 / 16.0);

  private final String registryName;

  /**
   * Constructs a panel block.
   *
   * @param registryName its registry name
   */
  protected AbstractBlockWayfindingPanel(String registryName) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 2.0F, 6.0F, 0F, 0);
    this.registryName = registryName;
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

  /** The presets the editor's preset button steps through, this block's own first. */
  public abstract WayfindingPreset[] getPresets();

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmExtendedBlockState(this, new IProperty[]{FACING},
        new IUnlistedProperty[]{FACE});
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state;
  }

  /**
   * The cell's face: its sign and place, and the mount, rods and posts its world gives.
   *
   * @param supports whether to look for the rods' ceiling and the posts' floor (the model needs
   *                 them, a hitbox does not)
   */
  @Nullable
  public static WayfindingFace faceOf(IBlockAccess world, BlockPos pos, IBlockState state,
      boolean supports) {
    TileEntity te = BlockUtils.getTileEntitySafe(world, pos);
    if (!(te instanceof AbstractTileEntityWayfindingPanel)
        || !state.getPropertyKeys().contains(FACING)) {
      return null;
    }
    AbstractTileEntityWayfindingPanel cell = (AbstractTileEntityWayfindingPanel) te;
    EnumFacing facing = state.getValue(FACING);
    WayfindingSign sign = cell.getSign();
    int col = cell.getCol();
    int row = cell.getRow();
    int w = cell.getPanelWidth();
    int h = cell.getPanelHeight();
    boolean inside = w > 0 && h > 0;
    EnumFacing right = WayfindingPanels.leftOf(facing).getOpposite();
    BlockPos controller = inside ? pos.offset(right, -col).down(row) : pos;
    WayfindingMount mount = WayfindingPanels.resolveMount(world, controller, facing,
        sign.getMount());
    WayfindingFace bare = new WayfindingFace(sign, col, row, w, h, mount, null, null);
    if (!inside || !supports) {
      return bare;
    }
    float[] rods = null;
    float[] posts = null;
    if (mount == WayfindingMount.HUNG && row == h - 1) {
      rods = supports(world, pos, bare, col, EnumFacing.UP);
    } else if (mount == WayfindingMount.POST && row == 0) {
      posts = supports(world, pos, bare, col, EnumFacing.DOWN);
    }
    return rods == null && posts == null ? bare
        : new WayfindingFace(sign, col, row, w, h, mount, rods, posts);
  }

  /**
   * The rods (up) or posts (down) in this cell's column: each reaches through air to the first
   * block, and one with nothing within {@link WayfindingPanels#MAX_REACH} is not drawn.
   *
   * @return x and how far past the cell, in pairs; null for none
   */
  @Nullable
  private static float[] supports(IBlockAccess world, BlockPos pos, WayfindingFace face, int col,
      EnumFacing direction) {
    float[] xs = face.supportXs(direction == EnumFacing.DOWN);
    float[] out = null;
    for (float x : xs) {
      int at = (int) Math.floor(x);
      if (Math.max(0, Math.min(face.width - 1, at)) != col) {
        continue;
      }
      int reach = WayfindingPanels.reach(world, pos, direction);
      if (reach < 0) {
        continue;
      }
      float[] grown = out == null ? new float[2] : java.util.Arrays.copyOf(out, out.length + 2);
      grown[grown.length - 2] = x;
      grown[grown.length - 1] = reach;
      out = grown;
    }
    return out;
  }

  @Override
  @Nonnull
  public IBlockState getExtendedState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    if (!(state instanceof IExtendedBlockState)) {
      return state;
    }
    WayfindingFace face = faceOf(world, pos, state, true);
    return face == null ? state : ((IExtendedBlockState) state).withProperty(FACE, face);
  }

  /**
   * The cell's share of the slab, facing north, or the whole cell's slab before the tile entity
   * is there (a box is asked for while a chunk loads).
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    WayfindingFace face = source == null ? null : faceOf(source, pos, state, false);
    if (face == null) {
      return DEFAULT_BOX;
    }
    float c = face.cellCol();
    float r = face.cellRow();
    float x0 = Math.max(face.panelX0(), c);
    float x1 = Math.min(face.panelX0() + face.panelWidth(), c + 1);
    float y0 = Math.max(face.panelY0(), r);
    float y1 = Math.min(face.panelY0() + face.panelHeight(), r + 1);
    double z0 = face.slabZ0() / 16.0;
    double z1 = face.slabZ1() / 16.0;
    if (x1 - x0 < 1 / 16f || y1 - y0 < 1 / 16f) {
      // none of the panel is in this cell (a small panel on a big build): a sliver to break
      return new AxisAlignedBB(0, 0, z0, 1, 1 / 16.0, z1);
    }
    // the reader's x runs to the model's -x: their left edge of the cell is the model's x = 1
    return new AxisAlignedBB(c + 1 - x1, y0 - r, z0, c + 1 - x0, y1 - r, z1);
  }

  @Override
  public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
    // asked while light spreads, chunks loading included: never create a tile entity here
    TileEntity te;
    if (world instanceof World) {
      World w = (World) world;
      te = w.isBlockLoaded(pos)
          ? w.getChunk(pos).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK) : null;
    } else {
      te = BlockUtils.getTileEntitySafe(world, pos);
    }
    if (te instanceof AbstractTileEntityWayfindingPanel) {
      return ((AbstractTileEntityWayfindingPanel) te).getSign().isLit() ? LIGHT : 0;
    }
    return 0;
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
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  // --- the panel -----------------------------------------------------------------------------

  @Override
  public void onBlockAdded(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    super.onBlockAdded(world, pos, state);
    if (world.isRemote) {
      return;
    }
    EnumFacing facing = state.getValue(FACING);
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof AbstractTileEntityWayfindingPanel
        && !((AbstractTileEntityWayfindingPanel) te).hasOwnSign()) {
      WayfindingPanels.adoptNeighbourSign(world, pos, this, facing,
          (AbstractTileEntityWayfindingPanel) te);
    }
    WayfindingPanels.refresh(world, pos, this, facing);
  }

  /**
   * A cell set over itself facing another way (a {@code /setblock} of the same block) leaves the
   * panel it was in and joins the one it now faces with, as breaking it and placing it would. A
   * module calls this from whatever hears a block's state changed in place.
   */
  protected void onFacingChangedInPlace(World world, BlockPos pos, IBlockState before,
      IBlockState after) {
    EnumFacing was = before.getValue(FACING);
    EnumFacing now = after.getValue(FACING);
    if (was != now) {
      WayfindingPanels.refresh(world, pos, this, was);
      onBlockAdded(world, pos, after);
    }
  }

  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
    super.breakBlock(world, pos, state);
    if (!world.isRemote) {
      WayfindingPanels.refresh(world, pos, this, state.getValue(FACING));
    }
  }

  /**
   * An empty-handed click opens the editor on the cell clicked (the server checks reach to it,
   * and a wide panel's controller may be further off than that); a sneak-click turns the whole
   * panel's arrow to the next direction.
   */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!player.isSneaking()) {
      if (world.isRemote) {
        player.openGui(Csm.instance, GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
      }
      return true;
    }
    if (world.isRemote) {
      return true;
    }
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof AbstractTileEntityWayfindingPanel
        && world.isBlockModifiable(player, pos) && player.capabilities.allowEdit) {
      WayfindingSign sign = ((AbstractTileEntityWayfindingPanel) te).getSign();
      WayfindingArrow next = sign.getArrow().next();
      WayfindingPanels.applyToPanel(world, pos, sign.toBuilder().arrow(next).build());
      player.sendStatusMessage(new TextComponentTranslation("csm.wayfinding.arrow",
          new TextComponentTranslation("gui.csm.wayfinding.arrow." + next.getId())), true);
    }
    return true;
  }
}
