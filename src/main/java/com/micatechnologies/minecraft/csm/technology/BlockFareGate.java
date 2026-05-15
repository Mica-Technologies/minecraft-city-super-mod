package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Standard 1-block-wide fare gate. Right-clicking validates whatever fare media the player
 * is holding:
 *
 * <ul>
 *   <li>{@link ItemFareTicket} — consumed entirely (single-use), gate opens</li>
 *   <li>{@link ItemTransitCard} with balance ≥ 1 — one trip deducted, gate opens</li>
 *   <li>Empty hand or invalid item — chat error, gate stays closed</li>
 * </ul>
 *
 * <p>An open gate has no collision so the player can walk through; a closed gate is solid
 * across the cell. After {@link TileEntityFareGate#getAutoCloseTicks} ticks of being open
 * the TE flips OPEN back to false and the barrier reappears.</p>
 *
 * <p>Visual: a placeholder gray brushed-steel cube with a status-arrow band that turns red
 * when closed, green when open. Iterate the model in Blockbench whenever you're ready —
 * the block class doesn't bake any model assumptions in.</p>
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class BlockFareGate extends AbstractBlock implements ICsmTileEntityProvider {

  public static final PropertyDirection FACING = BlockHorizontal.FACING;
  public static final PropertyBool OPEN = PropertyBool.create("open");

  /** A thin horizontal slab in the middle of the cell — the visual "barrier" plane. */
  private static final AxisAlignedBB CLOSED_BBOX =
      new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
  /** No collision when open — player can walk through. */
  private static final AxisAlignedBB OPEN_BBOX =
      new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.05, 1.0);

  public BlockFareGate() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 0);
    setDefaultState(blockState.getBaseState()
        .withProperty(FACING, EnumFacing.NORTH)
        .withProperty(OPEN, false));
  }

  @Override
  public String getBlockRegistryName() {
    return "fare_gate";
  }

  // === State ↔ meta ===

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    int facingIdx = meta & 3;
    EnumFacing facing = EnumFacing.byHorizontalIndex(facingIdx);
    boolean open = (meta & 4) != 0;
    return getDefaultState().withProperty(FACING, facing).withProperty(OPEN, open);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(OPEN) ? 4 : 0);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, OPEN);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState()
        .withProperty(FACING, placer.getHorizontalFacing().getOpposite())
        .withProperty(OPEN, false);
  }

  // === Visuals / collision ===

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return CLOSED_BBOX;
  }

  @Override
  @Nullable
  public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn,
      BlockPos pos) {
    // Closed gate: solid wall. Open gate: a thin floor slab the player can step over but
    // doesn't physically block movement, so passage is genuinely free.
    return blockState.getValue(OPEN) ? OPEN_BBOX : CLOSED_BBOX;
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
  public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
    // Soft glow from the lit-green status arrow when open. No emission when closed.
    return state.getActualState(world, pos).getValue(OPEN) ? 5 : 0;
  }

  // === Interaction ===

  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing,
      float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    if (worldIn.isRemote) {
      return true;
    }

    // Already open: do nothing — player can just walk through.
    if (state.getValue(OPEN)) {
      return true;
    }

    ItemStack held = player.getHeldItemMainhand();
    if (held.getItem() instanceof ItemFareTicket) {
      held.shrink(1);
      openGate(worldIn, pos, state);
      sendChat(player, "§aFare validated — single-use ticket consumed");
      return true;
    }
    if (held.getItem() instanceof ItemTransitCard) {
      int balance = ItemTransitCard.getBalance(held);
      if (balance <= 0) {
        sendChat(player, "§cTransit card has no remaining trips. Reload at a vending machine.");
        playDeniedSound(worldIn, pos);
        return true;
      }
      ItemTransitCard.consumeTrip(held);
      openGate(worldIn, pos, state);
      sendChat(player, "§aFare validated — " + (balance - 1) + " trips remaining");
      return true;
    }
    sendChat(player, "§ePresent a fare ticket or transit card to enter.");
    playDeniedSound(worldIn, pos);
    return true;
  }

  private void openGate(World world, BlockPos pos, IBlockState state) {
    world.setBlockState(pos, state.withProperty(OPEN, true), 3);
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityFareGate) {
      ((TileEntityFareGate) te).markOpened();
    }
    world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
        SoundEvents.BLOCK_NOTE_PLING, SoundCategory.BLOCKS, 0.7F, 1.4F);
  }

  private void playDeniedSound(World world, BlockPos pos) {
    world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
        SoundEvents.BLOCK_NOTE_BASS, SoundCategory.BLOCKS, 0.7F, 0.6F);
  }

  private static void sendChat(EntityPlayer player, String message) {
    player.sendMessage(new TextComponentString(message));
  }

  // === Tile entity wiring ===

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityFareGate.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityfaregate";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityFareGate();
  }
}
