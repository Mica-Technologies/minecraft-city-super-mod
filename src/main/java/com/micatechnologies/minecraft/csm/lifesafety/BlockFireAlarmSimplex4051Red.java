package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockFireAlarmSimplex4051Red extends AbstractBlockFireAlarmSounder
    implements ICsmTileEntityProvider, IStrobeBlock {
  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityFireAlarmStrobe.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityfirealarmstrobe";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityFireAlarmStrobe();
  }

  @Override
  public float[] getStrobeLensFrom() {
    return new float[]{4.25f, 2.25f, 14f};
  }

  @Override
  public float[] getStrobeLensTo() {
    return new float[]{11.75f, 5f, 15f};
  }
  @Override
  public boolean isRedSlowToggleStrobe() {
    return true;
  }


  public static final PropertyInteger SOUND = PropertyInteger.create("sound", 0, 1);
  public static final String[] SOUND_NAMES = {"California Code", "March Time"};

  @Override
  public String getBlockRegistryName() {
    return "firealarmsimplex4051red";
  }

  @Override
  public String getSoundResourceName(IBlockState blockState) {
    if (blockState.getValue(SOUND) == 0) {
      return "csm:2910calcode";
    } else {
      return "csm:simplex_4051_marchtime";
    }
  }

      /**
     * Retrieves the bounding box of the block.
     *
     * @param state  the block state
     * @param source the block access
     * @param pos    the block position
     *
     * @return The bounding box of the block.
     *
     * @since 1.0
     */
    @Override
    public AxisAlignedBB getBlockBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {
        return new AxisAlignedBB(0.125000, 0.062500, 0.937500, 0.875000, 0.937500, 1.000000);
    }

  @Override
  public IBlockState getStateFromMeta(int meta) {
    int facingVal = meta % 6;
    int soundVal = (int) Math.floor((double) meta / 6.0);
    return this.getDefaultState()
        .withProperty(FACING, EnumFacing.byIndex(facingVal))
        .withProperty(SOUND, soundVal);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    int facingVal = state.getValue(FACING).getIndex();
    int soundVal = state.getValue(SOUND) * 6;
    return facingVal + soundVal;
  }

  @Override
  protected net.minecraft.block.state.BlockStateContainer createBlockState() {
    return new net.minecraft.block.state.BlockStateContainer(this, FACING, SOUND);
  }

  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
      float hitX, float hitY, float hitZ) {
    if (playerIn.isSneaking()) {
      IBlockState newBlockState = state.cycleProperty(SOUND);
      worldIn.setBlockState(pos, newBlockState);
      if (!worldIn.isRemote) {
        playerIn.sendMessage(new TextComponentString(
            "Alarm sound changed to: " + SOUND_NAMES[newBlockState.getValue(SOUND)]));
      }
      return true;
    }
    return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
  }
}
