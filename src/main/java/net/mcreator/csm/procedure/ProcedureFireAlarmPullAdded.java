package net.mcreator.csm.procedure;

import net.minecraft.world.World;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.block.state.IBlockState;

import net.mcreator.csm.ElementsCitySuperMod;
import net.mcreator.csm.CitySuperModVariables;

@ElementsCitySuperMod.ModElement.Tag
public class ProcedureFireAlarmPullAdded extends ElementsCitySuperMod.ModElement {
	public ProcedureFireAlarmPullAdded(ElementsCitySuperMod instance) {
		super(instance, 1057);
	}

	public static void executeProcedure(java.util.HashMap<String, Object> dependencies) {
		if (dependencies.get("entity") == null) {
			System.err.println("Failed to load dependency entity for procedure FireAlarmPullAdded!");
			return;
		}
		if (dependencies.get("x") == null) {
			System.err.println("Failed to load dependency x for procedure FireAlarmPullAdded!");
			return;
		}
		if (dependencies.get("y") == null) {
			System.err.println("Failed to load dependency y for procedure FireAlarmPullAdded!");
			return;
		}
		if (dependencies.get("z") == null) {
			System.err.println("Failed to load dependency z for procedure FireAlarmPullAdded!");
			return;
		}
		if (dependencies.get("world") == null) {
			System.err.println("Failed to load dependency world for procedure FireAlarmPullAdded!");
			return;
		}
		Entity entity = (Entity) dependencies.get("entity");
		int x = (int) dependencies.get("x");
		int y = (int) dependencies.get("y");
		int z = (int) dependencies.get("z");
		World world = (World) dependencies.get("world");
		if (!world.isRemote) {
			BlockPos _bp = new BlockPos((int) x, (int) y, (int) z);
			TileEntity _tileEntity = world.getTileEntity(_bp);
			IBlockState _bs = world.getBlockState(_bp);
			if (_tileEntity != null)
				_tileEntity.getTileData().setDouble("panelXCoord", (CitySuperModVariables.WorldVariables.get(world).currentPanelX));
			world.notifyBlockUpdate(_bp, _bs, _bs, 3);
		}
		if (!world.isRemote) {
			BlockPos _bp = new BlockPos((int) x, (int) y, (int) z);
			TileEntity _tileEntity = world.getTileEntity(_bp);
			IBlockState _bs = world.getBlockState(_bp);
			if (_tileEntity != null)
				_tileEntity.getTileData().setDouble("panelYCoord", (CitySuperModVariables.WorldVariables.get(world).currentPanelY));
			world.notifyBlockUpdate(_bp, _bs, _bs, 3);
		}
		if (!world.isRemote) {
			BlockPos _bp = new BlockPos((int) x, (int) y, (int) z);
			TileEntity _tileEntity = world.getTileEntity(_bp);
			IBlockState _bs = world.getBlockState(_bp);
			if (_tileEntity != null)
				_tileEntity.getTileData().setDouble("panelZCoord", (CitySuperModVariables.WorldVariables.get(world).currentPanelZ));
			world.notifyBlockUpdate(_bp, _bs, _bs, 3);
		}
		world.playSound((EntityPlayer) null, x, y, z,
				(net.minecraft.util.SoundEvent) net.minecraft.util.SoundEvent.REGISTRY.getObject(new ResourceLocation("csm:PanelComponentAdded")),
				SoundCategory.NEUTRAL, (float) 3, (float) 1);
		if (entity instanceof EntityPlayer && !world.isRemote) {
			((EntityPlayer) entity).sendStatusMessage(
					new TextComponentString((("Linked to Panel at X ") + "" + ((CitySuperModVariables.WorldVariables.get(world).currentPanelX)) + ""
							+ (", Y ") + "" + ((CitySuperModVariables.WorldVariables.get(world).currentPanelY)) + "" + (", Z ") + ""
							+ ((CitySuperModVariables.WorldVariables.get(world).currentPanelZ)))),
					(true));
		}
	}
}
