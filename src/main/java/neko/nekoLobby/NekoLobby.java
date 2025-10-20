package neko.nekoLobby;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.World;

public final class NekoLobby extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby] 插件已启动!");
        getServer().getPluginManager().registerEvents(this, this);
        
        // 锁定所有世界的时间为白天
        lockTimeToDay();
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
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("spawn")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("set")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "只有玩家可以设置出生点!");
                    return true;
                }
                
                Player player = (Player) sender;
                
                // 检查权限
                if (!player.hasPermission("nekospawn.setspawn")) {
                    player.sendMessage(ChatColor.RED + "你没有权限设置出生点!");
                    return true;
                }
                
                Location loc = player.getLocation();
                
                // 保存位置和朝向到配置文件
                getConfig().set("spawn.world", loc.getWorld().getName());
                getConfig().set("spawn.x", loc.getX());
                getConfig().set("spawn.y", loc.getY());
                getConfig().set("spawn.z", loc.getZ());
                getConfig().set("spawn.yaw", loc.getYaw());
                getConfig().set("spawn.pitch", loc.getPitch());
                saveConfig();
                
                player.sendMessage(ChatColor.GREEN + "出生点已设置!");
                return true;
            }
        }
        return false;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FileConfiguration config = getConfig();
        
        // 检查是否设置了出生点
        if (config.contains("spawn.world")) {
            String worldName = config.getString("spawn.world");
            double x = config.getDouble("spawn.x");
            double y = config.getDouble("spawn.y");
            double z = config.getDouble("spawn.z");
            float yaw = (float) config.getDouble("spawn.yaw");
            float pitch = (float) config.getDouble("spawn.pitch");
            
            Location spawnLocation = new Location(getServer().getWorld(worldName), x, y, z, yaw, pitch);
            player.teleport(spawnLocation);
        }
    }
    
    private void lockTimeToDay() {
        for (World world : getServer().getWorlds()) {
            world.setGameRuleValue("doDaylightCycle", "false");
            world.setTime(6000); // 6000 ticks = 中午 (白天)
        }
    }
}
