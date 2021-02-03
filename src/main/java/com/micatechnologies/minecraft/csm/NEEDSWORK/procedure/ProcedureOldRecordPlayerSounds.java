package com.micatechnologies.minecraft.csm.NEEDSWORK.procedure;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.world.World;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.Entity;

@ElementsCitySuperMod.ModElement.Tag
public class ProcedureOldRecordPlayerSounds extends ElementsCitySuperMod.ModElement {
	public ProcedureOldRecordPlayerSounds(ElementsCitySuperMod instance) {
		super(instance, 1113);
	}

	public static void executeProcedure(java.util.HashMap<String, Object> dependencies) {
		if (dependencies.get("entity") == null) {
			System.err.println("Failed to load dependency entity for procedure OldRecordPlayerSounds!");
			return;
		}
		if (dependencies.get("x") == null) {
			System.err.println("Failed to load dependency x for procedure OldRecordPlayerSounds!");
			return;
		}
		if (dependencies.get("y") == null) {
			System.err.println("Failed to load dependency y for procedure OldRecordPlayerSounds!");
			return;
		}
		if (dependencies.get("z") == null) {
			System.err.println("Failed to load dependency z for procedure OldRecordPlayerSounds!");
			return;
		}
		if (dependencies.get("world") == null) {
			System.err.println("Failed to load dependency world for procedure OldRecordPlayerSounds!");
			return;
		}
		Entity entity = (Entity) dependencies.get("entity");
		int x = (int) dependencies.get("x");
		int y = (int) dependencies.get("y");
		int z = (int) dependencies.get("z");
		World world = (World) dependencies.get("world");
		if ((Math.random() < 0.25)) {
			if (entity instanceof EntityPlayer && !world.isRemote) {
				((EntityPlayer) entity).sendStatusMessage(new TextComponentString("OwO what's this record player doing!?"), (true));
			}
			world.playSound((EntityPlayer) null, x, y, z,
					(net.minecraft.util.SoundEvent) net.minecraft.util.SoundEvent.REGISTRY.getObject(new ResourceLocation("csm:oldrecordplayer")),
					SoundCategory.NEUTRAL, (float) 1, (float) 1);
		} else if ((Math.random() < 0.75)) {
			if (entity instanceof EntityPlayer && !world.isRemote) {
				((EntityPlayer) entity).sendStatusMessage(new TextComponentString("OwO what's this record player doing!?"), (true));
			}
			world.playSound((EntityPlayer) null, x, y, z,
					(net.minecraft.util.SoundEvent) net.minecraft.util.SoundEvent.REGISTRY.getObject(new ResourceLocation("csm:oldrecordplayer2")),
					SoundCategory.NEUTRAL, (float) 1, (float) 1);
		}
	}
}
