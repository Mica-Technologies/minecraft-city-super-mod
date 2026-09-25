package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A rocker light switch on the wall. It is a lever: right-click flips it, and while it is on
 * ({@link #POWERED}, stored) it gives redstone power to the blocks round it and strong power to
 * the wall behind it, so it can switch this module's lamps, lights and ceiling fans (which follow
 * redstone), a redstone lamp, a door, or anything else a lever can.
 *
 * <p>It can also be linked to one block up to {@value SwitchLinks#MAX_DISTANCE} blocks away:
 * right-click that block with the switch in hand, then place the switch ({@link ItemLightSwitch},
 * {@link SwitchLinks}). Switched on, it then powers the linked block too, through a relay it
 * puts beside it; switched off or broken, it takes the relay away. The link is kept by a
 * {@link TileEntityLightSwitch}; a sneak-right-click with an empty hand says what it is linked
 * to.</p>
 *
 * @since 2026.9
 */
public class BlockLightSwitch extends BlockResidentialFurniture
    implements ICsmTileEntityProvider {

  /** Whether the switch is on. */
  public static final PropertyBool POWERED = PropertyBool.create("powered");

  /**
   * Constructs a light switch.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths
   */
  public BlockLightSwitch(String registryName, int[] box) {
    super(registryName, box, Material.CIRCUITS, SoundType.STONE, 0.5F);
    setDefaultState(getDefaultState().withProperty(POWERED, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, POWERED);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(POWERED, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(POWERED) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(POWERED, false);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      // Only reached with both hands empty: say what the switch is linked to.
      if (!world.isRemote) {
        SwitchLinks.describe(world, pos, player);
      }
      return true;
    }
    if (!world.isRemote) {
      boolean on = !state.getValue(POWERED);
      world.setBlockState(pos, state.withProperty(POWERED, on), 3);
      world.playSound(null, pos, on ? SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON
          : SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF, SoundCategory.BLOCKS, 0.3F, 1.4F);
      notifyPowered(world, pos, state);
      if (on) {
        SwitchLinks.powerOn(world, pos, player);
      } else {
        SwitchLinks.powerOff(world, pos);
      }
    }
    return true;
  }

  @Override
  protected ItemBlock createItemBlock() {
    return new ItemLightSwitch(this);
  }

  /** The link the item carried goes to the placed switch. */
  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    if (!world.isRemote) {
      SwitchLinks.placed(world, pos, stack,
          placer instanceof EntityPlayer ? (EntityPlayer) placer : null);
    }
  }

  /** Tells the blocks round the switch, and round the wall it is on, that its power changed. */
  static void notifyPowered(World world, BlockPos pos, IBlockState state) {
    world.notifyNeighborsOfStateChange(pos, state.getBlock(), false);
    world.notifyNeighborsOfStateChange(pos.offset(state.getValue(FACING).getOpposite()),
        state.getBlock(), false);
  }

  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    if (!world.isRemote) {
      SwitchLinks.powerOff(world, pos);
    }
    super.breakBlock(world, pos, state);
    if (state.getValue(POWERED)) {
      notifyPowered(world, pos, state);
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canProvidePower(@Nonnull IBlockState state) {
    return true;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getWeakPower(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
    return state.getValue(POWERED) ? 15 : 0;
  }

  /** Strong power into the wall it is on, as a lever's. */
  @Override
  @SuppressWarnings("deprecation")
  public int getStrongPower(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
    return state.getValue(POWERED) && state.getValue(FACING) == side ? 15 : 0;
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

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityLightSwitch.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitylightswitch";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityLightSwitch();
  }
}
