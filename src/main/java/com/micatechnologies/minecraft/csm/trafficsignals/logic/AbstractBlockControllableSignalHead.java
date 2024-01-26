package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficsignals.ItemNSSignalLinker;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHead;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
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


  public static final PropertyEnum<TileEntityTrafficSignalHead.BODY_COLOR> BODY_COLOR =
      PropertyEnum.create("bodycolor", TileEntityTrafficSignalHead.BODY_COLOR.class);
  public static final PropertyEnum<TileEntityTrafficSignalHead.VISOR_TYPE> VISOR_TYPE =
      PropertyEnum.create("visortype", TileEntityTrafficSignalHead.VISOR_TYPE.class);

  public AbstractBlockControllableSignalHead(Material p_i45394_1_) {
    super(p_i45394_1_);
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

  /**
   * Gets the tile entity class for the block.
   *
   * @return the tile entity class for the block
   *
   * @since 1.0
   */
  public Class<TileEntityTrafficSignalHead> getTileEntityClass() {
    return TileEntityTrafficSignalHead.class;
  }

  /**
   * Gets the tile entity name for the block.
   *
   * @return the tile entity name for the block
   *
   * @since 1.0
   */
  public String getTileEntityName() {
    return "tileentitytrafficsignalhead";
  }

  /**
   * Gets the {@link IBlockState} of the block to use for placement with the specified parameters.
   *
   * @param worldIn the world the block is being placed in
   * @param pos     the position the block is being place at
   * @param facing  the facing direction of the placement hit
   * @param hitX    the X coordinate of the placement hit
   * @param hitY    the Y coordinate of the placement hit
   * @param hitZ    the Z coordinate of the placement hit
   * @param meta    the meta value of the block state
   * @param placer  the placer of the block
   *
   * @return the {@link IBlockState} of the block to use for placement
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    IBlockState stateForPlacement =
        super.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer);
    TileEntity tileEntity = worldIn.getTileEntity(pos);
    if (tileEntity instanceof TileEntityTrafficSignalHead) {
      TileEntityTrafficSignalHead signalHead = (TileEntityTrafficSignalHead) tileEntity;
      return stateForPlacement.withProperty(COLOR, SIGNAL_OFF)
          .withProperty(BODY_COLOR, signalHead.getBodyColor())
          .withProperty(VISOR_TYPE, signalHead.getVisorType());
    } else {
      return stateForPlacement.withProperty(COLOR, SIGNAL_OFF);
    }
  }

  @Override
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, COLOR, BODY_COLOR, VISOR_TYPE);
  }

  @Override
  @SuppressWarnings("deprecation")
  public @NotNull IBlockState getActualState(IBlockState state,
      @NotNull
      IBlockAccess worldIn,
      @NotNull
      BlockPos pos) {
    TileEntity tileEntity = worldIn.getTileEntity(pos);
    if (tileEntity instanceof TileEntityTrafficSignalHead) {
      TileEntityTrafficSignalHead signalHead = (TileEntityTrafficSignalHead) tileEntity;
      return state.withProperty(BODY_COLOR, signalHead.getBodyColor())
          .withProperty(VISOR_TYPE, signalHead.getVisorType())
          .withProperty(FACING, signalHead.getFacing());
    }
    return state;
  }

  @Override
  public boolean onBlockActivated(World p_180639_1_,
      BlockPos p_180639_2_,
      IBlockState p_180639_3_,
      EntityPlayer p_180639_4_,
      EnumHand p_180639_5_,
      EnumFacing p_180639_6_,
      float p_180639_7_,
      float p_180639_8_,
      float p_180639_9_) {
    if (p_180639_4_.inventory.getCurrentItem() != null &&
        p_180639_4_.inventory.getCurrentItem().getItem() instanceof ItemNSSignalLinker) {
      return super.onBlockActivated(p_180639_1_, p_180639_2_, p_180639_3_, p_180639_4_, p_180639_5_,
          p_180639_6_,
          p_180639_7_, p_180639_8_, p_180639_9_);
    }

    try {
      TileEntity rawTileEntity = p_180639_1_.getTileEntity(p_180639_2_);
      if (rawTileEntity instanceof TileEntityTrafficSignalHead) {
        TileEntityTrafficSignalHead tileEntity
            = (TileEntityTrafficSignalHead) rawTileEntity;
        if (p_180639_4_.isSneaking()) {
          tileEntity.getNextVisorPaintColor();
        } else {
          tileEntity.getNextBodyPaintColor();
        }
      } else {
        System.err.println(
            "Unable to send a traffic signal request due to tile entity missing error!");
      }
    } catch (Exception e) {
      System.err.println("An error occurred while activating a traffic signal request!");
      e.printStackTrace();
    }

    return true;
  }

}