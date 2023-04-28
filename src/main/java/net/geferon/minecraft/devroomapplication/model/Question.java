package net.geferon.minecraft.devroomapplication.model;

import java.util.List;

public class Question {
    public enum Type {
        MultiChoice,
        Matching
    }

    public String title;
//    public String description;
    public Type type;
    public String answer;
    public List<Answer> answers;
}
