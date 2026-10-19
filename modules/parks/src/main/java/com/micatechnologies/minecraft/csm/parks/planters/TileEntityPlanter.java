package com.micatechnologies.minecraft.csm.parks.planters;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

/**
 * What is planted in a planter: a plant block's registry name and metadata, and nothing else.
 * Data only: no renderer and no ticking. The plant is drawn by the planter's baked model
 * ({@link PlanterBakedModel}), which reads it through the block's extended state, so a change
 * rebuilds the chunk section once, and a sync that changes nothing rebuilds nothing
 * ({@link #getBakedModelKey()}).
 *
 * <p>NBT, settable by {@code /blockdata} (the client redraws, through the block update the
 * command sends):</p>
 * <ul>
 *   <li>{@code plant}: the plant block's registry name, {@code csm:shrub_lavender} or
 *       {@code minecraft:red_flower}; a bare name is CSM's, a potted plant's name stands for
 *       the plant in it, and an empty string empties the planter. A {@code :n} suffix sets the
 *       variant when {@code variant} is not given;</li>
 *   <li>{@code variant}: the plant block's metadata, 0 to 15 (which flower, which sapling).</li>
 * </ul>
 * <p>These two keys are long on purpose: the generator agent asked for them by these names,
 * and an empty planter writes neither.</p>
 *
 * @since 2026.10
 */
public class TileEntityPlanter extends AbstractTileEntity {

  public static final String NBT_PLANT = "plant";
  public static final String NBT_VARIANT = "variant";

  /** The plant's registry name, or null when empty. */
  @Nullable
  private String plant;
  private int variant;

  /** The plant's state, resolved from the name on first use; see {@link #getPlantState()}. */
  @Nullable
  private volatile IBlockState resolved;
  private volatile boolean isResolved;

  @Override
  public void readNBT(NBTTagCompound compound) {
    plant = null;
    variant = 0;
    if (compound.hasKey(NBT_PLANT, Constants.NBT.TAG_STRING)) {
      String text = compound.getString(NBT_PLANT).trim();
      if (!text.isEmpty()) {
        try {
          PlantId id = PlantId.parse(text);
          plant = id.getName();
          variant = id.getMeta();
        } catch (IllegalArgumentException e) {
          plant = null;
        }
      }
    }
    if (plant != null && compound.hasKey(NBT_VARIANT, Constants.NBT.TAG_ANY_NUMERIC)) {
      variant = PlantId.clampMeta(compound.getInteger(NBT_VARIANT));
    }
    isResolved = false;
    resolved = null;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (plant != null) {
      compound.setString(NBT_PLANT, plant);
      compound.setInteger(NBT_VARIANT, variant);
    }
    return compound;
  }

  /** The planted plant's registry name, or null when empty. */
  @Nullable
  public String getPlant() {
    return plant;
  }

  /** The planted plant's metadata. */
  public int getVariant() {
    return variant;
  }

  /** Whether anything is stored, plantable or not. */
  public boolean hasPlant() {
    return plant != null;
  }

  /**
   * The plant's block state, or null when empty or when what is stored is not a plantable block
   * (a {@code /blockdata} typo, or a mod since removed: it is kept, and drawn as nothing).
   * Resolved once and cached; safe to call from a chunk builder thread.
   */
  @Nullable
  public IBlockState getPlantState() {
    if (!isResolved) {
      String name = plant;
      resolved = name == null ? null
          : PlanterPlants.resolve(new PlantId(name.substring(0, name.indexOf(':')),
              name.substring(name.indexOf(':') + 1), variant));
      isResolved = true;
    }
    return resolved;
  }

  /**
   * Plants a plant, or empties the planter, and sends the change to the clients watching it,
   * whose chunk section rebuilds with the plant drawn.
   *
   * @param state the plant, or null to empty
   */
  public void setPlant(@Nullable IBlockState state) {
    if (state == null) {
      plant = null;
      variant = 0;
    } else {
      PlantId id = PlanterPlants.idOf(state);
      plant = id.getName();
      variant = id.getMeta();
    }
    resolved = state;
    isResolved = true;
    if (world != null && !world.isRemote) {
      markDirtySync(world, pos, world.getBlockState(pos), false);
    }
  }

  /** Empties the planter without telling anyone: the block is about to go. */
  void clearSilently() {
    plant = null;
    variant = 0;
    resolved = null;
    isResolved = true;
  }

  /** The chunk section rebuilds on a sync only when the plant changed. */
  @Override
  protected long getBakedModelKey() {
    return plant == null ? 0L : plant.hashCode() * 31L + variant + 1L;
  }
}
