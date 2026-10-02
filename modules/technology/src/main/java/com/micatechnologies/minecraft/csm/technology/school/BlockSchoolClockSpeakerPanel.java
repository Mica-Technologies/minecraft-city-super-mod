package com.micatechnologies.minecraft.csm.technology.school;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTtsLinkerItem;
import com.micatechnologies.minecraft.csm.technology.TileEntitySpeaker;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A clock/speaker panel, as hangs in a classroom over the door: a long grey panel with two
 * pinstripes, a clock at one end and an octagonal speaker grille at the other, two blocks wide or,
 * upright, two blocks tall with the clock on top.
 *
 * <p>Two blocks, placed and broken as one as a door is. The block placed is the primary half
 * ({@link #SECONDARY} false): a wide panel's clock, with the speaker to the placer's right; an
 * upright panel's speaker, with the clock above it. Each half has its own model, cut at the
 * block line, so each is lit in its own block. The clock half carries a
 * {@link TileEntitySchoolClock} for the hands; the speaker half an ordinary
 * {@link TileEntitySpeaker}, so the TTS Linker and the Bell System Linker take it like any other
 * speaker and a right-click cycles its ambient sound. Only the primary half drops the item.</p>
 *
 * <p>{@link #SECONDARY} is stored, in the bit above the facing.</p>
 *
 * @since 2026.10
 */
public class BlockSchoolClockSpeakerPanel extends BlockSchoolFixture
    implements ICsmTileEntityProvider, ISchoolClock {

  /** Whether this is the half placed second (to the right, or on top). */
  public static final PropertyBool SECONDARY = PropertyBool.create("secondary");

  private static final SchoolClockDial[] NONE = new SchoolClockDial[0];

  private final boolean vertical;
  private final AxisAlignedBB speakerBox;
  private final SchoolClockDial[] dials;

  /**
   * Constructs a panel.
   *
   * @param registryName its registry name
   * @param vertical     whether it stands two blocks tall (clock on top) rather than wide
   * @param clockBox     the clock half's box facing north, in sixteenths
   * @param speakerBox   the speaker half's box facing north, in sixteenths
   * @param dial         the clock's dial, in its half's model frame
   */
  public BlockSchoolClockSpeakerPanel(String registryName, boolean vertical, double[] clockBox,
      double[] speakerBox, SchoolClockDial dial) {
    super(registryName, clockBox);
    this.vertical = vertical;
    this.speakerBox = toBox(speakerBox);
    this.dials = new SchoolClockDial[]{dial};
    setDefaultState(getDefaultState().withProperty(SECONDARY, false));
  }

  // --- the halves --------------------------------------------------------------------------

  /** Whether the half with this metadata is the clock. */
  private boolean isClockHalf(boolean secondary) {
    return secondary == vertical;
  }

  /**
   * Where the other half of the half at {@code pos} is.
   *
   * @param pos       a half's position
   * @param facing    the panel's facing
   * @param secondary whether that half is the secondary one
   *
   * @return the other half's position
   */
  public BlockPos partner(BlockPos pos, EnumFacing facing, boolean secondary) {
    if (vertical) {
      return secondary ? pos.down() : pos.up();
    }
    // The secondary half is to the placer's right: west of a panel facing north.
    return secondary ? pos.offset(facing.rotateY()) : pos.offset(facing.rotateYCCW());
  }

  /**
   * Where the speaker half of the panel that the half at {@code pos} belongs to is.
   *
   * @param pos   a half's position
   * @param state that half's state
   *
   * @return the speaker half's position
   */
  public BlockPos speakerHalf(BlockPos pos, IBlockState state) {
    boolean secondary = state.getValue(SECONDARY);
    return isClockHalf(secondary) ? partner(pos, state.getValue(FACING), secondary) : pos;
  }

  @Override
  public SchoolClockDial[] getClockDials(int meta) {
    return isClockHalf((meta & 4) != 0) ? dials : NONE;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, SECONDARY);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(SECONDARY, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(SECONDARY) ? 4 : 0);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return isClockHalf(state.getValue(SECONDARY)) ? super.getBlockBoundingBox(state, source, pos)
        : speakerBox;
  }

  // --- placing and breaking as one piece ---------------------------------------------------

  @Override
  protected ItemBlock createItemBlock() {
    return new ItemPanel(this);
  }

  /**
   * Places the panel's second half with its first, and refuses a place where the second half
   * would not fit. The block's facing is not known until the placer is, which is why this is the
   * item's job rather than {@code canPlaceBlockAt}'s.
   */
  private static final class ItemPanel extends ItemBlock {

    ItemPanel(Block block) {
      super(block);
    }

    @Override
    public boolean placeBlockAt(@Nonnull ItemStack stack, @Nonnull EntityPlayer player,
        World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ,
        @Nonnull IBlockState newState) {
      BlockSchoolClockSpeakerPanel panel = (BlockSchoolClockSpeakerPanel) block;
      IBlockState first = newState.withProperty(SECONDARY, false);
      BlockPos other = panel.partner(pos, first.getValue(FACING), false);
      if (other.getY() < 0 || other.getY() >= world.getHeight() || !world.isBlockLoaded(other)
          || !world.getBlockState(other).getBlock().isReplaceable(world, other)
          || !player.canPlayerEdit(other, side, stack)) {
        return false;
      }
      if (!super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, first)) {
        return false;
      }
      world.setBlockState(other, first.withProperty(SECONDARY, true), 3);
      return true;
    }
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(SECONDARY, false);
  }

  /** A creative player breaking the secondary half takes the primary with it, dropping nothing. */
  @Override
  public void onBlockHarvested(World world, BlockPos pos, IBlockState state,
      EntityPlayer player) {
    if (state.getValue(SECONDARY) && player.capabilities.isCreativeMode) {
      BlockPos first = partner(pos, state.getValue(FACING), true);
      if (world.getBlockState(first).getBlock() == this) {
        world.setBlockToAir(first);
      }
    }
    super.onBlockHarvested(world, pos, state, player);
  }

  /**
   * The other half goes with this one: the primary half clears the secondary; a secondary half
   * broken on its own breaks the primary, which drops the panel.
   */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    boolean secondary = state.getValue(SECONDARY);
    BlockPos other = partner(pos, state.getValue(FACING), secondary);
    IBlockState otherState = world.getBlockState(other);
    if (otherState.getBlock() == this && otherState.getValue(SECONDARY) != secondary) {
      if (secondary) {
        world.destroyBlock(other, true);
      } else {
        world.setBlockToAir(other);
      }
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  @Nonnull
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return state.getValue(SECONDARY) ? Items.AIR : super.getItemDropped(state, rand, fortune);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(@Nonnull IBlockState state) {
    return EnumPushReaction.BLOCK;
  }

  // --- the speaker half ----------------------------------------------------------------------

  /**
   * On the speaker half, as on every speaker: stands aside for the TTS Linker, and otherwise
   * cycles the ambient sound. The clock half does nothing.
   */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (isClockHalf(state.getValue(SECONDARY))) {
      return false;
    }
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    ItemStack held = player.getHeldItem(hand);
    if (!held.isEmpty() && held.getItem() instanceof ICsmTtsLinkerItem) {
      return false;
    }
    if (world.isRemote) {
      return true;
    }
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntitySpeaker) {
      String name = ((TileEntitySpeaker) te).cycleAmbientSound();
      player.sendMessage(new TextComponentString("§bSpeaker ambient: §f" + name));
    }
    return true;
  }

  // --- tile entities -------------------------------------------------------------------------

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return true;
  }

  /**
   * Registered under the clock's name; the speaker half's {@link TileEntitySpeaker} is
   * registered by the tab's speakers.
   */
  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntitySchoolClock.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityschoolclock";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return isClockHalf((meta & 4) != 0) ? new TileEntitySchoolClock() : new TileEntitySpeaker();
  }
}
