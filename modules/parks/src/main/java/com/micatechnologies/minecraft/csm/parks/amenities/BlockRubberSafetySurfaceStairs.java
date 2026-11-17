package com.micatechnologies.minecraft.csm.parks.amenities;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockStairs;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import java.util.Objects;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;

/**
 * Stairs of the full-block rubber safety surface ({@link BlockRubberSafetySurface}, #365), for a
 * ramp up to a raised play deck or a mound, one per colour
 * ({@code ground_rubber_safety_<colour>_stairs}). They place, turn upside down and join into
 * corners as any stairs do. The full block of the same colour
 * ({@code ground_rubber_safety_<colour>_full}) is their model block, so it must be registered
 * first (the Parks tab lists it earlier).
 *
 * @since 2026.11
 */
public class BlockRubberSafetySurfaceStairs extends AbstractBlockStairs {

  private static final String SUFFIX = "_stairs";

  /**
   * The registry name being constructed. The stairs' base constructor asks for it before this
   * class's field is set, so the constructor parks it here first.
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs one colour of the stairs.
   *
   * @param registryName {@code ground_rubber_safety_<colour>_stairs}
   */
  public BlockRubberSafetySurfaceStairs(String registryName) {
    super(modelBlock(registryName), BlockRubberSafetySurface.SOUND, null, 0,
        BlockRubberSafetySurface.HARDNESS, BlockRubberSafetySurface.RESISTANCE, 0F, 255);
    this.registryName = registryName;
    PENDING_REGISTRY_NAME.remove();
  }

  /** The full block of the same colour, and parks the registry name for the base constructor. */
  private static Block modelBlock(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    String full = registryName.substring(0, registryName.length() - SUFFIX.length()) + "_full";
    return Objects.requireNonNull(CsmRegistry.getBlock(full),
        "the full block " + full + " must be registered before its stairs");
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  /**
   * The vanilla stairs' properties, in CSM's container rather than vanilla's, which would keep a
   * neighbour table on each of the forty states.
   */
  @Override
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, HALF, SHAPE);
  }
}
