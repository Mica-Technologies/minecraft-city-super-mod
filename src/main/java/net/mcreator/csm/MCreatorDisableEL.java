package net.mcreator.csm;

import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;

@Elementscsm.ModElement.Tag
public class MCreatorDisableEL extends Elementscsm.ModElement {
	public MCreatorDisableEL(Elementscsm instance) {
		super(instance, 1030);
	}

	public static void executeProcedure(java.util.HashMap<String, Object> dependencies) {
		if (dependencies.get("x") == null) {
			System.err.println("Failed to load dependency x for procedure MCreatorDisableEL!");
			return;
		}
		if (dependencies.get("y") == null) {
			System.err.println("Failed to load dependency y for procedure MCreatorDisableEL!");
			return;
		}
		if (dependencies.get("z") == null) {
			System.err.println("Failed to load dependency z for procedure MCreatorDisableEL!");
			return;
		}
		if (dependencies.get("world") == null) {
			System.err.println("Failed to load dependency world for procedure MCreatorDisableEL!");
			return;
		}
		int x = (int) dependencies.get("x");
		int y = (int) dependencies.get("y");
		int z = (int) dependencies.get("z");
		World world = (World) dependencies.get("world");
		world.notifyNeighborsOfStateChange(new BlockPos((int) x, (int) y, (int) z), world.getBlockState(new BlockPos((int) x, (int) y, (int) z))
				.getBlock(), true);
	}
}
