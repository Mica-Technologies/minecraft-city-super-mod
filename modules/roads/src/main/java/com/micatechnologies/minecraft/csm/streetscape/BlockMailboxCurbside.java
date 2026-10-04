package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.codeutils.BlockUtils;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A curbside mailbox on a post, whose red flag is up while it holds mail.
 *
 * <p>The flag is actual state read from the tile entity, which is sent one bit per compartment
 * saying whether it holds anything and nothing about what; the tile entity rebuilds the chunk
 * only when that bit flips ({@link TileEntityMailbox#getBakedModelKey}).</p>
 *
 * @version 1.0
 */
public class BlockMailboxCurbside extends BlockMailbox {

  /** Whether the flag is up: the box holds mail. */
  public static final PropertyBool FLAG = PropertyBool.create("flag");

  public BlockMailboxCurbside(String registryName, UtilityBoxSpec spec, float[][] doors,
      int[] slots, String[] doorNames) {
    super(registryName, spec, doors, slots, doorNames);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(FLAG, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, FLAG);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    IBlockState actual = super.getActualState(state, world, pos);
    TileEntity te = BlockUtils.getTileEntitySafe(world, pos);
    return actual.withProperty(FLAG,
        te instanceof TileEntityMailbox && ((TileEntityMailbox) te).hasMail(0));
  }
}
