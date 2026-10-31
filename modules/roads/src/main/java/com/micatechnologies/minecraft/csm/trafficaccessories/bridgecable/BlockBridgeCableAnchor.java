package com.micatechnologies.minecraft.csm.trafficaccessories.bridgecable;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A bridge cable anchor: the socket on a pylon face, the clevis, the saddle on a suspension tower
 * and the deck anchor on an edge beam. One class for the four, told apart by {@link
 * BridgeCableAnchorKind}; they share one tile entity, which holds the cables the anchor owns.
 *
 * <p>The socket and the clevis bolt to the face they were placed against and face away from it,
 * any of six ways. The saddle and the deck anchor sit on top of something and face one of four
 * ways, which only says which way their groove or fork runs.
 *
 * <p>The cables are purely visual: no collision, nothing they pass through is checked. This block
 * and its hitbox are the only part of a cable a player can touch.
 */
public class BlockBridgeCableAnchor extends AbstractBlockRotatableNSEWUD
    implements ICsmTileEntityProvider, ICsmNoSnowAccumulation {

  private static final ThreadLocal<BridgeCableAnchorKind> PENDING = new ThreadLocal<>();

  private final BridgeCableAnchorKind kind;

  /**
   * @param registryName one of the {@link BridgeCableAnchorKind} registry names; the tab passes
   *                     the name rather than the kind so the block index can read it off the line
   */
  public BlockBridgeCableAnchor(String registryName) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 3.0F, 12.0F, 0F, 0);
    this.kind = PENDING.get();
    PENDING.remove();
  }

  /** Holds the kind for the super constructor, which asks for the registry name. */
  private static Material stash(String registryName) {
    final BridgeCableAnchorKind kind = BridgeCableAnchorKind.byRegistryName(registryName);
    if (kind == null) {
      throw new IllegalArgumentException("Not a bridge cable anchor: " + registryName);
    }
    PENDING.set(kind);
    return Material.IRON;
  }

  private BridgeCableAnchorKind kind() {
    return kind != null ? kind : PENDING.get();
  }

  public BridgeCableAnchorKind getKind() {
    return kind();
  }

  @Override
  public String getBlockRegistryName() {
    return kind().getRegistryName();
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityBridgeCableAnchor.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitybridgecableanchor";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityBridgeCableAnchor();
  }

  /**
   * A socket or clevis faces away from the face it was put against, so it sits on that face
   * whichever way the builder was looking. A saddle or deck anchor runs the way the builder
   * faces, which is the way the cable goes on along a bridge.
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    final EnumFacing placed = kind().isWallMounted()
        ? facing
        : placer.getHorizontalFacing();
    return getDefaultState().withProperty(FACING, placed);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return kind().getNorthBox();
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
  public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
      ITooltipFlag flag) {
    super.addInformation(stack, world, tooltip, flag);
    tooltip.add("Link with the Bridge Cable Tool, or by /blockdata:");
    tooltip.add("{target:[x,y,z],style:\"stay\",thick:4,color:\"white\"}");
  }
}
