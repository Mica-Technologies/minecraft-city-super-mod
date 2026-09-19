package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.ItemStackHandler;

/**
 * The Door Workshop's contents: three material slots (frame, upper, lower), an edit slot for doors
 * to re-program, the output, the design on its screen, and the designs saved on it -- all saved
 * with the block, so a workshop remembers how it was set up.
 *
 * <p>The design is a whole door: three materials and the behaviour. A block put in a material slot
 * becomes the design's material, and stays its material when it is taken out, so an empty slot
 * shows (as a ghost) the block the next door still needs. What a door is made of is always what
 * the slot holds, or, for a slot that is empty, the design's material -- which only a player in
 * creative may make a door from. A player in creative uses nothing up.</p>
 *
 * <p>Making, re-programming, copying and the saved designs all happen here, on the server, from
 * what the slots actually hold; the screen only asks.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class TileEntityDoorWorkshop extends AbstractTileEntity {

  public static final int FRAME = 0;
  public static final int UPPER = 1;
  public static final int LOWER = 2;
  public static final int EDIT = 3;
  public static final int OUTPUT = 4;

  /** The most designs a workshop keeps. */
  public static final int MAX_DESIGNS = 12;
  /** The longest name a design may have. */
  public static final int MAX_NAME = 24;

  private final ItemStackHandler inventory = new ItemStackHandler(5) {
    @Override
    protected void onContentsChanged(int slot) {
      markDirty();
      if (slot <= LOWER && world != null && !world.isRemote) {
        IBlockState material = slotMaterial(slot);
        if (material != null && material != designMaterial(design, slot)) {
          setDesign(withMaterial(design, slot, material));
        }
      }
    }
  };

  /** The door on the screen. */
  private CustomDoorSettings design = CustomDoorSettings.DEFAULT;

  /** The designs saved on this workshop, in the order they were saved. */
  private final List<Design> designs = new ArrayList<>();

  /**
   * A saved design and its name.
   *
   * @since 1.0
   */
  public static final class Design {

    public final String name;
    public final CustomDoorSettings settings;

    Design(String name, CustomDoorSettings settings) {
      this.name = name;
      this.settings = settings;
    }
  }

  public ItemStackHandler getInventory() {
    return inventory;
  }

  public CustomDoorSettings getDesign() {
    return design;
  }

  public List<Design> getDesigns() {
    return Collections.unmodifiableList(designs);
  }

  /**
   * Replaces the design. On the client, the screen's own copy until the server's arrives.
   *
   * @param design the design
   *
   * @since 1.0
   */
  void setDesign(CustomDoorSettings design) {
    this.design = design;
    if (world != null && !world.isRemote) {
      markDirtySync(world, pos, true);
    }
  }

  /**
   * Gives the design another behaviour, keeping its materials.
   *
   * @param b the behaviour (its materials are ignored)
   *
   * @since 1.0
   */
  void setBehaviour(CustomDoorSettings b) {
    setDesign(design.withBehaviour(b.movement(), b.sound(), b.openTicks(), b.autoCloseTicks(),
        b.redstone(), b.proximity()));
  }

  // --- materials ------------------------------------------------------------------------------------

  /**
   * Whether a stack can be a door's material: a block item of a block a door can be made of.
   *
   * @param stack the stack
   *
   * @return whether it can
   *
   * @since 1.0
   */
  public static boolean isMaterial(ItemStack stack) {
    return !stack.isEmpty() && stack.getItem() instanceof ItemBlock
        && CustomDoorMaterials.allowed(((ItemBlock) stack.getItem()).getBlock());
  }

  @SuppressWarnings("deprecation")
  private static IBlockState stateOf(ItemStack stack) {
    Block block = ((ItemBlock) stack.getItem()).getBlock();
    return block.getStateFromMeta(stack.getItem().getMetadata(stack.getMetadata()));
  }

  /** The material the stack in a material slot is, or null if it is empty or no material. */
  @Nullable
  private IBlockState slotMaterial(int slot) {
    ItemStack stack = inventory.getStackInSlot(slot);
    return isMaterial(stack) ? stateOf(stack) : null;
  }

  /**
   * The design's material for a material slot.
   *
   * @param settings the design
   * @param slot     {@link #FRAME}, {@link #UPPER} or {@link #LOWER}
   *
   * @return its material
   *
   * @since 1.0
   */
  public static IBlockState designMaterial(CustomDoorSettings settings, int slot) {
    return slot == FRAME ? settings.frame() : slot == UPPER ? settings.upper() : settings.lower();
  }

  private static CustomDoorSettings withMaterial(CustomDoorSettings s, int slot,
      IBlockState material) {
    return s.withMaterials(slot == FRAME ? material : s.frame(),
        slot == UPPER ? material : s.upper(), slot == LOWER ? material : s.lower());
  }

  /**
   * The door the next Make would make: each material from its slot, or from the design where the
   * slot is empty.
   *
   * @return the door's settings
   *
   * @since 1.0
   */
  public CustomDoorSettings effective() {
    CustomDoorSettings s = design;
    for (int slot = FRAME; slot <= LOWER; slot++) {
      IBlockState m = slotMaterial(slot);
      if (m != null) {
        s = withMaterial(s, slot, m);
      }
    }
    return s;
  }

  // --- making ---------------------------------------------------------------------------------------

  /**
   * Makes one door into the output -- if every material slot holds a material, or the maker is in
   * creative, and the output can take it. Uses up one of each material unless in creative.
   *
   * @param creative whether the maker is in creative
   *
   * @return whether a door was made
   *
   * @since 1.0
   */
  boolean make(boolean creative) {
    if (BlockCustomDoor.instance() == null) {
      return false;
    }
    for (int slot = FRAME; slot <= LOWER; slot++) {
      if (!creative && slotMaterial(slot) == null) {
        return false;
      }
    }
    ItemStack door = BlockCustomDoor.instance().stack(effective());
    ItemStack out = inventory.getStackInSlot(OUTPUT);
    if (out.isEmpty()) {
      inventory.setStackInSlot(OUTPUT, door);
    } else if (ItemStack.areItemsEqual(out, door) && ItemStack.areItemStackTagsEqual(out, door)
        && out.getCount() < out.getMaxStackSize()) {
      ItemStack grown = out.copy();
      grown.grow(1);
      inventory.setStackInSlot(OUTPUT, grown);
    } else {
      return false;
    }
    if (!creative) {
      inventory.extractItem(FRAME, 1, false);
      inventory.extractItem(UPPER, 1, false);
      inventory.extractItem(LOWER, 1, false);
    }
    return true;
  }

  /**
   * Makes doors until the materials run out or the output is a full stack.
   *
   * @param creative whether the maker is in creative
   *
   * @return how many were made
   *
   * @since 1.0
   */
  int makeAll(boolean creative) {
    int made = 0;
    while (made < 64 && make(creative)) {
      made++;
    }
    return made;
  }

  /**
   * Gives the doors in the edit slot the design's behaviour, keeping what they are made of.
   *
   * @return whether there were doors to re-program
   *
   * @since 1.0
   */
  boolean apply() {
    ItemStack doors = inventory.getStackInSlot(EDIT);
    if (doors.isEmpty() || !(doors.getItem() instanceof ItemCustomDoor)) {
      return false;
    }
    CustomDoorSettings was = ItemCustomDoor.getSettings(doors);
    ItemStack copy = doors.copy();
    ItemCustomDoor.setSettings(copy, was.withBehaviour(design.movement(), design.sound(),
        design.openTicks(), design.autoCloseTicks(), design.redstone(), design.proximity()));
    inventory.setStackInSlot(EDIT, copy);
    return true;
  }

  /**
   * Takes the whole design -- materials and behaviour -- from the door in the edit slot.
   *
   * @return whether there was a door to copy
   *
   * @since 1.0
   */
  boolean copy() {
    ItemStack door = inventory.getStackInSlot(EDIT);
    if (door.isEmpty() || !(door.getItem() instanceof ItemCustomDoor)) {
      return false;
    }
    setDesign(ItemCustomDoor.getSettings(door));
    return true;
  }

  // --- saved designs --------------------------------------------------------------------------------

  /**
   * Saves the design under a name, replacing a design of the same name.
   *
   * @param name the name, cut to {@link #MAX_NAME}
   *
   * @return whether it was saved (false if the name is blank, or the list is full)
   *
   * @since 1.0
   */
  boolean saveDesign(String name) {
    String n = clean(name);
    if (n.isEmpty()) {
      return false;
    }
    for (int i = 0; i < designs.size(); i++) {
      if (designs.get(i).name.equals(n)) {
        designs.set(i, new Design(n, design));
        markDirtySync(world, pos, true);
        return true;
      }
    }
    if (designs.size() >= MAX_DESIGNS) {
      return false;
    }
    designs.add(new Design(n, design));
    markDirtySync(world, pos, true);
    return true;
  }

  boolean loadDesign(int index) {
    if (index < 0 || index >= designs.size()) {
      return false;
    }
    setDesign(designs.get(index).settings);
    return true;
  }

  boolean deleteDesign(int index) {
    if (index < 0 || index >= designs.size()) {
      return false;
    }
    designs.remove(index);
    markDirtySync(world, pos, true);
    return true;
  }

  /**
   * A name as it may be saved: printable characters only, trimmed, at most {@link #MAX_NAME}.
   *
   * @param name the name typed
   *
   * @return the name to keep
   *
   * @since 1.0
   */
  public static String clean(String name) {
    StringBuilder b = new StringBuilder();
    for (char c : name.toCharArray()) {
      if (c >= ' ' && c != 127 && c != '§' && b.length() < MAX_NAME) {
        b.append(c);
      }
    }
    return b.toString().trim();
  }

  // --- NBT ------------------------------------------------------------------------------------------

  private static final String KEY_INVENTORY = "inv";
  private static final String KEY_DESIGNS = "designs";
  private static final String KEY_NAME = "n";

  @Override
  public void readNBT(NBTTagCompound compound) {
    if (compound.hasKey(KEY_INVENTORY)) {
      inventory.deserializeNBT(compound.getCompoundTag(KEY_INVENTORY));
    }
    design = CustomDoorSettings.read(compound);
    designs.clear();
    NBTTagList list = compound.getTagList(KEY_DESIGNS, Constants.NBT.TAG_COMPOUND);
    for (int i = 0; i < list.tagCount() && designs.size() < MAX_DESIGNS; i++) {
      NBTTagCompound t = list.getCompoundTagAt(i);
      designs.add(new Design(clean(t.getString(KEY_NAME)), CustomDoorSettings.read(t)));
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setTag(KEY_INVENTORY, inventory.serializeNBT());
    design.write(compound);
    NBTTagList list = new NBTTagList();
    for (Design d : designs) {
      NBTTagCompound t = new NBTTagCompound();
      t.setString(KEY_NAME, d.name);
      d.settings.write(t);
      list.appendTag(t);
    }
    compound.setTag(KEY_DESIGNS, list);
    return compound;
  }

  /**
   * Clients need the design and the saved designs, for the screen, and not the slots, which the
   * container keeps in step itself.
   */
  @Override
  @Nonnull
  public NBTTagCompound getUpdateTag() {
    NBTTagCompound tag = super.getUpdateTag();
    tag.removeTag(KEY_INVENTORY);
    return tag;
  }
}
