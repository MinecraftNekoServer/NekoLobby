package neko.nekoLobby;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.ChatColor;

public final class NekoLobby extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby] 插件已启动!");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 插件已关闭!");
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        EntityType type = entity.getType();
        
        // 允许玩家和掉落物生成
        if (type == EntityType.PLAYER || entity instanceof Item) {
            return;
        }
        
        // 取消其他所有实体的生成
        event.setCancelled(true);
    }
}
