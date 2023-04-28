package net.geferon.minecraft.devroomapplication;

import com.google.inject.Injector;
import net.geferon.minecraft.devroomapplication.commands.QuizScoresCommand;
import net.geferon.minecraft.devroomapplication.commands.StartQuizCommand;
import net.geferon.minecraft.devroomapplication.services.BinderModule;
import net.geferon.minecraft.devroomapplication.services.DatabaseService;
import net.geferon.minecraft.devroomapplication.services.QuizService;
import org.bukkit.plugin.java.JavaPlugin;

public final class DevRoomApplicationPlugin extends JavaPlugin {
    private Injector injector;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        var db = DatabaseService.getDataSource(getConfig());
        if (db == null) {
            getLogger().severe("Connection to the database could not be established.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Dependency injection
        BinderModule module = new BinderModule(this, db);
        //Injector injector = Guice.createInjector(module);
        injector = module.createInjector();

        // Plugin startup logic
        injector.getInstance(DatabaseService.class).initialize();
        injector.getInstance(QuizService.class).load();

        getCommand("startquiz").setExecutor(injector.getInstance(StartQuizCommand.class));
        getCommand("startquiz").setTabCompleter(injector.getInstance(StartQuizCommand.class));
        getCommand("quizscores").setExecutor(injector.getInstance(QuizScoresCommand.class));
    }

    @Override
    public void onDisable() {
        if (injector == null) return;

        injector.getInstance(DatabaseService.class).dispose();
    }
}
