package com.micatechnologies.minecraft.csm.codeutils;

/**
 * Marker interface for CSM blocks whose {@code updateTick} does real work, and which therefore
 * want {@link AbstractTileEntity#markDirtySync(net.minecraft.world.World,
 * net.minecraft.util.math.BlockPos, boolean)} to keep scheduling a block update for them.
 *
 * <p>Scheduling a block update is not free: it allocates a {@code NextTickListEntry} and inserts
 * it into (then removes it from) the server's pending-tick set. {@code markDirtySync} used to do
 * that on every state sync for every tile entity in the mod, but {@link net.minecraft.block.Block}
 * only does something with a scheduled tick if the block overrides {@code updateTick} — and almost
 * none of ours do, so for signal heads, thermostats, dynamic signs and the rest the scheduled tick
 * landed on vanilla's empty implementation and was pure overhead.</p>
 *
 * <p>Applied to the three blocks that genuinely consume a scheduled tick. Note that all three also
 * seed their own chain from {@code onBlockAdded} and re-arm it from {@code updateTick}, so they do
 * not strictly depend on {@code markDirtySync} to keep running; the marker preserves the previous
 * behaviour exactly rather than relying on that, which keeps this a pure performance change.</p>
 *
 * @author Mica Technologies
 * @since 2026.8
 */
public interface ICsmScheduledTickConsumer {
}
