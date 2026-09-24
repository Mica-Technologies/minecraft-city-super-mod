package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * A small piece set on a countertop or a table: tableware, a chopping board, a kettle, a stand
 * mixer, the counter appliances. It is drawn standing on whatever it is placed on
 * ({@link #REST}, actual state, from the block below; see {@link SurfaceRest}), so a plate on a
 * dining table lies on the table top rather than hovering over it, and its box moves with it.
 *
 * <p>A piece given a sound plays it on right-click (the kettle's whistle, with a puff of steam
 * from its spout; the mixer's whirr).</p>
 *
 * @since 2026.9
 */
public class BlockCounterPiece extends BlockResidentialFurniture {

  /** What the piece stands on. */
  public static final PropertyEnum<SurfaceRest> REST =
      PropertyEnum.create("rest", SurfaceRest.class);

  private final BlockRenderLayer layer;
  @Nullable
  private final ICsmSound clickSound;
  private final float clickPitch;
  /** Where steam comes out when it is clicked, facing north in sixteenths, or null for none. */
  @Nullable
  private final double[] steam;

  /**
   * Constructs a piece with nothing to do on right-click.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north standing on the floor, in sixteenths
   * @param material     its material
   * @param sound        its block sound
   * @param layer        the render layer (cutout for a round plate's rim, translucent for glass)
   */
  public BlockCounterPiece(String registryName, int[] box, Material material, SoundType sound,
      BlockRenderLayer layer) {
    this(registryName, box, material, sound, layer, null, 1.0F, null);
  }

  /**
   * Constructs a piece that plays a sound on right-click.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north standing on the floor, in sixteenths
   * @param material     its material
   * @param sound        its block sound
   * @param layer        the render layer
   * @param clickSound   what it plays on right-click, or null
   * @param clickPitch   the pitch it plays it at
   * @param steam        where steam puffs out, {x, y, z} facing north in sixteenths, or null
   */
  public BlockCounterPiece(String registryName, int[] box, Material material, SoundType sound,
      BlockRenderLayer layer, @Nullable ICsmSound clickSound, float clickPitch,
      @Nullable double[] steam) {
    super(registryName, box, material, sound, 0.8F);
    this.layer = layer;
    this.clickSound = clickSound;
    this.clickPitch = clickPitch;
    this.steam = steam;
    setDefaultState(getDefaultState().withProperty(REST, SurfaceRest.FLOOR));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, REST);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
    return super.getActualState(state, world, pos)
        .withProperty(REST, SurfaceRest.under(world, pos));
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    AxisAlignedBB box = super.getBlockBoundingBox(state, source, pos);
    if (state.getPropertyKeys().contains(REST)) {
      return box.offset(0, -state.getValue(REST).getDrop() / 16.0, 0);
    }
    return box;
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
        world.playSound(null, pos, event, SoundCategory.BLOCKS, 0.7F, clickPitch);
      }
      if (steam != null && world instanceof WorldServer) {
        puffSteam((WorldServer) world, pos, state.getActualState(world, pos));
      }
    }
    return true;
  }

  /** A few wisps of steam from the spout, turned with the piece. */
  private void puffSteam(WorldServer world, BlockPos pos, IBlockState state) {
    EnumFacing f = state.getValue(FACING);
    double x = steam[0] / 16.0 - 0.5;
    double z = steam[2] / 16.0 - 0.5;
    // Turn (x, z) from north to the piece's facing, as the blockstate's y does.
    for (int i = 0; i < f.getHorizontalIndex() + 2; i++) {
      double t = x;
      x = -z;
      z = t;
    }
    double y = (steam[1] - state.getValue(REST).getDrop()) / 16.0;
    world.spawnParticle(EnumParticleTypes.CLOUD, pos.getX() + 0.5 + x, pos.getY() + y,
        pos.getZ() + 0.5 + z, 6, 0.02, 0.05, 0.02, 0.01);
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return layer;
  }
}
