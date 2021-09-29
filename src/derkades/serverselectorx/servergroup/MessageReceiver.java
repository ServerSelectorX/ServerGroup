package derkades.serverselectorx.servergroup;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class MessageReceiver implements Listener {

	@EventHandler
	public void on(final PluginMessageEvent event) {
		if (!event.getTag().equalsIgnoreCase(SSXServerGroup.CHANNEL_NAME)) {
			return;
		}

		final ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
		final String subChannel = in.readUTF();
		if (subChannel.equals("Connect")) {
			if (event.getReceiver() instanceof ProxiedPlayer) {
				final String targetGame = in.readUTF();
				Connect.handle((ProxiedPlayer) event.getReceiver(), targetGame, Connect.RETRIES);
			} else {
				System.err.println("Invalid message received");
			}
		} else {
			System.err.println("Received plugin message on unknown subchannel: " + subChannel);
		}
	}

}