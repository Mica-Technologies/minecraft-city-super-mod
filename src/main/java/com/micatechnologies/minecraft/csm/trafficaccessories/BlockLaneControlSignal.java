package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkMountType;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockLaneControlSignal extends AbstractBlockRotatableNSEW
        implements ICsmTileEntityProvider {

    private static final int GUI_ID = 13;

    public BlockLaneControlSignal() {
        super(Material.ROCK);
        this.setSoundType(SoundType.METAL);
        this.setHarvestLevel("pickaxe", 1);
        this.setHardness(2F);
        this.setResistance(10F);
    }

    @Override
    public String getBlockRegistryName() {
        return "lane_control_signal";
    }

    // region Tile Entity

    @Override
    public Class<? extends TileEntity> getTileEntityClass() {
        return TileEntityLaneControlSignal.class;
    }

    @Override
    public String getTileEntityName() {
        return "tileentitylanecontrolsignal";
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityLaneControlSignal();
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    // endregion

    // region Block Properties

    @Override
    public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source,
            BlockPos pos) {
        return new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 0.625);
    }

    @Nullable
    @Override
    public RayTraceResult collisionRayTrace(IBlockState state, World worldIn, BlockPos pos,
            Vec3d start, Vec3d end) {
        AxisAlignedBB bb = getBoundingBox(state, worldIn, pos);
        AxisAlignedBB clamped = new AxisAlignedBB(
                Math.max(0.0, bb.minX), Math.max(0.0, bb.minY),
                Math.max(0.0, bb.minZ),
                Math.min(1.0, bb.maxX), Math.min(1.0, bb.maxY),
                Math.min(1.0, bb.maxZ));
        return rayTrace(pos, start, end, clamped);
    }

    @Override
    public boolean getBlockIsOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean getBlockIsFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
            @Nullable EnumFacing facing) {
        return false;
    }

    @Nonnull
    @Override
    public BlockRenderLayer getBlockRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return 0;
    }

    // endregion

    // region Block Events

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state,
            EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityLaneControlSignal) {
                CrosswalkMountType detected = detectMountType(worldIn, pos, state);
                ((TileEntityLaneControlSignal) te).setMountType(detected);
            }
        }
    }

    private CrosswalkMountType detectMountType(World worldIn, BlockPos pos,
            IBlockState state) {
        EnumFacing facing = state.getValue(FACING);

        EnumFacing behind = facing.getOpposite();
        if (isAttachableBlock(worldIn, pos.offset(behind))) {
            return CrosswalkMountType.REAR;
        }

        EnumFacing left = facing.rotateY();
        if (isAttachableBlock(worldIn, pos.offset(left))) {
            return CrosswalkMountType.LEFT;
        }

        EnumFacing right = facing.rotateYCCW();
        if (isAttachableBlock(worldIn, pos.offset(right))) {
            return CrosswalkMountType.RIGHT;
        }

        return CrosswalkMountType.BASE;
    }

    private boolean isAttachableBlock(World worldIn, BlockPos pos) {
        IBlockState adjState = worldIn.getBlockState(pos);
        return adjState.isFullCube() || adjState.isOpaqueCube()
                || adjState.getBlock() instanceof
                com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole
                || adjState.getBlock() instanceof
                com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPoleDiagonal;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
            EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY,
            float hitZ) {
        player.openGui(Csm.instance, GUI_ID, worldIn, pos.getX(), pos.getY(),
                pos.getZ());
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        if (worldIn.isRemote) {
            TileEntitySpecialRenderer<?> renderer =
                    TileEntityRendererDispatcher.instance.renderers.get(
                            TileEntityLaneControlSignal.class);
            if (renderer instanceof TileEntityLaneControlSignalRenderer) {
                ((TileEntityLaneControlSignalRenderer) renderer).cleanupDisplayList(pos);
            }
        }
        super.breakBlock(worldIn, pos, state);
    }

    // endregion
}
