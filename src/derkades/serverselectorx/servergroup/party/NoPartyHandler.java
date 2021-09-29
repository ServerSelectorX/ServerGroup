package derkades.serverselectorx.servergroup.party;

import java.util.Objects;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class NoPartyHandler extends AbstractPartyHandler {

	public static final NoPartyHandler INSTANCE = new NoPartyHandler();

	@Override
	public int getPartySize(final ProxiedPlayer player) {
		return 1;
	}

	@Override
	public boolean isTeleportAllowed(final ProxiedPlayer player) {
		return true;
	}

	@Override
	public void teleportParty(final ProxiedPlayer player, final BaseComponent[] teleportMessage, final ServerInfo targetServer) {
		Objects.requireNonNull(player);
		Objects.requireNonNull(targetServer);

		// Teleport just the player to the target server
		if (teleportMessage != null) {
			player.sendMessage(teleportMessage);
		}
		player.connect(targetServer);

	}



}
