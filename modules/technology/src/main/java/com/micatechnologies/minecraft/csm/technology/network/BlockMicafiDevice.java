package com.micatechnologies.minecraft.csm.technology.network;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.CsmPoleFit;
import com.micatechnologies.minecraft.csm.codeutils.DirectionEight;
import com.micatechnologies.minecraft.csm.codeutils.BlockUtils;
import com.micatechnologies.minecraft.csm.codeutils.ICsmPoleFitted;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A Micafi device hung on a wall or a pole: a wall access point, an in-wall access point, a
 * building bridge, an outdoor radio. It faces whoever places it, away from the wall or pole it
 * is put against, and never stands on end.
 *
 * <p>Right-click changes its status light ({@link MicafiLight}): an access point's ring or a
 * radio's link light, picked per block at construction. The light is two bits of metadata beside
 * the two of the facing; the blockstate swaps the lit texture, so nothing ticks and there is no
 * tile entity. Sneak-click does nothing (but on an {@link Aimed} device, with an empty hand), so
 * a block can still be placed against it.</p>
 *
 * <p>{@link PoleFitted} is the same block drawn to meet the pole behind it ({@link CsmPoleFit}),
 * for the pieces whose clamp is meant for a pole: on a wall the clamp is hidden in it. It also
 * turns a further 45 degrees round a pole diagonally behind it, eight ways in all, with nothing
 * more stored. {@link Aimed} is a wall-arm device whose head swivels on the arm.</p>
 *
 * @since 2026.10
 */
public class BlockMicafiDevice extends AbstractBlockRotatableNSEW {

  /**
   * The registry name and light, for the constructors above this one, which ask for the name and
   * build the state container before this class's fields are set.
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();
  private static final ThreadLocal<PropertyEnum<?>> PENDING_LIGHT = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB boundingBox;
  private final PropertyEnum<?> light;

  /**
   * @param registryName the block's registry name
   * @param box          the device's box facing north, in sixteenths: x0, y0, z0, x1, y1, z1
   * @param light        {@link MicafiLight#RING} or {@link MicafiLight#LINK}
   */
  public BlockMicafiDevice(String registryName, double[] box, PropertyEnum<?> light) {
    super(pending(registryName, light), SoundType.METAL, "pickaxe", 1, 1F, 10F, 0F, 0);
    this.registryName = registryName;
    this.light = light;
    this.boundingBox = new AxisAlignedBB(box[0] / 16, box[1] / 16, box[2] / 16, box[3] / 16,
        box[4] / 16, box[5] / 16);
    PENDING_REGISTRY_NAME.remove();
    PENDING_LIGHT.remove();
    setDefaultState(withLight(getDefaultState(), 0));
  }

  private static Material pending(String name, PropertyEnum<?> light) {
    PENDING_REGISTRY_NAME.set(name);
    PENDING_LIGHT.set(light);
    return Material.IRON;
  }

  private PropertyEnum<?> light() {
    return light != null ? light : PENDING_LIGHT.get();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private IBlockState withLight(IBlockState state, int bits) {
    PropertyEnum property = light();
    return state.withProperty(property, MicafiLight.fromBits(property, bits));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private IBlockState nextLight(IBlockState state) {
    PropertyEnum property = light();
    return state.withProperty(property, MicafiLight.next((Enum) state.getValue(property)));
  }

  private int lightBits(IBlockState state) {
    return ((Enum<?>) state.getValue(light())).ordinal() & 3;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Nonnull
  @Override
  protected BlockStateContainer createBlockState() {
    IProperty<?>[] properties = CsmPoleFit.properties(this, FACING, light());
    if (this instanceof PoleFitted) {
      properties = append(properties, PoleFitted.DIAGONAL);
    }
    if (this instanceof Aimed) {
      properties = append(properties, MicafiAim.PROPERTY);
    }
    return new CsmBlockStateContainer(this, properties);
  }

  private static IProperty<?>[] append(IProperty<?>[] properties, IProperty<?> property) {
    IProperty<?>[] out = Arrays.copyOf(properties, properties.length + 1);
    out[properties.length] = property;
    return out;
  }

  @Nonnull
  @Override
  public IBlockState getStateFromMeta(int meta) {
    return withLight(getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)),
        meta >> 2);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (lightBits(state) << 2);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return sneakActivated(world, pos, state, player, hand);
    }
    if (!world.isRemote) {
      world.setBlockState(pos, nextLight(state), 3);
      world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
          0.3F, 1.2F);
    }
    return true;
  }

  /**
   * A sneak-click (with an empty hand: holding something, sneaking places it instead). Nothing,
   * on all but an {@link Aimed} device, so a block can still be placed against it.
   *
   * @return whether the click was used
   */
  protected boolean sneakActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand) {
    return false;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return boundingBox;
  }

  /** Nothing can be mounted on a device: its faces are not a wall (vanilla calls every face of
   * every block solid unless told otherwise). */
  @Nonnull
  @Override
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

  /** Cutout: a round face is a square picture with its corners cut away. */
  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  /**
   * The same device drawn to meet the pole behind it. A separate class because the opt-in is a
   * marker interface, which has to be known inside the {@code Block} constructor.
   */
  public static class PoleFitted extends BlockMicafiDevice implements ICsmPoleFitted {

    /**
     * Actual state only: the device is turned a further 45 degrees clockwise about a pole that
     * stands diagonally behind it, at {@code pos - diagonalOf(facing)}, rather than the one
     * straight behind it.
     */
    public static final PropertyBool DIAGONAL = PropertyBool.create("diagonal");

    /** The model turned about a diagonal pole, facing north-east, as a selection box. */
    private AxisAlignedBB diagonalBox;

    public PoleFitted(String registryName, double[] box, PropertyEnum<?> light) {
      super(registryName, box, light);
      setDefaultState(getDefaultState().withProperty(DIAGONAL, false));
      this.diagonalBox = diagonalBox(super.getBlockBoundingBox(getDefaultState(), null, null));
    }

    @Override
    protected ItemBlock createItemBlock() {
      return new ItemBlockMicafiPoleDevice(this);
    }

    /**
     * The 45-degree turn clockwise of a stored facing: the direction the device points when the
     * pole is diagonally behind it.
     *
     * @param facing the stored facing
     *
     * @return north-east for north, south-east for east, and so on
     */
    public static DirectionEight diagonalOf(EnumFacing facing) {
      switch (facing) {
        case EAST:
          return DirectionEight.SE;
        case SOUTH:
          return DirectionEight.SW;
        case WEST:
          return DirectionEight.NW;
        default:
          return DirectionEight.NE;
      }
    }

    /**
     * The stored facing for a direction: a cardinal is itself, a diagonal is stored as the
     * cardinal 45 degrees anticlockwise of it ({@link #diagonalOf} undoes it).
     *
     * @param aim the direction the device points
     *
     * @return the facing to store in metadata
     */
    public static EnumFacing storedFacing(DirectionEight aim) {
      switch (aim) {
        case NE:
        case N:
          return EnumFacing.NORTH;
        case SE:
        case E:
          return EnumFacing.EAST;
        case SW:
        case S:
          return EnumFacing.SOUTH;
        default:
          return EnumFacing.WEST;
      }
    }

    /**
     * The direction a placer's yaw points a device: towards the placer, in eight steps.
     *
     * @param yaw the placer's yaw
     *
     * @return the direction the device faces
     */
    public static DirectionEight aimFromYaw(float yaw) {
      int step = Math.floorMod(Math.round(yaw / 45.0F), 8);
      final DirectionEight[] byStep = {DirectionEight.N, DirectionEight.NE, DirectionEight.E,
          DirectionEight.SE, DirectionEight.S, DirectionEight.SW, DirectionEight.W,
          DirectionEight.NW};
      return byStep[step];
    }

    /**
     * @param world the world
     * @param pos   a position
     *
     * @return the pole at {@code pos}, or null
     */
    @Nullable
    static AbstractBlockTrafficPole poleAt(IBlockAccess world, BlockPos pos) {
      Block block = world.getBlockState(pos).getBlock();
      return block instanceof AbstractBlockTrafficPole ? (AbstractBlockTrafficPole) block : null;
    }

    /**
     * Whether a device at {@code pos} stored facing {@code facing} turns about a diagonal pole:
     * nothing it could be mounted on stands straight behind it (no pole, no full block), and a
     * pole stands at {@code pos - diagonalOf(facing)}.
     *
     * @param world  the world
     * @param pos    the device's position
     * @param facing its stored facing
     *
     * @return the diagonal pole, or null when the device is mounted straight
     */
    @Nullable
    static AbstractBlockTrafficPole diagonalPole(IBlockAccess world, BlockPos pos,
        EnumFacing facing) {
      BlockPos behind = pos.offset(facing.getOpposite());
      if (poleAt(world, behind) != null) {
        return null;
      }
      IBlockState behindState = world.getBlockState(behind);
      if (behindState.isFullCube()
          && behindState.getBlockFaceShape(world, behind, facing) == BlockFaceShape.SOLID) {
        return null;
      }
      DirectionEight d = diagonalOf(facing);
      return poleAt(world, pos.add(-d.getOffsetX(), 0, -d.getOffsetZ()));
    }

    @Nonnull
    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
        float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
      IBlockState state = getDefaultState();
      DirectionEight aim = aimFromYaw(placer.rotationYaw);
      if (aim.isDiagonal()) {
        EnumFacing stored = storedFacing(aim);
        if (diagonalPole(worldIn, pos, stored) != null) {
          return state.withProperty(FACING, stored);
        }
      }
      // put against the side of a pole: face away from it, whichever way the placer looks
      if (facing.getAxis().isHorizontal()
          && poleAt(worldIn, pos.offset(facing.getOpposite())) != null) {
        return state.withProperty(FACING, facing);
      }
      return super.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer);
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
      IBlockState actual = super.getActualState(state, worldIn, pos);
      AbstractBlockTrafficPole pole = diagonalPole(worldIn, pos, state.getValue(FACING));
      if (pole == null) {
        return actual.withProperty(DIAGONAL, false);
      }
      return actual.withProperty(DIAGONAL, true)
          .withProperty(CsmPoleFit.PROPERTY, CsmPoleFit.forPoleRadius(pole.getPoleRadius()));
    }

    @Override
    public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source,
        BlockPos pos) {
      if (state.getPropertyKeys().contains(DIAGONAL) && state.getValue(DIAGONAL)) {
        return diagonalBox;
      }
      return super.getBlockBoundingBox(state, source, pos);
    }

    /**
     * The north-frame box of a device turned about a diagonal pole, before the facing turns it:
     * the box's corners carried round the pole 45 degrees clockwise, from straight behind
     * ({@code z + 1}) to diagonally behind ({@code x - 1, z + 1}), as the blockstate's
     * {@code northeast} transform carries the model; then the box round them, inside the block.
     *
     * @param box the device's box facing north
     *
     * @return the turned box
     */
    static AxisAlignedBB diagonalBox(AxisAlignedBB box) {
      double s = Math.sqrt(0.5);
      double minX = Double.MAX_VALUE;
      double minZ = Double.MAX_VALUE;
      double maxX = -Double.MAX_VALUE;
      double maxZ = -Double.MAX_VALUE;
      for (double x : new double[]{box.minX, box.maxX}) {
        for (double z : new double[]{box.minZ, box.maxZ}) {
          // about the block's middle: turn -45 degrees about y (north to north-east), then move
          // so the pole straight behind lands on the pole diagonally behind
          double u = x - 0.5;
          double v = z - 0.5;
          double tu = u * s - v * s + (-1 + s);
          double tv = u * s + v * s + (1 - s);
          minX = Math.min(minX, tu + 0.5);
          maxX = Math.max(maxX, tu + 0.5);
          minZ = Math.min(minZ, tv + 0.5);
          maxZ = Math.max(maxZ, tv + 0.5);
        }
      }
      return new AxisAlignedBB(clamp01(minX), box.minY, clamp01(minZ), clamp01(maxX), box.maxY,
          clamp01(maxZ));
    }

    private static double clamp01(double v) {
      return Math.max(0.0D, Math.min(1.0D, v));
    }

    /**
     * Registers a state mapper that folds {@link #DIAGONAL} into the facing the blockstate reads:
     * {@code facing=northeast} for a north-facing device turned about a diagonal pole, and so on,
     * so the blockstate's eight facings each carry their own turn; and into the pole fit,
     * {@code polefit=thin_diagonal} and so on, whose models are the same with a zero rotation on
     * every element. Without it vanilla's {@code FaceBakery} squares every plain element's faces
     * back to the axes after the 45-degree turn, and the device bakes as stepped slabs.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public void registerModels() {
      super.registerModels();
      ModelLoader.setCustomStateMapper(this, new StateMapperBase() {
        @Nonnull
        @Override
        protected ModelResourceLocation getModelResourceLocation(@Nonnull IBlockState state) {
          return new ModelResourceLocation(Objects.requireNonNull(getRegistryName()),
              variantOf(state));
        }
      });
    }

    /**
     * @param state a state of this block
     *
     * @return its blockstate variant: every property but {@link #DIAGONAL}, sorted by name, the
     *     facing named for the way the device points and the pole fit marked {@code _diagonal}
     *     when it is turned about a diagonal pole
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    String variantOf(IBlockState state) {
      StringBuilder out = new StringBuilder();
      for (Map.Entry<IProperty<?>, Comparable<?>> e : state.getProperties().entrySet()) {
        IProperty property = e.getKey();
        if (property == DIAGONAL) {
          continue;
        }
        if (out.length() > 0) {
          out.append(',');
        }
        out.append(property.getName()).append('=');
        if (property == FACING) {
          EnumFacing facing = (EnumFacing) e.getValue();
          out.append(state.getValue(DIAGONAL) ? facingName(diagonalOf(facing))
              : facing.getName());
        } else {
          out.append(property.getName(e.getValue()));
          if (property == CsmPoleFit.PROPERTY && state.getValue(DIAGONAL)) {
            out.append("_diagonal");
          }
        }
      }
      return out.toString();
    }

    /**
     * @param d a diagonal
     *
     * @return its blockstate name: northeast, southeast, southwest, northwest
     */
    static String facingName(DirectionEight d) {
      switch (d) {
        case SE:
          return "southeast";
        case SW:
          return "southwest";
        case NW:
          return "northwest";
        default:
          return "northeast";
      }
    }
  }
  /**
   * A device on a wall arm whose head turns on the arm's swivel ({@link MicafiAim}): left, centre
   * or right, level or tipped down, cycled by sneak-clicking with an empty hand. The arm and its
   * wall plate stay as placed; the blockstate draws the head as a submodel turned about the ball
   * joint. The metadata is full, so the aim is held by a {@link TileEntityMicafiAim} that does not
   * tick, and read into the actual state.
   */
  public static class Aimed extends BlockMicafiDevice implements ICsmTileEntityProvider {

    public Aimed(String registryName, double[] box, PropertyEnum<?> light) {
      super(registryName, box, light);
    }

    @Override
    protected boolean sneakActivated(World world, BlockPos pos, IBlockState state,
        EntityPlayer player, EnumHand hand) {
      if (!player.getHeldItem(hand).isEmpty()) {
        return false;
      }
      if (!world.isRemote) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityMicafiAim) {
          TileEntityMicafiAim aim = (TileEntityMicafiAim) te;
          aim.setAim(aim.getAim().next());
          aim.markDirtySync(world, pos, state, true);
          world.playSound(null, pos, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS,
              0.25F, 1.6F);
        }
      }
      return true;
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
      IBlockState actual = super.getActualState(state, worldIn, pos);
      TileEntity te = BlockUtils.getTileEntitySafe(worldIn, pos);
      MicafiAim aim = te instanceof TileEntityMicafiAim ? ((TileEntityMicafiAim) te).getAim()
          : MicafiAim.CENTRE;
      return actual.withProperty(MicafiAim.PROPERTY, aim);
    }

    @Override
    public Class<? extends TileEntity> getTileEntityClass() {
      return TileEntityMicafiAim.class;
    }

    @Override
    public String getTileEntityName() {
      return "tileentitymicafiaim";
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(@Nonnull World worldIn, int meta) {
      return new TileEntityMicafiAim();
    }
  }
}
