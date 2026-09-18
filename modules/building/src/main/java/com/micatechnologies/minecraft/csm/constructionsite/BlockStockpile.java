package com.micatechnologies.minecraft.csm.constructionsite;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A stockpile of soil, gravel or sand, built up in eight layers the way vanilla snow is.
 *
 * <p>Using a stockpile on one that is not yet full adds a layer; on a full one it places a new
 * block above as usual. A heap is shaped by hand -- full in the middle, fewer layers toward the
 * edges -- rather than every heap being one fixed mound. Breaking one drops a
 * stockpile item per layer. The layer count is the metadata.</p>
 *
 * <p>The three materials differ in nothing but a registry name, what they are made of and their
 * texture, so there is one class, constructed by registry name; the name is handed across on the
 * thread as {@link BlockSiteProp} does. The models come from
 * {@code dev-env-utils/scripts/gen_earthworks.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockStockpile extends AbstractBlock {

  /**
   * How many eighths of a block the pile stands. Stored.
   *
   * @since 1.0
   */
  public static final PropertyInteger LAYERS = PropertyInteger.create("layers", 1, 8);

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private static final AxisAlignedBB[] BOXES = new AxisAlignedBB[9];

  static {
    for (int i = 1; i <= 8; i++) {
      BOXES[i] = new AxisAlignedBB(0, 0, 0, 1, i / 8.0, 1);
    }
  }

  private final String registryName;

  /**
   * Constructs a {@link BlockStockpile}.
   *
   * @param registryName the registry name
   * @param material     what it is made of, which also decides its sound
   *
   * @since 1.0
   */
  public BlockStockpile(String registryName, Material material) {
    super(pendingMaterial(registryName, material),
        material == Material.SAND ? SoundType.SAND : SoundType.GROUND, "shovel", 0, 0.6F, 3F, 0F,
        0);
    this.registryName = registryName;
    setDefaultState(blockState.getBaseState().withProperty(LAYERS, 1));
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material pendingMaterial(String registryName, Material material) {
    PENDING_REGISTRY_NAME.set(registryName);
    return material;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, LAYERS);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(LAYERS, (meta & 7) + 1);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(LAYERS) - 1;
  }

  /**
   * Adds a layer when used with more of the same, until the pile is a full block.
   *
   * @since 1.0
   */
  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    ItemStack held = playerIn.getHeldItem(hand);
    int layers = state.getValue(LAYERS);
    if (held.isEmpty() || held.getItem() != Item.getItemFromBlock(this) || layers >= 8) {
      return false;
    }
    if (!worldIn.isRemote) {
      worldIn.setBlockState(pos, state.withProperty(LAYERS, layers + 1), 3);
      if (!playerIn.capabilities.isCreativeMode) {
        held.shrink(1);
      }
      SoundType sound = getSoundType(state, worldIn, pos, playerIn);
      worldIn.playSound(null, pos, sound.getPlaceSound(), SoundCategory.BLOCKS,
          (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
    }
    return true;
  }

  @Override
  public int quantityDropped(IBlockState state, int fortune, @Nonnull Random random) {
    return state.getValue(LAYERS);
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOXES[state.getValue(LAYERS)];
  }

  @Override
  @Nonnull
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return face == EnumFacing.DOWN || state.getValue(LAYERS) == 8 ? BlockFaceShape.SOLID
        : BlockFaceShape.UNDEFINED;
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

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }
}
