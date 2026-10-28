package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * An exhaust grille (#314): the room end of an exhaust duct, a lay-in egg-crate grille flush under
 * the ceiling. Linked to a thermostat with the linker exactly as a vent is, it is where the
 * system's exhaust fans draw air out of a room -- a negative vent. A room whose grilles take out
 * more air than its vents put in is under negative pressure, as a lab, a toilet room or a kitchen
 * is meant to be; see {@link HvacAirBalance}. Placed over a fume hood or a biosafety cabinet it
 * stands for that hood's exhaust connection.
 *
 * <p>Like a vent its 1.5 px plate is thinner than a quarter block, so its cell stays part of the
 * room. Model from {@code dev-env-utils/scripts/gen_hvac_exhaust.py}.</p>
 *
 * @author Mica Technologies
 * @since 2026.10
 */
public class BlockHvacExhaustGrille extends AbstractBlock implements ICsmTileEntityProvider {

  private static final AxisAlignedBB BOX = new AxisAlignedBB(0.0625, 0.90625, 0.0625, 0.9375,
      1.0, 0.9375);

  public BlockHvacExhaustGrille() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "hvac_exhaust_grille";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }

  @Nonnull
  @Override
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
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

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityHvacExhaustGrille.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityhvacexhaustgrille";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityHvacExhaustGrille();
  }
}
