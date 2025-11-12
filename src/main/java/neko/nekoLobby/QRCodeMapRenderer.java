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
            // 缩放图片到地图大小 (128x128)
            Image scaled = image.getScaledInstance(128, 128, Image.SCALE_SMOOTH);
            BufferedImage resized = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.drawImage(scaled, 0, 0, null);
            g.dispose();

            // 绘制到地图画布
            canvas.drawImage(0, 0, resized);
            rendered = true;
        } catch (Exception e) {
            // 在地图渲染器中无法直接向玩家发送消息，所以这里不发送消息
            e.printStackTrace();
        }
    }

    /**
     * 从URL加载二维码图片
     */
    public static BufferedImage loadQRCodeFromUrl(String url) throws IOException {
        URL u = new URL(url);
        try (InputStream in = u.openStream()) {
            return ImageIO.read(in);
        }
    }

    /**
     * 显示支付二维码地图
     */
    public static void showQRCodeOnMap(Player player, String qrCodeUrl) {
        try {
            // 处理URL中的转义字符
            String cleanUrl = qrCodeUrl.replace("/", "/");

            // 获取插件实例
            JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin("NekoLobby");

            // 异步下载图片
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    BufferedImage qrImage = loadQRCodeFromUrl(cleanUrl);

                    // 在主线程中创建地图并渲染
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            // 创建地图视图
                            MapView mapView = Bukkit.createMap(player.getWorld());
                            mapView.getRenderers().clear();
                            mapView.addRenderer(new QRCodeMapRenderer(qrImage));

                            // 创建地图物品
                            ItemStack mapItem = new ItemStack(Material.MAP, 1, mapView.getId());
                            MapMeta meta = (MapMeta) mapItem.getItemMeta();
                            meta.setDisplayName(ChatColor.GOLD + "支付二维码地图");
                            mapItem.setItemMeta(meta);

                            // 放入玩家物品栏第三槽位（索引为2）

                            player.getInventory().setItem(2, mapItem);

                            player.sendMessage(ChatColor.GREEN + "支付二维码地图已放入您的物品栏第三格！杂鱼快去扫描支付吧~");

                            player.sendMessage(ChatColor.YELLOW + "请扫描地图上的二维码完成支付，杂鱼要快点哦~");

                            player.sendMessage(ChatColor.GOLD + "支付完成后，购买的权益将自动激活，杂鱼真棒~");



                            // 记录活跃二维码

                            activeMaps.put(player, cleanUrl);
                        } catch (Exception e) {
                            player.sendMessage(ChatColor.RED + "创建二维码地图时出现错误了呢，杂鱼不要灰心，再试试吧: " + e.getMessage());
                            e.printStackTrace();
                            // 如果创建地图失败，发送链接消息
                            player.sendMessage(ChatColor.GREEN + "支付二维码已生成啦，杂鱼！");
                            player.sendMessage(ChatColor.YELLOW + "请访问以下链接扫描二维码完成支付，杂鱼要快点哦~");
                            player.sendMessage(ChatColor.AQUA + cleanUrl);
                            player.sendMessage(ChatColor.GOLD + "支付完成后，购买的权益将自动激活，杂鱼真棒~");

                            

                            // 记录活跃二维码

                            activeMaps.put(player, cleanUrl);
                        }
                    });
                } catch (Exception e) {
                    // 在主线程中发送错误消息
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(ChatColor.RED + "加载支付二维码时出现错误了呢，杂鱼不要灰心，再试试吧: " + e.getMessage());
                        e.printStackTrace();
                        
                        // 发送链接消息作为备选方案

                        player.sendMessage(ChatColor.GREEN + "支付二维码已生成啦，杂鱼！");

                        player.sendMessage(ChatColor.YELLOW + "请访问以下链接扫描二维码完成支付，杂鱼要快点哦~");

                        player.sendMessage(ChatColor.AQUA + cleanUrl);

                        player.sendMessage(ChatColor.GOLD + "支付完成后，购买的权益将自动激活，杂鱼真棒~");

                        

                        // 记录活跃二维码

                        activeMaps.put(player, cleanUrl);
                    });
                }
            });
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "生成支付二维码时出现错误了呢，杂鱼不要灰心，再试试吧: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取玩家的活跃地图
     */
    public static String getPlayerActiveQRCode(Player player) {
        return activeMaps.get(player);
    }

    /**
     * 移除玩家的活跃地图
     */
    public static String removePlayerActiveQRCode(Player player) {
        return activeMaps.remove(player);
    }
}