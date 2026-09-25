package com.micatechnologies.minecraft.csm.transit.platform;

import com.micatechnologies.minecraft.csm.transit.TransitSounds;
import com.micatechnologies.minecraft.csm.transit.fare.ItemFareTicket;
import com.micatechnologies.minecraft.csm.transit.fare.ItemTransitCard;
import java.util.Random;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A tap validator on a post, for a platform or a station with no gates: hold a fare to it and
 * it takes it as the fare gates do -- a {@link ItemFareTicket} is used up, a
 * {@link ItemTransitCard} gives up one trip -- and shows a green tick; a card with no trips left,
 * or anything else, gets a red cross. The screen holds for {@link #HOLD_TICKS} and goes back to
 * idle. {@link #LIGHT} is stored in the two bits above the facing: 0 idle, 1 accepted,
 * 2 refused; the blockstate swaps the screen's texture for it.
 *
 * @since 2026.9
 */
public class BlockPlatformValidator extends BlockPlatformFixture {

  /** The screen: 0 idle, 1 accepted, 2 refused. */
  public static final PropertyInteger LIGHT = PropertyInteger.create("light", 0, 2);

  /** How long the tick or the cross stays up. */
  private static final int HOLD_TICKS = 30;

  /**
   * Constructs a validator.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockPlatformValidator(String registryName, double[] box) {
    super(registryName, box);
    setDefaultState(getDefaultState().withProperty(LIGHT, 0));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, LIGHT);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(LIGHT, Math.min(2, (meta >> 2) & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(LIGHT) << 2);
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return state.getValue(LIGHT) != 0 ? 5 : 0;
  }

  @Override
  public int getLightValue(@Nonnull IBlockState state, IBlockAccess world,
      @Nonnull BlockPos pos) {
    return getLightValue(state);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    if (world.isRemote) {
      return true;
    }
    ItemStack held = player.getHeldItemMainhand();
    boolean accepted = false;
    TextComponentTranslation message;
    if (held.getItem() instanceof ItemFareTicket) {
      held.shrink(1);
      accepted = true;
      message = new TextComponentTranslation("csm.transit.validator.ticket");
    } else if (held.getItem() instanceof ItemTransitCard) {
      int balance = ItemTransitCard.getBalance(held);
      if (balance > 0 && ItemTransitCard.consumeTrip(held)) {
        accepted = true;
        message = new TextComponentTranslation("csm.transit.validator.card",
            String.valueOf(balance - 1));
      } else {
        message = new TextComponentTranslation("csm.transit.validator.empty");
      }
    } else {
      message = new TextComponentTranslation("csm.transit.validator.none");
    }
    player.sendStatusMessage(message, true);
    SoundEvent sound = (accepted ? TransitSounds.VALIDATOR_ACCEPT : TransitSounds.VALIDATOR_DENY)
        .getSoundEvent();
    if (sound != null) {
      world.playSound(null, pos, sound, SoundCategory.BLOCKS, 0.7F, 1.0F);
    }
    world.setBlockState(pos, state.withProperty(LIGHT, accepted ? 1 : 2), 3);
    world.scheduleUpdate(pos, this, HOLD_TICKS);
    return true;
  }

  /** The tick or the cross has been up long enough: back to idle. */
  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    if (!world.isRemote && state.getValue(LIGHT) != 0) {
      world.setBlockState(pos, state.withProperty(LIGHT, 0), 3);
    }
  }
}
