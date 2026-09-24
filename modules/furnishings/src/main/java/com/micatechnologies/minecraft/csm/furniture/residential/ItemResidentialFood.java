package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.CsmConstants;
import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.ICsmItem;
import com.micatechnologies.minecraft.csm.codeutils.IHasModel;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;

/**
 * Food and drink the kitchen appliances make: toast from the toaster, a smoothie from the
 * blender, coffee from the coffee machine. A drink is drunk rather than eaten (the potion
 * animation and sound) and leaves nothing behind: it comes in a paper cup.
 *
 * @since 2026.9
 */
public class ItemResidentialFood extends ItemFood implements IHasModel, ICsmItem {

  private static final Map<String, ItemResidentialFood> BY_NAME = new HashMap<>();

  private final String registryName;
  private final boolean drink;

  /**
   * Constructs a food.
   *
   * @param registryName its registry name
   * @param hunger       the hunger it restores, in half shanks
   * @param saturation   its saturation modifier (bread's is 0.6)
   * @param drink        whether it is drunk rather than eaten
   * @param effect       an effect it gives, or null
   */
  public ItemResidentialFood(String registryName, int hunger, float saturation, boolean drink,
      @Nullable PotionEffect effect) {
    super(hunger, saturation, false);
    this.registryName = registryName;
    this.drink = drink;
    setTranslationKey(registryName);
    setRegistryName(CsmConstants.MOD_NAMESPACE, registryName);
    if (effect != null) {
      setPotionEffect(effect, 1.0F);
      setAlwaysEdible();
    }
    CsmRegistry.registerItem(this);
    BY_NAME.put(registryName, this);
  }

  /**
   * The food made under {@code registryName}, for a recipe's result.
   *
   * @param registryName the registry name
   *
   * @return the item, or null if it has not been made yet
   */
  @Nullable
  public static Item get(String registryName) {
    return BY_NAME.get(registryName);
  }

  /**
   * A stack of the food made under {@code registryName}, or empty if there is none.
   *
   * @param registryName the registry name
   * @param count        how many
   *
   * @return the stack
   */
  public static ItemStack stack(String registryName, int count) {
    Item item = get(registryName);
    return item == null ? ItemStack.EMPTY : new ItemStack(item, count);
  }

  @Override
  public String getItemRegistryName() {
    return registryName;
  }

  @Override
  @Nonnull
  public EnumAction getItemUseAction(@Nonnull ItemStack stack) {
    return drink ? EnumAction.DRINK : EnumAction.EAT;
  }

  @Override
  public void registerModels() {
    Csm.proxy.setCustomModelResourceLocation(this, 0, "inventory");
  }
}
