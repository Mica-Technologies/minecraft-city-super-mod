package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityDynamicStreetSign;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignMount;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A street name blade carried across the top of a sign post -- the green plate over a STOP sign
 * at an intersection, with a second blade crossing it for the other street.
 *
 * <p>This replaces the two blank "street name sign mount" blocks, which drew an empty plate
 * nothing could be written on and had to be decorated by placing something else in front of
 * them. The blade is now lettered in world, and it is lettered by the <b>same editor and the
 * same document</b> as the [dynamic street sign](../../../../../../../assets/docs/DYNAMIC_STREET_SIGN_SYSTEM.md):
 * prefix, name, suffix, city line, block number, route shield or civic logo, arrow, colour,
 * border, corners, internal illumination and a second blade are all inherited rather than
 * reimplemented. What differs is where the blade sits and what holds it up.</p>
 *
 * <p>It is an ordinary {@link AbstractBlockSign}, so it stacks on the mod's sign posts and takes
 * the eight facings, the extension post onto a slab, the setback and the back-to-back pairing
 * exactly as the 472 signs beside it do.</p>
 *
 * <p>Two of them ship, and the difference is the bracket, which is the block's rather than the
 * player's: the flat clamp plate a blade bolts into, and the collar a blade passes through.
 * Both take one or two blades.</p>
 *
 * <p>GUI id 29 -- the dynamic street sign's own editor, with the mount shown and not offered.</p>
 *
 * @version 1.0
 * @since 2026.9.20
 */
public class BlockStreetNameBlade extends AbstractBlockSign implements ICsmTileEntityProvider {

  /** GUI id these blocks open; the editor is the dynamic street sign's. */
  public static final int GUI_ID = 29;

  /**
   * Carries the registry name to the superclass constructor, which asks for it before this
   * class's fields are assigned -- the same hand-off {@link BlockTrafficSign} makes, and for
   * the same reason. Without it the block registers as {@code csm:null}.
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final StreetSignMount mount;

  /**
   * @param registryName the block's registry name
   * @param mount        the post-top bracket this block carries
   *
   * @since 1.0
   */
  public BlockStreetNameBlade(String registryName, StreetSignMount mount) {
    this(initRegistryName(registryName), registryName, mount);
  }

  private BlockStreetNameBlade(Void ignored, String registryName, StreetSignMount mount) {
    this.registryName = registryName;
    this.mount = mount;
  }

  private static Void initRegistryName(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return null;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  /** The bracket this block's blades are carried on. */
  public StreetSignMount getMount() {
    return mount;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return mount == StreetSignMount.POST_TOP_CROSS
        ? TileEntityStreetNameBladeCross.class : TileEntityStreetNameBladeClamp.class;
  }

  @Override
  public String getTileEntityName() {
    return mount == StreetSignMount.POST_TOP_CROSS
        ? "tileentitystreetnamebladecross" : "tileentitystreetnamebladeclamp";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return mount == StreetSignMount.POST_TOP_CROSS
        ? new TileEntityStreetNameBladeCross() : new TileEntityStreetNameBladeClamp();
  }

  /**
   * Opens the editor, and consumes the click on BOTH sides -- a {@code @SideOnly(Side.CLIENT)}
   * override is absent from the class on a server, so the server would otherwise fall through to
   * the default and place the held block instead.
   */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing,
      float hitX, float hitY, float hitZ) {
    if (world.isRemote) {
      player.openGui(Csm.instance, GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
    }
    return true;
  }

  /**
   * Keeps the blade's illumination switchable, as the dynamic street sign's is. The power state
   * lives on the tile entity because the block's metadata is full with the facing.
   */
  @Override
  public void neighborChanged(IBlockState state, World world, BlockPos pos,
      net.minecraft.block.Block blockIn, BlockPos fromPos) {
    super.neighborChanged(state, world, pos, blockIn, fromPos);
    if (world.isRemote) {
      return;
    }
    TileEntity tileEntity = world.getTileEntity(pos);
    if (tileEntity instanceof TileEntityDynamicStreetSign) {
      ((TileEntityDynamicStreetSign) tileEntity)
          .setPowered(world.getRedstonePowerFromNeighbors(pos) > 0);
    }
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, net.minecraft.world.IBlockAccess access,
      BlockPos pos, EnumFacing facing) {
    return true;
  }
}
