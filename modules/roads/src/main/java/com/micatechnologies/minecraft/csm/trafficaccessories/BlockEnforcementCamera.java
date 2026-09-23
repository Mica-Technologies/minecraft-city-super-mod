package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole;
import com.micatechnologies.minecraft.csm.codeutils.CsmPoleFit;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import com.micatechnologies.minecraft.csm.codeutils.ICsmPoleFitted;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTrafficPoleIgnored;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A traffic enforcement device -- red light camera, speed camera or the flash unit that fires for
 * them -- that mounts on the mod's own traffic poles rather than carrying a pole of its own.
 *
 * <p>Decorative: the models are procedural OBJs from
 * {@code dev-env-utils/scripts/gen_enforcement_cameras.py}, one per device, with the white and
 * black finishes picked by texture overrides in each blockstate. The block is never constructed
 * directly; it comes in the two flavours that say how it meets the pole:
 *
 * <ul>
 *   <li>{@link PoleTop} sits in the block above a pole, its slip-fitter collar sleeved down over
 *   the pole's top. The pole ignores it, so it does not grow a mount stub up into the collar.</li>
 *   <li>{@link SideArm} hangs beside a pole on an arm running sideways into it, turned to face
 *   the road, and is {@link ICsmPoleFitted}: its arm and clamp plate are drawn for whichever of
 *   the three pole widths is there, on whichever side it is.</li>
 * </ul>
 *
 * <p>They are subclasses rather than a flag because both markers must be known inside
 * {@code createBlockState}, which runs before any subclass field exists.
 *
 * @since 2026.9
 */
public abstract class BlockEnforcementCamera extends AbstractBlockRotatableNSEW
    implements ICsmNoSnowAccumulation {

  /** The red light camera's pole-top housing and its collar. */
  public static final AxisAlignedBB BB_RED_LIGHT_CAMERA_TOP =
      new AxisAlignedBB(0.10, 0.0, 0.0, 0.90, 1.0, 0.92);
  /** The red light camera on its side arm; the arm itself is left out, as it runs into the pole. */
  public static final AxisAlignedBB BB_RED_LIGHT_CAMERA_SIDE =
      new AxisAlignedBB(0.26, 0.29, 0.0, 0.74, 0.85, 0.72);
  /** The flash unit on its side arm. */
  public static final AxisAlignedBB BB_FLASH_UNIT_SIDE =
      new AxisAlignedBB(0.28, 0.29, 0.06, 0.72, 0.89, 0.68);
  /** The speed camera's pole-top cabinet and its collar. */
  public static final AxisAlignedBB BB_SPEED_CAMERA_TOP =
      new AxisAlignedBB(0.16, 0.0, 0.16, 0.84, 1.0, 0.84);
  /** The speed camera on its side arm. */
  public static final AxisAlignedBB BB_SPEED_CAMERA_SIDE =
      new AxisAlignedBB(0.23, 0.29, 0.0, 0.77, 0.91, 0.72);

  /**
   * Passes the registry name to {@link #getBlockRegistryName()}, which the superclass constructor
   * calls before this class's fields are assigned.
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB boundingBox;

  private BlockEnforcementCamera(String registryName, AxisAlignedBB boundingBox) {
    super(pendingMaterial(registryName), SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 0);
    this.registryName = registryName;
    this.boundingBox = boundingBox;
  }

  private static Material pendingMaterial(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
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

  /**
   * A device on top of a pole, on a slip-fitter collar that fits any of the three pole widths.
   */
  public static class PoleTop extends BlockEnforcementCamera implements ICsmTrafficPoleIgnored {

    public PoleTop(String registryName, AxisAlignedBB boundingBox) {
      super(registryName, boundingBox);
    }
  }

  /**
   * A device hung beside a pole on an arm that runs sideways into it, turned to face the road
   * rather than away from the pole -- the way the side cameras on a real enforcement pole are hung.
   *
   * <p>{@link #FACING} is the way the device looks. Which side the arm runs to is {@link #ARM},
   * resolved in the actual state from whichever neighbour is a pole, and never stored; the pole
   * fit is then taken from that neighbour, not from the block behind as for the other
   * {@link ICsmPoleFitted} blocks. The blockstate names every combination of the two, since both
   * choose the model.
   */
  public static class SideArm extends BlockEnforcementCamera implements ICsmPoleFitted {

    /**
     * The arm's hand, as seen from behind the device looking the way it looks: {@code right} puts
     * the pole on the right. Actual-state only.
     */
    public static final PropertyEnum<ArmSide> ARM = PropertyEnum.create("arm", ArmSide.class);

    public SideArm(String registryName, AxisAlignedBB boundingBox) {
      super(registryName, boundingBox);
    }

    @Override
    @Nonnull
    protected BlockStateContainer createBlockState() {
      return new BlockStateContainer(this, FACING, ARM, CsmPoleFit.PROPERTY);
    }

    /**
     * Clicked onto the side of a pole, the device turns a quarter away from that face, toward
     * whichever side the placer is standing, so it hangs beside the pole looking back at them.
     * Placed any other way it faces the placer, as every rotatable block does.
     */
    @Override
    @Nonnull
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
        float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
      IBlockState state = super.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ,
          meta, placer);
      if (facing.getAxis().isHorizontal() && isPole(worldIn, pos.offset(facing.getOpposite()))) {
        EnumFacing turned = facing.rotateY();
        double toPlacerX = placer.posX - (pos.getX() + 0.5D);
        double toPlacerZ = placer.posZ - (pos.getZ() + 0.5D);
        if (turned.getXOffset() * toPlacerX + turned.getZOffset() * toPlacerZ < 0.0D) {
          turned = turned.getOpposite();
        }
        state = state.withProperty(FACING, turned);
      }
      return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    @Nonnull
    public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
        @Nonnull BlockPos pos) {
      EnumFacing looking = state.getValue(FACING);
      EnumFacing right = looking.rotateY();
      ArmSide arm = !isPole(worldIn, pos.offset(right)) && isPole(worldIn, pos.offset(
          right.getOpposite())) ? ArmSide.LEFT : ArmSide.RIGHT;
      EnumFacing toPole = arm == ArmSide.RIGHT ? right : right.getOpposite();
      // CsmPoleFit looks behind the direction a block reaches, so reach away from the pole.
      return CsmPoleFit.apply(state.withProperty(ARM, arm), worldIn, pos, toPole.getOpposite());
    }

    private static boolean isPole(IBlockAccess world, BlockPos pos) {
      return world.getBlockState(pos).getBlock() instanceof AbstractBlockTrafficPole;
    }
  }

  /**
   * Which side of a {@link SideArm} device its arm runs to.
   */
  public enum ArmSide implements IStringSerializable {
    RIGHT,
    LEFT;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase();
    }
  }
}
