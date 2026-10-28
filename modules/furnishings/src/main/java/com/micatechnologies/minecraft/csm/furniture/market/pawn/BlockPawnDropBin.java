package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialFurniture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

/**
 * The pawn drop-off bin: a click with an item pawns the stack held, paid to whoever clicks, with
 * no screen; a hopper feeding it pays whoever has claimed it ({@link TileEntityPawnDropBin}).
 *
 * <ul>
 *   <li>A sneaking click with an empty hand claims an unclaimed bin, or releases your own. Those
 *   who manage pawn shops release anyone's.</li>
 *   <li>An empty-handed click says whose it is and what it does.</li>
 * </ul>
 *
 * <p>It pays from nothing, so, like the counter, only {@value PawnPermissions#NODE_ADMIN} places
 * or breaks one.</p>
 *
 * @since 2026.10
 */
public class BlockPawnDropBin extends BlockResidentialFurniture implements ICsmTileEntityProvider {

  public BlockPawnDropBin(String registryName, int[] box) {
    super(registryName, box, false);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return false;
    }
    if (world.isRemote) {
      return true;
    }
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityPawnDropBin) || !(player instanceof EntityPlayerMP)) {
      return true;
    }
    TileEntityPawnDropBin bin = (TileEntityPawnDropBin) te;
    ItemStack held = player.getHeldItemMainhand();
    if (player.isSneaking()) {
      if (held.isEmpty()) {
        claim((EntityPlayerMP) player, bin);
      }
      return true;
    }
    if (held.isEmpty()) {
      player.sendMessage(bin.getPayee() == null
          ? new TextComponentTranslation("csm.pawn.bin.info_unclaimed")
          : new TextComponentTranslation("csm.pawn.bin.info", bin.getPayeeName()));
      return true;
    }
    int slot = player.inventory.currentItem;
    PawnTrade.Outcome out = PawnTrade.pawn((EntityPlayerMP) player, player.inventory, slot,
        slot + 1, null);
    PawnBlocks.tell(player, out.messages);
    if (out.taken > 0) {
      world.playSound(null, pos, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS,
          0.5F, 1.2F);
    }
    return true;
  }

  private static void claim(EntityPlayerMP player, TileEntityPawnDropBin bin) {
    if (bin.getPayee() == null) {
      if (player.isCreative() && !CsmConfig.isPawnAllowCreative()) {
        player.sendMessage(new TextComponentTranslation("csm.pawn.msg.creative"));
        return;
      }
      bin.setPayee(player.getUniqueID(), player.getName());
      player.sendMessage(new TextComponentTranslation(CsmConfig.isPawnBinAcceptsHoppers()
          ? "csm.pawn.bin.claimed" : "csm.pawn.bin.claimed_no_hoppers"));
    } else if (player.getUniqueID().equals(bin.getPayee()) || PawnPermissions.isAdmin(player)) {
      bin.setPayee(null, "");
      player.sendMessage(new TextComponentTranslation("csm.pawn.bin.released"));
    } else {
      player.sendMessage(new TextComponentTranslation("csm.pawn.bin.taken",
          bin.getPayeeName()));
    }
  }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    PawnBlocks.placed(world, pos, this, placer);
  }

  @Override
  public boolean removedByPlayer(@Nonnull IBlockState state, World world, @Nonnull BlockPos pos,
      @Nonnull EntityPlayer player, boolean willHarvest) {
    return PawnBlocks.mayBreak(world, player)
        && super.removedByPlayer(state, world, pos, player, willHarvest);
  }

  @Override
  @SuppressWarnings("deprecation")
  public float getPlayerRelativeBlockHardness(@Nonnull IBlockState state,
      @Nonnull EntityPlayer player, @Nonnull World world, @Nonnull BlockPos pos) {
    return PawnBlocks.mayDig(player)
        ? super.getPlayerRelativeBlockHardness(state, player, world, pos) : 0.0F;
  }

  @Override
  public float getExplosionResistance(World world, BlockPos pos, @Nullable Entity exploder,
      Explosion explosion) {
    return PawnBlocks.RESISTANCE;
  }

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityPawnDropBin.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitypawndropbin";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityPawnDropBin();
  }
}
