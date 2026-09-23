package com.micatechnologies.minecraft.csm.lifesafety.stations;

import com.micatechnologies.minecraft.csm.lifesafety.fireprotection.BlockFireProtectionProp;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A device the station alerting controller drives, linked to it with the fire alarm linker. The
 * controller sets {@link #ACTIVE} on each of its devices when a dispatch starts and clears it when
 * the alert ends; the device only shows it:
 *
 * <ul>
 *   <li>{@link Kind#SPEAKER}: the controller plays its tones from every speaker;</li>
 *   <li>{@link Kind#LIGHT}: an alert light, lit (light 12) while active;</li>
 *   <li>{@link Kind#RELAY}: gives redstone while active, for a garage-door opener, doors, the
 *       gong or anything else a station wants to happen on a call;</li>
 *   <li>{@link Kind#CLEARANCE}: the bay door clearance light, red until the controller turns it
 *       green once the doors have had time to open.</li>
 * </ul>
 *
 * <p>Stored in metadata: the facing and {@link #ACTIVE}.</p>
 *
 * @since 2026.9
 */
public class BlockStationAlertDevice extends BlockFireProtectionProp {

  /** What a device does when the controller activates it. */
  public enum Kind {
    SPEAKER, LIGHT, RELAY, CLEARANCE
  }

  public static final PropertyBool ACTIVE = PropertyBool.create("active");

  private static final ThreadLocal<Kind> PENDING_KIND = new ThreadLocal<>();

  private final Kind kind;

  public BlockStationAlertDevice(String registryName, Kind kind, int[] box) {
    super(stash(registryName, kind), box, false);
    this.kind = kind;
    PENDING_KIND.remove();
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(ACTIVE, false));
  }

  private static String stash(String registryName, Kind kind) {
    PENDING_KIND.set(kind);
    return registryName;
  }

  /** What this device does. */
  public Kind getKind() {
    return kind != null ? kind : PENDING_KIND.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, ACTIVE);
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
  public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
    return getKind() == Kind.LIGHT && state.getValue(ACTIVE) ? 12 : 0;
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canProvidePower(@Nonnull IBlockState state) {
    return getKind() == Kind.RELAY;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getWeakPower(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
    return getKind() == Kind.RELAY && state.getValue(ACTIVE) ? 15 : 0;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return getKind() == Kind.RELAY;
  }
}
