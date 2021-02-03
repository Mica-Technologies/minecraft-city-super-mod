package com.micatechnologies.minecraft.csm.NEEDSWORK.procedure;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.block.state.IBlockState;

@ElementsCitySuperMod.ModElement.Tag
public class ProcedureEnableFA extends ElementsCitySuperMod.ModElement {
	public ProcedureEnableFA(ElementsCitySuperMod instance) {
		super(instance, 1031);
	}

	public static void executeProcedure(java.util.HashMap<String, Object> dependencies) {
		if (dependencies.get("x") == null) {
			System.err.println("Failed to load dependency x for procedure EnableFA!");
			return;
		}
		if (dependencies.get("y") == null) {
			System.err.println("Failed to load dependency y for procedure EnableFA!");
			return;
		}
		if (dependencies.get("z") == null) {
			System.err.println("Failed to load dependency z for procedure EnableFA!");
			return;
		}
		if (dependencies.get("world") == null) {
			System.err.println("Failed to load dependency world for procedure EnableFA!");
			return;
		}
		int x = (int) dependencies.get("x");
		int y = (int) dependencies.get("y");
		int z = (int) dependencies.get("z");
		World world = (World) dependencies.get("world");
		if (!world.isRemote) {
			BlockPos _bp = new BlockPos((int) x, (int) y, (int) z);
			TileEntity _tileEntity = world.getTileEntity(_bp);
			IBlockState _bs = world.getBlockState(_bp);
			if (_tileEntity != null)
				_tileEntity.getTileData().setBoolean("active", (true));
			world.notifyBlockUpdate(_bp, _bs, _bs, 3);
		}
	}
}
