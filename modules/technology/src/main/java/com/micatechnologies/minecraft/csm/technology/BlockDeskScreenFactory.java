package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import java.util.Locale;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A {@link BlockDeskDeviceFactory} with a screen: a monitor, an open laptop, a handheld console in
 * its dock. Right-click changes what the screen shows ({@link Screen}: a desktop, a game, code, an
 * idle screensaver), each an animated texture the blockstate picks, so nothing ticks and there is
 * no tile entity. The facing and the screen are both kept in the metadata.
 *
 * @since 2026.10
 */
public class BlockDeskScreenFactory extends BlockDeskDeviceFactory {

  /** What a screen shows. Ordinals are saved in the metadata: only ever append. */
  public enum Screen implements IStringSerializable {
    DESKTOP, GAME, CODE, IDLE;

    @Nonnull
    @Override
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }

    /**
     * @return the screen a click changes this one to
     */
    public Screen next() {
      return values()[(ordinal() + 1) % values().length];
    }
  }

  /** What the screen shows. */
  public static final PropertyEnum<Screen> SCREEN = PropertyEnum.create("screen", Screen.class);

  /**
   * @param registryName the block's registry name
   * @param box          the device's box facing north, in sixteenths: x0, y0, z0, x1, y1, z1
   */
  public BlockDeskScreenFactory(String registryName, double[] box) {
    super(registryName, box);
    setDefaultState(getDefaultState().withProperty(SCREEN, Screen.DESKTOP));
  }

  @Nonnull
  @Override
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, SCREEN);
  }

  @Nonnull
  @Override
  public IBlockState getStateFromMeta(int meta) {
    Screen[] screens = Screen.values();
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(SCREEN, screens[((meta >> 2) & 3) % screens.length]);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(SCREEN).ordinal() << 2);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      Screen next = state.getValue(SCREEN).next();
      world.setBlockState(pos, state.withProperty(SCREEN, next), 3);
      world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
          0.3F, 0.8F);
    }
    return true;
  }
}
