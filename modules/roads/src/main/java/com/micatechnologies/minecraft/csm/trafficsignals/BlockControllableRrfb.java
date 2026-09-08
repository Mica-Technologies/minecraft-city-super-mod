package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import javax.annotation.Nonnull;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * RRFB (Rectangular Rapid Flashing Beacon): two rectangular amber indications in one housing,
 * mounted with a pedestrian crossing sign at an uncontrolled or midblock crossing. Dark until a
 * pedestrian calls it, then firing alternating rapid bursts at approaching traffic.
 *
 * <p>Links as a {@link SIGNAL_SIDE#PEDESTRIAN_BEACON}, so a controller drives it exactly as it
 * drives the HAWK: dark on the off state, lit while the crossing is being served.</p>
 *
 * <p>Unlike the configurable signal heads, this block has no custom renderer. The lamps run
 * their sequence inside an animated texture rather than at render time, which is what keeps
 * them in step: Minecraft advances texture animations from the global tick counter, so every
 * RRFB in the world flashes together the way a pair facing each other across a crossing should.
 * The strip is the 16-frame, 800 ms sequence FHWA Interim Approval IA-21 requires -- a
 * left/right wig-wag, then two flashes with <em>both</em> lamps lit, then a 250 ms dark gap. See
 * {@code dev-env-utils/scripts/gen_rrfb_textures.py}, which checks the sequence against the
 * standard's timings rather than against an assumption about it.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public class BlockControllableRrfb extends AbstractBlockControllableSignal {

  public BlockControllableRrfb() {
    super(Material.ROCK);
  }

  @Override
  public SIGNAL_SIDE getSignalSide(World world, BlockPos blockPos) {
    return SIGNAL_SIDE.PEDESTRIAN_BEACON;
  }

  /**
   * The beacon flashes whenever it is called, so it must be driven in the controller's flash
   * mode as well.
   *
   * @return always {@code true}
   */
  @Override
  public boolean doesFlash() {
    return true;
  }

  @Override
  public String getBlockRegistryName() {
    return "controllablerrfb";
  }

  /**
   * Matches the housing drawn by the model: the 15 x 4 unit bar on the facing side of the
   * block, three units deep, rather than the whole cube.
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return new AxisAlignedBB(0.03125D, 0.375D, 0.3125D, 0.96875D, 0.625D, 0.5D);
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  /**
   * Cutout so the transparent area around the housing does not draw as a black panel.
   */
  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }
}
