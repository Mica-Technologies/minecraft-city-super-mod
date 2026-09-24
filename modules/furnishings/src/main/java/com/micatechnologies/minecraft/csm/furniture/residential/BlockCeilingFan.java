package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A ceiling fan with a light kit, hung from the ceiling at the top of its block, run by
 * redstone: powered, its light is on and its blades turn; unpowered, both are off, so a light
 * switch runs it (on the wall beside it, or linked to it from anywhere near). Between changes of
 * power a right-click steps it through off, fan, fan and light, light only, and off again. While the fan
 * runs ({@link #FAN}) its blades are left out of the baked model and
 * {@code TileEntityCeilingFanRenderer} draws them turning; while the light is on ({@link #LIGHT})
 * the glass glows and it gives light.
 *
 * <p>{@link #LIGHT} and {@link #FAN} are stored in the two bits above the facing; whether it was
 * last powered is its {@link TileEntityCeilingFan}'s.</p>
 *
 * @since 2026.9
 */
public class BlockCeilingFan extends BlockResidentialFurniture
    implements ICsmTileEntityProvider, ISwitchable {

  /** Whether the light is on. */
  public static final PropertyBool LIGHT = PropertyBool.create("light");
  /** Whether the fan is running. */
  public static final PropertyBool FAN = PropertyBool.create("fan");

  /**
   * Constructs a ceiling fan.
   *
   * @param registryName its registry name, ending in its blades' finish
   * @param box          its box facing north, in sixteenths
   */
  public BlockCeilingFan(String registryName, int[] box) {
    super(registryName, box, false);
    setDefaultState(getDefaultState().withProperty(LIGHT, false).withProperty(FAN, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, LIGHT, FAN);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(LIGHT, (meta & 4) != 0)
        .withProperty(FAN, (meta & 8) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(LIGHT) ? 4 : 0)
        | (state.getValue(FAN) ? 8 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(LIGHT, false).withProperty(FAN, false);
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return state.getValue(LIGHT) ? 14 : 0;
  }

  @Override
  public int getLightValue(@Nonnull IBlockState state, IBlockAccess world,
      @Nonnull BlockPos pos) {
    return getLightValue(state);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      boolean fan = state.getValue(FAN);
      boolean light = state.getValue(LIGHT);
      // off -> fan -> fan and light -> light -> off
      boolean nextFan = !fan && !light || fan && !light;
      boolean nextLight = fan;
      world.setBlockState(pos, state.withProperty(FAN, nextFan).withProperty(LIGHT, nextLight),
          3);
      world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.25F,
          nextFan || nextLight ? 0.9F : 0.75F);
    }
    return true;
  }

  /** A change of redstone power turns fan and light both on, or both off. */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, block, fromPos);
    if (world.isRemote) {
      return;
    }
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityCeilingFan)) {
      return;
    }
    TileEntityCeilingFan fan = (TileEntityCeilingFan) te;
    boolean powered = world.isBlockPowered(pos);
    if (powered != fan.wasPowered()) {
      fan.setPowered(powered);
      world.setBlockState(pos, state.withProperty(FAN, powered).withProperty(LIGHT, powered), 3);
    }
  }

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityCeilingFan.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityceilingfan";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityCeilingFan();
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
