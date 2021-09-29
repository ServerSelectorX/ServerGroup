package derkades.serverselectorx.servergroup;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import derkades.serverselectorx.servergroup.party.AbstractPartyHandler;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class Connect {

	static int LOCK_WAIT;
	static int RETRY_WAIT;
	static int RETRIES;
	static Map<String, List<ServerInfo>> SERVER_GROUPS;

	// only allow one teleportation at a time to prevent issues with
	// player count changing while checking
	// lock is game specific
	private static Set<String> lock = new HashSet<>();

	static void handle(final ProxiedPlayer player, final String targetGroup, final int retries) {
		if (retries <= 0) {
			player.sendMessage(new ComponentBuilder("No servers available right now.").create());
			return;
		}

		if (lock.contains(targetGroup)) {
			System.out.println("Game teleport locked - " + targetGroup);
			SSXServerGroup.instance.getProxy().getScheduler().schedule(SSXServerGroup.instance,
					() -> handle(player, targetGroup, retries), LOCK_WAIT, TimeUnit.MILLISECONDS);
			return;
		}

		lock.add(targetGroup);

		final AbstractPartyHandler party = SSXServerGroup.partyHandler;

		if (!party.isTeleportAllowed(player)) {
			player.sendMessage(new ComponentBuilder("Only the party leader can teleport to servers").create());
			lock.remove(targetGroup);
			return;
		}

		final int partySize = party.getPartySize(player);

		if (!SERVER_GROUPS.containsKey(targetGroup)) {
			player.sendMessage(new ComponentBuilder("This server group does not exist").create());
			lock.remove(targetGroup);
			return;
		}

		ProxyServer.getInstance().getScheduler().runAsync(SSXServerGroup.instance, () -> {
			final TreeMap<Integer, ServerInfo> teleportCandidates =
					SSXServerGroup.pingForTeleportCandidates(SERVER_GROUPS.get(targetGroup), partySize);

			ProxyServer.getInstance().getScheduler().schedule(SSXServerGroup.instance, () -> {
				// Send message and try again if no servers are available
				if (teleportCandidates.isEmpty()) {
					player.sendMessage(new ComponentBuilder("No servers available right now. Trying again in a moment..").create());
					SSXServerGroup.instance.getProxy().getScheduler().schedule(SSXServerGroup.instance,
							() -> handle(player, targetGroup, retries - 1), RETRY_WAIT, TimeUnit.MILLISECONDS);
					lock.remove(targetGroup);
					return;
				}

				// From all available servers, select server with least free slots
				final ServerInfo targetServer = teleportCandidates.firstEntry().getValue();

				final BaseComponent[] teleportMessage = new ComponentBuilder("Teleporting to " + targetServer.getName()).create();

				party.teleportParty(player, teleportMessage, targetServer);

				lock.remove(targetGroup);
			}, 0, TimeUnit.SECONDS);
		});
	}

}
