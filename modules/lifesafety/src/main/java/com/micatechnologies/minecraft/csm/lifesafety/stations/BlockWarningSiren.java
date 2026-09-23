package com.micatechnologies.minecraft.csm.lifesafety.stations;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.lifesafety.fireprotection.BlockFireProtectionProp;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

/**
 * An outdoor warning siren on top of its pole: the rotating siren, whose horn turns while it
 * sounds, or the electronic array, which stands still. Link it to a
 * {@link BlockSirenController} with the fire alarm linker.
 *
 * <p>At rest, the whole siren is baked into the chunk and costs nothing. While it sounds
 * ({@link #ACTIVE}), a rotating siren's baked model leaves the horn out and
 * {@link TileEntityWarningSirenRenderer} draws that same horn turning. {@link #HEAD} is never set
 * on a placed block: it names the horn's model alone, which is what the renderer draws.</p>
 *
 * <p>Stored in metadata: the facing and {@link #ACTIVE}.</p>
 *
 * @since 2026.9
 */
public class BlockWarningSiren extends BlockFireProtectionProp implements ICsmTileEntityProvider {

  public static final PropertyBool ACTIVE = PropertyBool.create("active");
  public static final PropertyBool HEAD = PropertyBool.create("head");

  private static final ThreadLocal<Boolean> PENDING_ROTATES = new ThreadLocal<>();

  private final boolean rotates;

  public BlockWarningSiren(String registryName, int[] box, boolean rotates) {
    super(stash(registryName, rotates), box, true);
    this.rotates = rotates;
    PENDING_ROTATES.remove();
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(ACTIVE, false).withProperty(HEAD, false));
  }

  private static String stash(String registryName, boolean rotates) {
    PENDING_ROTATES.set(rotates);
    return registryName;
  }

  /** Whether the horn turns while it sounds. */
  public boolean rotates() {
    return rotates || Boolean.TRUE.equals(PENDING_ROTATES.get());
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, ACTIVE, HEAD);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(ACTIVE) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(ACTIVE, (meta & 4) != 0);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityWarningSiren.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitywarningsiren";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityWarningSiren();
  }
}
