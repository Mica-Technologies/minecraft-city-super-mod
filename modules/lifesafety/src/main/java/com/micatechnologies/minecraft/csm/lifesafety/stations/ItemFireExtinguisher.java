package com.micatechnologies.minecraft.csm.lifesafety.stations;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import com.micatechnologies.minecraft.csm.lifesafety.fireprotection.BlockFireProtectionProp;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A working fire extinguisher. Hold right-click to spray: a cone of powder about five blocks
 * long that puts out fire blocks and anything burning in it. Its charge is the durability bar,
 * about ten seconds of spraying; an SCBA fill station refills it (use it on the station). Sneak
 * and click a wall to hang it there instead -- it becomes the ABC extinguisher on its bracket.
 *
 * @since 2026.9
 */
public class ItemFireExtinguisher extends AbstractItem {

  /** Ticks of spray in a full charge. */
  public static final int CHARGE = 200;
  /** How far the spray reaches, in blocks. */
  private static final double REACH = 5.0;
  /** The half-angle of the spray cone, as the cosine. */
  private static final double CONE_COS = Math.cos(Math.toRadians(25));

  public ItemFireExtinguisher() {
    super(CHARGE, 1);
  }

  @Override
  public String getItemRegistryName() {
    return "fire_extinguisher_item";
  }

  @Override
  @Nonnull
  public EnumAction getItemUseAction(@Nonnull ItemStack stack) {
    return EnumAction.BOW;
  }

  @Override
  public int getMaxItemUseDuration(@Nonnull ItemStack stack) {
    return 72000;
  }

  @Override
  @Nonnull
  public ActionResult<ItemStack> onItemRightClick(@Nonnull World world, EntityPlayer player,
      @Nonnull EnumHand hand) {
    ItemStack stack = player.getHeldItem(hand);
    if (stack.getItemDamage() >= stack.getMaxDamage()) {
      return new ActionResult<>(EnumActionResult.FAIL, stack);
    }
    player.setActiveHand(hand);
    return new ActionResult<>(EnumActionResult.SUCCESS, stack);
  }

  @Override
  @Nonnull
  public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
      EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (!player.isSneaking() || facing.getAxis() == EnumFacing.Axis.Y) {
      return EnumActionResult.PASS;
    }
    Block wall = CsmRegistry.getBlock("fire_extinguisher_abc");
    BlockPos at = pos.offset(facing);
    if (!(wall instanceof BlockFireProtectionProp)
        || !world.getBlockState(at).getBlock().isReplaceable(world, at)
        || !player.canPlayerEdit(at, facing, player.getHeldItem(hand))) {
      return EnumActionResult.FAIL;
    }
    if (!world.isRemote) {
      IBlockState state = wall.getDefaultState().withProperty(BlockFireProtectionProp.FACING,
          facing);
      world.setBlockState(at, state, 3);
      world.playSound(null, at, SoundEvents.BLOCK_METAL_PLACE, SoundCategory.BLOCKS, 1F, 1F);
      if (!player.capabilities.isCreativeMode) {
        player.getHeldItem(hand).shrink(1);
      }
    }
    return EnumActionResult.SUCCESS;
  }

  @Override
  public void onUsingTick(ItemStack stack, EntityLivingBase user, int count) {
    World world = user.world;
    if (stack.getItemDamage() >= stack.getMaxDamage()) {
      user.stopActiveHand();
      return;
    }
    Vec3d eye = user.getPositionEyes(1F);
    Vec3d look = user.getLookVec();
    if (world.isRemote) {
      for (int i = 0; i < 6; i++) {
        double spread = 0.18;
        Vec3d dir = look.add((world.rand.nextDouble() - 0.5) * spread,
            (world.rand.nextDouble() - 0.5) * spread, (world.rand.nextDouble() - 0.5) * spread);
        world.spawnParticle(EnumParticleTypes.CLOUD, eye.x + look.x * 0.6,
            eye.y + look.y * 0.6 - 0.2, eye.z + look.z * 0.6, dir.x * 0.6, dir.y * 0.6,
            dir.z * 0.6);
      }
      return;
    }
    if (!(user instanceof EntityPlayer) || !((EntityPlayer) user).capabilities.isCreativeMode) {
      stack.setItemDamage(stack.getItemDamage() + 1);
    }
    if (count % 3 == 0) {
      world.playSound(null, user.posX, user.posY, user.posZ, SoundEvents.BLOCK_FIRE_EXTINGUISH,
          SoundCategory.PLAYERS, 0.3F, 1.6F);
    }
    if (count % 2 == 0) {
      putOut(world, eye, look);
    }
  }

  /** Puts out the fire blocks and burning entities in the spray's cone. */
  private static void putOut(World world, Vec3d eye, Vec3d look) {
    int r = (int) Math.ceil(REACH);
    BlockPos centre = new BlockPos(eye);
    for (BlockPos p : BlockPos.getAllInBoxMutable(centre.add(-r, -r, -r), centre.add(r, r, r))) {
      if (world.getBlockState(p).getBlock() != Blocks.FIRE) {
        continue;
      }
      Vec3d to = new Vec3d(p.getX() + 0.5 - eye.x, p.getY() + 0.5 - eye.y,
          p.getZ() + 0.5 - eye.z);
      double d = to.length();
      if (d <= REACH + 0.5 && d > 0 && to.dotProduct(look) / d >= CONE_COS) {
        world.setBlockToAir(p.toImmutable());
      }
    }
    AxisAlignedBB area = new AxisAlignedBB(eye.x - REACH, eye.y - REACH, eye.z - REACH,
        eye.x + REACH, eye.y + REACH, eye.z + REACH);
    for (Entity e : world.getEntitiesWithinAABB(Entity.class, area)) {
      if (!e.isBurning()) {
        continue;
      }
      Vec3d to = e.getPositionVector().add(0, e.height / 2, 0).subtract(eye);
      double d = to.length();
      if (d <= REACH && d > 0 && to.dotProduct(look) / d >= CONE_COS) {
        e.extinguish();
      }
    }
  }

  /** Refills an extinguisher. */
  public static void refill(ItemStack stack) {
    stack.setItemDamage(0);
  }

  @Override
  @SideOnly(Side.CLIENT)
  @SuppressWarnings("deprecation")
  public void addInformation(@Nonnull ItemStack stack, @Nullable World world,
      @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flag) {
    int left = stack.getMaxDamage() - stack.getItemDamage();
    tooltip.add(TextFormatting.GRAY + I18n.translateToLocalFormatted(
        "csm.lifesafety.extinguisher.charge", MathHelper.ceil(left * 100.0 / CHARGE)));
    tooltip.add(TextFormatting.DARK_GRAY + I18n.translateToLocal(
        "csm.lifesafety.extinguisher.hint"));
  }
}
