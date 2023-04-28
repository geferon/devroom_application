package net.geferon.minecraft.devroomapplication.model;

import org.bukkit.entity.Player;

import java.util.List;

public interface ActiveQuizInterface {
    List<Question> pickQuestions(int amount);

    Question nextQuestion();

    Question getCurrent();

    boolean hasNext();

    void start();

    void stop();

    void rewardPlayer(Player ply);

    boolean isActive();
}
