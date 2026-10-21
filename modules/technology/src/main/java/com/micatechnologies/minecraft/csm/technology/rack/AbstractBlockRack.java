package com.micatechnologies.minecraft.csm.technology.rack;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.BlockUtils;
import com.micatechnologies.minecraft.csm.codeutils.ICsmGlassFronted;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * A rack that holds rack-mount units ({@link ItemRackUnit}): the 42U racks and the 12U wall
 * cabinet. What is mounted lives on a {@link TileEntityServerRack} in the rack's lowest cell and
 * reaches the model as the unlisted {@link #CONTENTS} property, which adds no states;
 * {@code RackBakedModel} draws the units into the chunk mesh, so there is no renderer.
 *
 * <p>Right-click with a unit to mount it in the highest free span that fits; sneak-right-click
 * with an empty hand to take out the unit at the height clicked; right-click with an empty hand
 * to read how full the rack is. Breaking the rack drops what it holds. Any cell of the rack
 * takes the click.</p>
 *
 * <p>A pre-filled rack ({@link #isFillable()} false) is drawn from its blockstate alone: no tile
 * entity, no extended state, and a click does nothing.</p>
 *
 * @since 2026.10
 */
public abstract class AbstractBlockRack extends AbstractBlockRotatableNSEW
    implements ICsmTileEntityProvider, ICsmGlassFronted {

  /** What is mounted, from the tile entity. */
  public static final IUnlistedProperty<RackContents> CONTENTS =
      new IUnlistedProperty<RackContents>() {
        @Override
        public String getName() {
          return "contents";
        }

        @Override
        public boolean isValid(RackContents value) {
          return true;
        }

        @Override
        public Class<RackContents> getType() {
          return RackContents.class;
        }

        @Override
        public String valueToString(RackContents value) {
          return String.valueOf(value);
        }
      };

  /**
   * Hands the constructor's arguments to {@link #getBlockRegistryName()} and
   * {@code createBlockState()}, which the superclass constructor calls before this class's fields
   * are set: {registry name, frame, fillable}.
   */
  protected static final ThreadLocal<Object[]> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final RackFrame frame;
  private final boolean fillable;

  protected AbstractBlockRack(String registryName, RackFrame frame, boolean fillable) {
    super(stash(registryName, frame, fillable), SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 0);
    this.registryName = registryName;
    this.frame = frame;
    this.fillable = fillable;
    PENDING.remove();
  }

  private static Material stash(String registryName, RackFrame frame, boolean fillable) {
    PENDING.set(new Object[]{registryName, frame, fillable});
    return Material.IRON;
  }

  /** The frame, during construction too. */
  protected RackFrame pendingFrame() {
    return frame != null ? frame : (RackFrame) PENDING.get()[1];
  }

  /** Whether units can be mounted, during construction too. */
  protected boolean pendingFillable() {
    return frame != null ? fillable : (Boolean) PENDING.get()[2];
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : (String) PENDING.get()[0];
  }

  /** The kind of rack. */
  public RackFrame getFrame() {
    return frame;
  }

  /** Whether units can be mounted; false for a pre-filled rack. */
  public boolean isFillable() {
    return fillable;
  }

  /**
   * Which cell of the rack a state is: 0 for the lowest.
   *
   * @param state the state
   * @return the cell
   */
  public abstract int cellOf(IBlockState state);

  /** The rack's lowest cell, where its tile entity is. */
  public BlockPos basePos(BlockPos pos, IBlockState state) {
    return pos.down(cellOf(state));
  }

  // --- drawing ---------------------------------------------------------------------------

  @Override
  public boolean isGlassFronted() {
    return !frame.isOpen();
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return pendingFrame().isOpen() ? BlockRenderLayer.SOLID : BlockRenderLayer.TRANSLUCENT;
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

  /**
   * The state with what its rack holds, for the model. Falls back to an empty rack when the tile
   * entity is not there yet, as during chunk load.
   */
  @Override
  @Nonnull
  public IBlockState getExtendedState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    if (!fillable || !(state instanceof IExtendedBlockState)) {
      return state;
    }
    TileEntity te = BlockUtils.getTileEntitySafe(world, basePos(pos, state));
    RackContents contents = te instanceof TileEntityServerRack
        ? ((TileEntityServerRack) te).getContents() : RackContents.EMPTY;
    return ((IExtendedBlockState) state).withProperty(CONTENTS, contents);
  }

  // --- the tile entity -------------------------------------------------------------------

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return fillable && cellOf(state) == 0;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityServerRack.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityserverrack";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return fillable && cellOf(getStateFromMeta(meta)) == 0 ? new TileEntityServerRack() : null;
  }

  // --- mounting and removing units -------------------------------------------------------

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!fillable) {
      return false;
    }
    ItemStack held = player.getHeldItem(hand);
    boolean mount = held.getItem() instanceof ItemRackUnit;
    boolean empty = held.isEmpty() && hand == EnumHand.MAIN_HAND;
    if (!mount && !empty) {
      return false;
    }
    if (world.isRemote) {
      return true;
    }
    TileEntity te = world.getTileEntity(basePos(pos, state));
    if (!(te instanceof TileEntityServerRack)) {
      return true;
    }
    TileEntityServerRack rack = (TileEntityServerRack) te;
    RackContents contents = rack.getContents();
    if (mount) {
      RackUnitType type = ((ItemRackUnit) held.getItem()).getType();
      if (!frame.accepts(type)) {
        tell(player, "message.csm.rack.too_deep");
        return true;
      }
      int top = contents.findSpanFromTop(type.getHeightU(), frame.getTotalU());
      if (top < 0) {
        tell(player, "message.csm.rack.full", type.getHeightU());
        return true;
      }
      rack.setContents(contents.with(type, top));
      if (!player.capabilities.isCreativeMode) {
        held.shrink(1);
      }
      world.playSound(null, pos, SoundEvents.BLOCK_METAL_PLACE, SoundCategory.BLOCKS, 0.8F, 1.2F);
      tell(player, "message.csm.rack.installed", top);
    } else if (player.isSneaking()) {
      int u = frame.uAt(cellOf(state) * 16.0f + hitY * 16.0f);
      int index = contents.indexAt(u);
      if (index < 0) {
        tell(player, "message.csm.rack.empty_here", u);
        return true;
      }
      RackUnitType type = contents.type(index);
      int top = contents.top(index);
      rack.setContents(contents.without(index));
      if (!player.capabilities.isCreativeMode) {
        ItemHandlerHelper.giveItemToPlayer(player, stackOf(type));
      }
      world.playSound(null, pos, SoundEvents.BLOCK_METAL_BREAK, SoundCategory.BLOCKS, 0.8F, 1.0F);
      tell(player, "message.csm.rack.removed", top);
    } else {
      tell(player, "message.csm.rack.summary", contents.usedU(), frame.getTotalU(),
          contents.size());
    }
    return true;
  }

  private static void tell(EntityPlayer player, String key, Object... args) {
    player.sendStatusMessage(new TextComponentTranslation(key, args), true);
  }

  /** One of a unit's item. */
  static ItemStack stackOf(RackUnitType type) {
    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("csm",
        type.getRegistryName()));
    return item == null ? ItemStack.EMPTY : new ItemStack(item);
  }

  /** Drops everything mounted in the rack whose lowest cell is at {@code base}, and empties it. */
  protected void dropContents(World world, BlockPos base) {
    TileEntity te = world.getTileEntity(base);
    if (!(te instanceof TileEntityServerRack)) {
      return;
    }
    TileEntityServerRack rack = (TileEntityServerRack) te;
    RackContents contents = rack.getContents();
    for (int i = 0; i < contents.size(); i++) {
      ItemStack stack = stackOf(contents.type(i));
      if (!stack.isEmpty()) {
        InventoryHelper.spawnItemStack(world, base.getX(), base.getY(), base.getZ(), stack);
      }
    }
    rack.setContents(RackContents.EMPTY);
  }
}
