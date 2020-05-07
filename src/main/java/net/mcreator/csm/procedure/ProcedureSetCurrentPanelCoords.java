package net.mcreator.csm.procedure;

import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;

import net.mcreator.csm.ElementsCitySuperMod;
import net.mcreator.csm.CitySuperModVariables;

@ElementsCitySuperMod.ModElement.Tag
public class ProcedureSetCurrentPanelCoords extends ElementsCitySuperMod.ModElement {
	public ProcedureSetCurrentPanelCoords(ElementsCitySuperMod instance) {
		super(instance, 1056);
	}

	public static void executeProcedure(java.util.HashMap<String, Object> dependencies) {
		if (dependencies.get("x") == null) {
			System.err.println("Failed to load dependency x for procedure SetCurrentPanelCoords!");
			return;
		}
		if (dependencies.get("y") == null) {
			System.err.println("Failed to load dependency y for procedure SetCurrentPanelCoords!");
			return;
		}
		if (dependencies.get("z") == null) {
			System.err.println("Failed to load dependency z for procedure SetCurrentPanelCoords!");
			return;
		}
		if (dependencies.get("world") == null) {
			System.err.println("Failed to load dependency world for procedure SetCurrentPanelCoords!");
			return;
		}
		int x = (int) dependencies.get("x");
		int y = (int) dependencies.get("y");
		int z = (int) dependencies.get("z");
		World world = (World) dependencies.get("world");
		world.notifyNeighborsOfStateChange(new BlockPos((int) x, (int) y, (int) z),
				world.getBlockState(new BlockPos((int) x, (int) y, (int) z)).getBlock(), true);
		CitySuperModVariables.WorldVariables.get(world).currentPanelX = (double) x;
		CitySuperModVariables.WorldVariables.get(world).syncData(world);
		CitySuperModVariables.WorldVariables.get(world).currentPanelY = (double) y;
		CitySuperModVariables.WorldVariables.get(world).syncData(world);
		CitySuperModVariables.WorldVariables.get(world).currentPanelZ = (double) z;
		CitySuperModVariables.WorldVariables.get(world).syncData(world);
	}
}
