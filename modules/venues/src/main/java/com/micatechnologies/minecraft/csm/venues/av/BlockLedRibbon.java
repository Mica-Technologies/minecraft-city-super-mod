package com.micatechnologies.minecraft.csm.venues.av;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A straight length of LED ribbon board ({@code led_ribbon_board}): a 1-block fascia band on the
 * back of its cell, facing out over the floor, its LED face playing one of four patterns. It has
 * no tile entity and no renderer: the pattern is an animated texture.
 *
 * <p>One 64 px texture holds all four patterns, each four blocks long, and a block shows the
 * quarter its <b>phase</b> picks: its place along the band (actual state, from its position), so
 * lengths side by side play one sweep that runs four blocks before it repeats, and every block
 * of every band keeps time with every other.</p>
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), the way the LED face looks; bits
 * 2-3 the pattern ({@link RibbonPattern}: 0 navy and ice blue, 1 crimson and gold, 2 green and
 * white, 3 the white crawl). A sneak-click with an empty hand steps the pattern.</p>
 *
 * @since 2026.10
 */
public class BlockLedRibbon extends BlockVenue {

  /** The pattern. Stored, bits 2-3. */
  public static final PropertyEnum<RibbonPattern> PATTERN = PropertyEnum.create("pattern",
      RibbonPattern.class);
  /** Which quarter of the four-block pattern this length shows. Actual. */
  public static final PropertyInteger PHASE = PropertyInteger.create("phase", 0, 3);

  /** The band: the back 3 px of the cell, full height. */
  private static final AxisAlignedBB BAND = new AxisAlignedBB(0, 0, 13 / 16.0, 1, 1, 1);

  /**
   * Constructs a straight.
   *
   * @param registryName its registry name
   */
  public BlockLedRibbon(String registryName) {
    super(registryName, Material.IRON, SoundType.METAL, 1.5F);
    setDefaultState(defaultState());
  }

  /** The default state; the text piece has no pattern. */
  protected IBlockState defaultState() {
    return blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(PATTERN, RibbonPattern.TEAM_NAVY).withProperty(PHASE, 0);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, PATTERN, PHASE);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(PATTERN, RibbonPattern.of(meta >> 2));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | state.getValue(PATTERN).ordinal() << 2;
  }

  /**
   * The phase of a band facing {@code facing} at {@code pos}: its place along the band, counted
   * to the right of someone looking at the LED face, every four blocks.
   */
  public static int phase(BlockPos pos, EnumFacing facing) {
    EnumFacing right = facing.rotateYCCW();
    return Math.floorMod(pos.getX() * right.getXOffset() + pos.getZ() * right.getZOffset(), 4);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state.withProperty(PHASE, phase(pos, state.getValue(FACING)));
  }

  /** The LED face looks back at the placer. */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!player.isSneaking() || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      RibbonPattern next = RibbonPattern.of(state.getValue(PATTERN).ordinal() + 1);
      world.setBlockState(pos, state.withProperty(PATTERN, next), 3);
      player.sendStatusMessage(new TextComponentTranslation("csm.venues.ribbon.pattern",
          new TextComponentTranslation("csm.venues.ribbon.pattern." + next.ordinal())), true);
    }
    return true;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    // in the model's frame: the base class turns it to the facing
    return BAND;
  }
}
