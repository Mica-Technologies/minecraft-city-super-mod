package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.furniture.market.pawn.PawnValuation.Quote;
import com.micatechnologies.minecraft.csm.furniture.market.pawn.PawnValueSolver.Source;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * {@code /csmpawn}: the pawn shop's values, for server operators.
 *
 * <ul>
 *   <li>{@code value [hand|<item>]} -- what an item is worth and what one pays.</li>
 *   <li>{@code explain [hand|<item>]} -- the cheapest recipe chain behind that value.</li>
 *   <li>{@code set <hand|item> <value|clear>} -- fixes (or clears) an item's value in
 *   {@code pawn_values.json}.</li>
 *   <li>{@code block <hand|item>} -- stops (or lets) pawn shops take an item.</li>
 *   <li>{@code reload} -- reads the file again and works every value out again.</li>
 *   <li>{@code dump} -- writes every item's value to {@code csm_pawn_values.csv} in the server
 *   directory.</li>
 *   <li>{@code sell} -- pawns the held stack, as a pawn shop would (paid, capped and refused the
 *   same way), for trying the payout without a shop.</li>
 *   <li>{@code ledger [player]} -- a player's emerald remainder, today's payouts and what they
 *   are owed.</li>
 * </ul>
 *
 * <p>An item is {@code modid:item} or {@code modid:item@meta}; {@code hand} is the held stack.
 * </p>
 *
 * @since 2026.10
 */
public class CommandPawn extends CommandBase {

  private static final String[] SUBCOMMANDS =
      {"value", "explain", "set", "block", "reload", "dump", "sell", "ledger"};

  @Override
  public String getName() {
    return "csmpawn";
  }

  @Override
  public String getUsage(ICommandSender sender) {
    return "/csmpawn <value|explain> [hand|<item>] | set <hand|item> <value|clear> | "
        + "block <hand|item> | reload | dump | sell | ledger [player]";
  }

  @Override
  public int getRequiredPermissionLevel() {
    return 2;
  }

  @Override
  public void execute(MinecraftServer server, ICommandSender sender, String[] args)
      throws CommandException {
    if (args.length == 0) {
      throw new WrongUsageException(getUsage(sender));
    }
    switch (args[0]) {
      case "value":
        value(sender, target(sender, args, 1));
        break;
      case "explain":
        explain(sender, target(sender, args, 1));
        break;
      case "set":
        if (args.length != 3) {
          throw new WrongUsageException("/csmpawn set <hand|item> <value|clear>");
        }
        set(sender, target(sender, args, 1), args[2]);
        break;
      case "block":
        if (args.length != 2) {
          throw new WrongUsageException("/csmpawn block <hand|item>");
        }
        String key = keyOf(target(sender, args, 1));
        boolean now = PawnValuation.toggleBlocked(key);
        say(sender, key + (now ? " is now refused by pawn shops." : " is taken again."));
        break;
      case "reload":
        long ms = PawnValuation.rebuild();
        say(sender, "Pawn values reloaded: " + PawnValuation.result().keys().size()
            + " items valued in " + ms + " ms.");
        break;
      case "dump":
        dump(server, sender);
        break;
      case "sell":
        sell(getCommandSenderAsPlayer(sender));
        break;
      case "ledger":
        ledger(sender, args.length > 1 ? getPlayer(server, sender, args[1])
            : getCommandSenderAsPlayer(sender));
        break;
      default:
        throw new WrongUsageException(getUsage(sender));
    }
  }

  /** The stack named at {@code args[index]}, or the held one. */
  private static ItemStack target(ICommandSender sender, String[] args, int index)
      throws CommandException {
    String arg = args.length > index ? args[index] : "hand";
    if ("hand".equals(arg)) {
      if (!(sender instanceof EntityPlayer)) {
        throw new CommandException("Name an item: modid:item or modid:item@meta.");
      }
      ItemStack held = ((EntityPlayer) sender).getHeldItemMainhand();
      if (held.isEmpty()) {
        throw new CommandException("Hold the item, or name it.");
      }
      return held.copy();
    }
    int at = arg.indexOf('@');
    String name = at < 0 ? arg : arg.substring(0, at);
    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(name));
    if (item == null) {
      throw new CommandException("No item " + name + ".");
    }
    int meta = at < 0 ? 0 : parseInt(arg.substring(at + 1), 0, 32767);
    return new ItemStack(item, 1, meta);
  }

  /** The key a command acts on: the item's subtype only when it has subtypes. */
  private static String keyOf(ItemStack stack) {
    String key = PawnValuation.key(stack);
    return stack.getHasSubtypes() ? key : PawnValueSolver.item(key);
  }

  private static void value(ICommandSender sender, ItemStack stack) {
    String key = PawnValuation.key(stack);
    Quote one = PawnValuation.quote(stack);
    double base = PawnValuation.baseValue(key);
    Source source = PawnValuation.baseSource(key);
    String name = stack.getDisplayName() + " (" + key + ")";
    if (!one.isAccepted()) {
      say(sender, name + ": refused. " + one.refusal);
      return;
    }
    say(sender, String.format(Locale.ROOT, "%s: worth %.2f each (%s)%s; pays %.2f for %d.",
        name, base, source == null ? "?" : source.name().toLowerCase(Locale.ROOT),
        one.valueEach != base ? String.format(Locale.ROOT, ", %.2f as it is", one.valueEach) : "",
        one.payout, stack.getCount()));
    if (stack.getCount() == 1 && stack.getMaxStackSize() > 1) {
      ItemStack full = stack.copy();
      full.setCount(stack.getMaxStackSize());
      say(sender, String.format(Locale.ROOT, "  A stack of %d pays %.2f.", full.getCount(),
          PawnValuation.quote(full).payout));
    }
  }

  private static void explain(ICommandSender sender, ItemStack stack) {
    for (String line : PawnValueSolver.explain(PawnValuation.result(), PawnValuation.key(stack),
        6)) {
      say(sender, line);
    }
  }

  private static void set(ICommandSender sender, ItemStack stack, String value)
      throws CommandException {
    String key = keyOf(stack);
    if ("clear".equals(value)) {
      PawnValuation.setOverride(key, null);
      say(sender, key + " is worth what its recipes make it again: "
          + String.format(Locale.ROOT, "%.2f", PawnValuation.baseValue(PawnValuation.key(stack))));
      return;
    }
    double v = parseDouble(value, 0, 1_000_000_000);
    PawnValuation.setOverride(key, v);
    say(sender, String.format(Locale.ROOT, "%s is now worth %.2f (pays %.2f each).", key, v,
        v * CsmConfig.getPawnPayoutFraction()));
  }

  private static void dump(MinecraftServer server, ICommandSender sender)
      throws CommandException {
    List<String> rows = new ArrayList<>();
    for (Item item : ForgeRegistries.ITEMS.getValuesCollection()) {
      NonNullList<ItemStack> subtypes = NonNullList.create();
      try {
        // SEARCH lists every subtype of an item in any tab; an item in none lists nothing.
        item.getSubItems(CreativeTabs.SEARCH, subtypes);
      } catch (RuntimeException e) {
        subtypes.clear();
      }
      if (subtypes.isEmpty()) {
        subtypes.add(new ItemStack(item));
      }
      for (ItemStack stack : subtypes) {
        String key = PawnValuation.key(stack);
        double base = PawnValuation.baseValue(key);
        Source source = PawnValuation.baseSource(key);
        Quote q = PawnValuation.quote(stack);
        rows.add(csv(key) + "," + csv(stack.getDisplayName()) + ","
            + (Double.isNaN(base) ? "" : String.format(Locale.ROOT, "%.4f", base)) + ","
            + (source == null ? "" : source.name().toLowerCase(Locale.ROOT)) + ","
            + (q.isAccepted() ? String.format(Locale.ROOT, "%.2f", q.payout) : csv(q.refusal)));
      }
    }
    Collections.sort(rows);
    File out = new File(server.getDataDirectory(), "csm_pawn_values.csv");
    try (Writer w = Files.newBufferedWriter(out.toPath(), StandardCharsets.UTF_8)) {
      w.write("key,name,value,source,pays_one\n");
      for (String row : rows) {
        w.write(row);
        w.write('\n');
      }
    } catch (IOException e) {
      throw new CommandException("Could not write " + out + ": " + e);
    }
    say(sender, "Wrote " + rows.size() + " values to " + out.getAbsolutePath());
  }

  private static void sell(EntityPlayerMP player) {
    int slot = player.inventory.currentItem;
    PawnTrade.Outcome out = PawnTrade.pawn(player, player.inventory, slot, slot + 1, null);
    if (out.messages.isEmpty()) {
      say(player, "Hold something to pawn.");
    }
    for (String line : out.messages) {
      say(player, line);
    }
  }

  private static void ledger(ICommandSender sender, EntityPlayerMP player) {
    PawnLedger.Entry e = PawnLedger.get(player.world).entry(player.getUniqueID());
    double cap = CsmConfig.getPawnMaxPayoutPerDay();
    say(sender, String.format(Locale.ROOT,
        "%s: emerald remainder %.2f, paid today %.2f%s, owed %.2f. Paying %s now.",
        player.getName(), e.emeraldRemainder, e.day == PawnTrade.today() ? e.paidToday : 0,
        cap > 0 ? String.format(Locale.ROOT, " of %.2f", cap) : "", e.owed,
        PawnEconomy.isMoneyMode() ? "money" : String.format(Locale.ROOT,
            "emeralds at %.2f each", CsmConfig.getPawnMoneyPerEmerald())));
  }

  private static String csv(String text) {
    return "\"" + text.replace("\"", "\"\"") + "\"";
  }

  private static void say(ICommandSender sender, String message) {
    sender.sendMessage(new TextComponentString(message));
  }

  @Override
  public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
      String[] args, @Nullable BlockPos targetPos) {
    if (args.length == 1) {
      return getListOfStringsMatchingLastWord(args, SUBCOMMANDS);
    }
    if (args.length == 2 && "ledger".equals(args[0])) {
      return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
    }
    if (args.length == 2 && !"reload".equals(args[0]) && !"dump".equals(args[0])
        && !"sell".equals(args[0])) {
      List<String> names = new ArrayList<>();
      names.add("hand");
      for (ResourceLocation id : ForgeRegistries.ITEMS.getKeys()) {
        names.add(id.toString());
      }
      return getListOfStringsMatchingLastWord(args, names);
    }
    if (args.length == 3 && "set".equals(args[0])) {
      return getListOfStringsMatchingLastWord(args, "clear");
    }
    return Collections.emptyList();
  }
}
