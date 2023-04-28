package net.geferon.minecraft.devroomapplication.services;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.geferon.minecraft.devroomapplication.DevRoomApplicationPlugin;
import net.geferon.minecraft.devroomapplication.model.ActiveQuiz;
import net.geferon.minecraft.devroomapplication.model.ActiveQuizFactory;
import net.geferon.minecraft.devroomapplication.model.ActiveQuizInterface;
import org.bukkit.Server;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.java.JavaPlugin;
import org.mariadb.jdbc.MariaDbDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;

public class BinderModule extends AbstractModule {
    private final DevRoomApplicationPlugin plugin;
    private final DataSource dataSource;

    public BinderModule(DevRoomApplicationPlugin plugin, DataSource dataSource) {
        this.plugin = plugin;
        this.dataSource = dataSource;
    }

    public Injector createInjector() {
        return Guice.createInjector(this);
    }

    @Override
    protected void configure() {
        this.bind(DevRoomApplicationPlugin.class).toInstance(this.plugin);
        this.bind(JavaPlugin.class).to(DevRoomApplicationPlugin.class);

        // Services in the plugin itself
        //this.bind(Logger.class).toInstance(this.plugin.getLogger());
        this.bind(Server.class).toInstance(this.plugin.getServer());
        var config = this.plugin.getConfig();
        this.bind(Configuration.class).toInstance(config);

        // Factories
        this.binder().install(new FactoryModuleBuilder()
                .implement(ActiveQuizInterface.class, ActiveQuiz.class)
                .build(ActiveQuizFactory.class));

        // DB
        //this.bind(HikariDataSource.class).toInstance(dataSource);
        //this.bind(DataSource.class).to(HikariDataSource.class);
        this.bind(DataSource.class).toInstance(dataSource);
    }
}
