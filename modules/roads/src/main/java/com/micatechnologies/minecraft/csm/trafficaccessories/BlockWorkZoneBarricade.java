package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A barricade that joins onto the ones beside it, so a line of them reads as one long run.
 *
 * <p>Each barricade always draws its rails and the upright on its left-hand edge. The pieces
 * that come and go are the ENDS: the overhang past the upright, the cap over the rail ends, and
 * on the right-hand side the upright itself. They are drawn only where nothing connects.</p>
 *
 * <p>That asymmetry — left upright always, right upright only when free — is what leaves exactly
 * one upright on each joint. Drawing both and hiding both at a seam would leave the joint with
 * no leg at all; drawing both and hiding neither would leave two back to back.</p>
 *
 * <p>Barricades only join along their own length, and only to a barricade of the same kind facing
 * the same way. The striping slopes toward the side traffic should pass, so a keep-left joined to
 * a keep-right would contradict itself in the middle of the run.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockWorkZoneBarricade extends BlockWorkZoneDeviceRotatable
    implements ICsmTileEntityProvider {

  /**
   * Whether a matching barricade adjoins the model's left-hand end.
   *
   * @since 1.0
   */
  public static final PropertyBool CONNECT_LEFT = PropertyBool.create("connectleft");

  /**
   * Whether a matching barricade adjoins the model's right-hand end.
   *
   * @since 1.0
   */
  public static final PropertyBool CONNECT_RIGHT = PropertyBool.create("connectright");

  /**
   * How tall this barricade's uprights are, in 1/16 block units. The renderer clamps warning
   * lights to the top of them and stands a mounted sign above them.
   *
   * @since 1.0
   */
  private final float topY;

  /**
   * Constructs a {@link BlockWorkZoneBarricade} instance.
   *
   * @param registryName the registry name of the barricade
   * @param topY         the height of its uprights, in 1/16 block units
   * @param boundingBox  the bounding box of the barricade, in block space
   *
   * @since 1.0
   */
  public BlockWorkZoneBarricade(String registryName, float topY, AxisAlignedBB boundingBox) {
    super(registryName, boundingBox);
    this.topY = topY;
    setDefaultState(this.blockState.getBaseState()
        .withProperty(FACING, EnumFacing.NORTH)
        .withProperty(CONNECT_LEFT, false)
        .withProperty(CONNECT_RIGHT, false));
  }

  /**
   * Gets how tall this barricade's uprights are, in 1/16 block units.
   *
   * @return the upright height
   *
   * @since 1.0
   */
  public float getTopY() {
    return topY;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, CONNECT_LEFT, CONNECT_RIGHT);
  }

  /**
   * Works out which ends this barricade needs, from its neighbours.
   *
   * <p>Connection is resolved here rather than stored, so breaking a barricade out of the middle
   * of a run closes up the two halves without anything having to be notified.</p>
   *
   * @param state  the block state
   * @param access the block access
   * @param pos    the block position
   *
   * @return the state with its connections set
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess access,
      @Nonnull BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);
    return state
        .withProperty(CONNECT_LEFT, joinsAt(access, pos, facing, facing.rotateYCCW()))
        .withProperty(CONNECT_RIGHT, joinsAt(access, pos, facing, facing.rotateY()));
  }

  /**
   * Gets whether the block one step in the given direction is a barricade this one should join.
   *
   * @param access    the block access
   * @param pos       this barricade's position
   * @param facing    this barricade's facing
   * @param direction the direction to look in
   *
   * @return true if the neighbour is the same barricade facing the same way
   *
   * @since 1.0
   */
  private boolean joinsAt(IBlockAccess access, BlockPos pos, EnumFacing facing,
      EnumFacing direction) {
    IBlockState neighbour = access.getBlockState(pos.offset(direction));
    return neighbour.getBlock() == this
        && neighbour.getPropertyKeys().contains(FACING)
        && neighbour.getValue(FACING) == facing;
  }

  /**
   * Mounts a sign, cycles the warning lights, or takes a mounted sign back off.
   *
   * <p>Holding a sign and clicking mounts THAT sign, whatever it is, rather than picking from a
   * list someone had to curate: the renderer reads the sign's own face and panel shape off its
   * model, so a sign added to the mod later works here the day it is added. Clicking with an
   * empty hand cycles the lights, and sneaking takes the sign off and hands it back.</p>
   *
   * @param world  the world the block is in
   * @param pos    the block position
   * @param state  the block state
   * @param player the player
   * @param hand   the hand used
   * @param facing the face clicked
   * @param hitX   the x coordinate of the hit
   * @param hitY   the y coordinate of the hit
   * @param hitZ   the z coordinate of the hit
   *
   * @return true if the click was handled
   *
   * @since 1.0
   */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    TileEntity tileEntity = world.getTileEntity(pos);
    if (!(tileEntity instanceof TileEntityBarricade)) {
      return false;
    }
    TileEntityBarricade barricade = (TileEntityBarricade) tileEntity;
    ItemStack held = player.getHeldItem(hand);

    if (player.isSneaking()) {
      if (!world.isRemote) {
        removeSign(world, pos, state, player, barricade);
      }
      return true;
    }

    Block signBlock = signBlockOf(held);
    if (signBlock != null) {
      if (!world.isRemote) {
        removeSign(world, pos, state, player, barricade);
        barricade.setSign(signBlock.getRegistryName());
        if (!player.isCreative()) {
          held.shrink(1);
        }
        barricade.markDirtySync(world, pos, state);
        say(player, "Mounted " + new ItemStack(signBlock).getDisplayName());
      }
      return true;
    }

    if (!world.isRemote) {
      BarricadeFlashers next = barricade.getFlashers().next();
      barricade.setFlashers(next);
      barricade.markDirtySync(world, pos, state);
      say(player, "Warning lights: " + next.getFriendlyName());
    }
    return true;
  }

  /**
   * Takes any mounted sign off and gives it back to the player.
   *
   * @param world     the world the block is in
   * @param pos       the block position
   * @param state     the block state
   * @param player    the player
   * @param barricade the barricade's tile entity
   *
   * @since 1.0
   */
  private void removeSign(World world, BlockPos pos, IBlockState state, EntityPlayer player,
      TileEntityBarricade barricade) {
    Block mounted = barricade.getSignBlock();
    barricade.setSign(null);
    barricade.markDirtySync(world, pos, state);
    if (mounted == null) {
      return;
    }
    ItemStack returned = new ItemStack(mounted);
    if (!player.isCreative() && !player.inventory.addItemStackToInventory(returned)) {
      player.dropItem(returned, false);
    }
    say(player, "Removed " + returned.getDisplayName());
  }

  /**
   * Gets the sign block a held stack would mount, or null if the stack is not a sign.
   *
   * <p>A sign is any block of this mod's whose class sits in the traffic signs package. Testing
   * the package rather than keeping a list is what makes every sign in the mod eligible without
   * anything having to enumerate them.</p>
   *
   * @param stack the held stack
   *
   * @return the sign block, or null
   *
   * @since 1.0
   */
  @Nullable
  private static Block signBlockOf(ItemStack stack) {
    if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemBlock)) {
      return null;
    }
    Block block = ((ItemBlock) stack.getItem()).getBlock();
    if (block == null || block.getRegistryName() == null) {
      return null;
    }
    return block.getClass().getName().contains(".trafficsigns.") ? block : null;
  }

  /**
   * Tells the player what just changed, on the action bar rather than in chat so that cycling
   * does not fill the log.
   *
   * @param player  the player
   * @param message the message
   *
   * @since 1.0
   */
  private static void say(EntityPlayer player, String message) {
    player.sendStatusMessage(new TextComponentString(
        TextFormatting.YELLOW + message), true);
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityBarricade();
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityBarricade.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityworkzonebarricade";
  }
}
