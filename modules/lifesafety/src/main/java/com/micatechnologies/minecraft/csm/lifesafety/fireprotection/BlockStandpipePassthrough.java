package com.micatechnologies.minecraft.csm.lifesafety.fireprotection;

import com.micatechnologies.minecraft.csm.codeutils.CsmExtendedBlockState;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * A standpipe pass-through: a length of standpipe that goes through a wall, floor or ceiling and
 * takes on the look of the block it stands in, so a run passes cleanly through it.
 *
 * <p>Bare, it is an ordinary free-standing standpipe pipe (it is one: every pipe joins it). Dressed,
 * it is that block: its model's own quads, baked into the chunk mesh round a trim plate on each face
 * the pipe leaves through ({@link StandpipePassthroughModels}), opaque and solid like the block,
 * so neighbours cull against it and light stops at it. No renderer, nothing ticks: dressed, it costs
 * what the block it copies costs to draw.</p>
 *
 * <ul>
 *   <li>Right-click holding a block: dressed as that block (used up in survival, given back when
 *   it is taken off or the pass-through is broken).</li>
 *   <li>Right-click with an empty hand: dressed as the wall, floor or ceiling beside it, the first
 *   full block across the pipe's run.</li>
 *   <li>Sneak and right-click with an empty hand: bare again.</li>
 * </ul>
 *
 * <p>Only full, opaque, plain blocks are taken (no tile entity, drawn solid or cutout): what a
 * wall is made of. Stored: {@code covered} (metadata bit 0), which makes the block opaque and
 * solid; the block itself is on {@link TileEntityStandpipePassthrough}.</p>
 *
 * @since 2026.10
 */
public class BlockStandpipePassthrough extends BlockStandpipePipe implements ICsmTileEntityProvider {

  public static final PropertyBool COVERED = PropertyBool.create("covered");

  /** The block a dressed pass-through draws, for the model. */
  public static final IUnlistedProperty<IBlockState> MIMIC = new IUnlistedProperty<IBlockState>() {
    @Override
    public String getName() {
      return "mimic";
    }

    @Override
    public boolean isValid(IBlockState value) {
      return true;
    }

    @Override
    public Class<IBlockState> getType() {
      return IBlockState.class;
    }

    @Override
    public String valueToString(IBlockState value) {
      return String.valueOf(value);
    }
  };

  public BlockStandpipePassthrough(String registryName, float radius) {
    super(registryName, radius);
    setDefaultState(getDefaultState().withProperty(COVERED, false));
  }

  /** Whether a pass-through may be dressed as {@code state}: a plain, full, opaque block. */
  @SuppressWarnings("deprecation")
  public static boolean canMimic(@Nullable IBlockState state) {
    if (state == null) {
      return false;
    }
    Block block = state.getBlock();
    return !(block instanceof BlockStandpipePipe) && !block.hasTileEntity(state)
        && state.isOpaqueCube() && state.isFullCube()
        && state.getRenderType() == EnumBlockRenderType.MODEL
        && (block.canRenderInLayer(state, BlockRenderLayer.SOLID)
        || block.canRenderInLayer(state, BlockRenderLayer.CUTOUT)
        || block.canRenderInLayer(state, BlockRenderLayer.CUTOUT_MIPPED));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmExtendedBlockState(this, new IProperty[]{NORTH, SOUTH, EAST, WEST, UP, DOWN,
        JOINT, FLOOR, CEILING, COVERED}, new IUnlistedProperty[]{MIMIC});
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(COVERED, (meta & 1) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(COVERED) ? 1 : 0;
  }

  @Override
  @Nonnull
  public IBlockState getExtendedState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    if (!(state instanceof IExtendedBlockState) || !state.getValue(COVERED)) {
      return state;
    }
    TileEntity te = world.getTileEntity(pos);
    IBlockState mimic = te instanceof TileEntityStandpipePassthrough
        ? ((TileEntityStandpipePassthrough) te).getMimic() : null;
    return mimic == null ? state : ((IExtendedBlockState) state).withProperty(MIMIC, mimic);
  }

  // --- dressing ------------------------------------------------------------------------------

  @Override
  @SuppressWarnings("deprecation")
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityStandpipePassthrough)) {
      return false;
    }
    TileEntityStandpipePassthrough pass = (TileEntityStandpipePassthrough) te;
    ItemStack held = player.getHeldItem(hand);
    if (held.isEmpty() && player.isSneaking()) {
      if (!world.isRemote && pass.getMimic() != null) {
        giveBack(world, pos, player, pass);
        pass.setMimic(null, false);
        world.setBlockState(pos, state.withProperty(COVERED, false), 3);
      }
      return true;
    }
    IBlockState mimic;
    boolean paid = false;
    if (held.getItem() instanceof ItemBlock) {
      ItemBlock item = (ItemBlock) held.getItem();
      mimic = item.getBlock().getStateFromMeta(item.getMetadata(held.getMetadata()));
      paid = !player.capabilities.isCreativeMode;
    } else if (held.isEmpty()) {
      mimic = neighbour(world, pos);
    } else {
      return false;
    }
    if (!canMimic(mimic)) {
      if (!world.isRemote) {
        player.sendStatusMessage(new TextComponentTranslation(held.isEmpty()
            ? "csm.lifesafety.standpipe_passthrough.no_wall"
            : "csm.lifesafety.standpipe_passthrough.not_block"),
            true);
      }
      return true;
    }
    if (!world.isRemote) {
      giveBack(world, pos, player, pass);
      if (paid) {
        held.shrink(1);
      }
      world.setBlockState(pos, state.withProperty(COVERED, true), 3);
      pass.setMimic(mimic, paid);
    }
    return true;
  }

  /** Hands back the block a survival player paid for this pass-through's look. */
  private static void giveBack(World world, BlockPos pos, @Nullable EntityPlayer player,
      TileEntityStandpipePassthrough pass) {
    IBlockState mimic = pass.getMimic();
    if (mimic == null || !pass.isPaid()) {
      return;
    }
    ItemStack stack = new ItemStack(mimic.getBlock(), 1, mimic.getBlock().damageDropped(mimic));
    if (player == null || !player.inventory.addItemStackToInventory(stack)) {
      spawnAsEntity(world, pos, stack);
    }
  }

  /**
   * The wall, floor or ceiling beside the pass-through: the first full block across the run the
   * pipe makes (any side, where it turns).
   */
  @Nullable
  private IBlockState neighbour(World world, BlockPos pos) {
    IBlockState actual = getActualState(world.getBlockState(pos), world, pos);
    EnumFacing.Axis run = null;
    for (EnumFacing side : EnumFacing.values()) {
      if (actual.getValue(arm(side)) && joins(world, pos, side)) {
        run = run == null || run == side.getAxis() ? side.getAxis() : null;
      }
    }
    for (EnumFacing side : EnumFacing.values()) {
      if (run != null && side.getAxis() == run) {
        continue;
      }
      IBlockState other = world.getBlockState(pos.offset(side));
      if (canMimic(other)) {
        return other;
      }
    }
    return null;
  }

  @Override
  public void breakBlock(World world, BlockPos pos, IBlockState state) {
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityStandpipePassthrough) {
      giveBack(world, pos, null, (TileEntityStandpipePassthrough) te);
    }
    super.breakBlock(world, pos, state);
  }

  // --- a dressed pass-through is the block it copies -----------------------------------------

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return state.getValue(COVERED) ? FULL_BLOCK_AABB
        : super.getBlockBoundingBox(state, source, pos);
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return state.getValue(COVERED);
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return state.getValue(COVERED);
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightOpacity(IBlockState state) {
    return state.getValue(COVERED) ? 255 : 0;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess world, @Nonnull IBlockState state,
      @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
    return state.getValue(COVERED) ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
  }

  /** The pipe and its trim in the cutout pass; the copied block in whichever pass it draws in. */
  @Override
  public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
    return layer == BlockRenderLayer.CUTOUT || layer == BlockRenderLayer.SOLID
        || layer == BlockRenderLayer.CUTOUT_MIPPED;
  }

  // --- the tile entity -------------------------------------------------------------------------

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityStandpipePassthrough.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitystandpipepassthrough";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityStandpipePassthrough();
  }
}
