package com.micatechnologies.minecraft.csm.healthcare.systems;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareSounds;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The medical gas master alarm panel ({@code medical_gas_master_alarm}): the wall panel at the
 * nurses' station that shows each gas's pressure as a lamp, NORMAL in green, and goes to alarm
 * on a redstone signal (the oxygen line's LOW lamp flashing red, an animated texture), sounding
 * its buzzer ({@link HealthcareSounds#MEDICAL_GAS_ALARM}) until it is silenced.
 *
 * <p><b>Metadata</b> follows the module's lit pieces, {@code facing * 4 + state}, with the
 * facing's horizontal index (S 0, W 1, N 2, E 3; the way out of the wall) and the state's two
 * bits: bit 0 in alarm (from redstone; a {@code /setblock} with it is put right at once), bit 1
 * the buzzer silenced (the amber lamp by the silence key). A click toggles the buzzer: silenced
 * it stays silenced until clicked again, so a panel can be left quiet. The buzzer sounds as the
 * alarm starts and every {@link #BUZZ_TICKS} ticks while it lasts, from a scheduled block tick:
 * no tile entity, nothing ticking while the gases are normal.</p>
 *
 * @since 2026.10
 */
public class BlockMedicalGasAlarm extends AbstractBlockRotatableNSEW {

  /** Bit 0 alarm, bit 1 buzzer silenced. */
  public static final PropertyInteger STATE = PropertyInteger.create("state", 0, 3);

  public static final int ALARM = 1;
  public static final int SILENCED = 2;
  /** From one buzz to the next, in ticks (the buzz is 2 s). */
  public static final int BUZZ_TICKS = 50;

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB box;

  /**
   * Constructs the panel.
   *
   * @param registryName its registry name
   * @param box          its box facing north (on a wall at +Z), in sixteenths
   */
  public BlockMedicalGasAlarm(String registryName, int[] box) {
    super(stash(registryName), HealthcareMaterial.PLASTIC.getSound(), "pickaxe", 0, 1.5F, 3.0F,
        0.0F, 0, false);
    this.registryName = registryName;
    this.box = new AxisAlignedBB(box[0] / 16.0, box[1] / 16.0, box[2] / 16.0, box[3] / 16.0,
        box[4] / 16.0, box[5] / 16.0);
    setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(STATE, 0));
    PENDING.remove();
  }

  private static net.minecraft.block.material.Material stash(String registryName) {
    PENDING.set(registryName);
    return HealthcareMaterial.PLASTIC.getMaterial();
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, STATE);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta >> 2 & 3))
        .withProperty(STATE, meta & 3);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() * 4 + state.getValue(STATE);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing front = facing.getAxis().isHorizontal()
        ? facing : placer.getHorizontalFacing().getOpposite();
    return getDefaultState().withProperty(FACING, front);
  }

  // --- the alarm -----------------------------------------------------------------------------

  private void follow(World world, BlockPos pos, IBlockState state) {
    int s = state.getValue(STATE);
    boolean alarm = world.isBlockPowered(pos);
    int want = (s & SILENCED) | (alarm ? ALARM : 0);
    if (want == s) {
      return;
    }
    world.setBlockState(pos, state.withProperty(STATE, want), 3);
    if (alarm && (s & ALARM) == 0 && (want & SILENCED) == 0) {
      buzz(world, pos);
      if (!world.isBlockTickPending(pos, this)) {
        world.scheduleBlockUpdate(pos, this, BUZZ_TICKS, 0);
      }
    }
  }

  private static void buzz(World world, BlockPos pos) {
    SoundEvent buzz = HealthcareSounds.MEDICAL_GAS_ALARM.getSoundEvent();
    if (buzz != null) {
      world.playSound(null, pos, buzz, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }
  }

  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    if (world.isRemote || state.getValue(STATE) != ALARM) {
      return;
    }
    buzz(world, pos);
    world.scheduleBlockUpdate(pos, this, BUZZ_TICKS, 0);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    if (!world.isRemote) {
      follow(world, pos, state);
    }
  }

  @Override
  public void onBlockAdded(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    super.onBlockAdded(world, pos, state);
    if (!world.isRemote) {
      follow(world, pos, state);
    }
  }

  /** A click silences the buzzer, or turns it back on. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking() && !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      int s = state.getValue(STATE) ^ SILENCED;
      world.setBlockState(pos, state.withProperty(STATE, s), 3);
      boolean silenced = (s & SILENCED) != 0;
      player.sendStatusMessage(new TextComponentTranslation(silenced
          ? "csm.healthcare.gasalarm.muted" : "csm.healthcare.gasalarm.unmuted"), true);
      if (!silenced && (s & ALARM) != 0 && !world.isBlockTickPending(pos, this)) {
        buzz(world, pos);
        world.scheduleBlockUpdate(pos, this, BUZZ_TICKS, 0);
      }
    }
    return true;
  }

  // --- shape ---------------------------------------------------------------------------------

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return box;
  }

  @Nullable
  @Override
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
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
    return true;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
