package derkades.serverselectorx.servergroup.party;

import java.util.Objects;

import de.simonsator.partyandfriends.api.pafplayers.OnlinePAFPlayer;
import de.simonsator.partyandfriends.api.pafplayers.PAFPlayerManager;
import de.simonsator.partyandfriends.api.party.PartyManager;
import de.simonsator.partyandfriends.api.party.PlayerParty;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class SimonsatorPartyHandler extends AbstractPartyHandler {

	public static final SimonsatorPartyHandler INSTANCE = new SimonsatorPartyHandler();

	@Override
	public int getPartySize(final ProxiedPlayer player) {
		final OnlinePAFPlayer pafPlayer = PAFPlayerManager.getInstance().getPlayer(player);
		final PlayerParty party = PartyManager.getInstance().getParty(pafPlayer);
		return party == null ? 1 : party.getPlayers().size();
	}

	@Override
	public boolean isTeleportAllowed(final ProxiedPlayer player) {
		final OnlinePAFPlayer pafPlayer = PAFPlayerManager.getInstance().getPlayer(player);
		final PlayerParty party = PartyManager.getInstance().getParty(pafPlayer);
		return (party != null && !party.getLeader().equals(pafPlayer));
	}

	@Override
	public void teleportParty(final ProxiedPlayer player, final BaseComponent[] teleportMessage, final ServerInfo targetServer) {
		Objects.requireNonNull(player);
		Objects.requireNonNull(targetServer);

		// Teleport player to server;
		if (teleportMessage != null) {
			player.sendMessage(teleportMessage);
		}
		player.connect(targetServer);

		// Teleport all party members to server as well, if a party exists

		final OnlinePAFPlayer pafPlayer = PAFPlayerManager.getInstance().getPlayer(player);
		final PlayerParty party = PartyManager.getInstance().getParty(pafPlayer);
		if (party != null) {
			// getPlayers() does not include the party leader
			party.getPlayers().stream().map(OnlinePAFPlayer::getPlayer).forEach(p -> {
				if (teleportMessage != null) {
					p.sendMessage(teleportMessage);
				}
				p.connect(targetServer);
			});
		}
	}



}
