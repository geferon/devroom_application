package net.geferon.minecraft.devroomapplication.model;

import com.google.inject.assistedinject.Assisted;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.geferon.minecraft.devroomapplication.DevRoomApplicationPlugin;
import net.geferon.minecraft.devroomapplication.services.QuizService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class ActiveQuiz implements Listener, ActiveQuizInterface {
    // Services
    private final DevRoomApplicationPlugin plugin;
    private final Configuration config;
    private final QuizService service;

    // Parameters
    public Category category;

    @Inject
    public ActiveQuiz(DevRoomApplicationPlugin plugin, Configuration config, QuizService service, @Assisted Category category) {
        this.plugin = plugin;
        this.config = config;
        this.service = service;
        this.category = category;
    }

    public HashMap<UUID, Integer> playerScores = new HashMap<>();
    public List<Question> questionsToAsk;
    private int current = 0;
    private Player currentWinner = null;
    //private Set<Question> questionQueue;
    private BukkitTask currentTask = null;
    boolean running = false;

    @Override
    public List<Question> pickQuestions(int amount) {
        if (amount != -1 && amount != category.questions.size()) {
            questionsToAsk = new ArrayList<>();
            var questionsCopySet = new ArrayList<>(category.questions);
            var rnd = ThreadLocalRandom.current();
            amount = Math.min(amount, category.questions.size());
            for (int i = 0; i < amount; i++) {
                questionsToAsk.add(questionsCopySet.remove(rnd.nextInt(questionsCopySet.size())));
            }
        } else {
            questionsToAsk = new ArrayList<>(category.questions);
            Collections.shuffle(questionsToAsk);
        }

        return questionsToAsk;
    }

    @Override
    public Question nextQuestion() {
        currentWinner = null;
        current++;
        if (!hasNext()) return null;

        return questionsToAsk.get(current);
    }

    @Override
    public Question getCurrent() {
        if (current < 0 || current >= questionsToAsk.size()) return null;

        return questionsToAsk.get(current);
    }

    @Override
    public boolean hasNext() {
        return (current + 1) < questionsToAsk.size();
    }

    @Override
    public void start() {
        running = true;

        Bukkit.broadcast(Component.text("A quiz is starting!").color(NamedTextColor.GREEN));
        Bukkit.broadcast(Component.text("-".repeat(15)).color(NamedTextColor.GREEN));
        printQuestion();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void stop() {
        running = false;

        HandlerList.unregisterAll(this);
        if (currentTask != null) {
            currentTask.cancel();
        }
    }

    private void continueQuiz() {
        if (hasNext()) {
            var secondsToContinue = config.getInt("seconds-between-questions");
            Bukkit.broadcast(
                    Component.text("The next question will be in " + secondsToContinue + " seconds.")
                            .color(NamedTextColor.GREEN)
            );

            currentTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                nextQuestion();
                printQuestion();
            }, 20 * secondsToContinue);
        } else {
            Bukkit.broadcast(Component.text("The quiz has FINISHED!"));

            stop();
        }
    }

    @Override
    public void rewardPlayer(Player ply) {
        var currentScore = playerScores.get(ply.getUniqueId());
        if (currentScore == null) currentScore = 0;
        currentScore += 1;
        playerScores.put(ply.getUniqueId(), currentScore);

        service.grantPlayerPoint(ply);

        currentWinner = ply;
        printCurrentWinner();
    }

    private void printCurrentWinner() {
        var builder = Component.text()
                .color(NamedTextColor.GREEN)
                .append(currentWinner.displayName().color(NamedTextColor.BLUE))
                .append(Component.text(" got the right answer! Which was: "));

        var current = getCurrent();
        switch (current.type) {
            case MultiChoice -> {
                var answerChoice = IntStream.range(0, current.answers.size())
                        .filter(i -> current.answers.get(i).valid)
                        .findFirst();

                if (answerChoice.isEmpty()) break; // This should never happen...
                var answer = current.answers.get(answerChoice.getAsInt());
                builder.append(
                    Component.text((answerChoice.getAsInt() + 1) + ". " + answer.text)
                            .color(NamedTextColor.GOLD)
                );
            }
            case Matching -> {
                builder.append(Component.text(current.answer).color(NamedTextColor.GOLD));
            }
        }

        Bukkit.broadcast(builder.build());
    }

    private void printWinners() {
        Bukkit.broadcast(Component.text("These are the top 3 players:").decorate(TextDecoration.BOLD).decorate(TextDecoration.UNDERLINED));

        var topPlayers = playerScores.entrySet()
                .stream()
                .sorted(Comparator.comparingInt(Map.Entry::getValue))
                .limit(3)
                .iterator();

        int i = 0;
        while (topPlayers.hasNext()) {
            i++;
            var entry = topPlayers.next();
            var ply = plugin.getServer().getOfflinePlayer(entry.getKey());
            Bukkit.broadcast(
                    Component.text()
                            .color(NamedTextColor.GOLD)
                            .append(Component.text(i + ". ").color(NamedTextColor.GRAY))
                            .append(Component.text(ply.getName()))
                            .build()
            );
        }
    }

    private void printQuestion() {
        var current = getCurrent();
        Bukkit.broadcast(Component.text(current.title)
                .decorate(TextDecoration.UNDERLINED)
                .decorate(TextDecoration.BOLD));
        switch (current.type) {
            case MultiChoice -> {
                for (int i = 0; i < current.answers.size(); i++) {
                    var answer = current.answers.get(i);
                    Bukkit.broadcast(
                            Component.text()
                                    .append(Component.text((i + 1) + ". ").color(NamedTextColor.GRAY))
                                    .append(Component.text(answer.text).color(NamedTextColor.GOLD))
                                    .build()
                    );
                }
            }
//            case Matching -> {
//            }
        }
    }

    @Override
    public boolean isActive() {
        return running;
    }

    @EventHandler
    protected void onChatEvent(AsyncChatEvent event) {
        if (currentWinner != null) return;

        var current = getCurrent();
        var message = PlainTextComponentSerializer.plainText().serialize(event.originalMessage());

        boolean gotItRight = false;
        switch (current.type) {
            case MultiChoice -> {
                Integer choice = null;
                try {
                    choice = Integer.parseInt(message);
                } catch (NumberFormatException e) {
                    // Ignore
                }

                for (int i = 0; i < current.answers.size(); i++) {
                    var answer = current.answers.get(i);
                    if (!answer.valid) continue;

                    if (choice != null ? choice == (i + 1) : message.equalsIgnoreCase(answer.text)) {
                        rewardPlayer(event.getPlayer());
                        gotItRight = true;
                        break;
                    }
                }
                event.setCancelled(true);
            }
            case Matching -> {
                if (message.equalsIgnoreCase(current.answer)) {
                    event.setCancelled(true);
                    rewardPlayer(event.getPlayer());
                    gotItRight = true;
                }
            }
        }

        if (gotItRight) {
            // Run in main thread
            plugin.getServer().getScheduler().runTask(plugin, this::continueQuiz);
        }
    }
}
