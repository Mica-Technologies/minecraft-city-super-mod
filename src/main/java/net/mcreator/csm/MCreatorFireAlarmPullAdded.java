package net.mcreator.csm;

import net.minecraft.world.World;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.Entity;

@Elementscsm.ModElement.Tag
public class MCreatorFireAlarmPullAdded extends Elementscsm.ModElement {
	public MCreatorFireAlarmPullAdded(Elementscsm instance) {
		super(instance, 1057);
	}

	public static void executeProcedure(java.util.HashMap<String, Object> dependencies) {
		if (dependencies.get("entity") == null) {
			System.err.println("Failed to load dependency entity for procedure MCreatorFireAlarmPullAdded!");
			return;
		}
		if (dependencies.get("x") == null) {
			System.err.println("Failed to load dependency x for procedure MCreatorFireAlarmPullAdded!");
			return;
		}
		if (dependencies.get("y") == null) {
			System.err.println("Failed to load dependency y for procedure MCreatorFireAlarmPullAdded!");
			return;
		}
		if (dependencies.get("z") == null) {
			System.err.println("Failed to load dependency z for procedure MCreatorFireAlarmPullAdded!");
			return;
		}
		if (dependencies.get("world") == null) {
			System.err.println("Failed to load dependency world for procedure MCreatorFireAlarmPullAdded!");
			return;
		}
		Entity entity = (Entity) dependencies.get("entity");
		int x = (int) dependencies.get("x");
		int y = (int) dependencies.get("y");
		int z = (int) dependencies.get("z");
		World world = (World) dependencies.get("world");
		{
			TileEntity tileEntity = world.getTileEntity(new BlockPos((int) x, (int) y, (int) z));
			if (tileEntity != null)
				tileEntity.getTileData().setDouble("panelXCoord", (csmVariables.WorldVariables.get(world).currentPanelX));
		}
		{
			TileEntity tileEntity = world.getTileEntity(new BlockPos((int) x, (int) y, (int) z));
			if (tileEntity != null)
				tileEntity.getTileData().setDouble("panelYCoord", (csmVariables.WorldVariables.get(world).currentPanelY));
		}
		{
			TileEntity tileEntity = world.getTileEntity(new BlockPos((int) x, (int) y, (int) z));
			if (tileEntity != null)
				tileEntity.getTileData().setDouble("panelZCoord", (csmVariables.WorldVariables.get(world).currentPanelZ));
		}
		world.playSound((EntityPlayer) null, x, y, z,
				(net.minecraft.util.SoundEvent) net.minecraft.util.SoundEvent.REGISTRY.getObject(new ResourceLocation("csm:PanelComponentAdded")),
				SoundCategory.NEUTRAL, (float) 3, (float) 1);
		if (entity instanceof EntityPlayer && !world.isRemote) {
			((EntityPlayer) entity).sendStatusMessage(
					new TextComponentString(
							(("Linked to Panel at X ") + "" + ((csmVariables.WorldVariables.get(world).currentPanelX)) + "" + (", Y ") + ""
									+ ((csmVariables.WorldVariables.get(world).currentPanelY)) + "" + (", Z ") + "" + ((csmVariables.WorldVariables
									.get(world).currentPanelZ)))), (true));
		}
	}
}
