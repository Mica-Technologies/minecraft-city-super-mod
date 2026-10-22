package com.micatechnologies.minecraft.csm.transit.platform;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * A platform clock, its hands telling the world's time: the double-faced drum hung from the
 * ceiling, the large one with a dial about two blocks across, and the large one single-faced on
 * a wall. The drum and dials are the baked model; {@link TileEntityPlatformClockRenderer} draws
 * the two hands on each dial, from the dial geometry the block carries (the generator writes it
 * into the tab line from the numbers the model is drawn with).
 *
 * @since 2026.9
 */
public class BlockPlatformClock extends BlockPlatformFixture implements ICsmTileEntityProvider {

  private final double dialX;
  private final double dialY;
  private final double dialFaceZ;
  private final double scale;
  private final boolean doubleFaced;

  /**
   * Constructs the standard double-faced clock.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockPlatformClock(String registryName, double[] box) {
    this(registryName, box, new double[]{8.0, 8.0, 6.3, 1.0}, true, 0);
  }

  /**
   * Constructs a clock.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   * @param dial         facing north, in sixteenths: the dial's middle x and y, its front face z,
   *                     and the hands' scale against the standard clock's
   * @param doubleFaced  whether a second dial faces south, the front one turned half round the
   *                     block's middle
   * @param light        the light it gives (a backlit dial), 0 to 15
   */
  public BlockPlatformClock(String registryName, double[] box, double[] dial,
      boolean doubleFaced, int light) {
    super(registryName, box, false, light);
    this.dialX = dial[0];
    this.dialY = dial[1];
    this.dialFaceZ = dial[2];
    this.scale = dial[3];
    this.doubleFaced = doubleFaced;
  }

  public double getDialX() {
    return dialX;
  }

  public double getDialY() {
    return dialY;
  }

  public double getDialFaceZ() {
    return dialFaceZ;
  }

  public double getScale() {
    return scale;
  }

  public boolean isDoubleFaced() {
    return doubleFaced;
  }

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityPlatformClock.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityplatformclock";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityPlatformClock();
  }
}
