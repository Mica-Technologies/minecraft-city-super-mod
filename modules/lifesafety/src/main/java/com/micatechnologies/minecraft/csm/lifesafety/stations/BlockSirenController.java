package com.micatechnologies.minecraft.csm.lifesafety.stations;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.lifesafety.ItemFireAlarmLinker;
import com.micatechnologies.minecraft.csm.lifesafety.fireprotection.BlockFireProtectionProp;
import java.util.Locale;
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
 * The outdoor warning siren controller. Right-click chooses: alert, attack, fire, test, cancel,
 * or the weekly test switch; sneak-right-click carries the choice out on every linked siren. A
 * redstone signal coming on does the same, so a weather station, a button in the dispatch centre
 * or another mod can sound the sirens. Link sirens to it with the fire alarm linker.
 *
 * <p>Stored in metadata: the facing and {@link #POWERED}.</p>
 *
 * @since 2026.9
 */
public class BlockSirenController extends BlockFireProtectionProp implements
    ICsmTileEntityProvider, ILinkedDeviceController.ControllerBlock {

  public static final PropertyBool POWERED = PropertyBool.create("powered");

  public BlockSirenController(String registryName, int[] box) {
    super(registryName, box, true);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(POWERED, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, POWERED);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(POWERED) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(POWERED, (meta & 4) != 0);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    if (player.getHeldItemMainhand().getItem() instanceof ItemFireAlarmLinker) {
      return false;
    }
    if (world.isRemote) {
      return true;
    }
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntitySirenController)) {
      return true;
    }
    TileEntitySirenController controller = (TileEntitySirenController) te;
    if (player.isSneaking()) {
      controller.activate();
      if (controller.getChoice() == TileEntitySirenController.Choice.WEEKLY_TEST) {
        player.sendMessage(new TextComponentTranslation(controller.isWeekly()
            ? "csm.lifesafety.siren.weekly_on" : "csm.lifesafety.siren.weekly_off"));
      } else {
        player.sendMessage(new TextComponentTranslation("csm.lifesafety.siren.done",
            choiceName(controller.getChoice()), controller.getSirenCount()));
      }
    } else {
      TileEntitySirenController.Choice choice = controller.cycle();
      player.sendMessage(new TextComponentTranslation("csm.lifesafety.siren.choice",
          choiceName(choice), controller.getSirenCount()));
    }
    return true;
  }

  private static TextComponentTranslation choiceName(TileEntitySirenController.Choice choice) {
    return new TextComponentTranslation(
        "csm.lifesafety.siren.choice." + choice.name().toLowerCase(Locale.ROOT));
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
      if (powered && te instanceof TileEntitySirenController) {
        ((TileEntitySirenController) te).activate();
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
    return TileEntitySirenController.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitysirencontroller";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntitySirenController();
  }
}
