package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A bathroom, restroom or laundry fitting of porcelain, metal or plastic that faces whoever
 * places it: a toilet paper holder, a towel rail, a radiator, a grab bar, a dispenser, the
 * wall-hung urinal, the ironing board. Most hang on the wall behind them (their model's back
 * at +Z). A fitting given a sound plays it on right-click: the urinal flushes.
 *
 * @since 2026.9
 */
public class BlockBathroomFixture extends BlockResidentialFurniture {

  @Nullable
  private final ICsmSound clickSound;
  private final float clickPitch;

  /**
   * Constructs a fitting with nothing to do on right-click.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths
   * @param material     what it is made of
   */
  public BlockBathroomFixture(String registryName, int[] box, FixtureMaterial material) {
    this(registryName, box, material, null, 1.0F);
  }

  /**
   * Constructs a fitting that plays a sound on right-click.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths
   * @param material     what it is made of
   * @param clickSound   what it plays on right-click, or null
   * @param clickPitch   the pitch it plays it at
   */
  public BlockBathroomFixture(String registryName, int[] box, FixtureMaterial material,
      @Nullable ICsmSound clickSound, float clickPitch) {
    super(registryName, box, material.getMaterial(), material.getSound(),
        material.getHardness());
    this.clickSound = clickSound;
    this.clickPitch = clickPitch;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (clickSound == null || player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      SoundEvent event = clickSound.getSoundEvent();
      if (event != null) {
        world.playSound(null, pos, event, SoundCategory.BLOCKS, 0.8F, clickPitch);
      }
    }
    return true;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
