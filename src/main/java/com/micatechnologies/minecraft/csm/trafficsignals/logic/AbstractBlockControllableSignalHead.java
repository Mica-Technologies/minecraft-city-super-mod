package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import java.util.Arrays;
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
   * Returns whether the given blockstate color value (0=red, 1=yellow, 2=green, 3=off)
   * should light up sections with the given bulb color. The default maps color=0 to RED,
   * color=1 to YELLOW, color=2 to GREEN, and color=3 to nothing.
   *
   * Override for signals that respond to multiple color states, such as single-section
   * flasher signals that light on both color=0 (red phase) and color=1 (yellow phase)
   * so they work with the controller's flash mode.
   */
  public boolean shouldLightBulb(int colorState, TrafficSignalBulbColor bulbColor) {
    if (colorState == 0 && bulbColor == TrafficSignalBulbColor.RED) return true;
    if (colorState == 1 && bulbColor == TrafficSignalBulbColor.YELLOW) return true;
    if (colorState == 2 && bulbColor == TrafficSignalBulbColor.GREEN) return true;
    return false;
  }

  /**
   * Returns whether the given blockstate color value (0=red, 1=yellow, 2=green, 3=off)
   * should light up ALL sections regardless of their bulb color. Default is false.
   * Override to true for single-section flasher signals that should light on multiple
   * controller color states.
   */
  public boolean shouldLightAllSections(int colorState) {
    return false;
  }

  /**
   * Returns the Y offset (in model units) to shift the entire signal rendering.
   * Override in subclasses whose JSON model positions the signal body at a different
   * Y origin than the standard 3-section vertical (which uses Y=0 as baseline).
   * For example, single-section signals need +2 to match their model's Y=2-14 range.
   */
  public float getSignalYOffset() {
    return 0.0f;
  }

  /**
   * Returns per-section Y offsets for the renderer. Default uses the standard vertical
   * stack formula (evenly spaced 12 units apart, centered). Override for non-standard
   * layouts such as add-on signals where multiple sections overlap at the same position.
   */
  public float[] getSectionYPositions(int sectionCount) {
    float[] positions = new float[sectionCount];
    for (int i = 0; i < sectionCount; i++) {
      positions[i] = ((sectionCount - 1 - i) - (sectionCount - 1) / 2.0f) * 12.0f;
    }
    return positions;
  }

  /**
   * Returns per-section sizes (12 or 8) for the renderer. Default is 12 for all sections.
   * Override for 8-inch signals or mixed-size (8-8-12, 12-8-8) signals.
   */
  public int[] getSectionSizes(int sectionCount) {
    int[] sizes = new int[sectionCount];
    Arrays.fill(sizes, 12);
    return sizes;
  }

  /**
   * Returns per-section X offsets for the renderer. Default is 0 for all sections
   * (straight vertical stack). Override for doghouse signals where lower sections
   * are shifted left or right relative to the top section.
   */
  public float[] getSectionXPositions(int sectionCount) {
    return new float[sectionCount]; // all zeros
  }

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