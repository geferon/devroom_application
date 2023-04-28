package net.geferon.minecraft.devroomapplication.commands;

import net.geferon.minecraft.devroomapplication.exceptions.InvalidCategoryException;
import net.geferon.minecraft.devroomapplication.services.QuizService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class StartQuizCommand implements CommandExecutor, TabCompleter {
    private final QuizService quizService;

    @Inject
    public StartQuizCommand(QuizService quizService) {
        this.quizService = quizService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 1)
            return false;

        if (quizService.isQuizActive()) {
            sender.sendMessage("There's currently a quiz active");
            return true;
        }

        var category = args[0];

        try {
            quizService.startQuiz(category);

            sender.sendMessage(Component.text("Quiz successfully started!").color(NamedTextColor.GREEN));
        } catch (InvalidCategoryException e) {
            sender.sendMessage(Component.text("Invalid category supplied").color(NamedTextColor.RED));
        }

        return true;
    }

    private void addApplicable(String testing, String arg, List<String> array) {
        if (testing.toLowerCase().startsWith(arg.toLowerCase()))
            array.add(testing);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        var tabs = new ArrayList<String>();
        for (var key : quizService.getCategories().keySet()) {
            addApplicable(key, args[0], tabs);
        }
        return tabs;
    }
}
