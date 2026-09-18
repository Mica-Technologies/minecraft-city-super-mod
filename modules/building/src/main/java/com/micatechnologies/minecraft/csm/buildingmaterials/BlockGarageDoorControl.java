package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.constructionsite.BuildingGuiProvider;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
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
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A wall control for a garage door: the lit push button inside a garage, the commercial OPEN /
 * CLOSE / STOP station, or the PIN keypad outside. One class, constructed by registry name.
 *
 * <p>A control works whichever door, or opener, it is linked to: sneak-click the control, then
 * sneak-click the door (see {@link GarageDoorLinks}). The link is kept in a small
 * {@link TileEntityGarageDoorControl} -- data only, never ticked or drawn, so a control costs
 * nothing a frame. The button toggles the door as a one-button opener does (start, stop, reverse);
 * the station's three buttons are told apart by where it was clicked; the keypad opens a PIN
 * screen, and the right code toggles the door.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockGarageDoorControl extends AbstractBlock implements ICsmTileEntityProvider {

  public static final PropertyDirection FACING = BlockHorizontal.FACING;

  /**
   * The kinds of control, by registry name.
   *
   * @since 1.0
   */
  public enum Kind {
    BUTTON("garage_door_button", new AxisAlignedBB(5, 5, 15, 11, 11, 16)),
    STATION("garage_door_station", new AxisAlignedBB(5.5, 3, 14.5, 10.5, 13, 16)),
    KEYPAD("garage_door_keypad", new AxisAlignedBB(5, 3.5, 15, 11, 12.5, 16));

    private final String registryName;
    /** The control, with its front to the north and the wall behind it to the south. */
    private final AxisAlignedBB north;

    Kind(String registryName, AxisAlignedBB pixels) {
      this.registryName = registryName;
      this.north = new AxisAlignedBB(pixels.minX / 16, pixels.minY / 16, pixels.minZ / 16,
          pixels.maxX / 16, pixels.maxY / 16, pixels.maxZ / 16);
    }

    static Kind of(String registryName) {
      for (Kind k : values()) {
        if (k.registryName.equals(registryName)) {
          return k;
        }
      }
      throw new IllegalArgumentException("No garage door control " + registryName);
    }
  }

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final Kind kind;

  /**
   * Constructs a {@link BlockGarageDoorControl}.
   *
   * @param registryName {@code garage_door_button}, {@code garage_door_station} or
   *                     {@code garage_door_keypad}
   *
   * @since 1.0
   */
  public BlockGarageDoorControl(String registryName) {
    super(pendingMaterial(registryName), SoundType.METAL, "pickaxe", 0, 1F, 5F, 0F, 0);
    this.registryName = registryName;
    this.kind = Kind.of(registryName);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material pendingMaterial(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return Material.CIRCUITS;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  public Kind kind() {
    return kind;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex();
  }

  /**
   * On a wall, facing out from it; placed on a floor or ceiling, facing the player.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing f = facing.getAxis().isHorizontal() ? facing
        : placer.getHorizontalFacing().getOpposite();
    return getDefaultState().withProperty(FACING, f);
  }

  /** The keypad remembers who put it up: only they may change its code or relink it. */
  @Override
  public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
    TileEntity te = worldIn.getTileEntity(pos);
    if (!worldIn.isRemote && te instanceof TileEntityGarageDoorControl
        && placer instanceof EntityPlayer) {
      ((TileEntityGarageDoorControl) te).setOwner(placer.getUniqueID());
    }
  }

  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return false;
    }
    TileEntity te = worldIn.getTileEntity(pos);
    if (!(te instanceof TileEntityGarageDoorControl)) {
      return false;
    }
    TileEntityGarageDoorControl control = (TileEntityGarageDoorControl) te;
    if (playerIn.isSneaking()) {
      if (kind == Kind.KEYPAD && !control.mayManage(playerIn.getUniqueID())) {
        if (!worldIn.isRemote) {
          playerIn.sendStatusMessage(new TextComponentTranslation("gui.csm.garage.not_owner"),
              true);
        }
        return true;
      }
      GarageDoorLinks.start(playerIn, worldIn, pos);
      return true;
    }
    if (kind == Kind.KEYPAD) {
      if (worldIn.isRemote) {
        playerIn.openGui(Csm.instance, BuildingGuiProvider.KEYPAD_GUI_ID, worldIn, pos.getX(),
            pos.getY(), pos.getZ());
      }
      return true;
    }
    if (!worldIn.isRemote) {
      BlockGarageDoor.Command command = BlockGarageDoor.Command.TOGGLE;
      if (kind == Kind.STATION) {
        // Top to bottom: OPEN, CLOSE, STOP.
        float y = hitY * 16;
        command = y >= 9.5F ? BlockGarageDoor.Command.OPEN
            : y >= 6.5F ? BlockGarageDoor.Command.CLOSE : BlockGarageDoor.Command.STOP;
      }
      worldIn.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
          0.3F, 0.7F);
      if (!control.operate(command)) {
        playerIn.sendStatusMessage(new TextComponentTranslation("gui.csm.garage.not_linked"),
            true);
      }
    }
    return true;
  }

  @Override
  public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
    TileEntity te = worldIn.getTileEntity(pos);
    if (!worldIn.isRemote && te instanceof TileEntityGarageDoorControl) {
      ((TileEntityGarageDoorControl) te).removed();
    }
    super.breakBlock(worldIn, pos, state);
  }

  // --- tile entity: the link, and the keypad's code ---------------------------------------------

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World worldIn, int meta) {
    return new TileEntityGarageDoorControl();
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityGarageDoorControl.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitygaragedoorcontrol";
  }

  // --- light and shape ----------------------------------------------------------------------------

  /** The push button's lamp glows, as the lit button on a garage wall does. */
  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return kind == Kind.BUTTON ? 4 : 0;
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BlockGarageDoor.turn(kind.north, state.getValue(FACING));
  }

  /** Nothing to bump into, as with a vanilla button. */
  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, @Nonnull World worldIn,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn,
      boolean isActualState) {
  }

  @Override
  @Nonnull
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

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
