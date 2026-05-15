package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
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
 * Standard 1-block-wide fare gate. The gate is directional:
 *
 * <ul>
 *   <li><b>Exterior side</b> (the side the {@link #FACING} property points toward — i.e.
 *   the side the player was standing on when placing) requires fare media. Right-clicking
 *   with an {@link ItemFareTicket} consumes it; right-clicking with an
 *   {@link ItemTransitCard} deducts one trip from its NBT balance. Either path opens the
 *   gate in {@link GateState#OPEN_ENTRY}, which renders the arrow indicator green.</li>
 *
 *   <li><b>Interior side</b> auto-opens via proximity detection on
 *   {@link TileEntityFareGate}. When a new player enters the cell directly behind the
 *   gate (interior cell), the gate flips to {@link GateState#OPEN_EXIT}, rendering the
 *   exterior arrow as a red X so anyone approaching from outside knows the gate is being
 *   used for an exit and they shouldn't try to enter.</li>
 * </ul>
 *
 * <p>An open gate has no meaningful collision, so the player can walk through; a closed
 * gate is solid across the cell. After {@link TileEntityFareGate#getAutoCloseTicks} ticks
 * of being open the TE flips state back to {@link GateState#CLOSED}.</p>
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class BlockFareGate extends AbstractBlock implements ICsmTileEntityProvider {

  public static final PropertyDirection FACING = BlockHorizontal.FACING;
  public static final PropertyEnum<GateState> STATE =
      PropertyEnum.create("state", GateState.class);

  /**
   * Solid wall when closed. Extends one cell down and one cell up from the placed cell so
   * the player can't crouch underneath or jump over the visible 3-block-tall barrier.
   * Block AABBs are allowed to extend outside the placed cell — vanilla handles offset
   * collision boxes correctly (entities collide against the AABB at the block's world pos).
   */
  private static final AxisAlignedBB CLOSED_BBOX =
      new AxisAlignedBB(0.0, -1.0, 0.0, 1.0, 2.0, 1.0);
  /** Thin floor slab so an open gate doesn't physically block the player. */
  private static final AxisAlignedBB OPEN_BBOX =
      new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.05, 1.0);

  public BlockFareGate() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 0);
    setDefaultState(blockState.getBaseState()
        .withProperty(FACING, EnumFacing.NORTH)
        .withProperty(STATE, GateState.CLOSED));
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
    int stateIdx = (meta >> 2) & 3;
    GateState[] states = GateState.values();
    GateState state = (stateIdx >= 0 && stateIdx < states.length)
        ? states[stateIdx] : GateState.CLOSED;
    return getDefaultState()
        .withProperty(FACING, EnumFacing.byHorizontalIndex(facingIdx))
        .withProperty(STATE, state);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex()
        | (state.getValue(STATE).ordinal() << 2);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, STATE);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    // FACING points back at the placer — i.e. toward the cell the placer was standing in.
    // We treat that side as the "exterior" (where players approach to enter), and the
    // opposite side as the "interior" (where players exit).
    return getDefaultState()
        .withProperty(FACING, placer.getHorizontalFacing().getOpposite())
        .withProperty(STATE, GateState.CLOSED);
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
    return blockState.getValue(STATE).isOpen() ? OPEN_BBOX : CLOSED_BBOX;
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
    GateState s = state.getActualState(world, pos).getValue(STATE);
    return s.isOpen() ? 5 : 0;
  }

  // === Interaction (exterior side: pay-to-enter) ===

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
    if (state.getValue(STATE).isOpen()) {
      // Already open: walk through.
      return true;
    }

    ItemStack held = player.getHeldItemMainhand();
    if (held.getItem() instanceof ItemFareTicket) {
      held.shrink(1);
      openGate(worldIn, pos, state, GateState.OPEN_ENTRY);
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
      openGate(worldIn, pos, state, GateState.OPEN_ENTRY);
      sendChat(player, "§aFare validated — " + (balance - 1) + " trips remaining");
      return true;
    }
    sendChat(player, "§ePresent a fare ticket or transit card to enter.");
    playDeniedSound(worldIn, pos);
    return true;
  }

  /**
   * Server-side helper used by both the click-driven entry path and the proximity-driven
   * exit path on {@link TileEntityFareGate} to flip the block to an open state, mark the
   * TE so the auto-close timer starts, and play the appropriate chime.
   */
  void openGate(World world, BlockPos pos, IBlockState state, GateState newState) {
    world.setBlockState(pos, state.withProperty(STATE, newState), 3);
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityFareGate) {
      ((TileEntityFareGate) te).markOpened();
    }
    // Slightly different chimes for entry vs exit so the audio reinforces the visual.
    float pitch = newState == GateState.OPEN_ENTRY ? 1.4F : 1.0F;
    world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
        SoundEvents.BLOCK_NOTE_PLING, SoundCategory.BLOCKS, 0.7F, pitch);
  }

  private void playDeniedSound(World world, BlockPos pos) {
    world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
        SoundEvents.BLOCK_NOTE_BASS, SoundCategory.BLOCKS, 0.7F, 0.6F);
  }

  private static void sendChat(EntityPlayer player, String message) {
    player.sendMessage(new TextComponentString(message));
  }

  /** Returns the cell directly on the interior side of the gate (opposite the exterior). */
  static BlockPos interiorCell(BlockPos gatePos, IBlockState state) {
    EnumFacing exterior = state.getValue(FACING);
    return gatePos.offset(exterior.getOpposite());
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
