package net.mcreator.csm;

import net.minecraft.world.World;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.EntityPlayer;

@Elementscsm.ModElement.Tag
public class MCreatorFireAlarmSimplexOldVEAlt extends Elementscsm.ModElement {
	public MCreatorFireAlarmSimplexOldVEAlt(Elementscsm instance) {
		super(instance, 1060);
	}

	public static void executeProcedure(java.util.HashMap<String, Object> dependencies) {
		if (dependencies.get("x") == null) {
			System.err.println("Failed to load dependency x for procedure MCreatorFireAlarmSimplexOldVEAlt!");
			return;
		}
		if (dependencies.get("y") == null) {
			System.err.println("Failed to load dependency y for procedure MCreatorFireAlarmSimplexOldVEAlt!");
			return;
		}
		if (dependencies.get("z") == null) {
			System.err.println("Failed to load dependency z for procedure MCreatorFireAlarmSimplexOldVEAlt!");
			return;
		}
		if (dependencies.get("world") == null) {
			System.err.println("Failed to load dependency world for procedure MCreatorFireAlarmSimplexOldVEAlt!");
			return;
		}
		int x = (int) dependencies.get("x");
		int y = (int) dependencies.get("y");
		int z = (int) dependencies.get("z");
		World world = (World) dependencies.get("world");
		world.playSound((EntityPlayer) null, x, y, z, (net.minecraft.util.SoundEvent) net.minecraft.util.SoundEvent.REGISTRY
				.getObject(new ResourceLocation("csm:simplex_voice_evac_old_alt")), SoundCategory.NEUTRAL, (float) 4, (float) 1);
	}
}
