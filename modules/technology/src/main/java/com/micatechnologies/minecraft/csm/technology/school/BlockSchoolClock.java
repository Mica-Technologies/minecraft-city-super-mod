package com.micatechnologies.minecraft.csm.technology.school;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * A school clock telling the world's time: the classroom clock, the double-dial clock on a wall
 * bracket and the double-dial clock hung from a ceiling. The case and dials are its baked model;
 * {@link TileEntitySchoolClockRenderer} draws the hands on each of the dials given here, whose
 * numbers {@code gen_technology_school.py} writes into the tab line from the model it drew.
 *
 * @since 2026.10
 */
public class BlockSchoolClock extends BlockSchoolFixture
    implements ICsmTileEntityProvider, ISchoolClock {

  private final SchoolClockDial[] dials;

  /**
   * Constructs a clock.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   * @param dials        its dials, in the model's frame
   */
  public BlockSchoolClock(String registryName, double[] box, SchoolClockDial... dials) {
    super(registryName, box);
    this.dials = dials.clone();
  }

  @Override
  public SchoolClockDial[] getClockDials(int meta) {
    return dials;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntitySchoolClock.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityschoolclock";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntitySchoolClock();
  }
}
