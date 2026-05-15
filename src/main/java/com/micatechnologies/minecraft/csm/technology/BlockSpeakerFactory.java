package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.codeutils.BlockRotatableNSEWUDFactory;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/**
 * Factory variant of {@link BlockRotatableNSEWUDFactory} that attaches a
 * {@link TileEntitySpeaker} to every placement, turning otherwise cosmetic speaker blocks
 * (Atlas, Bose, FourJay, JBL, Valcom) into linkable TTS broadcast endpoints. The TE pauses
 * ticking when no TTS link exists, so unlinked decorative placements remain effectively
 * free at runtime.
 *
 * <p>All instances share the {@code tileentityspeaker} TE registration; the duplicate-name
 * guard in {@code Csm#init} silently skips re-registration after the first speaker block
 * registers it.</p>
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class BlockSpeakerFactory extends BlockRotatableNSEWUDFactory
    implements ICsmTileEntityProvider {

  public BlockSpeakerFactory(String registryName, Material material, SoundType soundType,
      String harvestToolClass, int harvestLevel, float hardness, float resistance,
      float lightLevel, int lightOpacity, AxisAlignedBB boundingBox,
      boolean opaqueCube, boolean fullCube, boolean connectsRedstone,
      BlockRenderLayer renderLayer, boolean passable, boolean nullCollision) {
    super(registryName, material, soundType, harvestToolClass, harvestLevel, hardness,
        resistance, lightLevel, lightOpacity, boundingBox, opaqueCube, fullCube,
        connectsRedstone, renderLayer, passable, nullCollision);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntitySpeaker.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityspeaker";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntitySpeaker();
  }

  /**
   * Right-click handling for the ambient mode picker. With the {@link ItemTtsLinker} in
   * hand we return false so the linker item's own {@code onItemUse} runs (TTS link / unlink
   * UX). Otherwise an empty-handed click cycles the speaker's ambient sound selection.
   */
  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing,
      float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    ItemStack held = player.getHeldItem(hand);
    if (!held.isEmpty() && held.getItem() instanceof ItemTtsLinker) {
      return false;
    }
    if (worldIn.isRemote) {
      return true;
    }
    TileEntity te = worldIn.getTileEntity(pos);
    if (!(te instanceof TileEntitySpeaker)) {
      return true;
    }
    String newName = ((TileEntitySpeaker) te).cycleAmbientSound();
    player.sendMessage(new TextComponentString("§bSpeaker ambient: §f" + newName));
    return true;
  }
}
