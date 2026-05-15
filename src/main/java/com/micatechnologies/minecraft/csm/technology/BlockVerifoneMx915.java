package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.CsmSounds;
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
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Verifone MX915 payment terminal. Mostly a decorative checkout prop, but right-clicking
 * triggers the device's signature "transaction approved" chime so the block actually feels
 * like a checkout terminal in shops, fare gates, etc.
 *
 * <p>The same metadata encoding (facing 0–5) is used as the previous cosmetic factory
 * registration so existing world placements decode unchanged.</p>
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class BlockVerifoneMx915 extends AbstractBlockRotatableNSEWUD {

  private static final AxisAlignedBB BBOX = new AxisAlignedBB(
      0.3125, 0.0, 0.3125, 0.6875, 0.0625, 0.6875);

  public BlockVerifoneMx915() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0.6F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "vf915";
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
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing,
      float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    if (!worldIn.isRemote) {
      // Pass null source so every nearby client (including the activator) hears it
      // positionally; the server is authoritative about the chime.
      worldIn.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
          CsmSounds.SOUND.VERIFONE_MX915.getSoundEvent(),
          SoundCategory.BLOCKS, 1.0F, 1.0F);
    }
    return true;
  }
}
