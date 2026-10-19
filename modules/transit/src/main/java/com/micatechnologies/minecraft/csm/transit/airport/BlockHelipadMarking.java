package com.micatechnologies.minecraft.csm.transit.airport;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A piece of a helipad's marking, laid over the deck one pixel thick as tactile paving is: the
 * plain deck and its perimeter stripe ({@code airport_helipad_surface}), the circle
 * ({@code airport_helipad_circle}), the H in its square, plain or a hospital's red
 * ({@code airport_helipad_h}, {@code airport_helipad_h_red}), and the mass and size boxes
 * ({@code airport_helipad_box}). One class, constructed by registry name.
 *
 * <p>The only state is {@link #PIECE}, which is the metadata: which picture of the block the cell
 * shows and how it is turned, so a generator lays a whole pad with {@code /setblock} alone (or
 * {@code /csmtransit helipad}). {@link HelipadLayout} says which block and piece each cell of the
 * marking is. Placed by hand, a piece takes the turn the player faces; a click with an empty hand
 * steps it on, a sneaking click back.</p>
 *
 * @since 2026.10
 */
public class BlockHelipadMarking extends AbstractBlock {

  /** The piece: {@code picture * 4 + quarter turns clockwise}, as {@link HelipadLayout} says. */
  public static final PropertyInteger PIECE = PropertyInteger.create("piece", 0, 15);

  private static final AxisAlignedBB MARKING = new AxisAlignedBB(0, 0, 0, 1, 1 / 16.0, 1);

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a helipad marking block.
   *
   * @param registryName its registry name
   */
  public BlockHelipadMarking(String registryName) {
    super(stash(registryName), SoundType.STONE, "pickaxe", 0, 0.8F, 2F, 0F, 0);
    this.registryName = registryName;
    setDefaultState(blockState.getBaseState().withProperty(PIECE, 0));
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.ROCK;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, PIECE);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(PIECE, meta & 15);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(PIECE);
  }

  /**
   * The quarter turn a piece placed by hand takes: the way the player faces, so a box's legend
   * reads the right way up from where they stand.
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(PIECE, turnOf(placer.getHorizontalFacing()));
  }

  /**
   * The quarter turns clockwise from north to a facing: north 0, east 1, south 2, west 3.
   *
   * @param facing a horizontal facing
   * @return its turn
   */
  public static int turnOf(EnumFacing facing) {
    return (facing.getHorizontalIndex() + 2) & 3;
  }

  /** A click with an empty hand steps the piece on, a sneaking click back. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      int piece = (state.getValue(PIECE) + (player.isSneaking() ? 15 : 1)) & 15;
      world.setBlockState(pos, state.withProperty(PIECE, piece), 2);
      player.sendStatusMessage(new TextComponentTranslation("csm.transit.helipad_piece",
          String.valueOf(piece)), true);
    }
    return true;
  }

  // --- support --------------------------------------------------------------------------------

  private static boolean supported(World world, BlockPos pos) {
    BlockPos below = pos.down();
    return world.getBlockState(below).isSideSolid(world, below, EnumFacing.UP);
  }

  @Override
  public boolean canPlaceBlockAt(World worldIn, @Nonnull BlockPos pos) {
    return super.canPlaceBlockAt(worldIn, pos) && supported(worldIn, pos);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    if (!worldIn.isRemote && !supported(worldIn, pos)) {
      dropBlockAsItem(worldIn, pos, state, 0);
      worldIn.setBlockState(pos, Blocks.AIR.getDefaultState());
    }
  }

  // --- shape ----------------------------------------------------------------------------------

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return MARKING;
  }

  /** Solid underneath, so it sits flush on the deck it covers; nothing else is a face. */
  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
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
    return BlockRenderLayer.SOLID;
  }
}
