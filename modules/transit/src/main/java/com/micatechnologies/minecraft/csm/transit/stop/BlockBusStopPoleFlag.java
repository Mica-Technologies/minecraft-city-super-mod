package com.micatechnologies.minecraft.csm.transit.stop;

import com.micatechnologies.minecraft.csm.codeutils.wayfinding.AbstractBlockWayfindingPanel;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingPreset;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * The full-size bus stop pole's flag: BUS STOP, the agency's name and the stop's routes on a
 * sign in the agency's livery, a block wide and two high, beside the top of a pole of Roads' sign
 * posts, with a stop-name blade ({@link BlockBusStopBlade}) across the top.
 *
 * <p>The flag is Core's wayfinding panel ({@code assets/docs/WAYFINDING_PANELS.md}), baked with
 * no renderer and edited with an empty-handed click; its presets are the agencies' liveries
 * ({@link BusStopSignPresets#FLAG}), and the editor's custom colours are a custom agency. Its
 * third (or fourth) line is the stop's routes, read by the boards and displays
 * ({@link TileEntityBusStopPoleFlag}), so a route may be a letter ("B") as well as a number.</p>
 *
 * <p><b>Placing.</b> The item ({@link ItemPole}) builds the whole stop in one click on the ground,
 * facing the placer: {@link #POLE_HEIGHT} sign posts, the two flag cells beside the top two on
 * the reader's right, and two blade cells above the post and the flag. In survival it takes the
 * two flags from the stack, the posts from the inventory and the blade if there is one (a stop
 * without a blade is built when there is none). Sneaking, or with no room, it places one flag cell
 * as any wayfinding panel is placed, to grow or build a sign by hand. The pieces are ordinary
 * blocks afterwards: each breaks and drops on its own. Because the posts are Roads' sign posts,
 * road signs still stack on the pole.</p>
 *
 * @since 2026.11
 */
public class BlockBusStopPoleFlag extends AbstractBlockWayfindingPanel {

  /** Sign posts in the pole. */
  public static final int POLE_HEIGHT = 4;
  /** Flag cells, stacked, beside the top of the pole. */
  public static final int FLAG_HEIGHT = 2;
  /** Roads' sign post. */
  private static final ResourceLocation SIGN_POST = new ResourceLocation("csm", "signpost");
  /** The blade. */
  private static final ResourceLocation BLADE = new ResourceLocation("csm", "bus_stop_name_blade");

  /**
   * Constructs the flag.
   *
   * @param registryName its registry name
   */
  public BlockBusStopPoleFlag(String registryName) {
    super(registryName);
  }

  @Override
  public WayfindingPreset[] getPresets() {
    return BusStopSignPresets.FLAG;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityBusStopPoleFlag.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitybusstoppoleflag";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityBusStopPoleFlag();
  }

  @Override
  @Nonnull
  protected ItemBlock createItemBlock() {
    return new ItemPole(this);
  }

  /** The pole's item: builds the whole stop; sneaking, one flag cell. */
  public static class ItemPole extends ItemBlock {

    public ItemPole(Block block) {
      super(block);
    }

    @Override
    public boolean placeBlockAt(@Nonnull ItemStack stack, @Nonnull EntityPlayer player,
        World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ,
        @Nonnull IBlockState newState) {
      if (player.isSneaking()) {
        return super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
      }
      Block post = Block.REGISTRY.getObject(SIGN_POST);
      Block blade = Block.REGISTRY.getObject(BLADE);
      EnumFacing facing = newState.getValue(FACING);        // towards the reader
      EnumFacing right = facing.getOpposite().rotateY();    // the reader's right, as they look
      boolean creative = player.capabilities.isCreativeMode;

      List<BlockPos> posts = new ArrayList<>();
      List<BlockPos> flags = new ArrayList<>();
      List<BlockPos> blades = new ArrayList<>();
      for (int i = 0; i < POLE_HEIGHT; i++) {
        posts.add(pos.up(i));
      }
      for (int i = 0; i < FLAG_HEIGHT; i++) {
        flags.add(pos.offset(right).up(POLE_HEIGHT - FLAG_HEIGHT + i));
      }
      blades.add(pos.up(POLE_HEIGHT));
      blades.add(pos.offset(right).up(POLE_HEIGHT));

      Item postItem = post == null ? null : Item.getItemFromBlock(post);
      Item bladeItem = blade == null ? null : Item.getItemFromBlock(blade);
      boolean havePosts = post != null && (creative || count(player, postItem) >= posts.size());
      boolean haveFlags = creative || stack.getCount() >= flags.size();
      boolean withBlade = blade != null
          && (creative || count(player, bladeItem) >= blades.size());
      List<BlockPos> all = new ArrayList<>(posts);
      all.addAll(flags);
      if (withBlade) {
        all.addAll(blades);
      }
      BlockPos blocked = null;
      for (BlockPos p : all) {
        if (p.getY() >= world.getHeight() || !world.getBlockState(p).getBlock()
            .isReplaceable(world, p) || !player.canPlayerEdit(p, EnumFacing.UP, stack)) {
          blocked = p;
          break;
        }
      }
      if (!havePosts || !haveFlags || blocked != null) {
        if (!world.isRemote) {
          player.sendStatusMessage(blocked != null
              ? new TextComponentTranslation("csm.transit.stop_pole.blocked", blocked.getX(),
              blocked.getY(), blocked.getZ())
              : new TextComponentTranslation("csm.transit.stop_pole.short", posts.size(),
                  flags.size()), true);
        }
        return super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
      }
      if (world.isRemote) {
        return true;
      }
      IBlockState postState = post.getStateFromMeta(facing.getHorizontalIndex());
      for (BlockPos p : posts) {
        world.setBlockState(p, postState, 3);
      }
      IBlockState flagState = block.getDefaultState().withProperty(FACING, facing);
      for (BlockPos p : flags) {
        world.setBlockState(p, flagState, 3);
      }
      if (withBlade) {
        IBlockState bladeState = blade.getDefaultState().withProperty(FACING, facing);
        for (BlockPos p : blades) {
          world.setBlockState(p, bladeState, 3);
        }
      }
      if (!creative) {
        stack.shrink(flags.size() - 1);  // onItemUse takes the last
        take(player, postItem, posts.size());
        if (withBlade) {
          take(player, bladeItem, blades.size());
        }
      }
      return true;
    }

    private static int count(EntityPlayer player, @Nullable Item item) {
      if (item == null) {
        return 0;
      }
      int n = 0;
      for (ItemStack s : player.inventory.mainInventory) {
        if (s.getItem() == item) {
          n += s.getCount();
        }
      }
      for (ItemStack s : player.inventory.offHandInventory) {
        if (s.getItem() == item) {
          n += s.getCount();
        }
      }
      return n;
    }

    private static void take(EntityPlayer player, @Nullable Item item, int n) {
      if (item == null) {
        return;
      }
      List<ItemStack> stacks = new ArrayList<>(player.inventory.offHandInventory);
      stacks.addAll(player.inventory.mainInventory);
      for (ItemStack s : stacks) {
        if (n <= 0) {
          return;
        }
        if (s.getItem() == item) {
          int k = Math.min(n, s.getCount());
          s.shrink(k);
          n -= k;
        }
      }
    }
  }
}
