package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
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
 * Factory for horn strobes offering a CHOICE OF TWO tones, kept in the block's own metadata.
 * Sneak-right-click cycles the tone and reports the new one.
 *
 * <p>Two is the limit, and the reason is arithmetic: metadata has four bits, a NSEWUD appliance
 * spends six values of it on rotation, and 6 x 2 = 12 is the last multiple that fits. An appliance
 * wanting more has to keep the choice in a tile entity instead, which is what
 * {@link BlockFireAlarmSoundIndexStrobeFactory} is for. Where a sound is fixed altogether,
 * {@link BlockFireAlarmSounderStrobeFactory} is simpler than either.</p>
 */
public class BlockFireAlarmSounderStrobeMetaSoundFactory extends AbstractBlockFireAlarmSounder
    implements ICsmTileEntityProvider, IStrobeBlock {

  /**
   * The tone selection. One property object serves every instance: a block state property is
   * compared by name and type, so sharing it is what lets these blocks be built by a factory
   * rather than a class apiece.
   */
  public static final PropertyInteger SOUND = PropertyInteger.create("sound", 0, 1);

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

  public BlockFireAlarmSounderStrobeMetaSoundFactory(String registryName, AxisAlignedBB boundingBox,
      float[] strobeLensFrom, float[] strobeLensTo, String[] soundResourceNames,
      String[] soundDisplayNames) {
    this(initRegistryName(registryName), registryName, boundingBox, strobeLensFrom, strobeLensTo,
        soundResourceNames, soundDisplayNames);
  }

  private BlockFireAlarmSounderStrobeMetaSoundFactory(Void ignored, String registryName,
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

  @Override
  public String getSoundResourceName(IBlockState blockState) {
    return soundResourceNames[blockState.getValue(SOUND)];
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
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, SOUND);
  }

  @Override
  public IBlockState getStateFromMeta(int meta) {
    return this.getDefaultState()
        .withProperty(FACING, EnumFacing.byIndex(meta % 6))
        .withProperty(SOUND, (int) Math.floor((double) meta / 6.0));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getIndex() + state.getValue(SOUND) * 6;
  }

  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
      float hitX, float hitY, float hitZ) {
    if (playerIn.isSneaking()) {
      IBlockState cycled = state.cycleProperty(SOUND);
      worldIn.setBlockState(pos, cycled);
      if (!worldIn.isRemote) {
        playerIn.sendMessage(new TextComponentString(
            "Alarm horn sound changed to: " + soundDisplayNames[cycled.getValue(SOUND)]));
      }
      return true;
    }
    return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityFireAlarmStrobe.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityfirealarmstrobe";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityFireAlarmStrobe();
  }
}
