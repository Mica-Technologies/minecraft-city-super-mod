package com.micatechnologies.minecraft.csm.constructionsite;

import com.micatechnologies.minecraft.csm.codeutils.EntityCsmSeat;
import java.util.Random;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A portable toilet you can use: right-click it and you are inside, sitting on the seat, facing
 * the door. One person at a time; sneak to come back out through the door.
 *
 * <p>The sitting is the game's own riding, on an {@link EntityCsmSeat} Core provides. The model
 * (from {@code dev-env-utils/scripts/gen_facilities.py}) has an inside -- walls drawn inside and
 * out, the back of the door, the toilet -- since a player in there sees it; and its blockstate
 * stretches it to about 2.3 blocks, a real unit's height, which a block model cannot be drawn
 * at.</p>
 *
 * <p>It is two blocks, as a door is: the lower draws the whole toilet, the upper ({@link #UPPER})
 * draws nothing and is there to be clicked. The game only tests the block in each cell a look
 * passes through, so without it a look at the upper half of the door -- where a standing player
 * looks -- would hit the empty cell above and miss. Placing the lower places the upper; breaking
 * either takes both, and drops one toilet.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockPortableToilet extends BlockSiteFacingProp {

  /**
   * The upper, invisible half. Stored.
   *
   * @since 1.0
   */
  public static final PropertyBool UPPER = PropertyBool.create("upper");

  private static final AxisAlignedBB LOWER_BOX = BlockSiteProp.box16(0.25, 0, 0.25, 15.75, 16,
      15.75);
  private static final AxisAlignedBB UPPER_BOX = BlockSiteProp.box16(0.25, 0, 0.25, 15.75, 20.8,
      15.75);

  /** The toilet seat's height above the floor, and how far back from the centre it is. */
  private static final double SEAT_Y = 0.1;
  private static final double SEAT_BACK = 0.18;
  /** Places the rider so they sit on the seat rather than float above it. */
  private static final double RIDER_OFFSET = 0.25;

  /**
   * Constructs a {@link BlockPortableToilet}.
   *
   * @since 1.0
   */
  public BlockPortableToilet() {
    super("portable_toilet", Material.WOOD, SoundType.WOOD, "axe", LOWER_BOX);
  }

  @Override
  public String getBlockRegistryName() {
    return "portable_toilet";
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, UPPER);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(UPPER, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(UPPER) ? 4 : 0);
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return state.getValue(UPPER) ? UPPER_BOX : LOWER_BOX;
  }

  /**
   * Needs room for its upper half.
   *
   * @since 1.0
   */
  @Override
  public boolean canPlaceBlockAt(World worldIn, @Nonnull BlockPos pos) {
    return super.canPlaceBlockAt(worldIn, pos)
        && worldIn.getBlockState(pos.up()).getBlock().isReplaceable(worldIn, pos.up());
  }

  @Override
  public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
    worldIn.setBlockState(pos.up(), state.withProperty(UPPER, true), 3);
  }

  /**
   * One half goes when the other does. Removed this way it drops nothing: the half that was
   * broken drops the toilet.
   *
   * @since 1.0
   */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    BlockPos other = state.getValue(UPPER) ? pos.down() : pos.up();
    if (worldIn.getBlockState(other).getBlock() != this) {
      worldIn.setBlockToAir(pos);
    }
  }

  @Override
  @Nonnull
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return Item.getItemFromBlock(this);
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  /**
   * Takes the player inside and sits them down, if nobody is in there.
   *
   * @since 1.0
   */
  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    if (playerIn.isSneaking()) {
      return false;
    }
    if (worldIn.isRemote) {
      return true;
    }
    BlockPos base = state.getValue(UPPER) ? pos.down() : pos;
    EnumFacing front = state.getValue(FACING);
    double cx = base.getX() + 0.5 - front.getXOffset() * SEAT_BACK;
    double cz = base.getZ() + 0.5 - front.getZOffset() * SEAT_BACK;
    BlockPos out = base.offset(front);
    boolean sat = EntityCsmSeat.sit(worldIn, base, cx, base.getY() + SEAT_Y, cz, RIDER_OFFSET,
        front, out.getX() + 0.5, out.getY(), out.getZ() + 0.5, playerIn);
    if (sat) {
      worldIn.playSound(null, base, SoundEvents.BLOCK_WOODEN_DOOR_CLOSE, SoundCategory.BLOCKS,
          1.0F, 1.1F);
    }
    return true;
  }
}
