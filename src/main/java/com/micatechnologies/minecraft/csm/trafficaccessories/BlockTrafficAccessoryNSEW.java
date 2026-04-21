package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * NSEW-only counterpart to {@link BlockTrafficAccessoryNSEWUD} with a player-cyclable color
 * finish baked into block metadata. Mount blocks don't need up/down rotation, so we repurpose
 * the two bits freed from the facing byte for a four-value color enum — {@link MountColor}
 * covers White / Silver / Black / Matte White, matching the {@code metal_*} shared textures.
 *
 * <p>Sneak + right-click cycles the color. No tile entity required: the color lives in the
 * blockstate (bits 2-3 of metadata) alongside facing (bits 0-1), and the blockstate JSON
 * overrides texture {@code #0} per color variant. This keeps the block fully static, suitable
 * for later backporting to existing mount blocks without adding TE overhead.
 */
public class BlockTrafficAccessoryNSEW extends AbstractBlockRotatableNSEW
    implements ICsmNoSnowAccumulation {

  /**
   * Metal finish applied to the mount's shared texture ({@code #0}). Names must match the
   * {@code color} variant keys in the blockstate JSON, and their ordinals double as the 2-bit
   * encoding in block metadata.
   */
  public enum MountColor implements IStringSerializable {
    WHITE("white", "White"),
    SILVER("silver", "Silver"),
    BLACK("black", "Black"),
    MATTE_WHITE("matte_white", "Matte White");

    private final String name;
    private final String friendlyName;

    MountColor(String name, String friendlyName) {
      this.name = name;
      this.friendlyName = friendlyName;
    }

    @Override
    public String getName() {
      return name;
    }

    public String getFriendlyName() {
      return friendlyName;
    }

    public MountColor next() {
      MountColor[] values = values();
      return values[(ordinal() + 1) % values.length];
    }

    static MountColor fromOrdinal(int ordinal) {
      MountColor[] values = values();
      if (ordinal < 0 || ordinal >= values.length) return WHITE;
      return values[ordinal];
    }
  }

  public static final PropertyEnum<MountColor> COLOR =
      PropertyEnum.create("color", MountColor.class);

  /**
   * ThreadLocal used to pass the registry name to the superclass constructor. The AbstractBlock
   * constructor calls getBlockRegistryName() before subclass fields are initialized, so we
   * store the name here before calling super() and read it in getBlockRegistryName().
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB boundingBox;
  private final BlockRenderLayer renderLayer;
  private final boolean fullCube;

  public BlockTrafficAccessoryNSEW(String registryName, AxisAlignedBB boundingBox,
      BlockRenderLayer renderLayer, float hardness, boolean fullCube) {
    super(initRegistryName(registryName), SoundType.STONE, "pickaxe", 1, hardness, 10F, 0F, 0,
        false);
    this.registryName = registryName;
    this.boundingBox = boundingBox;
    this.renderLayer = renderLayer;
    this.fullCube = fullCube;
    this.setDefaultState(this.blockState.getBaseState()
        .withProperty(FACING, EnumFacing.NORTH)
        .withProperty(COLOR, MountColor.WHITE));
  }

  private static Material initRegistryName(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return Material.ROCK;
  }

  @Override
  public String getBlockRegistryName() {
    if (registryName != null) {
      return registryName;
    }
    return PENDING_REGISTRY_NAME.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, COLOR);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    // Bits 0-1: facing (0-3). Bits 2-3: color (0-3).
    int facingVal = meta & 0b11;
    int colorVal = (meta >> 2) & 0b11;
    return getDefaultState()
        .withProperty(FACING, EnumFacing.byHorizontalIndex(facingVal))
        .withProperty(COLOR, MountColor.fromOrdinal(colorVal));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    int facingBits = state.getValue(FACING).getHorizontalIndex() & 0b11;
    int colorBits = state.getValue(COLOR).ordinal() & 0b11;
    return facingBits | (colorBits << 2);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return this.getDefaultState()
        .withProperty(FACING, placer.getHorizontalFacing().getOpposite())
        .withProperty(COLOR, MountColor.WHITE);
  }

  /**
   * Sneak + right-click cycles the mount's color finish. Plain right-click is left alone so
   * players can still interact with adjacent blocks or place items near the mount without
   * accidentally repainting it. Color cycling is authoritative on the server; we return
   * {@code true} on the client so the vanilla item-use fallback doesn't fire in the same tick.
   */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side,
      float hitX, float hitY, float hitZ) {
    if (!player.isSneaking()) {
      return false;
    }
    if (world.isRemote) {
      return true;
    }
    MountColor next = state.getValue(COLOR).next();
    world.setBlockState(pos, state.withProperty(COLOR, next), 3);
    player.sendMessage(new TextComponentString(
        TextFormatting.GRAY + "Mount color: " + TextFormatting.WHITE + next.getFriendlyName()));
    return true;
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
    return fullCube;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return renderLayer;
  }
}
