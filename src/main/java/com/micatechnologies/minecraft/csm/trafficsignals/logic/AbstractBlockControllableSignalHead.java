package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficsignals.ItemNSSignalLinker;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHead;
import javax.annotation.Nullable;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractBlockControllableSignalHead extends AbstractBlockControllableSignal
    implements ICsmTileEntityProvider {

  public static final PropertyEnum<TrafficSignalBodyColor> BODY_COLOR =
      PropertyEnum.create("body_color", TrafficSignalBodyColor.class);

  public static final PropertyEnum<TrafficSignalBodyTilt> BODY_TILT =
      PropertyEnum.create("body_tilt", TrafficSignalBodyTilt.class);

  public static final PropertyEnum<TrafficSignalVisorType> VISOR_TYPE =
      PropertyEnum.create("visor_type", TrafficSignalVisorType.class);

  public AbstractBlockControllableSignalHead(Material p_i45394_1_) {
    super(p_i45394_1_);
  }

  @Override
  protected @NotNull BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, COLOR, BODY_COLOR, BODY_TILT, VISOR_TYPE);
  }

  @Override
  @SuppressWarnings("deprecation")
  public @NotNull IBlockState getActualState(@NotNull IBlockState state,
      @NotNull IBlockAccess worldIn,
      @NotNull BlockPos pos) {
    TileEntity tileEntity = worldIn.getTileEntity(pos);
    if (tileEntity instanceof TileEntityTrafficSignalHead trafficSignalHead) {
      TrafficSignalBodyColor statePaintColor = trafficSignalHead.getPaintColor();
      TrafficSignalBodyTilt stateBodyTilt = trafficSignalHead.getBodyTilt();
      TrafficSignalVisorType stateVisorType = trafficSignalHead.getVisorType();
      return state.withProperty(BODY_COLOR, statePaintColor)
          .withProperty(BODY_TILT, stateBodyTilt)
          .withProperty(VISOR_TYPE, stateVisorType);
    }
    return state;
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
    return new TileEntityTrafficSignalHead();
  }

  @Override
  public boolean onBlockActivated(World p_180639_1_, BlockPos p_180639_2_, IBlockState p_180639_3_,
      EntityPlayer p_180639_4_, EnumHand p_180639_5_, EnumFacing p_180639_6_, float p_180639_7_,
      float p_180639_8_, float p_180639_9_) {
    if (p_180639_4_.inventory.getCurrentItem() != null && p_180639_4_.inventory.getCurrentItem()
        .getItem() instanceof ItemNSSignalLinker) {
      return super.onBlockActivated(p_180639_1_, p_180639_2_, p_180639_3_, p_180639_4_, p_180639_5_,
          p_180639_6_, p_180639_7_, p_180639_8_, p_180639_9_);
    }

    if (!p_180639_1_.isRemote) {
      try {
        TileEntity rawTileEntity = p_180639_1_.getTileEntity(p_180639_2_);
        if (rawTileEntity instanceof TileEntityTrafficSignalHead) {
          TileEntityTrafficSignalHead tileEntity = (TileEntityTrafficSignalHead) rawTileEntity;
          if (p_180639_4_.isSneaking()) {
            tileEntity.getNextVisorType();
            p_180639_4_.sendMessage(new TextComponentString(
                "Traffic signal visor type set to " + tileEntity.getVisorType().getFriendlyName()));
          } else {
            // tileEntity.getNextPaintColor();
            // p_180639_4_.sendMessage(new TextComponentString(
            //     "Traffic signal paint color set to " + tileEntity.getPaintColor()
            //         .getFriendlyName()));
            TrafficSignalBodyTilt nextBodyTilt=tileEntity.getNextBodyTilt();
            p_180639_4_.sendMessage(new TextComponentString(
                "Traffic signal body tilt set to " + nextBodyTilt.getFriendlyName()));
          }
        } else {
          System.err.println(
              "Unable to send a traffic signal request due to tile entity missing error!");
        }
      } catch (Exception e) {
        System.err.println("An error occurred while activating a traffic signal request!");
        e.printStackTrace();
      }
    }
    return true;
  }

}