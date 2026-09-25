package com.micatechnologies.minecraft.csm.transit.airport;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.transit.platform.PlatformSigns;
import com.micatechnologies.minecraft.csm.transit.platform.TileEntityPlatformSign;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A lit airfield sign on two frangible legs: a taxiway location sign, a direction sign, a runway
 * holding position sign or a distance remaining sign. It is lit with the taxiway lights (it is a
 * {@link BlockAirfieldLight} on the {@code taxiway} circuit, switched by redstone or by a switch
 * anywhere on that circuit), and a click steps what it says instead.
 *
 * <p>What it says is one value in the platform signs' {@link TileEntityPlatformSign}, read as two
 * actual-state properties: {@link #LEGEND}, 1 to the sign's count, which swaps the face's texture,
 * and on a direction sign {@link #ARROW}, which picks the model with the arrow on that side. A
 * click steps the legend; a sneaking click turns a direction sign's arrow round, or steps any other
 * sign back one. The legend's property runs 1 to {@value #MAX_LEGENDS} on every sign, so one
 * class serves all four; a sign's blockstate gives the values past its count its last face.</p>
 *
 * @since 2026.9
 */
public class BlockAirfieldSign extends BlockAirfieldLight implements ICsmTileEntityProvider {

  /** Which side of the letter a direction sign's arrow is on. */
  public enum Arrow implements IStringSerializable {
    LEFT, RIGHT;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /** The most legends any sign has. */
  public static final int MAX_LEGENDS = 9;

  public static final PropertyInteger LEGEND = PropertyInteger.create("legend", 1, MAX_LEGENDS);
  public static final PropertyEnum<Arrow> ARROW = PropertyEnum.create("arrow", Arrow.class);

  /** The light a lit sign gives. */
  private static final int SIGN_LIGHT = 10;

  private final String[] labels;
  private final int count;
  private final boolean arrows;

  /**
   * Constructs an airfield sign.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   * @param labels       what each legend reads, 1 to {@link #MAX_LEGENDS} of them, in the order
   *                     of the blockstate's legend textures
   * @param arrows       whether it is a direction sign, whose arrow goes either side
   */
  public BlockAirfieldSign(String registryName, double[] box, String[] labels, boolean arrows) {
    super(registryName, box, "taxiway", SIGN_LIGHT);
    this.labels = labels.clone();
    this.count = Math.max(1, Math.min(MAX_LEGENDS, labels.length));
    this.arrows = arrows;
    setDefaultState(getDefaultState().withProperty(LEGEND, 1).withProperty(ARROW, Arrow.LEFT));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, LIT, POWERED, LEGEND, ARROW);
  }

  private int values() {
    return arrows ? count * 2 : count;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    int v = PlatformSigns.valueAt(world, pos, values()) - 1;
    return state.withProperty(LEGEND, v % count + 1)
        .withProperty(ARROW, v / count == 0 ? Arrow.LEFT : Arrow.RIGHT);
  }

  /** A click steps the legend; a sneaking click turns the arrow round or steps back. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return false;
    }
    if (world.isRemote) {
      return true;
    }
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityPlatformSign)) {
      return true;
    }
    TileEntityPlatformSign sign = (TileEntityPlatformSign) te;
    int v = Math.max(1, Math.min(values(), sign.getValue())) - 1;
    int legend = v % count;
    int arrow = v / count;
    if (arrows && player.isSneaking()) {
      arrow = 1 - arrow;
    } else {
      legend = (legend + (player.isSneaking() ? count - 1 : 1)) % count;
    }
    sign.setValue(arrow * count + legend + 1);
    world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
        0.3F, 0.8F);
    String label = labels[legend];
    if (arrows) {
      label = arrow == 0 ? "\u2190 " + label : label + " \u2192";
    }
    player.sendStatusMessage(new TextComponentTranslation("csm.transit.airfield_sign", label),
        true);
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityPlatformSign.class;
  }

  @Override
  public String getTileEntityName() {
    return PlatformSigns.TILE_ENTITY_NAME;
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityPlatformSign();
  }
}
