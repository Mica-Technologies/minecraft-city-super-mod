package net.mcreator.csm;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;

import net.minecraft.world.World;
import net.minecraft.world.IBlockAccess;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.Item;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.material.Material;
import net.minecraft.block.SoundType;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLever;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;


@Elementscsm.ModElement.Tag
public class MCreatorEIOWhite extends Elementscsm.ModElement {
	@GameRegistry.ObjectHolder("csm:eiowhite")
	public static final Block block = null;

	public MCreatorEIOWhite(Elementscsm instance) {
		super(instance, 309);
	}

	@Override
	public void initElements() {
		elements.blocks.add(() -> new BlockCustom());
		elements.items.add(() -> new ItemBlock(block).setRegistryName(block.getRegistryName()));
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerModels(ModelRegistryEvent event) {
		ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0, new ModelResourceLocation("csm:eiowhite", "inventory"));
	}

	public static class BlockCustom extends BlockLever {

		public BlockCustom() {
			super();
			setRegistryName("eiowhite");
			setUnlocalizedName("eiowhite");
			setCreativeTab(MCreatorMCLAAlarmsTab.tab);
		}

		@Override
		public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer entity, EnumHand hand, EnumFacing side,
				float hitX, float hitY, float hitZ) {
			super.onBlockActivated(world, pos, state, entity, hand, side, hitX, hitY, hitZ);
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			Block block = this;
			{
				java.util.HashMap<String, Object> $_dependencies = new java.util.HashMap<>();
				$_dependencies.put("entity", entity);
				$_dependencies.put("x", x);
				$_dependencies.put("y", y);
				$_dependencies.put("z", z);
				$_dependencies.put("world", world);
				MCreatorSetCurrentPanelCoords.executeProcedure($_dependencies);
				IBlockState bsc = world.getBlockState(pos);
				if (bsc.getValue(BlockLever.POWERED)) {
					MinecraftServer mcserv = FMLCommonHandler.instance().getMinecraftServerInstance();
					if (mcserv != null)
						mcserv.getPlayerList().sendMessage(new TextComponentString("Fire alarm panel at [" + x + "," + y + "," + z
								+ "] has been activated!"));
				} else {
					world.playSound((EntityPlayer) null, x, y, z, (net.minecraft.util.SoundEvent) net.minecraft.util.SoundEvent.REGISTRY
							.getObject(new ResourceLocation("csm:edwards_io_reset")), SoundCategory.NEUTRAL, (float) 3, (float) 1);
					MinecraftServer mcserv = FMLCommonHandler.instance().getMinecraftServerInstance();
					if (mcserv != null)
						mcserv.getPlayerList().sendMessage(new TextComponentString("Fire alarm panel at [" + x + "," + y + "," + z
								+ "] has been RESET! All Clear."));
				}

			}
			return true;
		}
	}
}
