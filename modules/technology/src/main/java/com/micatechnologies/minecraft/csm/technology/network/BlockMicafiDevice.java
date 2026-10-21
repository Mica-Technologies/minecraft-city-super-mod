package com.micatechnologies.minecraft.csm.technology.network;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.CsmPoleFit;
import com.micatechnologies.minecraft.csm.codeutils.ICsmPoleFitted;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A Micafi device hung on a wall or a pole: a wall access point, an in-wall access point, a
 * building bridge, an outdoor radio. It faces whoever places it, away from the wall or pole it
 * is put against, and never stands on end.
 *
 * <p>Right-click changes its status light ({@link MicafiLight}): an access point's ring or a
 * radio's link light, picked per block at construction. The light is two bits of metadata beside
 * the two of the facing; the blockstate swaps the lit texture, so nothing ticks and there is no
 * tile entity. Sneak-click does nothing, so a block can still be placed against it.</p>
 *
 * <p>{@link PoleFitted} is the same block drawn to meet the pole behind it ({@link CsmPoleFit}),
 * for the pieces whose clamp is meant for a pole: on a wall the clamp is hidden in it.</p>
 *
 * @since 2026.10
 */
public class BlockMicafiDevice extends AbstractBlockRotatableNSEW {

  /**
   * The registry name and light, for the constructors above this one, which ask for the name and
   * build the state container before this class's fields are set.
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();
  private static final ThreadLocal<PropertyEnum<?>> PENDING_LIGHT = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB boundingBox;
  private final PropertyEnum<?> light;

  /**
   * @param registryName the block's registry name
   * @param box          the device's box facing north, in sixteenths: x0, y0, z0, x1, y1, z1
   * @param light        {@link MicafiLight#RING} or {@link MicafiLight#LINK}
   */
  public BlockMicafiDevice(String registryName, double[] box, PropertyEnum<?> light) {
    super(pending(registryName, light), SoundType.METAL, "pickaxe", 1, 1F, 10F, 0F, 0);
    this.registryName = registryName;
    this.light = light;
    this.boundingBox = new AxisAlignedBB(box[0] / 16, box[1] / 16, box[2] / 16, box[3] / 16,
        box[4] / 16, box[5] / 16);
    PENDING_REGISTRY_NAME.remove();
    PENDING_LIGHT.remove();
    setDefaultState(withLight(getDefaultState(), 0));
  }

  private static Material pending(String name, PropertyEnum<?> light) {
    PENDING_REGISTRY_NAME.set(name);
    PENDING_LIGHT.set(light);
    return Material.IRON;
  }

  private PropertyEnum<?> light() {
    return light != null ? light : PENDING_LIGHT.get();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private IBlockState withLight(IBlockState state, int bits) {
    PropertyEnum property = light();
    return state.withProperty(property, MicafiLight.fromBits(property, bits));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private IBlockState nextLight(IBlockState state) {
    PropertyEnum property = light();
    return state.withProperty(property, MicafiLight.next((Enum) state.getValue(property)));
  }

  private int lightBits(IBlockState state) {
    return ((Enum<?>) state.getValue(light())).ordinal() & 3;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Nonnull
  @Override
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, CsmPoleFit.properties(this, FACING, light()));
  }

  @Nonnull
  @Override
  public IBlockState getStateFromMeta(int meta) {
    return withLight(getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)),
        meta >> 2);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (lightBits(state) << 2);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      world.setBlockState(pos, nextLight(state), 3);
      world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
          0.3F, 1.2F);
    }
    return true;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return boundingBox;
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

  /** Cutout: a round face is a square picture with its corners cut away. */
  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  /**
   * The same device drawn to meet the pole behind it. A separate class because the opt-in is a
   * marker interface, which has to be known inside the {@code Block} constructor.
   */
  public static class PoleFitted extends BlockMicafiDevice implements ICsmPoleFitted {

    public PoleFitted(String registryName, double[] box, PropertyEnum<?> light) {
      super(registryName, box, light);
    }
  }
}
