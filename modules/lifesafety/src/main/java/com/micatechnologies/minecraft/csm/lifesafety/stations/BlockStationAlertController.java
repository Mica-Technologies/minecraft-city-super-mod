package com.micatechnologies.minecraft.csm.lifesafety.stations;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.lifesafety.ItemFireAlarmLinker;
import com.micatechnologies.minecraft.csm.lifesafety.fireprotection.BlockFireProtectionProp;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The station alerting controller, the cabinet by the watch desk that sounds a call through the
 * station. Right-click chooses the zone (engine, ladder, medic, battalion, all call);
 * sneak-right-click dispatches it, or resets an alert in progress; a redstone signal coming on
 * dispatches too, so a button, a daylight sensor or another mod's dispatch can start a call. Link
 * its speakers, lights, relays and bay clearance lights to it with the fire alarm linker: click the
 * controller, then each device. See {@link TileEntityStationAlertController} for the sequence.
 *
 * <p>Stored in metadata: the facing, {@link #ACTIVE} (the cabinet's ALERT lamp) and
 * {@link #POWERED} (so only the rising edge of a signal dispatches).</p>
 *
 * @since 2026.9
 */
public class BlockStationAlertController extends BlockFireProtectionProp implements
    ICsmTileEntityProvider {

  public static final PropertyBool ACTIVE = PropertyBool.create("active");
  public static final PropertyBool POWERED = PropertyBool.create("powered");

  public BlockStationAlertController(String registryName, int[] box) {
    super(registryName, box, true);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(ACTIVE, false).withProperty(POWERED, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, ACTIVE, POWERED);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(ACTIVE) ? 4 : 0)
        | (state.getValue(POWERED) ? 8 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(ACTIVE, (meta & 4) != 0)
        .withProperty(POWERED, (meta & 8) != 0);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    // The linker selects the controller itself; leave the click to it.
    if (player.getHeldItemMainhand().getItem() instanceof ItemFireAlarmLinker) {
      return false;
    }
    if (world.isRemote) {
      return true;
    }
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityStationAlertController)) {
      return true;
    }
    TileEntityStationAlertController controller = (TileEntityStationAlertController) te;
    if (player.isSneaking()) {
      if (controller.isAlerting()) {
        controller.reset();
        player.sendMessage(new TextComponentTranslation("csm.lifesafety.station.reset"));
      } else {
        dispatch(controller, player);
      }
    } else {
      TileEntityStationAlertController.Zone zone = controller.cycleZone();
      player.sendMessage(new TextComponentTranslation("csm.lifesafety.station.zone",
          new TextComponentTranslation("csm.lifesafety.station.zone." + zone.name().toLowerCase()),
          controller.getDeviceCount()));
    }
    return true;
  }

  private static void dispatch(TileEntityStationAlertController controller,
      @Nullable EntityPlayer player) {
    controller.dispatch();
    if (player != null) {
      player.sendMessage(new TextComponentTranslation("csm.lifesafety.station.dispatch",
          new TextComponentTranslation(
              "csm.lifesafety.station.zone." + controller.getZone().name().toLowerCase())));
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    if (world.isRemote) {
      return;
    }
    boolean powered = world.isBlockPowered(pos);
    if (powered != state.getValue(POWERED)) {
      world.setBlockState(pos, state.withProperty(POWERED, powered), 2);
      TileEntity te = world.getTileEntity(pos);
      if (powered && te instanceof TileEntityStationAlertController) {
        dispatch((TileEntityStationAlertController) te, null);
      }
    }
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityStationAlertController.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitystationalertcontroller";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityStationAlertController();
  }
}
