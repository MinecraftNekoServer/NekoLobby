package neko.nekoLobby;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class QRCodeMapRenderer {
    
    private static final Map<Player, String> activeMaps = new HashMap<>();
    
    /**
     * 显示支付二维码地图
     */
    public static void showQRCodeOnMap(Player player, String qrCodeUrl) {
        try {
            // 处理URL中的转义字符
            String cleanUrl = qrCodeUrl.replace("\\/", "/");
            
            // 创建地图物品并放入玩家物品栏第三槽位（索引为2）
            ItemStack mapItem = createQRCodeMap(cleanUrl);
            if (mapItem != null) {
                // 放入玩家物品栏第三槽位（索引为2）
                player.getInventory().setItem(2, mapItem);
                player.sendMessage(ChatColor.GREEN + "支付二维码地图已放入您的物品栏第三格！");
                player.sendMessage(ChatColor.YELLOW + "请扫描地图上的二维码完成支付。");
                player.sendMessage(ChatColor.GOLD + "支付完成后，请使用 /vippay confirm 命令激活VIP权限。");
            } else {
                // 如果创建地图失败，发送链接消息
                player.sendMessage(ChatColor.GREEN + "支付二维码已生成！");
                player.sendMessage(ChatColor.YELLOW + "请访问以下链接扫描二维码完成支付：");
                player.sendMessage(ChatColor.AQUA + cleanUrl);
                player.sendMessage(ChatColor.GOLD + "支付完成后，请使用 /vippay confirm 命令激活VIP权限。");
            }
            
            // 记录活跃二维码
            activeMaps.put(player, cleanUrl);
            
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "生成支付二维码时出现错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建二维码地图
     */
    private static ItemStack createQRCodeMap(String qrCodeUrl) {
        try {
            // 由于Minecraft 1.12.2的限制，直接渲染图片到地图比较复杂
            // 所以我们创建一个带有二维码URL信息的物品
            // 使用兼容1.12.2的材质
            Material mapMaterial = Material.MAP; // 1.12.2版本使用MAP而不是FILLED_MAP
            ItemStack map = new ItemStack(mapMaterial);
            ItemMeta meta = map.getItemMeta();
            
            // 设置物品名称和描述
            meta.setDisplayName(ChatColor.GOLD + "支付二维码");
            meta.setLore(java.util.Arrays.asList(
                ChatColor.YELLOW + "扫描二维码完成支付",
                ChatColor.GRAY + "二维码链接: " + qrCodeUrl,
                ChatColor.GOLD + "支付完成后使用 /vippay confirm 激活VIP"
            ));
            map.setItemMeta(meta);
            
            return map;
        } catch (Exception e) {
            Bukkit.getLogger().severe("创建二维码地图失败: " + e.getMessage());
            e.printStackTrace();
            return null;
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