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
 * Factory for horn strobes that offer more selectable tones than a block's metadata can hold, and
 * so keep the choice in a {@link TileEntityFireAlarmSoundIndex}. Sneak-right-click cycles the tone
 * and reports the new one.
 *
 * <p>The counterpart to {@link BlockFireAlarmSounderStrobeFactory}, which is for appliances whose
 * sound is fixed or lives in metadata. Instances differ only in registry name, bounding box,
 * strobe lens and tone set, so they need no class of their own.</p>
 */
public class BlockFireAlarmSoundIndexStrobeFactory extends AbstractBlockFireAlarmSounder
    implements ICsmTileEntityProvider, IStrobeBlock, ISoundIndexBlock {

  /**
   * ThreadLocal used to pass the registry name to the superclass constructor. The AbstractBlock
   * constructor calls getBlockRegistryName() before subclass fields are initialized, so we
   * store the name here before calling super() and read it in getBlockRegistryName().
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB boundingBox;
  private final float[] strobeLensFrom;
  private final float[] strobeLensTo;
  private final String[] soundResourceNames;
  private final String[] soundDisplayNames;

  public BlockFireAlarmSoundIndexStrobeFactory(String registryName, AxisAlignedBB boundingBox,
      float[] strobeLensFrom, float[] strobeLensTo, String[] soundResourceNames,
      String[] soundDisplayNames) {
    this(initRegistryName(registryName), registryName, boundingBox, strobeLensFrom, strobeLensTo,
        soundResourceNames, soundDisplayNames);
  }

  private BlockFireAlarmSoundIndexStrobeFactory(Void ignored, String registryName,
      AxisAlignedBB boundingBox, float[] strobeLensFrom, float[] strobeLensTo,
      String[] soundResourceNames, String[] soundDisplayNames) {
    this.registryName = registryName;
    this.boundingBox = boundingBox;
    this.strobeLensFrom = strobeLensFrom;
    this.strobeLensTo = strobeLensTo;
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
  public float[] getStrobeLensFrom() {
    return strobeLensFrom;
  }

  @Override
  public float[] getStrobeLensTo() {
    return strobeLensTo;
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
