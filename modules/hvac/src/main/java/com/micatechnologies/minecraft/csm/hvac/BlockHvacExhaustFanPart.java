package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A cell of a {@link BlockHvacLabExhaustFan} other than its master ({@code hvac_exhaust_fan_part},
 * hidden).
 *
 * <p>Metadata: bits 0-1 the unit's facing (S 0, W 1, N 2, E 3), bits 2-3 what the cell draws
 * ({@link Piece}: nothing, a block of the nozzle, or the nozzle's top in its windband). Only the
 * cells over the master draw anything, each a whole octagon centred on the corner the unit's four
 * columns share, turned by the facing; the master writes both when it fills its footprint.
 * Everything else comes from the master: this cell's share of the unit's boxes for collision,
 * the unit's outline, its item when picked, the unit itself when broken, and a click (so the
 * linker links the fan from any cell).</p>
 *
 * @author Mica Technologies
 * @since 2026.10
 */
public class BlockHvacExhaustFanPart extends AbstractBlock {

  /** The one registry name every part shares. */
  public static final String REGISTRY_NAME = "hvac_exhaust_fan_part";

  /** What a part draws. */
  public enum Piece implements IStringSerializable {
    NONE, STACK, TOP;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase(java.util.Locale.ROOT);
    }
  }

  public static final PropertyDirection FACING = BlockHorizontal.FACING;
  public static final PropertyEnum<Piece> PIECE = PropertyEnum.create("piece", Piece.class);

  private static final AxisAlignedBB ORPHAN_BOX = new AxisAlignedBB(0, 0, 0, 1, 0.0625, 1);

  public BlockHvacExhaustFanPart() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 3F, 15F, 0F, 0);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(PIECE, Piece.NONE));
  }

  /**
   * {@link #REGISTRY_NAME}, spelled out: the source-reading tools find a block's registry name
   * from a literal returned here.
   */
  @Override
  public String getBlockRegistryName() {
    return "hvac_exhaust_fan_part";
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, PIECE);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    int piece = Math.min((meta >> 2) & 3, Piece.values().length - 1);
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(PIECE, Piece.values()[piece]);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | state.getValue(PIECE).ordinal() << 2;
  }

  // --- shape ---------------------------------------------------------------------------------

  /** This cell's share of the unit's boxes, or a thin slab for an orphan. */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    BlockPos master = BlockHvacLabExhaustFan.findMaster(source, pos);
    if (master == null) {
      return ORPHAN_BOX;
    }
    AxisAlignedBB cell = new AxisAlignedBB(pos);
    AxisAlignedBB share = null;
    for (AxisAlignedBB b : BlockHvacLabExhaustFan.worldBoxes(master,
        source.getBlockState(master).getValue(FACING))) {
      if (b.intersects(cell)) {
        AxisAlignedBB c = b.intersect(cell);
        share = share == null ? c : share.union(c);
      }
    }
    return share == null ? ORPHAN_BOX : share.offset(-pos.getX(), -pos.getY(), -pos.getZ());
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return getBlockBoundingBox(state, source, pos);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos) {
    BlockPos master = BlockHvacLabExhaustFan.findMaster(world, pos);
    return master == null ? ORPHAN_BOX.offset(pos)
        : BlockHvacLabExhaustFan.worldOutline(master, world.getBlockState(master)
            .getValue(FACING));
  }

  // --- breaking ------------------------------------------------------------------------------

  /** Breaking a part breaks the unit from its master, so the unit's one item drops. */
  @Override
  public boolean removedByPlayer(@Nonnull IBlockState state, World world, @Nonnull BlockPos pos,
      @Nonnull EntityPlayer player, boolean willHarvest) {
    BlockPos master = BlockHvacLabExhaustFan.findMaster(world, pos);
    if (master != null && !world.isRemote) {
      world.destroyBlock(master, !player.capabilities.isCreativeMode);
    }
    return super.removedByPlayer(state, world, pos, player, willHarvest);
  }

  /**
   * Removed some other way (a command, an explosion): the unit goes too, with no drop. The cell is
   * already gone from the world, so the master is looked for at every place the facing allows.
   */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
    if (!world.isRemote && !BlockHvacLabExhaustFan.DEMOLISHING.get()) {
      EnumFacing facing = state.getValue(FACING);
      for (int[] c : BlockHvacLabExhaustFan.CELLS) {
        BlockPos m = pos.subtract(BlockHvacLabExhaustFan.turn(c[0], c[1], c[2], facing));
        IBlockState s = world.getBlockState(m);
        if (s.getBlock() instanceof BlockHvacLabExhaustFan && s.getValue(FACING) == facing) {
          world.setBlockToAir(m);
          break;
        }
      }
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  @Nonnull
  public Item getItemDropped(@Nonnull IBlockState state, @Nonnull Random rand, int fortune) {
    return Items.AIR;
  }

  @Override
  @Nonnull
  public ItemStack getPickBlock(@Nonnull IBlockState state, RayTraceResult target,
      @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player) {
    BlockPos master = BlockHvacLabExhaustFan.findMaster(world, pos);
    return master == null ? ItemStack.EMPTY
        : new ItemStack(world.getBlockState(master).getBlock());
  }

  /** A click anywhere on the unit is a click on the master. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    BlockPos master = BlockHvacLabExhaustFan.findMaster(world, pos);
    if (master == null) {
      return false;
    }
    IBlockState s = world.getBlockState(master);
    return s.getBlock().onBlockActivated(world, master, s, player, hand, facing, hitX, hitY,
        hitZ);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(@Nonnull IBlockState state) {
    return EnumPushReaction.BLOCK;
  }

  /** Only the cells over the master draw: the nozzle and its windband. */
  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumBlockRenderType getRenderType(IBlockState state) {
    return state.getValue(PIECE) == Piece.NONE ? EnumBlockRenderType.INVISIBLE
        : EnumBlockRenderType.MODEL;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess world, @Nonnull IBlockState state,
      @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
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
}
