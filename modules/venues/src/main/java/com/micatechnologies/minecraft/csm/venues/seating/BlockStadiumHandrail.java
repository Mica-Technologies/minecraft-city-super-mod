package com.micatechnologies.minecraft.csm.venues.seating;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The centre-of-aisle handrail ({@code stadium_aisle_handrail}): a round stainless rail on a post
 * a cell, down the middle of an aisle. Each cell draws its share of the rail along the aisle's
 * nosing line (see {@link StadiumSlope}), its ends cut square to the run so the next cell's rail
 * meets it, and its own post standing on the tread under it, so a run needs no end piece.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), the way down the aisle,
 * towards the event floor; bits 2-3 the piece ({@link StadiumSlope}: 0 level, 1 steep, 2
 * shallow low, 3 shallow high). It stands in the air cell over the aisle's walking surface.
 * Placed by hand it is level; a sneak-click steps the piece on.</p>
 *
 * @since 2026.10
 */
public class BlockStadiumHandrail extends BlockVenue {

  /** Which part of the slope the cell draws. Stored, bits 2-3. */
  public static final PropertyEnum<StadiumSlope> PIECE = PropertyEnum.create("piece",
      StadiumSlope.class);

  private static final AxisAlignedBB SELECT = BlockStadiumSeat.box(6, 0, 0, 10, 16, 16);
  private static final AxisAlignedBB RAIL = BlockStadiumSeat.box(7, 0, 0, 9, 16, 16);

  /**
   * Constructs the handrail.
   *
   * @param registryName its registry name
   */
  public BlockStadiumHandrail(String registryName) {
    super(registryName, Material.IRON, SoundType.METAL, 1.5F);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(PIECE, StadiumSlope.LEVEL));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, PIECE);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(PIECE, StadiumSlope.values()[(meta >> 2) & 3]);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(PIECE).ordinal() << 2);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return SELECT;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
      AxisAlignedBB entityBox, List<AxisAlignedBB> boxes, @Nullable Entity entity,
      boolean isActualState) {
    addCollisionBoxToList(pos, entityBox, boxes,
        RotationUtils.rotateBoundingBoxByFacing(RAIL, state.getValue(FACING)));
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      StadiumSlope next = state.getValue(PIECE).next();
      world.setBlockState(pos, state.withProperty(PIECE, next), 3);
      player.sendStatusMessage(new TextComponentTranslation("csm.venues.slope." + next.getName()),
          true);
    }
    return true;
  }
}
