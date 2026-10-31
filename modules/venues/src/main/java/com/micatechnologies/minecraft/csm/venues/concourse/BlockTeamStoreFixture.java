package com.micatechnologies.minecraft.csm.venues.concourse;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import com.micatechnologies.minecraft.csm.venues.av.VenueColours;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A team store fixture whose garments wear the team's colours: the hat wall (nine caps on a
 * slatwall panel), the apparel table (folded tees in stacks) and the jersey mannequin; the
 * jersey wall rack is {@link BlockJerseyRack}. One set of grey models serves every team: the
 * garments carry tint 0 (the team colour) and tint 1 (the trim), and a block colour handler reads
 * them from the fixture's {@link TileEntityTeamColour}.
 *
 * <p>A click with an empty hand steps the team colour through the sixteen venue colours, a
 * sneak-click the trim; {@code /blockdata x y z {c:9,t:6}} sets both (crimson and gold).
 * Metadata is the facing (S 0, W 1, N 2, E 3), towards the shopper; the hat wall and the jersey
 * rack hang on the wall behind them.</p>
 *
 * @since 2026.10
 */
public class BlockTeamStoreFixture extends BlockVenue implements ICsmTileEntityProvider {

  /** Which fixture. */
  public enum Kind {
    /** The jersey wall rack: three jerseys face out on a waterfall arm. */
    JERSEY_RACK(BlockConcourseRun.box(1, 1, 2, 15, 16, 16)),
    /** The hat wall: nine caps on hooks on a slatwall panel. */
    HAT_WALL(BlockConcourseRun.box(0, 0, 10, 16, 16, 16)),
    /** The apparel table: folded tees in stacks on a nesting table. */
    APPAREL_TABLE(BlockConcourseRun.box(0, 0, 1, 16, 15, 15)),
    /** The jersey mannequin: a mannequin in the team's jersey. */
    MANNEQUIN(BlockConcourseRun.box(3, 0, 4, 13, 16, 12));

    final AxisAlignedBB box;

    Kind(AxisAlignedBB box) {
      this.box = box;
    }
  }

  private final Kind kind;

  /**
   * Constructs a fixture.
   *
   * @param registryName its registry name
   * @param kind         which fixture
   */
  public BlockTeamStoreFixture(String registryName, Kind kind) {
    super(registryName, kind == Kind.MANNEQUIN ? Material.ROCK : Material.WOOD,
        kind == Kind.MANNEQUIN ? SoundType.STONE : SoundType.WOOD, 1.0F);
    this.kind = kind;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return kind.box;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      TileEntity te = world.getTileEntity(pos);
      if (te instanceof TileEntityTeamColour) {
        boolean trim = player.isSneaking();
        int c = ((TileEntityTeamColour) te).step(trim);
        player.sendStatusMessage(new TextComponentTranslation(trim
            ? "csm.venues.store.trim" : "csm.venues.store.colour",
            new TextComponentTranslation(VenueColours.key(c))), true);
      }
    }
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityTeamColour.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityvenueteamcolour";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityTeamColour();
  }
}
