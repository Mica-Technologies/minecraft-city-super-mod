package com.micatechnologies.minecraft.csm.procedure;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.world.World;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.EntityPlayer;

@ElementsCitySuperMod.ModElement.Tag
public class ProcedureGalagaSound extends ElementsCitySuperMod.ModElement {
	public ProcedureGalagaSound(ElementsCitySuperMod instance) {
		super(instance, 1112);
	}

	public static void executeProcedure(java.util.HashMap<String, Object> dependencies) {
		if (dependencies.get("x") == null) {
			System.err.println("Failed to load dependency x for procedure GalagaSound!");
			return;
		}
		if (dependencies.get("y") == null) {
			System.err.println("Failed to load dependency y for procedure GalagaSound!");
			return;
		}
		if (dependencies.get("z") == null) {
			System.err.println("Failed to load dependency z for procedure GalagaSound!");
			return;
		}
		if (dependencies.get("world") == null) {
			System.err.println("Failed to load dependency world for procedure GalagaSound!");
			return;
		}
		int x = (int) dependencies.get("x");
		int y = (int) dependencies.get("y");
		int z = (int) dependencies.get("z");
		World world = (World) dependencies.get("world");
		world.playSound((EntityPlayer) null, x, y, z,
				(net.minecraft.util.SoundEvent) net.minecraft.util.SoundEvent.REGISTRY.getObject(new ResourceLocation("csm:galaga_cabinet")),
				SoundCategory.NEUTRAL, (float) 1, (float) 1);
	}
}