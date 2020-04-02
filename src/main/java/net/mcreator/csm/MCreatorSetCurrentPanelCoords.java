package net.mcreator.csm;

import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;

@Elementscsm.ModElement.Tag
public class MCreatorSetCurrentPanelCoords extends Elementscsm.ModElement {
	public MCreatorSetCurrentPanelCoords(Elementscsm instance) {
		super(instance, 1056);
	}

	public static void executeProcedure(java.util.HashMap<String, Object> dependencies) {
		if (dependencies.get("x") == null) {
			System.err.println("Failed to load dependency x for procedure MCreatorSetCurrentPanelCoords!");
			return;
		}
		if (dependencies.get("y") == null) {
			System.err.println("Failed to load dependency y for procedure MCreatorSetCurrentPanelCoords!");
			return;
		}
		if (dependencies.get("z") == null) {
			System.err.println("Failed to load dependency z for procedure MCreatorSetCurrentPanelCoords!");
			return;
		}
		if (dependencies.get("world") == null) {
			System.err.println("Failed to load dependency world for procedure MCreatorSetCurrentPanelCoords!");
			return;
		}
		int x = (int) dependencies.get("x");
		int y = (int) dependencies.get("y");
		int z = (int) dependencies.get("z");
		World world = (World) dependencies.get("world");
		world.notifyNeighborsOfStateChange(new BlockPos((int) x, (int) y, (int) z), world.getBlockState(new BlockPos((int) x, (int) y, (int) z))
				.getBlock(), true);
		csmVariables.WorldVariables.get(world).currentPanelX = (double) x;
		csmVariables.WorldVariables.get(world).syncData(world);
		csmVariables.WorldVariables.get(world).currentPanelY = (double) y;
		csmVariables.WorldVariables.get(world).syncData(world);
		csmVariables.WorldVariables.get(world).currentPanelZ = (double) z;
		csmVariables.WorldVariables.get(world).syncData(world);
	}
}
