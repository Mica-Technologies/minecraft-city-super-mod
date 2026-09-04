package com.micatechnologies.minecraft.csm.codeutils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Abstract class providing a clean, friendly interface for the creation of {@link CreativeTabs}.
 *
 * <h3>Tab Load Order Reference</h3>
 * <p>Each {@link CsmTab} subclass is annotated with {@code @CsmTab.Load(order = N)}. The complete
 * order list is:</p>
 * <pre>
 *   Order -10 = CsmTabRoadsHidden       (hidden: retired road/signal blocks)
 *   Order  -9 = CsmTabLightingHidden    (hidden: light-up air)
 *   Order  1 = CsmTabBuildingMaterials
 *   Order  2 = CsmTabHvac
 *   Order  3 = CsmTabLifeSafety
 *   Order  4 = CsmTabLighting
 *   Order  5 = CsmTabNovelties
 *   Order  6 = CsmTabPowerGrid
 *   Order  7 = CsmTabRoadSigns
 *   Order  8 = CsmTabTechnology
 *   Order  9 = CsmTabTrafficAccessories
 *   Order 10 = CsmTabTrafficSignals
 *   Order 11 = CsmTabFurniture
 *   Order 12 = CsmTabGaming
 *   Order 13 = CsmTabMaterials
 * </pre>
 * <p>When adding a new tab, choose the next available order value and update this list. A
 * module that owns retiring blocks ships its own hidden tab at a negative order; hidden tabs
 * have no display, so their order relative to each other does not matter, only that they come
 * before the visible tabs as the single hidden tab used to.</p>
 *
 * @author Mica Technologies
 * @version 1.0
 * @since 2023.3
 */
public abstract class CsmTab {

  /**
   * The count of items in the stack used for the tab icon.
   *
   * @since 1.0
   */
  private static final int TAB_ICON_STACK_ITEM_COUNT = 1;

  /**
   * The map of all {@link CsmTab} instances created.
   *
   * @since 1.0
   */
  private static final Map<Class<? extends CsmTab>, CsmTab> TABS = new HashMap<>();

  /**
   * The underlying creative tab instance being created and interfaced.
   *
   * @since 1.0
   */
  private final CreativeTabs tab;

  /**
   * Constructor for the {@link CsmTab} which creates the creative tab using the tab ID, tab icon,
   * and tab searchable property return from the respective abstract method implementations.
   *
   * @since 1.0
   */
  public CsmTab() {
    if (!getTabHidden()) {
      tab = new CreativeTabs(getTabId()) {
        /**
         * Creative tab implementation method for getting the tab icon item.
         *
         * @return an {@link ItemStack} to display as the tab icon item
         */
        @SideOnly(Side.CLIENT)
        @Override
        @Nonnull
        public ItemStack createIcon() {
          return getTabIconStack();
        }

        /**
         * Creative tab implementation method for getting if the tab is searchable.
         *
         * @return {@code true} if the tab is searchable (has a search bar),
         *         {@code false} otherwise
         */
        @SideOnly(Side.CLIENT)
        public boolean hasSearchBar() {
          return getTabSearchable();
        }
      };
    } else {
      tab = null;
    }
  }

  /**
   * Gets a boolean indicating if the tab is hidden (not displayed in the inventory).
   *
   * @return {@code true} if the tab is hidden, otherwise {@code false}
   *
   * @since 1.0
   */
  public abstract boolean getTabHidden();

  /**
   * Gets the ID (unique identifier) of the tab.
   *
   * @return the ID of the tab
   *
   * @since 1.0
   */
  public abstract String getTabId();

  /**
   * Gets the block to use as the icon of the tab.
   *
   * <p>Tabs whose contents are items rather than blocks (for example {@code CSM: Materials})
   * cannot supply a block here; those override {@link #getTabIconStack()} instead and leave this
   * method returning {@code null}.</p>
   *
   * @return the block to use as the icon of the tab, or {@code null} if the tab supplies its icon
   *     by overriding {@link #getTabIconStack()}
   *
   * @since 1.0
   */
  public Block getTabIcon() {
    return null;
  }

  /**
   * Gets the {@link ItemStack} to display as the tab's icon.
   *
   * <p>By default this wraps the block returned by {@link #getTabIcon()}. Tabs that contain only
   * items override this method directly to supply an item stack.</p>
   *
   * @return the {@link ItemStack} to display as the tab's icon
   *
   * @throws IllegalStateException if neither this method nor {@link #getTabIcon()} supplies an
   *     icon
   * @since 2026.7
   */
  @Nonnull
  public ItemStack getTabIconStack() {
    Block icon = getTabIcon();
    if (icon == null) {
      throw new IllegalStateException("Tab icon not found for tab: " + getTabId());
    }
    return new ItemStack(icon, TAB_ICON_STACK_ITEM_COUNT);
  }

  /**
   * Gets a boolean indicating if the tab is searchable (has its own search bar).
   *
   * @return {@code true} if the tab is searchable, otherwise {@code false}
   *
   * @since 1.0
   */
  public abstract boolean getTabSearchable();

  /**
   * Initializes all of the {@link CsmTab} implementations found in the {@link ASMDataTable} of the
   * {@link FMLPreInitializationEvent} during mod startup.
   *
   * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} to get the
   *                                  {@link ASMDataTable} from containing the {@link CsmTab}
   *                                  implementations
   *
   * @throws Exception if an error occurs during initialization
   * @since 1.0
   */
  public static void initTabs(FMLPreInitializationEvent fmlPreInitializationEvent)
      throws Exception {
    // Collect all CsmTab classes with the Load annotation
    List<Class<? extends CsmTab>> tabClasses = new ArrayList<>();
    for (ASMDataTable.ASMData asmData : fmlPreInitializationEvent.getAsmData()
        .getAll(CsmTab.Load.class.getName())) {
      Class<?> clazz = Class.forName(asmData.getClassName());
      if (clazz.getSuperclass() == CsmTab.class) {
        Class<? extends CsmTab> csmTabClass = clazz.asSubclass(CsmTab.class);
        tabClasses.add(csmTabClass);
      }
    }

    // Validate that no two tabs share the same order value
    Logger logger = LogManager.getLogger(CsmTab.class);
    Map<Integer, String> seenOrders = new HashMap<>();
    for (Class<? extends CsmTab> clazz : tabClasses) {
      int order = clazz.getAnnotation(Load.class).order();
      String previousClass = seenOrders.put(order, clazz.getSimpleName());
      if (previousClass != null) {
        logger.warn("Duplicate @CsmTab.Load order value {} found on {} and {}. "
            + "Tab initialization order may be non-deterministic.",
            order, previousClass, clazz.getSimpleName());
      }
    }

    // Sort tab classes based on their order in the Load annotation
    tabClasses.sort((class1, class2) -> {
      int order1 = class1.getAnnotation(Load.class).order();
      int order2 = class2.getAnnotation(Load.class).order();
      return Integer.compare(order1, order2);
    });

    // Initialize the sorted tabs
    for (Class<? extends CsmTab> csmTabClass : tabClasses) {
      CsmTab tab = csmTabClass.getDeclaredConstructor().newInstance();
      TABS.put(csmTabClass, tab);
      tab.initTabElements(fmlPreInitializationEvent);
    }
  }

  /**
   * Initializes all the elements belonging to the tab.
   *
   * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} that is being processed
   *
   * @since 1.0
   */
  public abstract void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent);

  /**
   * Resolves a {@link CreativeTabs} instance back to the tab id of the {@link CsmTab} that
   * created it.
   *
   * <p>This exists because {@link CreativeTabs#getTabLabel()} is annotated
   * {@code @SideOnly(Side.CLIENT)}: FML strips it from the dedicated server, so calling it from
   * common code compiles cleanly and then fails at runtime with a {@code NoSuchMethodError}.
   * Matching against the tabs this mod created is side-safe because {@link #getTabId()} is our
   * own method.</p>
   *
   * @param tab the creative tab to resolve, may be {@code null}
   *
   * @return the owning {@link CsmTab}'s id, or {@code null} if the tab is {@code null} or was
   *     not created by this mod
   *
   * @since 2026.7
   */
  @Nullable
  public static String getTabId(CreativeTabs tab) {
    if (tab == null) {
      return null;
    }
    for (CsmTab csmTab : TABS.values()) {
      if (csmTab.getTab() == tab) {
        return csmTab.getTabId();
      }
    }
    return null;
  }

  /**
   * Gets the {@link CreativeTabs} instance for the {@link CsmTab} implementation class.
   *
   * @param clazz the {@link CsmTab} implementation class to get the {@link CreativeTabs} instance
   *              for
   *
   * @return the {@link CreativeTabs} instance for the {@link CsmTab} implementation class
   *
   * @since 1.0
   */
  public static CreativeTabs get(Class<? extends CsmTab> clazz) {
    return TABS.get(clazz).getTab();
  }

  /**
   * Gets the underlying {@link CreativeTabs} instance being created and interfaced.
   *
   * @return the underlying {@link CreativeTabs} instance being created and interfaced
   *
   * @since 1.0
   */
  public CreativeTabs getTab() {
    return tab;
  }

  /**
   * Initializes the block with the specified class.
   *
   * @param blockClass                the class of the block to initialize
   * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} that is being processed
   *
   * @since 1.0
   */
  public Block initTabBlock(Class<? extends Block> blockClass,
      FMLPreInitializationEvent fmlPreInitializationEvent) {
    Block block = null;
    Object entry = initTabElement(blockClass, fmlPreInitializationEvent);
    if (entry != null) {
      block = blockClass.cast(entry).setCreativeTab(tab);
    }
    return block;
  }

  /**
   * Initializes the block with the given fully-qualified class name, but only if the named mod
   * is installed. When it is not, nothing is registered and the tab simply skips this entry.
   *
   * <p>This exists because a tab and a block in it can belong to different modules. The
   * Technology tab lists the Redstone TTS Module, whose block ships in the optional Text to
   * Speech module; naming the class in Java would make Technology depend on a module that
   * already depends on it. Calling this in the block's own place in {@code initTabElements}
   * keeps the tab's order identical whether or not the other module is installed — the entry is
   * either there, at that position, or absent.</p>
   *
   * <p>Resolving the class by name works because every mod jar is served by the same
   * {@code LaunchClassLoader}: a class in another CSM module's jar is visible here as long as
   * that jar is present, which is exactly what the mod id check establishes.</p>
   *
   * <p>{@link Loader#isModLoaded(String)} is valid to call from pre-initialization: FML builds
   * the mod list during construction, before it fires the first lifecycle event, so the answer
   * is already final and does not change later in the load.</p>
   *
   * @param modId                     the id of the mod that ships the block
   * @param blockClassName            the fully-qualified name of the block class
   * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} that is being processed
   *
   * @return the registered block, or {@code null} if the mod is absent or the class could not be
   *     loaded
   *
   * @since 2026.9
   */
  @Nullable
  public Block initTabBlockIfLoaded(String modId, String blockClassName,
      FMLPreInitializationEvent fmlPreInitializationEvent) {
    if (!Loader.isModLoaded(modId)) {
      return null;
    }
    Class<? extends Block> blockClass = loadTabElementClass(blockClassName, Block.class,
        fmlPreInitializationEvent);
    return blockClass == null ? null : initTabBlock(blockClass, fmlPreInitializationEvent);
  }

  /**
   * Initializes the item with the given fully-qualified class name, but only if the named mod is
   * installed. When it is not, nothing is registered and the tab simply skips this entry.
   *
   * <p>The item counterpart of
   * {@link #initTabBlockIfLoaded(String, String, FMLPreInitializationEvent)}; see there for why
   * the class is named as a string and why the mod check is sound during pre-initialization.</p>
   *
   * @param modId                     the id of the mod that ships the item
   * @param itemClassName             the fully-qualified name of the item class
   * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} that is being processed
   *
   * @return the registered item, or {@code null} if the mod is absent or the class could not be
   *     loaded
   *
   * @since 2026.9
   */
  @Nullable
  public Item initTabItemIfLoaded(String modId, String itemClassName,
      FMLPreInitializationEvent fmlPreInitializationEvent) {
    if (!Loader.isModLoaded(modId)) {
      return null;
    }
    Class<? extends Item> itemClass = loadTabElementClass(itemClassName, Item.class,
        fmlPreInitializationEvent);
    return itemClass == null ? null : initTabItem(itemClass, fmlPreInitializationEvent);
  }

  /**
   * Loads the named class and checks that it is the expected kind of tab element.
   *
   * <p>A failure here is logged rather than thrown: the mod is installed, so the class should be
   * present, but one missing creative-tab entry is a far better outcome than a crash at
   * pre-initialization.</p>
   *
   * @param className                 the fully-qualified name of the class to load
   * @param expectedType              the type the class must be assignable to
   * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} that is being processed
   * @param <T>                       the type of tab element
   *
   * @return the loaded class, or {@code null} if it could not be loaded or is of the wrong type
   *
   * @since 2026.9
   */
  @Nullable
  private <T> Class<? extends T> loadTabElementClass(String className, Class<T> expectedType,
      FMLPreInitializationEvent fmlPreInitializationEvent) {
    try {
      return Class.forName(className).asSubclass(expectedType);
    } catch (ClassNotFoundException | ClassCastException e) {
      fmlPreInitializationEvent.getModLog()
          .error("Error loading tab element class: {}", className, e);
      return null;
    }
  }

  /**
   * Registers a pre-constructed block instance with this tab. Use this instead of
   * {@link #initTabBlock(Class, FMLPreInitializationEvent)} when the block is created by a
   * factory with constructor parameters rather than via no-arg reflection.
   *
   * @param block the pre-constructed block instance to register
   *
   * @return the registered block
   *
   * @since 2024.1
   */
  public Block initTabBlock(Block block) {
    if (block != null) {
      block.setCreativeTab(tab);
    }
    return block;
  }

  /**
   * Registers a pre-constructed item instance with this tab. Use this instead of
   * {@link #initTabItem(Class, FMLPreInitializationEvent)} when the item is created by a
   * factory with constructor parameters rather than via no-arg reflection.
   *
   * @param item the pre-constructed item instance to register
   *
   * @return the registered item
   *
   * @since 2024.1
   */
  public Item initTabItem(Item item) {
    if (item != null) {
      item.setCreativeTab(tab);
    }
    return item;
  }

  /**
   * Initializes the element with the specified class.
   *
   * @param entryClass                the class of the element to initialize
   * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} that is being processed
   *
   * @return the initialized element
   *
   * @since 1.0
   */
  private Object initTabElement(Class<?> entryClass,
      FMLPreInitializationEvent fmlPreInitializationEvent) {
    try {
      return entryClass.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      fmlPreInitializationEvent.getModLog()
          .error("Error initializing tab element: {}", entryClass.getName(), e);
    }
    return null;
  }

  /**
   * Initializes the item with the specified class.
   *
   * @param itemClass                 the class of the item to initialize
   * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} that is being processed
   *
   * @since 1.0
   */
  public Item initTabItem(Class<? extends Item> itemClass,
      FMLPreInitializationEvent fmlPreInitializationEvent) {
    Item item = null;
    Object entry = initTabElement(itemClass, fmlPreInitializationEvent);
    if (entry != null) {
      item = itemClass.cast(entry).setCreativeTab(tab);
    }
    return item;
  }

  /**
   * The annotation interface used to identify/indicate implementations of {@link CsmTab} which
   * shall be loaded/enabled.
   *
   * @since 1.0
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE})
  public @interface Load {

    /**
     * The order in which the annotated {@link CsmTab} implementations should be processed.
     *
     * @return the processing order
     */
    int order();
  }
}
