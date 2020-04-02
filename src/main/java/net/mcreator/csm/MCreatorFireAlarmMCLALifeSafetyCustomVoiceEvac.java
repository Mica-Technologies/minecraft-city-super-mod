package net.mcreator.csm;

import net.minecraft.world.World;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.EntityPlayer;

@Elementscsm.ModElement.Tag
public class MCreatorFireAlarmMCLALifeSafetyCustomVoiceEvac extends Elementscsm.ModElement {
	public MCreatorFireAlarmMCLALifeSafetyCustomVoiceEvac(Elementscsm instance) {
		super(instance, 1059);
	}

	public static void executeProcedure(java.util.HashMap<String, Object> dependencies) {
		if (dependencies.get("x") == null) {
			System.err.println("Failed to load dependency x for procedure MCreatorFireAlarmMCLALifeSafetyCustomVoiceEvac!");
			return;
		}
		if (dependencies.get("y") == null) {
			System.err.println("Failed to load dependency y for procedure MCreatorFireAlarmMCLALifeSafetyCustomVoiceEvac!");
			return;
		}
		if (dependencies.get("z") == null) {
			System.err.println("Failed to load dependency z for procedure MCreatorFireAlarmMCLALifeSafetyCustomVoiceEvac!");
			return;
		}
		if (dependencies.get("world") == null) {
			System.err.println("Failed to load dependency world for procedure MCreatorFireAlarmMCLALifeSafetyCustomVoiceEvac!");
			return;
		}
		int x = (int) dependencies.get("x");
		int y = (int) dependencies.get("y");
		int z = (int) dependencies.get("z");
		World world = (World) dependencies.get("world");
		world.playSound((EntityPlayer) null, x, y, z,
				(net.minecraft.util.SoundEvent) net.minecraft.util.SoundEvent.REGISTRY.getObject(new ResourceLocation("csm:mclalsve")),
				SoundCategory.NEUTRAL, (float) 4, (float) 1);
	}
}
