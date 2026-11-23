package com.micatechnologies.minecraft.csm.technology.radio;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A handheld radio. It is a block so that it can be stood on a desk or a shelf, but it is mostly
 * carried: its item ({@link ItemBlockRadio}) opens the radio's screen when used and only sets the
 * radio down when the holder sneaks.
 *
 * <p>The radio's settings travel with it. Standing it up copies them from the stack into
 * {@link TileEntityRadio} (vanilla does that, from the stack's {@code BlockEntityTag}), and
 * breaking or picking the block gives a stack that carries them back. The block's only state is
 * the way it faces; no setting is a block-state property.</p>
 *
 * <p>The radios do not transmit. What they store is shaped for the day they do: see
 * {@link RadioChannel}.</p>
 *
 * @since 2026.11
 */
public class BlockRadio extends AbstractBlockRotatableNSEW implements ICsmTileEntityProvider {

  /** The GUI id of a placed radio's screen (x, y, z are the block's); unique across the mod. */
  public static final int GUI_ID_PLACED = 91;

  /**
   * The GUI id of a held radio's screen (x is the hand's ordinal); unique across the mod.
   */
  public static final int GUI_ID_HELD = 90;

  /**
   * The registry name, for the {@code AbstractBlock} constructor, which asks for it before this
   * class's fields are set.
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final RadioModel model;
  private final AxisAlignedBB boundingBox;

  /**
   * @param registryName the block's registry name
   * @param model        the make of radio
   * @param box          the radio's box facing north, in sixteenths: x0, y0, z0, x1, y1, z1
   */
  public BlockRadio(String registryName, RadioModel model, double[] box) {
    super(initRegistryName(registryName), SoundType.METAL, "pickaxe", 0, 0.3F, 2F, 0F, 0);
    this.registryName = registryName;
    this.model = model;
    this.boundingBox = new AxisAlignedBB(box[0] / 16, box[1] / 16, box[2] / 16, box[3] / 16,
        box[4] / 16, box[5] / 16);
    PENDING_REGISTRY_NAME.remove();
  }

  /**
   * Circuits, not iron: a radio comes away in the hand, and a tool-requiring material would
   * break without giving it back.
   */
  private static Material initRegistryName(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return Material.CIRCUITS;
  }

  /** The make of radio this block is. */
  public RadioModel getRadioModel() {
    return model;
  }

  @Override
  protected ItemBlock createItemBlock() {
    return new ItemBlockRadio(this);
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
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

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }

  // --- the screen ----------------------------------------------------------------------------

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || player.isSneaking()) {
      return false;
    }
    // Both sides, as every screen in the mod is opened: the client's call shows it, the server's
    // finds no container and does nothing.
    player.openGui(Csm.instance, GUI_ID_PLACED, world, pos.getX(), pos.getY(), pos.getZ());
    return true;
  }

  // --- settings go with the radio ------------------------------------------------------------

  /**
   * A stack of this radio carrying the settings of the one standing at {@code pos}.
   *
   * @param world the world
   * @param pos   the placed radio
   *
   * @return the stack
   */
  private ItemStack stackOf(IBlockAccess world, BlockPos pos) {
    ItemStack stack = new ItemStack(Item.getItemFromBlock(this));
    TileEntity tileEntity = world.getTileEntity(pos);
    if (tileEntity instanceof TileEntityRadio) {
      ItemBlockRadio.setSettings(stack, ((TileEntityRadio) tileEntity).getSettings()
          .forModel(model));
    }
    return stack;
  }

  @Override
  public void getDrops(@Nonnull NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos,
      @Nonnull IBlockState state, int fortune) {
    drops.add(stackOf(world, pos));
  }

  /**
   * Leaves the block in place when a player's break will drop it, so that
   * {@link #getDrops} still finds the tile entity; {@link #harvestBlock} removes it afterwards.
   */
  @Override
  public boolean removedByPlayer(@Nonnull IBlockState state, World world, @Nonnull BlockPos pos,
      @Nonnull EntityPlayer player, boolean willHarvest) {
    if (willHarvest) {
      return true;
    }
    return super.removedByPlayer(state, world, pos, player, false);
  }

  @Override
  public void harvestBlock(@Nonnull World world, @Nonnull EntityPlayer player,
      @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nullable TileEntity tileEntity,
      @Nonnull ItemStack tool) {
    super.harvestBlock(world, player, pos, state, tileEntity, tool);
    world.setBlockToAir(pos);
  }

  @Nonnull
  @Override
  public ItemStack getPickBlock(@Nonnull IBlockState state, RayTraceResult target,
      @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player) {
    return stackOf(world, pos);
  }

  // --- tile entity ---------------------------------------------------------------------------

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityRadio.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityradio";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityRadio();
  }
}
