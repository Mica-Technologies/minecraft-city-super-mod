package com.micatechnologies.minecraft.csm.constructionsite;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A 1x1 section of tower crane mast: four corner chords and the lacing between them, with a
 * ladder up the inside. Sections stack into one mast, to the build limit.
 *
 * <p>The livery is stored ({@link CraneLivery}, carried by the item's metadata, one creative
 * stack each). Whether this is the base section -- heavier chords and anchor plates -- is worked
 * out from the block below, so a mast built up from the ground always has its base at the foot.
 * The models come from {@code dev-env-utils/scripts/gen_crane.py}.</p>
 *
 * <p>The player stands inside the mast and climbs it by holding jump, as on scaffolding; only the
 * chords collide, so the mast can be entered from any face.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockCraneMast extends AbstractBlock implements ICsmSiteClimbable {

  /** The livery. Stored. */
  public static final PropertyEnum<CraneLivery> LIVERY = PropertyEnum.create("livery",
      CraneLivery.class);

  /** Whether more mast is below. Actual state only. */
  public static final PropertyBool DOWN = PropertyBool.create("down");

  /** The four chords, which are all that collides. */
  private static final AxisAlignedBB[] CHORDS = {
      BlockSiteProp.box16(0.5, 0, 0.5, 2, 16, 2),
      BlockSiteProp.box16(14, 0, 0.5, 15.5, 16, 2),
      BlockSiteProp.box16(0.5, 0, 14, 2, 16, 15.5),
      BlockSiteProp.box16(14, 0, 14, 15.5, 16, 15.5),
  };

  /**
   * Constructs a {@link BlockCraneMast}.
   *
   * @since 1.0
   */
  public BlockCraneMast() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 3F, 12F, 0F, 0);
    setDefaultState(blockState.getBaseState().withProperty(LIVERY, CraneLivery.YELLOW));
  }

  @Override
  public String getBlockRegistryName() {
    return "crane_mast";
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, LIVERY, DOWN);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(LIVERY, CraneLivery.fromOrdinal(meta & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(LIVERY).ordinal();
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getStateFromMeta(meta);
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    return state.withProperty(DOWN, worldIn.getBlockState(pos.down()).getBlock() == this);
  }

  @Override
  public int damageDropped(IBlockState state) {
    return state.getValue(LIVERY).ordinal();
  }

  @Override
  @Nonnull
  public ItemStack getPickBlock(@Nonnull IBlockState state, @Nonnull RayTraceResult target,
      @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player) {
    return new ItemStack(this, 1, damageDropped(state));
  }

  /**
   * An item that carries the livery in its metadata, so each can be held, named and placed.
   *
   * @since 1.0
   */
  @Override
  protected ItemBlock createItemBlock() {
    return new ItemBlockCraneLivery(this);
  }

  /**
   * One creative stack per livery.
   *
   * @since 1.0
   */
  @Override
  @SideOnly(Side.CLIENT)
  public void getSubBlocks(@Nonnull CreativeTabs tab, @Nonnull NonNullList<ItemStack> items) {
    for (CraneLivery livery : CraneLivery.values()) {
      items.add(new ItemStack(this, 1, livery.ordinal()));
    }
  }

  /**
   * An item model for every livery, not just metadata zero -- the default registers only zero
   * and every other stack shows the missing-texture chequer.
   *
   * @since 1.0
   */
  @Override
  public void registerModels() {
    Item item = Item.getItemFromBlock(this);
    for (CraneLivery livery : CraneLivery.values()) {
      Csm.proxy.setCustomModelResourceLocation(item, livery.ordinal(),
          "inventory_" + livery.getName());
    }
  }

  /**
   * Opens the crane's configuration with an empty hand, from any block of the crane, so a crane
   * whose head is two hundred blocks up can be set from the foot of its mast. True on both sides,
   * or the server goes on to use the held item; the screen opens on the client.
   *
   * @since 1.0
   */
  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !playerIn.getHeldItem(hand).isEmpty()
        || CraneLocator.findHead(worldIn, pos) == null) {
      return false;
    }
    if (worldIn.isRemote) {
      playerIn.openGui(Csm.instance, BuildingGuiProvider.CRANE_GUI_ID, worldIn, pos.getX(),
          pos.getY(), pos.getZ());
    }
    return true;
  }

  @Override
  public boolean isLadder(IBlockState state, IBlockAccess world, BlockPos pos,
      EntityLivingBase entity) {
    return true;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, @Nonnull World worldIn,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn,
      boolean isActualState) {
    for (AxisAlignedBB chord : CHORDS) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes, chord);
    }
  }

  @Override
  @Nonnull
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return SQUARE_BOUNDING_BOX;
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
