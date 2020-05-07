package net.mcreator.csm.block;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;

import net.minecraft.world.World;
import net.minecraft.world.IBlockAccess;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.NonNullList;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.Item;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Container;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.material.Material;
import net.minecraft.block.SoundType;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.Block;

import net.mcreator.csm.procedure.ProcedureFireAlarmPullAdded;
import net.mcreator.csm.creativetab.TabMCLAAlarmsTab;
import net.mcreator.csm.ElementsCitySuperMod;

@ElementsCitySuperMod.ModElement.Tag
public class BlockFacallpoint extends ElementsCitySuperMod.ModElement {
	@GameRegistry.ObjectHolder("csm:facallpoint")
	public static final Block block = null;
	public BlockFacallpoint(ElementsCitySuperMod instance) {
		super(instance, 272);
	}

	@Override
	public void initElements() {
		elements.blocks.add(() -> new BlockCustom());
		elements.items.add(() -> new ItemBlock(block).setRegistryName(block.getRegistryName()));
	}

	@Override
	public void init(FMLInitializationEvent event) {
		GameRegistry.registerTileEntity(TileEntityCustom.class, "csm:tileentityfacallpoint");
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerModels(ModelRegistryEvent event) {
		ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0, new ModelResourceLocation("csm:facallpoint", "inventory"));
	}
	public static class BlockCustom extends Block implements ITileEntityProvider {
		public static final PropertyDirection FACING = BlockDirectional.FACING;
		public BlockCustom() {
			super(Material.ROCK);
			setRegistryName("facallpoint");
			setUnlocalizedName("facallpoint");
			setSoundType(SoundType.GROUND);
			setHarvestLevel("pickaxe", 1);
			setHardness(2F);
			setResistance(10F);
			setLightLevel(0F);
			setLightOpacity(0);
			setCreativeTab(TabMCLAAlarmsTab.tab);
			this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
		}

		@Override
		public boolean isFullCube(IBlockState state) {
			return false;
		}

		@Override
		public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
			switch ((EnumFacing) state.getValue(BlockDirectional.FACING)) {
				case SOUTH :
				default :
					return new AxisAlignedBB(0.8D, 0.1D, 0.2D, 0.2D, 0.9D, 0D);
				case NORTH :
					return new AxisAlignedBB(0.2D, 0.1D, 0.8D, 0.8D, 0.9D, 1D);
				case WEST :
					return new AxisAlignedBB(0.8D, 0.1D, 0.8D, 1D, 0.9D, 0.2D);
				case EAST :
					return new AxisAlignedBB(0.2D, 0.1D, 0.2D, 0D, 0.9D, 0.8D);
				case UP :
					return new AxisAlignedBB(0.2D, 0.2D, 0.1D, 0.8D, 0D, 0.9D);
				case DOWN :
					return new AxisAlignedBB(0.2D, 0.8D, 0.9D, 0.8D, 1D, 0.1D);
			}
		}

		@Override
		protected net.minecraft.block.state.BlockStateContainer createBlockState() {
			return new net.minecraft.block.state.BlockStateContainer(this, new IProperty[]{FACING});
		}

		@Override
		public IBlockState getStateFromMeta(int meta) {
			return this.getDefaultState().withProperty(FACING, EnumFacing.getFront(meta));
		}

		@Override
		public int getMetaFromState(IBlockState state) {
			return ((EnumFacing) state.getValue(FACING)).getIndex();
		}

		@Override
		public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
				EntityLivingBase placer) {
			return this.getDefaultState().withProperty(FACING, EnumFacing.getDirectionFromEntityLiving(pos, placer));
		}

		@Override
		public boolean isOpaqueCube(IBlockState state) {
			return false;
		}

		@Override
		public TileEntity createNewTileEntity(World worldIn, int meta) {
			return new TileEntityCustom();
		}

		@Override
		public boolean eventReceived(IBlockState state, World worldIn, BlockPos pos, int eventID, int eventParam) {
			super.eventReceived(state, worldIn, pos, eventID, eventParam);
			TileEntity tileentity = worldIn.getTileEntity(pos);
			return tileentity == null ? false : tileentity.receiveClientEvent(eventID, eventParam);
		}

		@Override
		public EnumBlockRenderType getRenderType(IBlockState state) {
			return EnumBlockRenderType.MODEL;
		}

		@Override
		public void breakBlock(World world, BlockPos pos, IBlockState state) {
			TileEntity tileentity = world.getTileEntity(pos);
			InventoryHelper.dropInventoryItems(world, pos, (TileEntityCustom) tileentity);
			world.removeTileEntity(pos);
			super.breakBlock(world, pos, state);
		}

		@Override
		public boolean hasComparatorInputOverride(IBlockState state) {
			return true;
		}

		@Override
		public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos) {
			TileEntity tileentity = worldIn.getTileEntity(pos);
			if (tileentity instanceof TileEntityCustom)
				return Container.calcRedstoneFromInventory((TileEntityCustom) tileentity);
			else
				return 0;
		}

		@Override
		public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase entity, ItemStack itemstack) {
			super.onBlockPlacedBy(world, pos, state, entity, itemstack);
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
				ProcedureFireAlarmPullAdded.executeProcedure($_dependencies);
			}
		}

		@Override
		public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer entity, EnumHand hand, EnumFacing side, float hitX,
				float hitY, float hitZ) {
			super.onBlockActivated(world, pos, state, entity, hand, side, hitX, hitY, hitZ);
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			Block block = this;
			int panelX = 0;
			int panelY = 0;
			int panelZ = 0;
			boolean doif = false;
			world.playSound(entity, x, y, z,
					(net.minecraft.util.SoundEvent) net.minecraft.util.SoundEvent.REGISTRY.getObject(new ResourceLocation("csm:pullstation_pull")),
					SoundCategory.NEUTRAL, (float) 4, (float) 1);
			try {
				TileEntity tileEntity = world.getTileEntity(pos);
				if (tileEntity != null) {
					panelX = (int) tileEntity.getTileData().getDouble("panelXCoord");
					panelY = (int) tileEntity.getTileData().getDouble("panelYCoord");
					panelZ = (int) tileEntity.getTileData().getDouble("panelZCoord");
					doif = true;
				}
				if (doif) {
					BlockPos panelPos = new BlockPos(panelX, panelY, panelZ);
					IBlockState bsc = world.getBlockState(panelPos);
					if (!bsc.getValue(BlockLever.POWERED)) {
						Block b = bsc.getBlock();
						b.onBlockActivated(world, panelPos, bsc, entity, hand, side, panelX, panelY, panelZ);
					}
				}
			} catch (Exception e) {
				if (entity instanceof EntityPlayer && !world.isRemote) {
					((EntityPlayer) entity).sendStatusMessage(new TextComponentString(
							"OOPS! This pull station is IMPROPERLY configured. Please contact MCLA Staff as soon as possible."), (true));
				}
			}
			return true;
		}
	}

	public static class TileEntityCustom extends TileEntityLockableLoot {
		private NonNullList<ItemStack> stacks = NonNullList.<ItemStack>withSize(9, ItemStack.EMPTY);
		@Override
		public int getSizeInventory() {
			return 9;
		}

		@Override
		public boolean isEmpty() {
			for (ItemStack itemstack : this.stacks)
				if (!itemstack.isEmpty())
					return false;
			return true;
		}

		@Override
		public boolean isItemValidForSlot(int index, ItemStack stack) {
			return true;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return stacks.get(slot);
		}

		@Override
		public String getName() {
			return this.hasCustomName() ? this.customName : "container.facallpoint";
		}

		@Override
		public void readFromNBT(NBTTagCompound compound) {
			super.readFromNBT(compound);
			this.stacks = NonNullList.<ItemStack>withSize(this.getSizeInventory(), ItemStack.EMPTY);
			if (!this.checkLootAndRead(compound))
				ItemStackHelper.loadAllItems(compound, this.stacks);
			if (compound.hasKey("CustomName", 8))
				this.customName = compound.getString("CustomName");
		}

		@Override
		public NBTTagCompound writeToNBT(NBTTagCompound compound) {
			super.writeToNBT(compound);
			if (!this.checkLootAndWrite(compound))
				ItemStackHelper.saveAllItems(compound, this.stacks);
			if (this.hasCustomName())
				compound.setString("CustomName", this.customName);
			return compound;
		}

		@Override
		public int getInventoryStackLimit() {
			return 64;
		}

		@Override
		public String getGuiID() {
			return "csm:facallpoint";
		}

		@Override
		public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {
			this.fillWithLoot(playerIn);
			return new ContainerChest(playerInventory, this, playerIn);
		}

		@Override
		protected NonNullList<ItemStack> getItems() {
			return this.stacks;
		}
	}
}
