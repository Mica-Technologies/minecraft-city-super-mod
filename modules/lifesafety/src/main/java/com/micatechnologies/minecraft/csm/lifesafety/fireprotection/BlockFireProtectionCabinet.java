package com.micatechnologies.minecraft.csm.lifesafety.fireprotection;

import com.micatechnologies.minecraft.csm.lifesafety.LifeSafetySounds;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A cabinet with a door that opens: extinguisher and hose cabinets, the AED cabinet, the Knox
 * box. Right-click opens or shuts it; the open model shows the door swung back and what is
 * inside. An alarmed cabinet (the AED's) chirps when its door is opened, as the real ones do to
 * tell the front desk someone has taken the defibrillator.
 *
 * <p>Stored in metadata: the facing (two bits) and whether the door is open (the third).</p>
 *
 * @since 2026.9
 */
public class BlockFireProtectionCabinet extends BlockFireProtectionProp {

  public static final PropertyBool OPEN = PropertyBool.create("open");

  private final boolean alarmed;

  /**
   * Constructs a cabinet.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths: {x0, y0, z0, x1, y1, z1}
   * @param alarmed      whether opening the door sounds the cabinet's alarm
   */
  public BlockFireProtectionCabinet(String registryName, int[] box, boolean alarmed) {
    super(registryName, box, true);
    this.alarmed = alarmed;
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(OPEN, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, OPEN);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(OPEN) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(OPEN, (meta & 4) != 0);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    if (!world.isRemote) {
      boolean open = !state.getValue(OPEN);
      world.setBlockState(pos, state.withProperty(OPEN, open), 3);
      world.playSound(null, pos,
          open ? SoundEvents.BLOCK_IRON_DOOR_OPEN : SoundEvents.BLOCK_IRON_DOOR_CLOSE,
          SoundCategory.BLOCKS, 0.6F, 1.3F);
      if (open && alarmed) {
        SoundEvent alarm = LifeSafetySounds.AED_CABINET_ALARM.getSoundEvent();
        if (alarm != null) {
          world.playSound(null, pos, alarm, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
      }
    }
    return true;
  }
}
