package com.micatechnologies.minecraft.csm.transit.station;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The lit globe lamp that marks a subway entrance: a cast-iron post with an opal globe on top,
 * drawn two blocks tall from one block. The globe is always lit: it glows from its texture (an
 * {@code _e} companion for OptiFine) and the block gives light 14, since Transit may not use the
 * Lighting module's light logic. A click with an empty hand steps the globe's colour
 * ({@link Colour}, in the metadata), a sneaking click back. Railings join it as they would a
 * fence post.
 *
 * @since 2026.9
 */
public class BlockStationGlobe extends AbstractBlock {

  /** The globe's colour, in {@code gen_transit_stations.py}'s {@code GLOBES} order. */
  public enum Colour implements IStringSerializable {
    GREEN, RED, WHITE;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase();
    }
  }

  public static final PropertyEnum<Colour> COLOUR = PropertyEnum.create("colour", Colour.class);

  private static final AxisAlignedBB BOX = new AxisAlignedBB(5.4 / 16.0, 0, 5.4 / 16.0,
      10.6 / 16.0, 31 / 16.0, 10.6 / 16.0);

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a globe lamp.
   *
   * @param registryName its registry name
   */
  public BlockStationGlobe(String registryName) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 3.0F, 10.0F, 14 / 15.0F, 0);
    this.registryName = registryName;
    setDefaultState(blockState.getBaseState().withProperty(COLOUR, Colour.GREEN));
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, COLOUR);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    Colour[] all = Colour.values();
    return getDefaultState().withProperty(COLOUR, all[Math.max(0, meta) % all.length]);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(COLOUR).ordinal();
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      Colour[] all = Colour.values();
      int step = player.isSneaking() ? all.length - 1 : 1;
      Colour next = all[(state.getValue(COLOUR).ordinal() + step) % all.length];
      world.setBlockState(pos, state.withProperty(COLOUR, next), 3);
      world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
          0.3F, 0.8F);
      player.sendStatusMessage(new TextComponentTranslation("csm.transit.globe",
          new TextComponentTranslation("csm.transit.globe." + next.getName())), true);
    }
    return true;
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return face.getAxis() == EnumFacing.Axis.Y ? BlockFaceShape.CENTER
        : BlockFaceShape.MIDDLE_POLE;
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
    return BlockRenderLayer.CUTOUT;
  }
}
