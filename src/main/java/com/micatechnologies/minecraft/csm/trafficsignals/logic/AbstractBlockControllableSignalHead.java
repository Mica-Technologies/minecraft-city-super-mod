package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.codeutils.DirectionSixteen;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHeadRenderer;
import javax.annotation.Nullable;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractBlockControllableSignalHead extends AbstractBlockControllableSignal
    implements ICsmTileEntityProvider {

  public AbstractBlockControllableSignalHead(Material p_i45394_1_) {
    super(p_i45394_1_);
  }

  public DirectionSixteen getTiltedFacing(
      @NotNull IBlockAccess worldIn,
      @NotNull BlockPos pos, EnumFacing facing4) {
    TileEntity tileEntity = worldIn.getTileEntity(pos);
    if (tileEntity instanceof TileEntityTrafficSignalHead trafficSignalHead) {
      TrafficSignalBodyTilt stateBodyTilt = trafficSignalHead.getBodyTilt();
      return getTiltedFacing(stateBodyTilt, facing4);
    }
    return null;
  }

  public static DirectionSixteen getTiltedFacing(TrafficSignalBodyTilt stateBodyTilt, EnumFacing facing4) {
    DirectionSixteen stateTiltedFacing;
    if (facing4 == EnumFacing.NORTH) {
      // Rotate the signal head based on the body tilt
      if (stateBodyTilt == TrafficSignalBodyTilt.LEFT_ANGLE) {
        stateTiltedFacing = DirectionSixteen.NE;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.RIGHT_ANGLE) {
        stateTiltedFacing = DirectionSixteen.NW;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.LEFT_TILT) {
        stateTiltedFacing = DirectionSixteen.NNE;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.RIGHT_TILT) {
        stateTiltedFacing = DirectionSixteen.NNW;
      } else {
        stateTiltedFacing = DirectionSixteen.N;
      }
    } else if (facing4 == EnumFacing.EAST) {
      // Rotate the signal head based on the body tilt
      if (stateBodyTilt == TrafficSignalBodyTilt.LEFT_ANGLE) {
        stateTiltedFacing = DirectionSixteen.SE;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.RIGHT_ANGLE) {
        stateTiltedFacing = DirectionSixteen.NE;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.LEFT_TILT) {
        stateTiltedFacing = DirectionSixteen.ESE;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.RIGHT_TILT) {
        stateTiltedFacing = DirectionSixteen.ENE;
      } else {
        stateTiltedFacing = DirectionSixteen.E;
      }
    } else if (facing4 == EnumFacing.SOUTH) {
      // Rotate the signal head based on the body tilt
      if (stateBodyTilt == TrafficSignalBodyTilt.LEFT_ANGLE) {
        stateTiltedFacing = DirectionSixteen.SE;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.RIGHT_ANGLE) {
        stateTiltedFacing = DirectionSixteen.SW;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.LEFT_TILT) {
        stateTiltedFacing = DirectionSixteen.SSE;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.RIGHT_TILT) {
        stateTiltedFacing = DirectionSixteen.SSW;
      } else {
        stateTiltedFacing = DirectionSixteen.S;
      }
    } else {
      // Rotate the signal head based on the body tilt
      if (stateBodyTilt == TrafficSignalBodyTilt.LEFT_ANGLE) {
        stateTiltedFacing = DirectionSixteen.NW;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.RIGHT_ANGLE) {
        stateTiltedFacing = DirectionSixteen.SW;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.LEFT_TILT) {
        stateTiltedFacing = DirectionSixteen.WNW;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.RIGHT_TILT) {
        stateTiltedFacing = DirectionSixteen.WSW;
      } else {
        stateTiltedFacing = DirectionSixteen.W;
      }
    }

    return stateTiltedFacing;
  }

  /**
   * Gets the tile entity class for the block.
   *
   * @return the tile entity class for the block
   *
   * @since 1.0
   */
  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityTrafficSignalHead.class;
  }

  /**
   * Gets the tile entity name for the block.
   *
   * @return the tile entity name for the block
   *
   * @since 1.0
   */
  @Override
  public String getTileEntityName() {
    return "tileentitytrafficsignalhead";
  }


  /**
   * Gets a new tile entity for the block.
   *
   * @param worldIn the world
   * @param meta    the block metadata
   *
   * @return the new tile entity for the block
   *
   * @since 1.1
   */
  @Nullable
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityTrafficSignalHead(getDefaultTrafficSignalSectionInfo());
  }

  public abstract TrafficSignalSectionInfo[] getDefaultTrafficSignalSectionInfo();

  /**
   * Ensures a tile entity exists for this block. Handles migration of blocks that existed
   * in the world before being converted to custom rendering (they were saved without a TE).
   * Called from neighborChanged and onBlockActivated so the TE is created automatically
   * when the signal controller cycles colors or the player interacts.
   */
  private void ensureTileEntity(World worldIn, BlockPos pos) {
    if (!worldIn.isRemote && worldIn.getTileEntity(pos) == null) {
      worldIn.setTileEntity(pos, createNewTileEntity(worldIn, 0));
    }
  }

  @Override
  public void neighborChanged(IBlockState state, World worldIn, BlockPos pos,
      net.minecraft.block.Block blockIn, BlockPos fromPos) {
    ensureTileEntity(worldIn, pos);
    super.neighborChanged(state, worldIn, pos, blockIn, fromPos);
  }

  @Override
  public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
    // Clean up the renderer's cached display list for this position
    if (worldIn.isRemote) {
      TileEntitySpecialRenderer<?> renderer =
          TileEntityRendererDispatcher.instance.renderers.get(TileEntityTrafficSignalHead.class);
      if (renderer instanceof TileEntityTrafficSignalHeadRenderer) {
        ((TileEntityTrafficSignalHeadRenderer) renderer).cleanupDisplayList(pos);
      }
    }
    super.breakBlock(worldIn, pos, state);
  }

  @Override
  public boolean onBlockActivated(World p_180639_1_, BlockPos p_180639_2_, IBlockState p_180639_3_,
      EntityPlayer p_180639_4_, EnumHand p_180639_5_, EnumFacing p_180639_6_, float p_180639_7_,
      float p_180639_8_, float p_180639_9_) {
    ensureTileEntity(p_180639_1_, p_180639_2_);
    return super.onBlockActivated(p_180639_1_, p_180639_2_, p_180639_3_, p_180639_4_, p_180639_5_,
        p_180639_6_, p_180639_7_, p_180639_8_, p_180639_9_);
  }

}