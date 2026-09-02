package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.CsmConstants;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Creates the tile entity a CSM block should have but does not, as its chunk loads on the server.
 *
 * <p><b>Why a block can be missing its tile entity.</b> A tile entity is created in exactly one
 * place in normal play: {@code Chunk.setBlockState}, when the block is placed. Everywhere else the
 * world assumes that a block which wants one already has one saved beside it. So when a block that
 * never had a tile entity is given one in a later version -- the signal backplates gained one when
 * their rendering moved into a {@code TileEntitySpecialRenderer}; the wire mounts gained one when
 * they became live span hangers -- every one already standing in a saved world comes back without
 * it. The same happens to any block written straight into a chunk section without going through
 * {@code setBlockState}: a schematic or clipboard captured before the block had a tile entity, or a
 * world editor's fast path.
 *
 * <p><b>Why it looks the way it does.</b> The server never notices. Nothing on the server asks a
 * backplate for its tile entity, and the client only receives tile entities the server sends in the
 * chunk packet, so the client chunk comes up without one too. The chunk renderer collects tile
 * entity renderers with {@code EnumCreateEntityType.CHECK}, which does not create, and the block
 * itself draws nothing; the plate is invisible. Then something near the player -- a block probe,
 * the crosshair, a tool -- calls {@code World.getTileEntity}, which <em>does</em> create on the
 * client, and the client's own {@code addTileEntity} kicks the chunk into a rebuild. The plate
 * appears, at close range, out of nowhere. That client-side tile entity is never saved, so walking
 * away and back replays the whole thing from a chunk packet that still has no tile entity in it.
 * The invisible-until-close, invisible-again-later symptom is this cycle, exactly.
 *
 * <p><b>What this does about it.</b> On every server-side chunk load it walks the chunk's block
 * sections, and for each CSM block whose state wants a tile entity and has none, creates one and
 * marks the chunk for saving. After that the tile entity is in the chunk packet, in the save, and
 * the walk finds nothing to do the next time the chunk loads. Migration therefore happens once per
 * chunk, as the chunk is visited, at a cost that is a few hundred microseconds of palette lookups
 * spread over the world -- there is no scan of the save, and nothing to run by hand.
 *
 * <p>The tile entity is created with no NBT, which for every block this can apply to is the state a
 * freshly placed one starts in. A block whose tile entity carries data that cannot be defaulted
 * would need its own migration; none of ours does, and if one ever does, its default constructor is
 * the place to say so.
 *
 * <p>Only the server does this. A client chunk is created empty and filled in from the packet after
 * {@code ChunkEvent.Load} fires, so there would be nothing to find, and a client-created tile entity
 * would be the same ephemeral one the bug already makes.
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public class CsmTileEntityBackfillHandler {

  /** Section edge, in blocks. Chunk sections are cubes of this side. */
  private static final int SECTION_EDGE = 16;

  /**
   * Fills in any missing CSM tile entity in a chunk that has just loaded on the server.
   *
   * @param event the chunk load event.
   */
  @SubscribeEvent
  public void onChunkLoad(ChunkEvent.Load event) {
    final Chunk chunk = event.getChunk();
    final World world = chunk.getWorld();
    if (world == null || world.isRemote) {
      return;
    }

    final int created = backfill(world, chunk);
    if (created > 0) {
      chunk.markDirty();
      Csm.getLogger().info("Created {} missing tile entit{} for CSM blocks in chunk [{}, {}] of "
              + "dimension {}; they will be saved with the chunk.",
          created, created == 1 ? "y" : "ies", chunk.x, chunk.z, world.provider.getDimension());
    }
  }

  /**
   * Walks every block in the chunk and creates a tile entity wherever a CSM block wants one and
   * has none.
   *
   * <p>Reads the sections directly rather than asking the world for each position, because the
   * world's own lookup would go back through the chunk map for all sixty-five thousand of them.
   * Empty sections are skipped outright, which on most chunks is most of them.
   *
   * @param world the server world the chunk belongs to.
   * @param chunk the chunk to walk.
   *
   * @return how many tile entities were created.
   */
  private static int backfill(World world, Chunk chunk) {
    int created = 0;
    final int baseX = chunk.x << 4;
    final int baseZ = chunk.z << 4;
    final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    for (ExtendedBlockStorage section : chunk.getBlockStorageArray()) {
      if (section == Chunk.NULL_BLOCK_STORAGE || section.isEmpty()) {
        continue;
      }
      final int baseY = section.getYLocation();
      for (int y = 0; y < SECTION_EDGE; y++) {
        for (int z = 0; z < SECTION_EDGE; z++) {
          for (int x = 0; x < SECTION_EDGE; x++) {
            final IBlockState state = section.get(x, y, z);
            final Block block = state.getBlock();
            // hasTileEntity first: it is a cheap virtual call and false for nearly every block,
            // so the registry-name check below runs only on the handful that matter.
            if (!block.hasTileEntity(state) || !isCsmBlock(block)) {
              continue;
            }
            pos.setPos(baseX + x, baseY + y, baseZ + z);
            // CHECK, not IMMEDIATE: this is the one place that is allowed to create, and it does
            // so explicitly below so the count and the chunk's dirty flag stay honest.
            if (chunk.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK) != null) {
              continue;
            }
            final TileEntity tileEntity = block.createTileEntity(world, state);
            if (tileEntity == null) {
              continue;
            }
            // Goes through the world rather than the chunk alone, because by the time this event
            // fires the chunk has already handed its tile entities to the world; one added only
            // to the chunk would be saved and sent but never ticked or looked up.
            world.setTileEntity(pos.toImmutable(), tileEntity);
            created++;
          }
        }
      }
    }
    return created;
  }

  /**
   * Whether a block is one of ours. Decided by registry namespace rather than by base class, so a
   * CSM block that provides its tile entity without extending {@link AbstractBlock} is covered too,
   * and another mod's blocks are left strictly alone.
   */
  private static boolean isCsmBlock(Block block) {
    final ResourceLocation name = block.getRegistryName();
    return name != null && CsmConstants.MOD_NAMESPACE.equals(name.getNamespace());
  }
}
