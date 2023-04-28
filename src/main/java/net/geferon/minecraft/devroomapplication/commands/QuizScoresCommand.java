package net.geferon.minecraft.devroomapplication.commands;

import net.geferon.minecraft.devroomapplication.DevRoomApplicationPlugin;
import net.geferon.minecraft.devroomapplication.services.QuizService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class QuizScoresCommand implements CommandExecutor {
    private final DevRoomApplicationPlugin plugin;
    private final QuizService quizService;

    @Inject
    public QuizScoresCommand(DevRoomApplicationPlugin plugin, QuizService quizService) {
        this.plugin = plugin;
        this.quizService = quizService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0) {
            var ply = plugin.getServer().getOfflinePlayer(args[0]);
            var score = quizService.getPlayerScore(ply);
            if (score == null) {
                sender.sendMessage("The player supplied doesn't have any score");
                return true;
            }

            sender.sendMessage(ply.getName() + " currently has a score of " + score + " points");
        } else {
            var topPlayers = quizService.getPlayerScores()
                    .entrySet()
                    .stream()
                    .sorted(Comparator.comparingInt(Map.Entry::getValue))
                    .limit(10)
                    .toList();

            sender.sendMessage(Component.text("Top " + topPlayers.size() + " player(s):").decorate(TextDecoration.UNDERLINED));

            var topPlayersIterator = topPlayers.iterator();
            int i = 0;
            while (topPlayersIterator.hasNext()) {
                i++;

                var score = topPlayersIterator.next();
                var ply = plugin.getServer().getOfflinePlayer(score.getKey());

                sender.sendMessage(
                        Component.text()
                                .color(NamedTextColor.GOLD)
                                .append(Component.text(i + ". ").color(NamedTextColor.GRAY))
                                .append(Component.text(ply.getName() + ": "))
                                .append(Component.text(score.getValue() + " points").color(NamedTextColor.GREEN))
                                .build()
                );
            }
        }
        return true;
    }
}
