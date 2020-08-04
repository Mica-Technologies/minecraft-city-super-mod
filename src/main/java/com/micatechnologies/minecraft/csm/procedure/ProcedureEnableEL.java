package com.micatechnologies.minecraft.csm.procedure;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;

@ElementsCitySuperMod.ModElement.Tag
public class ProcedureEnableEL extends ElementsCitySuperMod.ModElement {
	public ProcedureEnableEL(ElementsCitySuperMod instance) {
		super(instance, 1029);
	}

	public static void executeProcedure(java.util.HashMap<String, Object> dependencies) {
		if (dependencies.get("x") == null) {
			System.err.println("Failed to load dependency x for procedure EnableEL!");
			return;
		}
		if (dependencies.get("y") == null) {
			System.err.println("Failed to load dependency y for procedure EnableEL!");
			return;
		}
		if (dependencies.get("z") == null) {
			System.err.println("Failed to load dependency z for procedure EnableEL!");
			return;
		}
		if (dependencies.get("world") == null) {
			System.err.println("Failed to load dependency world for procedure EnableEL!");
			return;
		}
		int x = (int) dependencies.get("x");
		int y = (int) dependencies.get("y");
		int z = (int) dependencies.get("z");
		World world = (World) dependencies.get("world");
		world.notifyNeighborsOfStateChange(new BlockPos((int) x, (int) y, (int) z),
				world.getBlockState(new BlockPos((int) x, (int) y, (int) z)).getBlock(), true);
	}
}
