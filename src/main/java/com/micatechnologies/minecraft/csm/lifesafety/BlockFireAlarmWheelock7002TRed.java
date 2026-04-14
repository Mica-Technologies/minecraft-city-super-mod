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

/**
 * Red Wheelock 7002T fire alarm horn/strobe block that plays a medium-speed alarm tone
 * and provides strobe flash capability when activated by the fire alarm control panel.
 *
 * @author Mica Technologies
 * @since 2026.4
 */

public class BlockFireAlarmWheelock7002TRed extends AbstractBlockFireAlarmSounder
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
    return new float[]{3.75f, 13f, 14.25f};
  }

  @Override
  public float[] getStrobeLensTo() {
    return new float[]{12.25f, 16f, 15.75f};
  }


  public static final PropertyInteger SOUND = PropertyInteger.create("sound", 0, 1);
  public static final String[] SOUND_NAMES = {"Normal", "Slow"};

  @Override
  public String getBlockRegistryName() {
    return "firealarmwheelock7002tred";
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
  //    @Override
  //    public IBlockState getStateForPlacement( World worldIn,
  //                                             BlockPos pos,
  //                                             EnumFacing facing,
  //                                             float hitX,
  //                                             float hitY,
  //                                             float hitZ,
  //                                             int meta,
  //                                             EntityLivingBase placer )
  //    {
  //        return this.getDefaultState()
  //                   .withProperty( FACING, EnumFacing.getDirectionFromEntityLiving( pos,
  //                   placer ) )
  //                   .withProperty( SOUND, 0 );
  //    }

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
        return new AxisAlignedBB(0.187500, 0.250000, 0.875000, 0.812500, 1.000000, 1.000000);
    }

  @Override
  protected net.minecraft.block.state.BlockStateContainer createBlockState() {
    return new net.minecraft.block.state.BlockStateContainer(this, FACING, SOUND);
  }

  @Override
  public String getSoundResourceName(IBlockState blockState) {
    if (blockState.getValue(SOUND) == 0) {
      return "csm:7002t_medspeed";
    } else {
      return "csm:7002t_slowspeed";
    }
  }

  @Override
  public boolean onBlockActivated(World p_onBlockActivated_1_,
      BlockPos p_onBlockActivated_2_,
      IBlockState p_onBlockActivated_3_,
      EntityPlayer p_onBlockActivated_4_,
      EnumHand p_onBlockActivated_5_,
      EnumFacing p_onBlockActivated_6_,
      float p_onBlockActivated_7_,
      float p_onBlockActivated_8_,
      float p_onBlockActivated_9_) {
    if (p_onBlockActivated_4_.isSneaking()) {
      IBlockState newBlockState = p_onBlockActivated_3_.cycleProperty(SOUND);
      p_onBlockActivated_1_.setBlockState(p_onBlockActivated_2_, newBlockState);
      if (!p_onBlockActivated_1_.isRemote) {
        p_onBlockActivated_4_.sendMessage(new TextComponentString(
            "Alarm horn sound changed to: " + SOUND_NAMES[newBlockState.getValue(SOUND)]));
      }
      return true;
    }
    return super.onBlockActivated(p_onBlockActivated_1_, p_onBlockActivated_2_,
        p_onBlockActivated_3_,
        p_onBlockActivated_4_, p_onBlockActivated_5_, p_onBlockActivated_6_,
        p_onBlockActivated_7_, p_onBlockActivated_8_, p_onBlockActivated_9_);
  }
}
