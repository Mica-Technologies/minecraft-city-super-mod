package com.micatechnologies.minecraft.csm.lifesafety.exitsign;

import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The item of an {@link AbstractBlockExitSign}: places the sign with the setup the stack carries,
 * mounted to whatever it was placed against, and lists that setup in its tooltip.
 *
 * @since 2026.9
 */
public class ItemBlockExitSign extends ItemBlock {

  private final AbstractBlockExitSign sign;

  public ItemBlockExitSign(AbstractBlockExitSign sign) {
    super(sign);
    this.sign = sign;
  }

  /**
   * Places the sign, then gives its tile entity the stack's setup with the mount the placement
   * implies (under a ceiling: ceiling; anywhere else: wall). Runs on both sides, so the client's
   * predicted sign shows the right model before the server's arrives.
   */
  @Override
  public boolean placeBlockAt(@Nonnull ItemStack stack, @Nonnull EntityPlayer player,
      @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EnumFacing side, float hitX,
      float hitY, float hitZ, @Nonnull IBlockState newState) {
    if (!super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState)) {
      return false;
    }
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityExitSign) {
      ExitSignConfig config = sign.configOf(stack)
          .withMount(AbstractBlockExitSign.mountForPlacement(side));
      ((TileEntityExitSign) te).setConfig(sign.getSpec().clamp(config));
    }
    return true;
  }

  /** Lists the setup: legend and colours, arrows, emergency heads. */
  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn,
      @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flagIn) {
    ExitSignSpec spec = sign.getSpec();
    ExitSignConfig config = sign.configOf(stack);
    StringBuilder line = new StringBuilder(name("legend", config.getLegend().getName()));
    if (spec.getLetterColours().size() > 1 || spec.getHousings().size() > 1) {
      line.append(", ").append(I18n.format("csm.exitsign.colours",
          name("letters", config.getLetters().getName()),
          name("housing", config.getHousing().getName())));
    }
    tooltip.add(TextFormatting.GRAY + line.toString());
    if (spec.getArrows().size() > 1) {
      tooltip.add(TextFormatting.GRAY + I18n.format("csm.exitsign.arrow") + ": "
          + name("arrow", config.getArrow().getName()));
    }
    if (spec.getHeadTypes().size() > 1 || config.getHeads() != ExitSignConfig.Heads.NONE) {
      tooltip.add(TextFormatting.GRAY + I18n.format("csm.exitsign.heads") + ": "
          + name("heads", config.getHeads().getName()));
    }
    tooltip.add(TextFormatting.DARK_GRAY + I18n.format("csm.exitsign.configure"));
  }

  @SideOnly(Side.CLIENT)
  private static String name(String option, String value) {
    return I18n.format("csm.exitsign." + option + "." + value.toLowerCase(Locale.ROOT));
  }
}
