package net.geferon.minecraft.devroomapplication.services;

import net.geferon.minecraft.devroomapplication.DevRoomApplicationPlugin;
import net.geferon.minecraft.devroomapplication.exceptions.InvalidCategoryException;
import net.geferon.minecraft.devroomapplication.model.ActiveQuiz;
import net.geferon.minecraft.devroomapplication.model.ActiveQuizFactory;
import net.geferon.minecraft.devroomapplication.model.Category;
import net.geferon.minecraft.devroomapplication.model.QuizFile;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Singleton
public class QuizService {
    private final DevRoomApplicationPlugin plugin;
    private final Configuration config;
    private final ActiveQuizFactory quizFactory;
    private final DatabaseService dbService;

    private Map<UUID, Integer> playerScores = new HashMap<>();

    @Inject
    public QuizService(DevRoomApplicationPlugin plugin, Configuration config, ActiveQuizFactory quizFactory, DatabaseService dbService) {
        this.plugin = plugin;
        this.config = config;
        this.quizFactory = quizFactory;
        this.dbService = dbService;
    }

    private QuizFile quizConfig;
    //private Map<String, Category> availableCategories = new HashMap<>();
    private ActiveQuiz activeQuiz;

    public void load() {
        // Load categories
        var quizFile = new File(plugin.getDataFolder(), "quiz.yml");
        if (!quizFile.exists()) {
            try (var strmOut = new FileOutputStream(quizFile)) {
                plugin.getResource("quiz.yml").transferTo(strmOut);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Yaml yaml = new Yaml(new Constructor(QuizFile.class));
        try (var strmIn = new FileInputStream(quizFile)) {
            quizConfig = yaml.load(strmIn);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Load player data
        // Run sync as we're doing it on startup
        try (
                var con = dbService.getConnection();
                var statement = con.prepareStatement(
                        "SELECT id, score FROM playerscores"
                );
        ) {
            var rs = statement.executeQuery();
            while (rs.next()) {
                playerScores.put(UUID.fromString(rs.getString(1)), rs.getInt(2));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Category> getCategories() {
        return quizConfig.categories;
    }

    public boolean hasCategory(String category) {
        return getCategories().containsKey(category);
    }

    public void startQuiz(String categoryKey) throws InvalidCategoryException {
        if (categoryKey == null) {
            var keys = quizConfig.categories.keySet().toArray(String[]::new);
            var rnd = ThreadLocalRandom.current().nextInt(0, keys.length);
            categoryKey = keys[rnd];
        }

        if (!quizConfig.categories.containsKey(categoryKey)) {
            throw new InvalidCategoryException();
        }

        var category = quizConfig.categories.get(categoryKey);

        if (activeQuiz != null) {
            activeQuiz.stop();
        }

        activeQuiz = quizFactory.create(category);
        activeQuiz.pickQuestions(config.getInt("questions-per-quiz"));
        activeQuiz.start();
    }

    public void stopQuiz() {
        if (activeQuiz == null) return;

        activeQuiz.stop();

        activeQuiz = null;
    }

    public boolean isQuizActive() {
        return activeQuiz != null && activeQuiz.isActive();
    }

    public void grantPlayerPoint(Player ply) {
        var uuid = ply.getUniqueId();
        var currentScore = playerScores.containsKey(uuid) ? playerScores.get(uuid) : 0;
        currentScore++;
        playerScores.put(uuid, currentScore);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (
                    var con = dbService.getConnection();
                    var statement = con.prepareStatement(
                            "INSERT INTO playerscores (id, score) VALUES (?, ?) ON DUPLICATE KEY UPDATE score = ?"
                    );
            ) {
                statement.setString(1, uuid.toString());
                statement.setInt(2, playerScores.get(uuid));
                statement.setInt(3, playerScores.get(uuid));

                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public Map<UUID, Integer> getPlayerScores() {
        return Collections.unmodifiableMap(playerScores);
    }

    public Integer getPlayerScore(UUID ply) {
        return playerScores.containsKey(ply) ? playerScores.get(ply) : null;
    }

    public Integer getPlayerScore(OfflinePlayer ply) {
        return getPlayerScore(ply.getUniqueId());
    }


}
