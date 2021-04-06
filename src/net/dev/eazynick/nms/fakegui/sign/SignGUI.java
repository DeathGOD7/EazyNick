package net.dev.eazynick.nms.fakegui.sign;

import java.lang.reflect.Field;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.dev.eazynick.EazyNick;
import net.dev.eazynick.nms.ReflectionHelper;
import net.dev.eazynick.utilities.Utils;

import io.netty.channel.*;

public class SignGUI implements Listener {

	public void open(Player player, String line1, String line2, String line3, String line4, EditCompleteListener listener) {
		EazyNick eazyNick = EazyNick.getInstance();
		ReflectionHelper reflectionHelper = eazyNick.getReflectUtils();
		Utils utils = eazyNick.getUtils();
		
		Block block = player.getWorld().getBlockAt(player.getLocation().clone().add(0, 5, 0));
		block.setType(Material.getMaterial((utils.isNewVersion() && !(eazyNick.getVersion().startsWith("1_13"))) ? "OAK_SIGN" : (eazyNick.getVersion().startsWith("1_13") ? "SIGN" : "SIGN_POST")));
		
		Sign sign = (Sign) block.getState();
		sign.setLine(0, line1);
		sign.setLine(1, line2);
		sign.setLine(2, line3);
		sign.setLine(3, line4);
		sign.update(false, false);
		
		Bukkit.getOnlinePlayers().stream().filter(currentPlayer -> (currentPlayer != player)).forEach(currentPlayer -> currentPlayer.sendBlockChange(block.getLocation(), Material.AIR, (byte) 0));
		
		Bukkit.getScheduler().runTaskLater(eazyNick, new Runnable() {
			
			@Override
			public void run() {
				try {
					boolean useCraftBlockEntityState = utils.isNewVersion() || Bukkit.getVersion().contains("1.12.2") || Bukkit.getVersion().contains("1.12.1");
					Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
					Object playerConnection = entityPlayer.getClass().getField("playerConnection").get(entityPlayer);

					Field tileField = (useCraftBlockEntityState ? reflectionHelper.getCraftClass("block.CraftBlockEntityState") : sign.getClass()).getDeclaredField(useCraftBlockEntityState ? "tileEntity" : "sign");
					tileField.setAccessible(true);
					Object tileSign = tileField.get(sign);

					Field editable = tileSign.getClass().getDeclaredField("isEditable");
					editable.setAccessible(true);
					editable.set(tileSign, true);

					Field handler = tileSign.getClass().getDeclaredField((eazyNick.getVersion().startsWith("1_15") || eazyNick.getVersion().startsWith("1_16")) ? "c" : (eazyNick.getVersion().startsWith("1_14") ? "j" : (eazyNick.getVersion().startsWith("1_13") ? "g" : "h")));
					handler.setAccessible(true);
					handler.set(tileSign, entityPlayer);
					
					playerConnection.getClass().getDeclaredMethod("sendPacket", reflectionHelper.getNMSClass("Packet")).invoke(playerConnection, reflectionHelper.getNMSClass("PacketPlayOutOpenSignEditor").getConstructor(reflectionHelper.getNMSClass("BlockPosition")).newInstance(reflectionHelper.getNMSClass("BlockPosition").getConstructor(double.class, double.class, double.class).newInstance(sign.getX(), sign.getY(), sign.getZ())));
					
					Bukkit.getScheduler().runTaskLater(eazyNick, () -> block.setType(Material.AIR), 3);
					
					Object networkManager = playerConnection.getClass().getDeclaredField("networkManager").get(playerConnection);
					Channel channel = (Channel) networkManager.getClass().getDeclaredField("channel").get(networkManager);
					
					Bukkit.getPluginManager().registerEvents(new Listener() {
						
						@EventHandler
						public void onQuit(PlayerQuitEvent event) {
							if(event.getPlayer() == player) {
								if (channel.pipeline().get("PacketInjector") != null)
									channel.pipeline().remove("PacketInjector");
							}
						}
						
						@EventHandler
						public void onKick(PlayerKickEvent event) {
							if(event.getPlayer() == player) {
								if (channel.pipeline().get("PacketInjector") != null)
									channel.pipeline().remove("PacketInjector");
							}
						}
						
					}, eazyNick);
					
					if (channel.pipeline().get("PacketInjector") == null) {
						channel.pipeline().addBefore("packet_handler", "PacketInjector", new ChannelDuplexHandler() {
							
							@Override
							public void channelRead(ChannelHandlerContext ctx, Object packet) throws Exception {
								if(packet.getClass().getName().endsWith("PacketPlayInUpdateSign")) {
									Object[] rawLines = (Object[]) reflectionHelper.getField(packet.getClass(), "b").get(packet);
									
									Bukkit.getScheduler().runTask(eazyNick, new Runnable() {
										
										@Override
										public void run() {
											try {
												String[] lines = new String[4];

												if(eazyNick.getVersion().startsWith("1_8")) {
													int i = 0;
													
													for (Object obj : rawLines) {
														lines[i] = (String) obj.getClass().getMethod("getText").invoke(obj);
														
														i++;
													}
												} else
													lines = (String[]) rawLines;
												
												if (channel.pipeline().get("PacketInjector") != null)
													channel.pipeline().remove("PacketInjector");
												
												listener.onEditComplete(new EditCompleteEvent(lines));
											} catch (Exception ex) {
												ex.printStackTrace();
											}
										}
									});
								}
							}
							
						});
					}
				} catch (Exception ex) {
		        	ex.printStackTrace();
		        }
			}
		}, 3);
	}
	
	public interface EditCompleteListener {
		
		void onEditComplete(EditCompleteEvent event);
		
	}
	
	public class EditCompleteEvent {
		
		private String[] lines;
		
		public EditCompleteEvent(String[] lines) {
			this.lines = lines;
		}
		
		public String[] getLines() {
			return lines;
		}
		
	}
    
}