package net.mcreator.csm;

import net.minecraft.world.World;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.EntityPlayer;

@Elementscsm.ModElement.Tag
public class MCreatorFireAlarmSimplexVoiceEvac extends Elementscsm.ModElement {
	public MCreatorFireAlarmSimplexVoiceEvac(Elementscsm instance) {
		super(instance, 1039);
	}

	public static void executeProcedure(java.util.HashMap<String, Object> dependencies) {
		if (dependencies.get("x") == null) {
			System.err.println("Failed to load dependency x for procedure MCreatorFireAlarmSimplexVoiceEvac!");
			return;
		}
		if (dependencies.get("y") == null) {
			System.err.println("Failed to load dependency y for procedure MCreatorFireAlarmSimplexVoiceEvac!");
			return;
		}
		if (dependencies.get("z") == null) {
			System.err.println("Failed to load dependency z for procedure MCreatorFireAlarmSimplexVoiceEvac!");
			return;
		}
		if (dependencies.get("world") == null) {
			System.err.println("Failed to load dependency world for procedure MCreatorFireAlarmSimplexVoiceEvac!");
			return;
		}
		int x = (int) dependencies.get("x");
		int y = (int) dependencies.get("y");
		int z = (int) dependencies.get("z");
		World world = (World) dependencies.get("world");
		world.playSound((EntityPlayer) null, x, y, z,
				(net.minecraft.util.SoundEvent) net.minecraft.util.SoundEvent.REGISTRY.getObject(new ResourceLocation("csm:sven")),
				SoundCategory.NEUTRAL, (float) 3, (float) 1);
	}
}
