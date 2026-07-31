package com.micatechnologies.minecraft.csm.materials;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The CSM Fabricator: the workbench that turns {@link CsmParts} into City Super Mod blocks.
 *
 * <p>Right-clicking opens a searchable picker listing every fabricable block in the mod. The
 * player chooses the output explicitly instead of arranging ingredients, which is what makes a
 * mod of this size craftable at all — see {@link CsmFabricatorCosts} for why per-block crafting
 * recipes are not workable here.</p>
 *
 * <p>The Fabricator holds no inventory and has no tile entity. Parts are taken straight from the
 * player's inventory when they confirm a selection, so there is nothing to store and nothing to
 * drop when the block is broken.</p>
 *
 * @author Mica Technologies
 * @since 2026.7
 */
@MethodsReturnNonnullByDefault
public class BlockCsmFabricator extends AbstractBlockRotatableNSEW {

  /**
   * The GUI id handled by {@code CsmGuiHandler} for the Fabricator picker.
   *
   * @since 2026.7
   */
  public static final int GUI_ID = 20;

  /**
   * Constructs a {@link BlockCsmFabricator}.
   *
   * @since 2026.7
   */
  public BlockCsmFabricator() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 255);
  }

  /**
   * Retrieves the registry name of the block.
   *
   * @return the registry name of the block
   *
   * @since 2026.7
   */
  @Override
  public String getBlockRegistryName() {
    return "csm_fabricator";
  }

  /**
   * Opens the Fabricator picker.
   *
   * <p>{@code openGui} is called on both sides without an {@code isRemote} guard: on the client
   * it dispatches straight through the GUI handler, and on the server it is a no-op for a
   * client-only screen. Guarding it would stop the screen ever opening in single player.</p>
   *
   * @since 2026.7
   */
  @Override
  @ParametersAreNonnullByDefault
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    player.openGui(Csm.instance, GUI_ID, worldIn, pos.getX(), pos.getY(), pos.getZ());
    return true;
  }

  /**
   * Retrieves the bounding box of the block, which fills the block cell.
   *
   * @since 2026.7
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return SQUARE_BOUNDING_BOX;
  }

  /**
   * Indicates the block is a full cube.
   *
   * @since 2026.7
   */
  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return true;
  }

  /**
   * Indicates the block is an opaque cube.
   *
   * @since 2026.7
   */
  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return true;
  }

  /**
   * Retrieves the render layer of the block.
   *
   * @since 2026.7
   */
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }

  /**
   * Indicates the block does not connect to redstone.
   *
   * @since 2026.7
   */
  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      EnumFacing facing) {
    return false;
  }
}
