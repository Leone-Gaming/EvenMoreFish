package com.oheers.fish.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.oheers.fish.EvenMoreFish;
import com.oheers.fish.competition.Competition;
import com.oheers.fish.config.MainConfig;
import com.oheers.fish.config.messages.ConfigMessage;
import com.oheers.fish.config.messages.Message;
import com.oheers.fish.config.messages.PrefixType;
import com.oheers.fish.gui.guis.MainMenuGUI;
import com.oheers.fish.gui.guis.SellGUI;
import com.oheers.fish.permissions.AdminPerms;
import com.oheers.fish.permissions.UserPerms;
import com.oheers.fish.selling.SellHelper;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;


@CommandAlias("%main")
public class EMFCommand extends BaseCommand {

    @Subcommand("next")
    @Description("%desc_general_next")
    @CommandPermission(UserPerms.NEXT)
    public void onNext(final CommandSender sender) {
        Message message = Competition.getNextCompetitionMessage();
        message.usePrefix(PrefixType.DEFAULT);
        message.broadcast(sender);
    }

    @Subcommand("toggle")
    @Description("%desc_general_toggle")
    @CommandPermission(UserPerms.TOGGLE)
    public void onToggle(final Player player) {
        EvenMoreFish.getInstance().performFishToggle(player);
    }

    @Subcommand("gui")
    @Description("%desc_general_gui")
    @CommandPermission(UserPerms.GUI)
    public void onGui(final Player player) {
        new MainMenuGUI(player).open();
    }

    @Default
    @HelpCommand
    @CommandPermission(UserPerms.HELP)
    @Description("%desc_general_help")
    public void onHelp(final CommandHelp help, final CommandSender sender) {
        new Message(ConfigMessage.HELP_GENERAL_TITLE).broadcast(sender);
        help.getHelpEntries().forEach(helpEntry -> {
            Message helpMessage = new Message(ConfigMessage.HELP_FORMAT);
            helpMessage.setVariable("{command}", "/" + helpEntry.getCommand());
            helpMessage.setVariable("{description}", helpEntry.getDescription());
            helpMessage.broadcast(sender);
        });
    }

    @Subcommand("top")
    @CommandPermission(UserPerms.TOP)
    @Description("%desc_general_top")
    public void onTop(final CommandSender sender) {
        if (!Competition.isActive()) {
            new Message(ConfigMessage.NO_COMPETITION_RUNNING).broadcast(sender);
            return;
        }

        if (sender instanceof Player player) {
            EvenMoreFish.getInstance().getActiveCompetition().sendPlayerLeaderboard(player);
            return;
        }

        if (sender instanceof ConsoleCommandSender consoleCommandSender) {
            EvenMoreFish.getInstance().getActiveCompetition().sendConsoleLeaderboard(consoleCommandSender);
        }
    }

}
