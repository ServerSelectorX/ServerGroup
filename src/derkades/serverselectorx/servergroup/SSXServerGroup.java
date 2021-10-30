package derkades.serverselectorx.servergroup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import derkades.serverselectorx.servergroup.party.AbstractPartyHandler;
import derkades.serverselectorx.servergroup.party.NoPartyHandler;
import derkades.serverselectorx.servergroup.party.SimonsatorPartyHandler;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class SSXServerGroup extends Plugin {

	static final String CHANNEL_NAME = "ssx:servergroup";
	static AbstractPartyHandler partyHandler = null;

	static SSXServerGroup instance;

	static Configuration config;

	@Override
	public void onEnable() {
		instance = this;

		getProxy().registerChannel(CHANNEL_NAME);

        if (!getDataFolder().exists()) {
			getDataFolder().mkdir();
		}

        final File file = new File(getDataFolder(), "config.yml");

        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        loadConfiguration();

        getProxy().getPluginManager().registerCommand(this, new HubCommand());
        getProxy().getPluginManager().registerListener(this, new MessageReceiver());
	}

	static void loadConfiguration() {
		try {
			config = ConfigurationProvider.getProvider(YamlConfiguration.class)
					.load(new File(instance.getDataFolder(), "config.yml"));
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		Connect.LOCK_WAIT = config.getInt("lock_wait.game");
		Connect.RETRY_WAIT = config.getInt("retry_wait.game");
		Connect.RETRIES = config.getInt("retries.game");
		partyHandler = config.getBoolean("party") ? SimonsatorPartyHandler.INSTANCE : NoPartyHandler.INSTANCE;

		Connect.SERVER_GROUPS = new HashMap<>();
		for (final String groupName : config.getSection("groups").getKeys()) {
			final String gamePrefix = config.getString("groups." + groupName);
			final List<ServerInfo> servers = instance.getProxy()
					.getServers()
					.entrySet()
					.stream()
					.filter(e -> e.getKey().startsWith(gamePrefix))
					.map(e -> e.getValue())
					.collect(Collectors.toList());
			Connect.SERVER_GROUPS.put(groupName, servers);
		}
	}

	static TreeMap<Integer, ServerInfo> pingForTeleportCandidates(
			final List<ServerInfo> servers,
			final int partySize) {
		Objects.requireNonNull(servers, "Servers is null");

		final List<CompletableFuture<Optional<Map.Entry<ServerInfo, ServerPing>>>> futures =
				new ArrayList<>(servers.size());

		for (final ServerInfo server : servers) {
			System.out.println(server.getName());
			futures.add(CompletableFuture.supplyAsync(() -> {
				final AtomicReference<ServerPing> result = new AtomicReference<>();

				System.out.println(server.getName() + "Start of async");

				server.ping((data, error) -> {
					if (error != null) {
						result.set(null);
						System.out.println(server.getName() + "Done error");
					} else {
						result.set(data);
						System.out.println(server.getName() + "Done success");
					}
					synchronized(result) {
						result.notify();
					}
				});

				try {
					System.out.println(server.getName() + "Start waiting");
					synchronized(result) {
						result.wait(500); // TODO Configurable timeout
					}
					final ServerPing ping = result.get();
					if (ping == null) {
						System.out.println(server.getName() + "Done waiting - fail");
						return Optional.empty();
					} else {
						System.out.println(server.getName() + "Done waiting - success");
						return Optional.of(Map.entry(server, ping));
					}
				} catch (final InterruptedException e) {
					System.out.println(server.getName() + "Interrupted");
					return Optional.empty();
				}
			}));
		}

		final TreeMap<Integer, ServerInfo> teleportCandidates = new TreeMap<>();

		futures.stream()
				.map(future -> future.join())
				.filter(Optional::isPresent)
				.map(Optional::get)
				.forEach(entry -> {
					final ServerPing ping = entry.getValue();
					final int freeSlots = ping.getPlayers().getMax() - ping.getPlayers().getOnline();
					if (freeSlots >= partySize) {
						final ServerInfo server = entry.getKey();
						teleportCandidates.put(freeSlots, server);
					}
				});

		return teleportCandidates;
	}

}
