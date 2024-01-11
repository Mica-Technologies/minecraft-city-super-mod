package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class BlockFireAlarmSimplex4903HornStrobeWhite extends AbstractBlockFireAlarmSounder {

  public static final PropertyInteger SOUND = PropertyInteger.create("sound", 0, 1);
  public static final String[] SOUND_NAMES = {"Old", "New"};

  @Override
  public String getSoundResourceName(IBlockState blockState) {
    if (blockState.getValue(SOUND) == 0) {
      return "csm:4030code44";
    } else {
      return "csm:stahorn";
    }
  }

  @Override
  public int getSoundTickLen(IBlockState blockState) {
    if (blockState.getValue(SOUND) == 0) {
      return 115;
    } else {
      return 60;
    }
  }

  @Override
  public String getBlockRegistryName() {
    return "firealarmsimplex4903hornstrobewhite";
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
  //        @Override
  //        public IBlockState getStateForPlacement( World worldIn,
  //                                                 BlockPos pos,
  //                                                 EnumFacing facing,
  //                                                 float hitX,
  //                                                 float hitY,
  //                                                 float hitZ,
  //                                                 int meta,
  //                                                 EntityLivingBase placer )
  //        {
  //            return this.getDefaultState()
  //                       .withProperty( FACING, EnumFacing.getDirectionFromEntityLiving( pos,
  //                       placer ) )
  //                       .withProperty( SOUND, 0 );
  //        }
}

