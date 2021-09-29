package derkades.serverselectorx.servergroup.party;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public abstract class AbstractPartyHandler {

	public abstract int getPartySize(ProxiedPlayer player);

	public abstract boolean isTeleportAllowed(ProxiedPlayer player);

	public abstract void teleportParty(ProxiedPlayer player, BaseComponent[] connectMessage, ServerInfo targetServer);

}
