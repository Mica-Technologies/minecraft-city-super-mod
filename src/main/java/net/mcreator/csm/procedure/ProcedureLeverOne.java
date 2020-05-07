package net.mcreator.csm.procedure;

import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;

import net.mcreator.csm.ElementsCitySuperMod;

@ElementsCitySuperMod.ModElement.Tag
public class ProcedureLeverOne extends ElementsCitySuperMod.ModElement {
	public ProcedureLeverOne(ElementsCitySuperMod instance) {
		super(instance, 1034);
	}

	public static void executeProcedure(java.util.HashMap<String, Object> dependencies) {
		if (dependencies.get("x") == null) {
			System.err.println("Failed to load dependency x for procedure LeverOne!");
			return;
		}
		if (dependencies.get("y") == null) {
			System.err.println("Failed to load dependency y for procedure LeverOne!");
			return;
		}
		if (dependencies.get("z") == null) {
			System.err.println("Failed to load dependency z for procedure LeverOne!");
			return;
		}
		if (dependencies.get("world") == null) {
			System.err.println("Failed to load dependency world for procedure LeverOne!");
			return;
		}
		int x = (int) dependencies.get("x");
		int y = (int) dependencies.get("y");
		int z = (int) dependencies.get("z");
		World world = (World) dependencies.get("world");
		if ((true)) {
			world.notifyNeighborsOfStateChange(new BlockPos((int) x, (int) y, (int) z),
					world.getBlockState(new BlockPos((int) x, (int) y, (int) z)).getBlock(), true);
		}
	}
}
