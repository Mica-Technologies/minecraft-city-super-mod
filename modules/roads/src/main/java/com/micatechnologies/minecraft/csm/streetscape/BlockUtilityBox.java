package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRoadSurfaceRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTrafficPoleIgnored;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A utility box standing on the verge: a pad-mount transformer, a telecom pedestal, a buried
 * cable marker post.
 *
 * <p>One class serves every one of them, constructed by registry name with its
 * {@link UtilityBoxSpec}. A box up to two cells a side is one unit: this block, the root, draws
 * the whole of it, and invisible {@link BlockUtilityBoxPart} blocks fill the other cells so the
 * unit is solid and can be clicked anywhere. Placing it places the whole unit or nothing, and
 * breaking any cell of it removes the whole unit and drops one item -- the mast arm curves'
 * rules, for the same reasons.</p>
 *
 * <p>It settles onto sloped and partial-height surfaces like the work zone devices.</p>
 *
 * @version 1.0
 */
public class BlockUtilityBox extends AbstractBlockRoadSurfaceRotatableNSEW
    implements ICsmNoSnowAccumulation, ICsmTrafficPoleIgnored {

  /**
   * Carries the registry name to the superclass constructor, which asks for it before this
   * class's fields are assigned (the same device {@code BlockWorkZoneDevice} uses).
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  /**
   * Set while this class removes a unit's parts itself, so a part's own removal does not start
   * a second demolition of the unit it belongs to.
   */
  static final ThreadLocal<Boolean> DEMOLISHING = ThreadLocal.withInitial(() -> false);

  private final String registryName;
  private final UtilityBoxSpec spec;

  /**
   * Constructs a {@link BlockUtilityBox} instance.
   *
   * @param registryName the registry name of the box
   * @param spec         its size, shape and decal
   */
  public BlockUtilityBox(String registryName, UtilityBoxSpec spec) {
    super(initRegistryName(registryName), SoundType.METAL, "pickaxe", 1, 3F, 12F, 0F, 0);
    this.registryName = registryName;
    this.spec = spec;
  }

  private static Material initRegistryName(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  public UtilityBoxSpec getSpec() {
    return spec;
  }

  /** The root cell's share of the unit, facing north; the base class turns it. */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return spec.getRootCellBoxNorth();
  }

  /** The whole unit is outlined, whichever cell the player is looking at. */
  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos) {
    return unitBoxInWorld(world, pos, state.getValue(FACING));
  }

  /** The unit's box in world coordinates, settled onto the surface below the root. */
  AxisAlignedBB unitBoxInWorld(IBlockAccess world, BlockPos root, EnumFacing facing) {
    return spec.getUnitBox(facing).offset(root)
        .offset(0.0, getRoadSurfaceOffset(world, root), 0.0);
  }

  /**
   * Places the rest of the unit, or refuses the whole of it if anything is in the way.
   */
  @Override
  public void onBlockPlacedBy(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state, @Nonnull EntityLivingBase placer, @Nonnull ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    if (world.isRemote || !spec.isMultiBlock()) {
      return;
    }
    EnumFacing facing = state.getValue(FACING);
    for (BlockPos cell : spec.partCells(pos, facing)) {
      if (!world.getBlockState(cell).getBlock().isReplaceable(world, cell)) {
        refuse(world, pos, placer, cell);
        return;
      }
    }
    IBlockState part = CsmRegistry.getBlock(BlockUtilityBoxPart.REGISTRY_NAME).getDefaultState();
    for (BlockPos cell : spec.partCells(pos, facing)) {
      world.setBlockState(cell, part, 3);
    }
  }

  private void refuse(World world, BlockPos pos, EntityLivingBase placer, BlockPos blockedBy) {
    DEMOLISHING.set(true);
    try {
      world.setBlockToAir(pos);
    } finally {
      DEMOLISHING.set(false);
    }
    if (placer instanceof EntityPlayer) {
      EntityPlayer player = (EntityPlayer) placer;
      if (!player.capabilities.isCreativeMode) {
        // The stack was already spent placing the root, and the root is now gone.
        player.inventory.addItemStackToInventory(new ItemStack(this));
      }
      player.sendStatusMessage(new TextComponentString(String.format(
          "Not enough room for this box -- blocked at %d, %d, %d", blockedBy.getX(),
          blockedBy.getY(), blockedBy.getZ())), true);
    }
  }

  /**
   * Takes the unit's parts with it. Only the root drops an item, so a unit costs one item
   * however it is taken apart.
   */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    if (!world.isRemote && !DEMOLISHING.get() && spec.isMultiBlock()) {
      DEMOLISHING.set(true);
      try {
        for (BlockPos cell : spec.partCells(pos, state.getValue(FACING))) {
          if (world.getBlockState(cell).getBlock() instanceof BlockUtilityBoxPart) {
            world.setBlockToAir(cell);
          }
        }
      } finally {
        DEMOLISHING.set(false);
      }
    }
    super.breakBlock(world, pos, state);
  }

  /**
   * Whether {@code player} may break the unit rooted at {@code root}. Every cell asks this, so a
   * unit that guards what it holds (a mailbox) is guarded whichever cell is hit. Anyone may,
   * here.
   */
  public boolean mayBreakUnit(World world, BlockPos root, EntityPlayer player) {
    return true;
  }

  /**
   * The root of the unit covering {@code cell}, searching every cell a unit could be rooted at,
   * or {@code null} if none covers it.
   */
  @Nullable
  static BlockPos findRoot(IBlockAccess world, BlockPos cell) {
    int reach = UtilityBoxSpec.MAX_CELLS - 1;
    for (int dy = -reach; dy <= 0; dy++) {
      for (int dx = -reach; dx <= reach; dx++) {
        for (int dz = -reach; dz <= reach; dz++) {
          BlockPos candidate = cell.add(dx, dy, dz);
          IBlockState state = world.getBlockState(candidate);
          if (state.getBlock() instanceof BlockUtilityBox) {
            BlockUtilityBox box = (BlockUtilityBox) state.getBlock();
            if (box.spec.covers(candidate, state.getValue(FACING), cell)) {
              return candidate;
            }
          }
        }
      }
    }
    return null;
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
}
