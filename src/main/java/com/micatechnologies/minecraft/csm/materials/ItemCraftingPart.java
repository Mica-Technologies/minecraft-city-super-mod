package com.micatechnologies.minecraft.csm.materials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * A crafting component ("part") used as the intermediate tier of the mod's survival crafting
 * chain: vanilla ores and ingots are crafted into {@link ItemCraftingPart}s, and those parts are
 * in turn crafted into City Super Mod blocks.
 *
 * <p>Parts have no behavior of their own — they exist purely as recipe ingredients — so they
 * differ only in registry name and tooltip. This class is therefore a factory in the same spirit
 * as {@link com.micatechnologies.minecraft.csm.codeutils.ItemDecorativeFactory}, avoiding a
 * separate class file for each of the parts. It is kept distinct from that class because a
 * decorative item and a crafting component are different things: decorative items deliberately
 * do nothing, whereas parts are functional recipe inputs.</p>
 *
 * @author Mica Technologies
 * @since 2026.7
 */
public class ItemCraftingPart extends AbstractItem {

  /**
   * ThreadLocal used to pass the registry name to the superclass constructor. The
   * {@link AbstractItem} constructor calls {@link #getItemRegistryName()} before subclass fields
   * are initialized, so the name is stashed here before calling {@code super()} and read back
   * out of the ThreadLocal during that window.
   *
   * @since 2026.7
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  /**
   * The registry name of this part.
   *
   * @since 2026.7
   */
  private final String registryName;

  /**
   * The tooltip line shown beneath the part's name, describing what it is used to build.
   *
   * @since 2026.7
   */
  private final String tooltip;

  /**
   * Constructs an {@link ItemCraftingPart}.
   *
   * @param registryName the registry name of the part (also its translation key)
   * @param tooltip      the tooltip line describing what the part is used for
   *
   * @since 2026.7
   */
  public ItemCraftingPart(String registryName, String tooltip) {
    this(initRegistryName(registryName), registryName, tooltip);
  }

  /**
   * Private delegate constructor. The unused first parameter exists only to force
   * {@link #initRegistryName(String)} to run before the implicit {@code super()} call.
   *
   * @since 2026.7
   */
  private ItemCraftingPart(Void ignored, String registryName, String tooltip) {
    this.registryName = registryName;
    this.tooltip = tooltip;
  }

  /**
   * Stashes the pending registry name so {@link #getItemRegistryName()} can return it while the
   * superclass constructor is still running.
   *
   * @param name the registry name to stash
   *
   * @return always {@code null}; the return value exists only to sequence the call
   *
   * @since 2026.7
   */
  private static Void initRegistryName(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return null;
  }

  /**
   * Retrieves the registry name of the part.
   *
   * @return the registry name of the part
   *
   * @since 2026.7
   */
  @Override
  public String getItemRegistryName() {
    if (registryName != null) {
      return registryName;
    }
    return PENDING_REGISTRY_NAME.get();
  }

  /**
   * Appends the part's descriptive tooltip line.
   *
   * @since 2026.7
   */
  @Override
  public void addInformation(ItemStack itemstack, World world, List<String> list,
      ITooltipFlag flag) {
    super.addInformation(itemstack, world, list, flag);
    if (tooltip != null && !tooltip.isEmpty()) {
      list.add(tooltip);
    }
  }
}
