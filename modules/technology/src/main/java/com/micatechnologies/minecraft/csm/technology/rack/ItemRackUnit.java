package com.micatechnologies.minecraft.csm.technology.rack;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A rack-mount unit, mounted by right-clicking a rack or cabinet with it
 * ({@link AbstractBlockRack}). One factory class: each instance is one {@link RackUnitType}.
 * Its item model is a small box on the unit sheet, so it adds no sprite.
 *
 * @since 2026.10
 */
public class ItemRackUnit extends AbstractItem {

  /** The type, for {@link #getItemRegistryName()}, called before this class's fields are set. */
  private static final ThreadLocal<RackUnitType> PENDING = new ThreadLocal<>();

  private final RackUnitType type;

  /**
   * @param type the unit
   */
  public ItemRackUnit(RackUnitType type) {
    this(stash(type), type);
  }

  private ItemRackUnit(Void ignored, RackUnitType type) {
    this.type = type;
    PENDING.remove();
  }

  private static Void stash(RackUnitType type) {
    PENDING.set(type);
    return null;
  }

  @Override
  public String getItemRegistryName() {
    return (type != null ? type : PENDING.get()).getRegistryName();
  }

  /** The unit. */
  public RackUnitType getType() {
    return type;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
      ITooltipFlag flag) {
    super.addInformation(stack, world, tooltip, flag);
    tooltip.add(I18n.format("tooltip.csm.rack_unit.size", type.getHeightU()));
    if (RackFrame.WALL_12U.accepts(type)) {
      tooltip.add(I18n.format("tooltip.csm.rack_unit.wall"));
    }
    tooltip.add(I18n.format("tooltip.csm.rack_unit.use"));
  }
}
