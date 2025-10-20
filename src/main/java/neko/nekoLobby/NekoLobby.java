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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.World;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public final class NekoLobby extends JavaPlugin implements Listener {
    // 存储玩家双击空格时间的Map
    private Map<Player, Long> lastSpacePress = new HashMap<>();
    // 存储隐藏玩家的集合
    private Set<Player> hiddenPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby] 插件已启动!");
        getServer().getPluginManager().registerEvents(this, this);
        lockTimeToDay();
    }

    @Override
    public void onDisable() {
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 插件已关闭!");
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() == EntityType.PLAYER || entity instanceof Item) return;
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
                if (!player.hasPermission("nekospawn.setspawn")) {
                    player.sendMessage(ChatColor.RED + "你没有权限设置出生点!");
                    return true;
                }
                Location loc = player.getLocation();
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

        // 给有权限的玩家开启飞行能力
        if (player.hasPermission("nekospawn.fly")) {
            player.setAllowFlight(true);
        }

        // 移除饥饿值
        player.setFoodLevel(20);
        player.setSaturation(20.0f);

        PlayerInventory inv = player.getInventory();
        inv.clear();

        // 指南针
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta cMeta = compass.getItemMeta();
        cMeta.setDisplayName(ChatColor.GREEN + "游戏菜单");
        cMeta.setLore(Collections.singletonList(ChatColor.GRAY + "右键打开游戏菜单"));
        compass.setItemMeta(cMeta);
        inv.setItem(0, compass);

        // 玩家头颅（兼容 1.12）
        Material skullMat = Material.matchMaterial("SKULL_ITEM");
        if (skullMat != null) {
            ItemStack head = new ItemStack(skullMat, 1, (short) 3);
            SkullMeta hMeta = (SkullMeta) head.getItemMeta();
            hMeta.setOwner(player.getName());
            hMeta.setDisplayName(ChatColor.BLUE + "个人档案");
            hMeta.setLore(Collections.singletonList(ChatColor.GRAY + "右键查看个人信息"));
            head.setItemMeta(hMeta);
            inv.setItem(1, head);
        }
        
        // 黄绿色染料（隐身功能）
        Material dyeMat = Material.matchMaterial("INK_SACK");
        if (dyeMat != null) {
            ItemStack dye = new ItemStack(dyeMat, 1, (short) 10); // 黄绿色染料
            ItemMeta dyeMeta = dye.getItemMeta();
            dyeMeta.setDisplayName(ChatColor.GREEN + "隐藏玩家");
            dyeMeta.setLore(Collections.singletonList(ChatColor.GRAY + "右键切换玩家显示/隐藏"));
            dye.setItemMeta(dyeMeta);
            inv.setItem(7, dye);
        }

        event.setJoinMessage(null);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (!p.hasPermission("nekospawn.build")) e.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        ItemStack item = e.getItemInHand(); // ✅ 修复未定义的 item
        if (!player.hasPermission("nekospawn.build")) {
            e.setCancelled(true);
            player.sendMessage(ChatColor.RED + "你没有权限放置方块!");
            return;
        }
        // 防止放置特殊物品
        if (item != null) {
            Material mat = item.getType();
            if (mat == Material.COMPASS || (mat == Material.matchMaterial("SKULL_ITEM") && item.getDurability() == 3)) {
                e.setCancelled(true);
                player.sendMessage(ChatColor.RED + "此物品不能被放置!");
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (!player.hasPermission("nekospawn.build")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerUseItems(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = e.getItem();
        
        // 处理所有右键点击事件（包括对空气点击）
        if (e.getAction().name().contains("RIGHT")) {
            // 检查是否为空气点击，如果是则获取手中物品
            if (item == null) {
                // 获取玩家当前选中的物品
                item = p.getInventory().getItemInHand();
                // 如果仍然为空则返回
                if (item == null) return;
            }

            if (item.getType() == Material.COMPASS && p.getInventory().getHeldItemSlot() == 0) {
                // 让客户端解析指令而不是服务端执行
                p.chat("/menu");
                e.setCancelled(true);
                return;
            }

            Material skull = Material.matchMaterial("SKULL_ITEM");
            // 允许对着空气右键使用个人档案
            if (skull != null && item.getType() == skull && item.getDurability() == 3 && p.getInventory().getHeldItemSlot() == 1) {
                openPlayerProfileGUI(p);
                e.setCancelled(true);
                return;
            }
            
            // 处理隐身功能染料
            Material dyeMat = Material.matchMaterial("INK_SACK");
            if (dyeMat != null && item.getType() == dyeMat && 
                (item.getDurability() == 10 || item.getDurability() == 8) && 
                p.getInventory().getHeldItemSlot() == 7) {
                // 移除权限检查，所有人都可以使用
                togglePlayerVisibility(p, item);
                e.setCancelled(true);
            }
        }
    }

    private void openPlayerProfileGUI(Player p) {
        p.sendMessage(ChatColor.GREEN + "个人档案功能正在开发中...");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        // 清除玩家数据
        lastSpacePress.remove(player);
        hiddenPlayers.remove(player);
        e.setQuitMessage(null);
    }
    
    // 切换玩家可见性
    private void togglePlayerVisibility(Player player, ItemStack dye) {
        // 切换玩家隐藏状态
        if (hiddenPlayers.contains(player)) {
            // 显示所有玩家
            for (Player onlinePlayer : getServer().getOnlinePlayers()) {
                player.showPlayer(onlinePlayer);
            }
            hiddenPlayers.remove(player);
            
            // 将染料变为黄绿色
            dye.setDurability((short) 10);
            ItemMeta meta = dye.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + "隐身开关");
            meta.setLore(Collections.singletonList(ChatColor.GRAY + "右键切换玩家显示/隐藏"));
            dye.setItemMeta(meta);
            
            player.sendMessage(ChatColor.GREEN + "玩家已显示");
        } else {
            // 隐藏所有玩家
            for (Player onlinePlayer : getServer().getOnlinePlayers()) {
                if (!onlinePlayer.equals(player)) { // 不隐藏自己
                    player.hidePlayer(onlinePlayer);
                }
            }
            hiddenPlayers.add(player);
            
            // 将染料变为灰色
            dye.setDurability((short) 8);
            ItemMeta meta = dye.getItemMeta();
            meta.setDisplayName(ChatColor.GRAY + "隐身开关");
            meta.setLore(Collections.singletonList(ChatColor.GRAY + "右键切换玩家显示/隐藏"));
            dye.setItemMeta(meta);
            
            player.sendMessage(ChatColor.GRAY + "玩家已隐藏");
        }
        
        // 更新玩家手中的物品
        player.getInventory().setItem(7, dye);
    }
    
    // 移除摔落伤害
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            // 只移除摔落伤害
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                event.setCancelled(true);
            }
        }
    }
    
    // 防止饥饿值下降
    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        // 保持食物等级为最大值
        if (player.getFoodLevel() < 20) {
            player.setFoodLevel(20);
        }
    }
    
    // 处理玩家切换飞行状态
    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        // 检查玩家是否有飞行权限
        if (!player.hasPermission("nekospawn.fly")) {
            event.setCancelled(true);
            player.setFlying(false);
            //player.sendMessage(ChatColor.RED + "你没有权限使用飞行功能!");
        }
    }
    
    // 处理玩家双击空格飞行
    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        
        // 检查玩家是否有飞行权限
        if (!player.hasPermission("nekospawn.fly")) {
            return;
        }
        
        // 只在玩家按住空格键时触发（潜行）
        if (!event.isSneaking()) {
            return;
        }
        
        // 检查玩家是否在地面上
        if (!player.isOnGround()) {
            return;
        }
        
        // 获取上次按下空格的时间
        long lastPress = lastSpacePress.getOrDefault(player, 0L);
        long currentTime = System.currentTimeMillis();
        
        // 如果在500毫秒内双击空格
        if (currentTime - lastPress < 500) {
            // 切换飞行状态
            if (player.getAllowFlight()) {
                player.setFlying(!player.isFlying());
                if (player.isFlying()) {
                    player.sendMessage(ChatColor.GREEN + "飞行已开启!");
                } else {
                    player.sendMessage(ChatColor.RED + "飞行已关闭!");
                }
            }
            // 重置时间
            lastSpacePress.remove(player);
        } else {
            // 记录按下空格的时间
            lastSpacePress.put(player, currentTime);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        e.setCancelled(true);
    }

    private void lockTimeToDay() {
        for (World w : getServer().getWorlds()) {
            w.setGameRuleValue("doDaylightCycle", "false");
            w.setGameRuleValue("doWeatherCycle", "false");
            w.setTime(6000);
            w.setStorm(false);
            w.setThundering(false);
        }
    }
}
