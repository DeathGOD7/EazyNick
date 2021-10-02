package net.dev.eazynick.commands;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import net.dev.eazynick.EazyNick;
import net.dev.eazynick.utilities.Utils;

public class ToggleBungeeNickCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		EazyNick eazyNick = EazyNick.getInstance();
		Utils utils = eazyNick.getUtils();
		
		if(sender instanceof Player) {
			Player player = (Player) sender;
			
			if(player.hasPermission("eazynick.nick.random") && player.hasPermission("eazynick.item")) {
				if (eazyNick.getSetupYamlFile().getConfiguration().getBoolean("BungeeCord"))
					utils.toggleBungeeNick(player);
			} else
				eazyNick.getLanguageYamlFile().sendMessage(player, utils.getNoPerm());
		} else
			utils.sendConsole(utils.getNotPlayer());
		
		return true;
	}
	
}
