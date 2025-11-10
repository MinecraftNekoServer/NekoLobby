package neko.nekoLobby;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class QRCodeMapRenderer extends MapRenderer {
    private static final Map<Player, String> activeMaps = new HashMap<>();
    private final BufferedImage image;
    private boolean rendered = false;

    public QRCodeMapRenderer(BufferedImage image) {
        this.image = image;
    }

    @Override
    public void render(MapView view, MapCanvas canvas, Player player) {
        if (rendered) return;

        try {
            // Scale image to map size (128x128)
            Image scaled = image.getScaledInstance(128, 128, Image.SCALE_SMOOTH);
            BufferedImage resized = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.drawImage(scaled, 0, 0, null);
            g.dispose();

            // Draw to map canvas
            canvas.drawImage(0, 0, resized);
            rendered = true;
        } catch (Exception e) {
            // Cannot send messages directly to player in map renderer, so we don't send messages here
            e.printStackTrace();
        }
    }

    /**
     * Load QR code image from URL
     */
    public static BufferedImage loadQRCodeFromUrl(String url) throws IOException {
        URL u = new URL(url);
        try (InputStream in = u.openStream()) {
            return ImageIO.read(in);
        }
    }

    /**
     * Show payment QR code on map
     */
    public static void showQRCodeOnMap(Player player, String qrCodeUrl) {
        try {
            // Handle escaped characters in URL
            String cleanUrl = qrCodeUrl.replace("\\/", "/");

            // Get plugin instance
            JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin("NekoLobby");

            // Download image asynchronously
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    BufferedImage qrImage = loadQRCodeFromUrl(cleanUrl);

                    // Create map and render in main thread
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            // Create map view
                            MapView mapView = Bukkit.createMap(player.getWorld());
                            mapView.getRenderers().clear();
                            mapView.addRenderer(new QRCodeMapRenderer(qrImage));

                            // Create map item
                            ItemStack mapItem = new ItemStack(Material.MAP, 1, mapView.getId());
                            MapMeta meta = (MapMeta) mapItem.getItemMeta();
                            meta.setDisplayName(ChatColor.GOLD + "Payment QR Code Map");
                            mapItem.setItemMeta(meta);

                            // Put in player's inventory slot 3 (index 2)
                            player.getInventory().setItem(2, mapItem);
                            player.sendMessage(ChatColor.GREEN + "Payment QR code map has been placed in slot 3 of your inventory!");
                            player.sendMessage(ChatColor.YELLOW + "Please scan the QR code on the map to complete payment.");
                            player.sendMessage(ChatColor.GOLD + "After payment, please use /vippay confirm command to activate VIP privileges.");

                            // Record active QR code
                            activeMaps.put(player, cleanUrl);
                        } catch (Exception e) {
                            player.sendMessage(ChatColor.RED + "Error creating QR code map: " + e.getMessage());
                            e.printStackTrace();
                            
                            // Send link message if map creation fails
                            player.sendMessage(ChatColor.GREEN + "Payment QR code has been generated!");
                            player.sendMessage(ChatColor.YELLOW + "Please visit the following link to scan the QR code to complete payment:");
                            player.sendMessage(ChatColor.AQUA + cleanUrl);
                            player.sendMessage(ChatColor.GOLD + "After payment, please use /vippay confirm command to activate VIP privileges.");
                            
                            // Record active QR code
                            activeMaps.put(player, cleanUrl);
                        }
                    });
                } catch (Exception e) {
                    // Send error message in main thread
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(ChatColor.RED + "Error loading payment QR code: " + e.getMessage());
                        e.printStackTrace();
                        
                        // Send link message as alternative
                        player.sendMessage(ChatColor.GREEN + "Payment QR code has been generated!");
                        player.sendMessage(ChatColor.YELLOW + "Please visit the following link to scan the QR code to complete payment:");
                        player.sendMessage(ChatColor.AQUA + cleanUrl);
                        player.sendMessage(ChatColor.GOLD + "After payment, please use /vippay confirm command to activate VIP privileges.");
                        
                        // Record active QR code
                        activeMaps.put(player, cleanUrl);
                    });
                }
            });
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Error generating payment QR code: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get player's active map
     */
    public static String getPlayerActiveQRCode(Player player) {
        return activeMaps.get(player);
    }

    /**
     * Remove player's active map
     */
    public static String removePlayerActiveQRCode(Player player) {
        return activeMaps.remove(player);
    }
}