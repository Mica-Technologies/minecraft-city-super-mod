package com.micatechnologies.minecraft.csm;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * The {@code /csm} administrative command. All subcommands require permission level 2 (op).
 *
 * <h3>Subcommands</h3>
 * <ul>
 *   <li>{@code /csm reloadconfig} — reloads the mod config file from disk</li>
 *   <li>{@code /csm poleignore list} — prints the current user-added traffic pole ignore ids</li>
 *   <li>{@code /csm poleignore add <block>} — adds a block id; accepts {@code modid:name} or a
 *       bare {@code name} (treated as {@code minecraft:name})</li>
 *   <li>{@code /csm poleignore remove <block>} — removes a block id</li>
 * </ul>
 * <p>
 * Mutations made through this command are persisted to the config file immediately, so they
 * survive restarts without requiring an additional save step.
 */
public class CommandCsm extends CommandBase {

  private static final String USAGE =
      "/csm <reloadconfig|poleignore <list|add|remove> [block]>";

  @Override
  public String getName() {
    return "csm";
  }

  @Override
  public List<String> getAliases() {
    return Lists.newArrayList("citysupermod");
  }

  @Override
  public String getUsage(ICommandSender sender) {
    return USAGE;
  }

  @Override
  public int getRequiredPermissionLevel() {
    return 2;
  }

  @Override
  public void execute(MinecraftServer server, ICommandSender sender, String[] args)
      throws CommandException {
    if (args.length == 0) {
      throw new WrongUsageException(USAGE);
    }
    String sub = args[0].toLowerCase();
    switch (sub) {
      case "reloadconfig":
        handleReloadConfig(sender);
        return;
      case "poleignore":
        handlePoleIgnore(sender, args);
        return;
      default:
        throw new WrongUsageException(USAGE);
    }
  }

  private static void handleReloadConfig(ICommandSender sender) {
    CsmConfig.reload();
    sendSuccess(sender, "CSM config reloaded from disk.");
  }

  private static void handlePoleIgnore(ICommandSender sender, String[] args)
      throws CommandException {
    if (args.length < 2) {
      throw new WrongUsageException("/csm poleignore <list|add|remove> [block]");
    }
    String action = args[1].toLowerCase();
    switch (action) {
      case "list":
        listPoleIgnores(sender);
        return;
      case "add":
        if (args.length < 3) {
          throw new WrongUsageException("/csm poleignore add <block>");
        }
        addPoleIgnore(sender, args[2]);
        return;
      case "remove":
        if (args.length < 3) {
          throw new WrongUsageException("/csm poleignore remove <block>");
        }
        removePoleIgnore(sender, args[2]);
        return;
      default:
        throw new WrongUsageException("/csm poleignore <list|add|remove> [block]");
    }
  }

  private static void listPoleIgnores(ICommandSender sender) {
    Set<ResourceLocation> ids = CsmConfig.getTrafficPoleIgnoreBlockIds();
    if (ids.isEmpty()) {
      sendInfo(sender, "No user-added traffic pole ignore blocks. "
          + "Built-in ignores still apply.");
      return;
    }
    List<String> sorted = new ArrayList<>();
    for (ResourceLocation rl : ids) {
      sorted.add(rl.toString());
    }
    Collections.sort(sorted);
    sendInfo(sender,
        "Traffic pole ignore blocks (" + sorted.size() + "): " + String.join(", ", sorted));
  }

  private static void addPoleIgnore(ICommandSender sender, String raw) throws CommandException {
    ResourceLocation rl = CsmConfig.parseBlockId(raw);
    if (rl == null) {
      throw new CommandException("Invalid block id: " + raw);
    }
    if (!ForgeRegistries.BLOCKS.containsKey(rl)) {
      throw new CommandException("Unknown block: " + rl
          + " (no such block is registered; check the mod id/name)");
    }
    if (CsmConfig.addTrafficPoleIgnoreBlock(rl)) {
      sendSuccess(sender, "Added \"" + rl + "\" to traffic pole ignore list.");
    } else {
      sendInfo(sender, "\"" + rl + "\" is already in the traffic pole ignore list.");
    }
  }

  private static void removePoleIgnore(ICommandSender sender, String raw) throws CommandException {
    ResourceLocation rl = CsmConfig.parseBlockId(raw);
    if (rl == null) {
      throw new CommandException("Invalid block id: " + raw);
    }
    if (CsmConfig.removeTrafficPoleIgnoreBlock(rl)) {
      sendSuccess(sender, "Removed \"" + rl + "\" from traffic pole ignore list.");
    } else {
      sendInfo(sender, "\"" + rl + "\" was not in the traffic pole ignore list.");
    }
  }

  @Override
  public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
      String[] args, @Nullable BlockPos targetPos) {
    if (args.length == 1) {
      return getListOfStringsMatchingLastWord(args, "reloadconfig", "poleignore");
    }
    if (args.length == 2 && "poleignore".equalsIgnoreCase(args[0])) {
      return getListOfStringsMatchingLastWord(args, "list", "add", "remove");
    }
    if (args.length == 3 && "poleignore".equalsIgnoreCase(args[0])) {
      String action = args[1].toLowerCase();
      if ("add".equals(action)) {
        List<String> allBlockIds = new ArrayList<>();
        for (ResourceLocation rl : ForgeRegistries.BLOCKS.getKeys()) {
          allBlockIds.add(rl.toString());
        }
        return getListOfStringsMatchingLastWord(args, allBlockIds);
      }
      if ("remove".equals(action)) {
        List<String> current = new ArrayList<>();
        for (ResourceLocation rl : CsmConfig.getTrafficPoleIgnoreBlockIds()) {
          current.add(rl.toString());
        }
        return getListOfStringsMatchingLastWord(args, current);
      }
    }
    return Collections.emptyList();
  }

  private static void sendSuccess(ICommandSender sender, String message) {
    sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "[CSM] " + message));
  }

  private static void sendInfo(ICommandSender sender, String message) {
    sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "[CSM] " + message));
  }
}
