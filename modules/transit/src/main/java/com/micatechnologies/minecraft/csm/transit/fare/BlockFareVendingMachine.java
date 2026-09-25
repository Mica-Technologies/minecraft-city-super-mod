package com.micatechnologies.minecraft.csm.transit.fare;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Fare Vending Machine block. Replaces the cosmetic factory entry — right-clicking opens the
 * {@link FareVendingGui} where the player can buy a single-use {@link ItemFareTicket} or a
 * reusable {@link ItemTransitCard} pre-loaded with 1/2/5/10/25 trips, or reload a card they
 * already hold.
 *
 * <p>The model is drawn by {@code gen_transit_fare_vending.py}: a free-standing machine two
 * blocks tall in the CITYLINE livery, drawn from this one block with ambient occlusion off (its
 * upper half sits in the air above and was shaded by this block's neighbours). The metadata
 * encoding is unchanged, so machines already placed decode as they were.</p>
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class BlockFareVendingMachine extends AbstractBlockRotatableNSEWUD {

  /** GUI handler ID used in {@link com.micatechnologies.minecraft.csm.CsmGuiHandler}. */
  public static final int GUI_ID = 16;

  /**
   * The machine's cabinet, header and tray facing north (the base class turns it): 14 units wide,
   * from the tray's lip to the back of the block, 30 units tall.
   */
  private static final AxisAlignedBB BBOX = new AxisAlignedBB(
      0.0625, 0.0, 0.3875, 0.9375, 1.875, 1.0);

  public BlockFareVendingMachine() {
    // a faint glow from the lit header and screen (light 6)
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2F, 10F, 0.4F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "farevend";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BBOX;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }

  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing,
      float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    player.openGui(Csm.instance, GUI_ID, worldIn, pos.getX(), pos.getY(), pos.getZ());
    return true;
  }
}
