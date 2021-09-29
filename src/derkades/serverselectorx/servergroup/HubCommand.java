package derkades.serverselectorx.servergroup;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class HubCommand extends Command {

	public HubCommand() {
		super("hub", null, "lobby");
	}

	@Override
	public void execute(final CommandSender sender, final String[] args) {
		if (sender instanceof ProxiedPlayer) {
			Connect.handle((ProxiedPlayer) sender, "hub", Connect.RETRIES);
		}
	}

}
