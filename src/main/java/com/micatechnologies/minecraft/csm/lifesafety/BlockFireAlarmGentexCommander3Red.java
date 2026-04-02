package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockFireAlarmGentexCommander3Red extends AbstractBlockFireAlarmSounder
    implements ICsmTileEntityProvider, IStrobeBlock {

  @Override
  public float[] getStrobeLensFrom() {
    return new float[]{5f, 10.6f, 14f};
  }

  @Override
  public float[] getStrobeLensTo() {
    return new float[]{11f, 14.6f, 15f};
  }

  private static final String[] SOUND_RESOURCE_NAMES = {
      "csm:gentex_gos_code3",
      "csm:gentex_gos_code3_chime",
      "csm:gentex_gos_whoop",
      "csm:gentex_gos_continuous_chime"
  };
  private static final String[] SOUND_DISPLAY_NAMES = {
      "Code 3 Horn",
      "Code 3 Chime",
      "Whoop",
      "Continuous Chime"
  };

  @Override
  public String getBlockRegistryName() {
    return "firealarmgentexcommander3red";
  }

  @Override
  public String getSoundResourceName(IBlockState blockState) {
    return SOUND_RESOURCE_NAMES[0];
  }

  /**
   * Gets the sound resource name using the tile entity's stored sound index.
   */
  public String getSoundResourceName(World world, BlockPos pos, IBlockState blockState) {
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityFireAlarmSoundIndex) {
      int idx = ((TileEntityFireAlarmSoundIndex) te).getSoundIndex();
      if (idx >= 0 && idx < SOUND_RESOURCE_NAMES.length) {
        return SOUND_RESOURCE_NAMES[idx];
      }
    }
    return SOUND_RESOURCE_NAMES[0];
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
        return new AxisAlignedBB(0.187500, 0.187500, 0.875000, 0.812500, 1.000000, 1.000000);
    }

  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
      float hitX, float hitY, float hitZ) {
    if (playerIn.isSneaking()) {
      TileEntity te = worldIn.getTileEntity(pos);
      if (te instanceof TileEntityFireAlarmSoundIndex) {
        TileEntityFireAlarmSoundIndex soundTE = (TileEntityFireAlarmSoundIndex) te;
        soundTE.cycleSoundIndex(SOUND_RESOURCE_NAMES.length);
        if (!worldIn.isRemote) {
          int newIdx = soundTE.getSoundIndex();
          playerIn.sendMessage(new TextComponentString(
              "Alarm sound changed to: " + SOUND_DISPLAY_NAMES[newIdx]));
        }
      }
      return true;
    }
    return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityFireAlarmSoundIndex.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityfirealarmsoundindex";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityFireAlarmSoundIndex();
  }
}
