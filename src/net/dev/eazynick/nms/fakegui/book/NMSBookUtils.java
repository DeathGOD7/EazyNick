package net.dev.eazynick.nms.fakegui.book;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.dev.eazynick.EazyNick;
import net.dev.eazynick.nms.ReflectionHelper;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class NMSBookUtils extends ReflectionHelper {

	private EazyNick eazyNick;
	
	public NMSBookUtils(EazyNick eazyNick) {
		this.eazyNick = eazyNick;
	}

	public void open(Player player, ItemStack book) {
		ItemStack hand = player.getItemInHand();
		
		player.setItemInHand(book);
		
		Bukkit.getScheduler().runTaskLater(eazyNick, () -> {
			try {
				String version = eazyNick.getVersion();
				boolean is17 = version.startsWith("1_17");
				Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
				Class<?> craftItemStackClass = getCraftClass("inventory.CraftItemStack");
				Object nmsItemStack = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class).invoke(null, book);
				
				if(!(version.startsWith("1_7") || version.startsWith("1_8"))) {
					Class<?> enumHand = getNMSClass(is17 ? "world.EnumHand" : "EnumHand");
					Object mainHand = getField(enumHand, is17 ? "a" : "MAIN_HAND").get(enumHand);
					
					if(Bukkit.getVersion().contains("1.14.4") || version.startsWith("1_15") || version.startsWith("1_16") || version.startsWith("1_17")) {
						Class<?> itemWrittenBook = getNMSClass(is17 ? "world.item.ItemWrittenBook" : "ItemWrittenBook");
						
						if ((boolean) itemWrittenBook.getMethod("a", getNMSClass(is17 ? "world.item.ItemStack" : "ItemStack"), getNMSClass(is17 ? "commands.CommandListenerWrapper" : "CommandListenerWrapper"), getNMSClass(is17 ? "world.entity.player.EntityHuman" : "EntityHuman")).invoke(itemWrittenBook, nmsItemStack, entityPlayer.getClass().getMethod("getCommandListener").invoke(entityPlayer), entityPlayer)) {
							Object activeContainer = entityPlayer.getClass().getField(is17 ? "bV" : "activeContainer").get(entityPlayer);
							
			                activeContainer.getClass().getMethod("c").invoke(activeContainer);
			            }
						
			            Object packet = getNMSClass(is17 ? "network.protocol.game.PacketPlayOutOpenBook" : "PacketPlayOutOpenBook").getConstructor(enumHand).newInstance(mainHand);
						Object playerConnection = entityPlayer.getClass().getField(is17 ? "b" : "playerConnection").get(entityPlayer);
						playerConnection.getClass().getMethod("sendPacket", getNMSClass(is17 ? "network.protocol.Packet" : "Packet")).invoke(playerConnection, packet);
					} else
						entityPlayer.getClass().getMethod("a", getNMSClass("ItemStack"), enumHand).invoke(entityPlayer, nmsItemStack, mainHand);
				} else {
					Object packet;
					
					if(version.startsWith("1_7"))
						packet = getNMSClass("PacketPlayOutCustomPayload").getConstructor(String.class, net.minecraft.util.io.netty.buffer.ByteBuf.class).newInstance("MC|BOpen", net.minecraft.util.io.netty.buffer.Unpooled.EMPTY_BUFFER);
					else
						packet = getNMSClass("PacketPlayOutCustomPayload").getConstructor(String.class, getNMSClass("PacketDataSerializer")).newInstance("MC|BOpen", getNMSClass("PacketDataSerializer").getConstructor(ByteBuf.class).newInstance(Unpooled.EMPTY_BUFFER));
				
					Object playerConnection = entityPlayer.getClass().getField("playerConnection").get(entityPlayer);
					playerConnection.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(playerConnection, packet);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				player.setItemInHand(hand);
			}
		}, 2);
	}
}
