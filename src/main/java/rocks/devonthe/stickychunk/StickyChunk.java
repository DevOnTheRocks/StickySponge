package rocks.devonthe.stickychunk;

import com.google.inject.Inject;
import rocks.devonthe.stickychunk.chunkload.ChunkLoadCallback;
import rocks.devonthe.stickychunk.chunkload.TicketManager;
import rocks.devonthe.stickychunk.command.CommandLoad;
import rocks.devonthe.stickychunk.command.CommandPersist;
import rocks.devonthe.stickychunk.command.CommandUnload;
import rocks.devonthe.stickychunk.config.ConfigManager;
import rocks.devonthe.stickychunk.data.DataStore;
import rocks.devonthe.stickychunk.database.SqliteDatabase;
import rocks.devonthe.stickychunk.listener.RegionAreaListener;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameAboutToStartServerEvent;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.world.World;
import rocks.devonthe.stickychunk.config.StickyChunkConfig;
import rocks.devonthe.stickychunk.database.IDatabase;

import java.nio.file.Path;

/**
 * Created by Cossacksman on 02/01/2017.
 */

@Plugin(
		id = "stickychunk",
		name = "StickyChunk",
		version = "0.10.0-SNAPSHOT",
		description = "A chunk persistence plugin for keeping your entities and blocks loaded.",
		authors = {"cossacksman"}
)
public class StickyChunk {
	@Inject
	private Logger logger;
	@Inject
	private Game game;
	@Inject
	@ConfigDir(sharedRoot = false)
	private Path configDirectory;
	@Inject
	@DefaultConfig(sharedRoot = false)
	private ConfigurationLoader<CommentedConfigurationNode> configManager;
	private ConfigManager pluginConfigManager;
	private StickyChunkConfig config;

	private TicketManager ticketManager;

	private DataStore dataStore;
	private IDatabase database;

	private static StickyChunk instance;

	@Listener
	public void onPostInitialization(GamePostInitializationEvent event) {
		instance = this;
	}

	@Listener
	public void onAboutToStart(GameAboutToStartServerEvent event) {
		// Initialize configs
		config = new StickyChunkConfig();
		pluginConfigManager = new ConfigManager(configManager);
		pluginConfigManager.save();


		ticketManager = new TicketManager();
		database = new SqliteDatabase();
		dataStore = new DataStore();

		dataStore.addPlayerRegions(database.loadRegionData());
		dataStore.addUsers(database.loadUserData());

		// Register callbacks
		game.getServer().getChunkTicketManager().registerCallback(this, new ChunkLoadCallback());

		// Register events
		Sponge.getGame().getEventManager().registerListeners(this, new RegionAreaListener());

		// Register commands
		registerCommands();
	}

	@Listener
	public void onServerStarted(GameStartedServerEvent event) {
		// Register tickets
	}

	@Listener
	public void onServerStopped(GameStoppedServerEvent event) {
		database.saveRegionData(dataStore.getCollatedRegions());
	}

	public DataStore getDataStore() {
		return dataStore;
	}

	public World getDefaultWorld() {
		String defaultWorldName = StickyChunk.getInstance().getGame().getServer().getDefaultWorldName();
		return StickyChunk.getInstance().getGame().getServer().getWorld(defaultWorldName).get();
	}

	private void registerCommands() {
		CommandPersist.register();
		CommandLoad.register();
		CommandUnload.register();
	}

	public static StickyChunk getInstance() {
		return instance;
	}

	public Logger getLogger() {
		return logger;
	}

	public Game getGame() {
		return game;
	}

	public Path getConfigDirectory() {
		return configDirectory;
	}

	public StickyChunkConfig getConfig() {
		return config;
	}

	public TicketManager getTicketManager() {
		return ticketManager;
	}
}
