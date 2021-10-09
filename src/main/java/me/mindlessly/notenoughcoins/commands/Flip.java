package me.mindlessly.notenoughcoins.commands;

import me.mindlessly.notenoughcoins.utils.ApiHandler;
import me.mindlessly.notenoughcoins.utils.ConfigHandler;
import me.mindlessly.notenoughcoins.utils.Utils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.ClickEvent.Action;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.config.Configuration;

import java.util.*;

public class Flip extends CommandBase {

  // Take initial set of lbins, take second set, and use compared set to identify the biggest
  // gainers/losers
  // namedSet is used to replace internal ids with actual item names
  public static LinkedHashMap<String, Double> initialDataset = new LinkedHashMap<>();
  public static LinkedHashMap<String, Double> namedDataset = new LinkedHashMap<>();
  public static double purse;
  public static ArrayList<String> commands = new ArrayList<String>();

  private int auctionPages = 0;
  private int cycle = 0;
  private Timer timer = new Timer();
  private boolean enable = false;

  @Override
  public boolean canCommandSenderUseCommand(ICommandSender sender) {
    return true;
  }

  @Override
  public String getCommandName() {
    return "flip";
  }

  @Override
  public String getCommandUsage(ICommandSender sender) {
    return "/flip";
  }

  @Override
  public void processCommand(ICommandSender sender, String[] args) {
    enable = !enable;

    if (enable) {
      ChatComponentText enableText =
          new ChatComponentText(
              EnumChatFormatting.GOLD
                  + ("NEC ")
                  + EnumChatFormatting.GREEN
                  + ("Flipper alerts enabled."));
      sender.addChatMessage(enableText);
      ApiHandler.getBins(initialDataset);
      auctionPages = ApiHandler.getNumberOfPages();

      timer.schedule(
          new TimerTask() {
            @Override
            public void run() {
              if (cycle == auctionPages) {
                cycle = 0;
              }
              String name = sender.getName();
              String id = ConfigHandler.getString(Configuration.CATEGORY_GENERAL, "APIKey");
              try {
                ApiHandler.getBins(initialDataset);
              } catch (Exception e) {
                sender.addChatMessage(new ChatComponentText("Could not load BINs."));
              }
              try {
                ApiHandler.updatePurseCoins(id, name);
              } catch (Exception e) {
                sender.addChatMessage(new ChatComponentText("Could not load purse."));
              }
              ApiHandler.itemIdsToNames(initialDataset);
              ApiHandler.getFlips(namedDataset, cycle, commands);
              if (namedDataset.size() > 0) {
                purse = Math.round(purse);
                /*ChatComponentText runtext = new ChatComponentText(
                	EnumChatFormatting.GOLD + ("NEC ") + EnumChatFormatting.AQUA + ("Suggested Flips:")
                );
                sender.addChatMessage(runtext);
                if (!enable) {
                	return;
                }
                sender.addChatMessage(
                		new ChatComponentText(
                			EnumChatFormatting.GOLD + "Your Budget: " + EnumChatFormatting.WHITE + (long) purse + "\n"
                		)
                	);*/
                int count = 0;

                for (Map.Entry<String, Double> entry : namedDataset.entrySet()) {
                  if (count == 3) {
                    break;
                  }
                  long profit = Math.abs(entry.getValue().longValue());
                  IChatComponent result =
                      new ChatComponentText(
                          EnumChatFormatting.AQUA
                              + "[NEC] "
                              + EnumChatFormatting.YELLOW
                              + ""
                              + entry.getKey()
                              + " "
                              + EnumChatFormatting.GOLD
                              + "+$"
                              + Utils.formatValue(profit));

                  ChatStyle style =
                      new ChatStyle()
                          .setChatClickEvent(
                              new ClickEvent(Action.RUN_COMMAND, commands.get(count)) {
                                @Override
                                public Action getAction() {
                                  // custom behavior
                                  return Action.RUN_COMMAND;
                                }
                              });
                  result.setChatStyle(style);

                  sender.addChatMessage(result);

                  count++;
                }
              }
              namedDataset.clear();
              cycle++;
            }
          },
          2000,
          2000);

      timer.schedule(
          new TimerTask() {
            @Override
            public void run() {
              auctionPages = ApiHandler.getNumberOfPages();
            }
          },
          60000,
          60000);

    } else {
      ChatComponentText enableText =
          new ChatComponentText(
              EnumChatFormatting.GOLD
                  + ("NEC ")
                  + EnumChatFormatting.RED
                  + ("Flipper alerts disabled."));
      sender.addChatMessage(enableText);
      timer.cancel();
      timer.purge();
      timer = new Timer();
    }
  }
}
