package com.micatechnologies.minecraft.csm.transit.airport;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformFixture;
import com.micatechnologies.minecraft.csm.transit.platform.PlatformSigns;
import com.micatechnologies.minecraft.csm.transit.platform.TileEntityPlatformSign;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The hanging gate sign: GATE over the gate's letter and number, on both faces, A1 to D20. A click
 * steps the number (1 to {@link FlightSchedule#GATE_NUMBERS} and round), a sneaking click steps
 * the letter (A to D and round); the action bar says the gate. The gate is kept as one value,
 * 1 to 80 (A1 is 1, B1 21), in the platform signs' {@link TileEntityPlatformSign}, and reaches the
 * model as two actual-state properties, {@link #LETTER} and {@link #NUMBER}, each of which swaps
 * the texture of its own cell, so 24 textures make every gate.
 *
 * @since 2026.9
 */
public class BlockGateSign extends BlockPlatformFixture implements ICsmTileEntityProvider {

  /** The gate's letter. */
  public enum Letter implements IStringSerializable {
    A, B, C, D;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase(java.util.Locale.ROOT);
    }
  }

  public static final PropertyEnum<Letter> LETTER = PropertyEnum.create("letter", Letter.class);
  public static final PropertyInteger NUMBER = PropertyInteger.create("number", 1,
      FlightSchedule.GATE_NUMBERS);

  private static final int GATES = Letter.values().length * FlightSchedule.GATE_NUMBERS;

  /**
   * Constructs a gate sign.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockGateSign(String registryName, double[] box) {
    super(registryName, box);
    setDefaultState(getDefaultState().withProperty(LETTER, Letter.A).withProperty(NUMBER, 1));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, LETTER, NUMBER);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    int v = PlatformSigns.valueAt(world, pos, GATES) - 1;
    return state.withProperty(LETTER, Letter.values()[v / FlightSchedule.GATE_NUMBERS])
        .withProperty(NUMBER, v % FlightSchedule.GATE_NUMBERS + 1);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || world.isRemote) {
      return true;
    }
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityPlatformSign)) {
      return true;
    }
    TileEntityPlatformSign sign = (TileEntityPlatformSign) te;
    int v = Math.max(1, Math.min(GATES, sign.getValue())) - 1;
    int letter = v / FlightSchedule.GATE_NUMBERS;
    int number = v % FlightSchedule.GATE_NUMBERS;
    if (player.isSneaking()) {
      letter = (letter + 1) % Letter.values().length;
    } else {
      number = (number + 1) % FlightSchedule.GATE_NUMBERS;
    }
    int next = letter * FlightSchedule.GATE_NUMBERS + number + 1;
    sign.setValue(next);
    world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
        0.3F, 0.8F);
    player.sendStatusMessage(new TextComponentTranslation("csm.transit.gate",
        FlightSchedule.gateName(next)), true);
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityPlatformSign.class;
  }

  @Override
  public String getTileEntityName() {
    return PlatformSigns.TILE_ENTITY_NAME;
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityPlatformSign();
  }
}
