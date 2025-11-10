package neko.nekoLobby;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class QRCodeMapRenderer {
    
    private static final Map<Player, String> activeMaps = new HashMap<>();
    
    /**
     * 显示支付二维码链接
     */
    public static void showQRCodeOnMap(Player player, String qrCodeUrl) {
        try {
            // 简单地发送消息，提示用户扫描二维码
            player.sendMessage(ChatColor.GREEN + "支付二维码已生成！");
            player.sendMessage(ChatColor.YELLOW + "请访问以下链接扫描二维码完成支付：");
            player.sendMessage(ChatColor.AQUA + qrCodeUrl);
            player.sendMessage(ChatColor.GOLD + "支付完成后，请使用 /vippay confirm 命令激活VIP权限。");
            
            // 记录活跃二维码
            activeMaps.put(player, qrCodeUrl);
            
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "生成支付二维码时出现错误: " + e.getMessage());
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
    public static void removePlayerActiveQRCode(Player player) {
        activeMaps.remove(player);
    }
}