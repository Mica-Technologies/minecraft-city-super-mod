package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Factory for sounders that offer more selectable tones than a block's metadata can hold, and so
 * keep the choice in a {@link TileEntityFireAlarmSoundIndex}. Sneak-right-click cycles the tone
 * and reports the new one. Used as is by devices with no strobe (the fire alarm bells);
 * {@link BlockFireAlarmSoundIndexStrobeFactory} adds a strobe.
 *
 * @since 2026.9
 */
public class BlockFireAlarmSoundIndexFactory extends AbstractBlockFireAlarmSounder
    implements ICsmTileEntityProvider, ISoundIndexBlock {

  /**
   * ThreadLocal used to pass the registry name to the superclass constructor. The AbstractBlock
   * constructor calls getBlockRegistryName() before subclass fields are initialized, so we
   * store the name here before calling super() and read it in getBlockRegistryName().
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB boundingBox;
  private final String[] soundResourceNames;
  private final String[] soundDisplayNames;

  public BlockFireAlarmSoundIndexFactory(String registryName, AxisAlignedBB boundingBox,
      String[] soundResourceNames, String[] soundDisplayNames) {
    this(initRegistryName(registryName), registryName, boundingBox, soundResourceNames,
        soundDisplayNames);
  }

  private BlockFireAlarmSoundIndexFactory(Void ignored, String registryName,
      AxisAlignedBB boundingBox, String[] soundResourceNames, String[] soundDisplayNames) {
    this.registryName = registryName;
    this.boundingBox = boundingBox;
    this.soundResourceNames = soundResourceNames;
    this.soundDisplayNames = soundDisplayNames;
  }

  private static Void initRegistryName(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return null;
  }

  @Override
  public String getBlockRegistryName() {
    if (registryName != null) {
      return registryName;
    }
    return PENDING_REGISTRY_NAME.get();
  }

  /**
   * The tone a freshly placed block plays, for callers that have no world to read the selection
   * from. {@link #getSoundResourceName(World, BlockPos, IBlockState)} is what the control panel
   * uses.
   */
  @Override
  public String getSoundResourceName(IBlockState blockState) {
    return soundResourceNames[0];
  }

  @Override
  public String getSoundResourceName(World world, BlockPos pos, IBlockState blockState) {
    TileEntity tileEntity = world.getTileEntity(pos);
    if (tileEntity instanceof TileEntityFireAlarmSoundIndex) {
      int index = ((TileEntityFireAlarmSoundIndex) tileEntity).getSoundIndex();
      if (index >= 0 && index < soundResourceNames.length) {
        return soundResourceNames[index];
      }
    }
    return soundResourceNames[0];
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return boundingBox;
  }

  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
      float hitX, float hitY, float hitZ) {
    if (playerIn.isSneaking()) {
      TileEntity tileEntity = worldIn.getTileEntity(pos);
      if (tileEntity instanceof TileEntityFireAlarmSoundIndex) {
        TileEntityFireAlarmSoundIndex soundIndex = (TileEntityFireAlarmSoundIndex) tileEntity;
        soundIndex.cycleSoundIndex(soundResourceNames.length);
        if (!worldIn.isRemote) {
          playerIn.sendMessage(new TextComponentString(
              "Alarm sound changed to: " + soundDisplayNames[soundIndex.getSoundIndex()]));
        }
      }
      return true;
    }
    return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityFireAlarmSoundIndex.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityfirealarmsoundindex";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityFireAlarmSoundIndex();
  }
}
