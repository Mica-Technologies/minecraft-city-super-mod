package com.micatechnologies.minecraft.csm.parks.zipline;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A zipline anchor: a steel bracket with a cable eye, set on a post, a wall top or a platform
 * edge. A launch anchor's line runs down to a landing anchor; the Zipline Linker strings it.
 * Right-click a launch anchor with an empty hand to ride its line ({@link EntityZiplineTrolley}).
 * Breaking either end takes the line down.
 *
 * <p>No facing: the cable eye is a cross, so it reads the same whichever way a line leaves.</p>
 *
 * @version 1.0
 * @since 2026.11
 */
public class BlockZiplineAnchor extends AbstractBlock implements ICsmTileEntityProvider {

  private static final AxisAlignedBB BOX = new AxisAlignedBB(0.1875, 0, 0.1875, 0.8125, 1,
      0.8125);

  /** The registry name of the launch anchor; any other name is a landing anchor. */
  public static final String LAUNCH = "zipline_anchor_launch";
  /** The registry name of the landing anchor. */
  public static final String LANDING = "zipline_anchor_landing";

  /** The name being constructed: AbstractBlock asks the registry name before a field is set. */
  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  /**
   * An anchor.
   *
   * @param registryName {@link #LAUNCH} or {@link #LANDING}
   *
   * @since 1.0
   */
  public BlockZiplineAnchor(String registryName) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 3.0F, 10.0F, 0.0F, 0);
    this.registryName = registryName;
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  public boolean isLaunch() {
    return LAUNCH.equals(getBlockRegistryName());
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (player.isSneaking()) {
      // Sneak-click a launch anchor: its line's lights on or off.
      if (!isLaunch()) {
        return false;
      }
      if (!world.isRemote) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityZiplineAnchor) {
          TileEntityZiplineAnchor anchor = (TileEntityZiplineAnchor) te;
          anchor.setLights(!anchor.hasLights());
          player.sendStatusMessage(new TextComponentTranslation(anchor.hasLights()
              ? "csm.zipline.lights_on" : "csm.zipline.lights_off"), true);
        }
      }
      return true;
    }
    if (!world.isRemote) {
      if (isLaunch()) {
        EntityZiplineTrolley.ride(world, pos, player);
      } else {
        player.sendStatusMessage(CommandZipline.describe(world, pos), true);
      }
    }
    return true;
  }

  @Override
  public void breakBlock(World world, BlockPos pos, IBlockState state) {
    if (!world.isRemote) {
      Ziplines.unlink(world, pos);
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
      ITooltipFlag flag) {
    if (isLaunch()) {
      tooltip.add(I18n.format("csm.zipline.anchor.tooltip"));
    }
  }

  // region Tile entity

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityZiplineAnchor.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityziplineanchor";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityZiplineAnchor();
  }

  // endregion

  // region Shape

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
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
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess world, @Nonnull IBlockState state,
      @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
    return face == EnumFacing.DOWN ? BlockFaceShape.CENTER_BIG : BlockFaceShape.UNDEFINED;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  // endregion
}
