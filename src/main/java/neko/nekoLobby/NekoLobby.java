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
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Collections;

import java.util.HashMap;

import java.util.Map;

import java.util.HashSet;

import java.util.Set;

import org.bukkit.Bukkit;

import org.bukkit.inventory.Inventory;

import java.util.ArrayList;

import java.util.List;

import java.sql.Connection;

import java.sql.DriverManager;

import java.sql.PreparedStatement;

import java.sql.ResultSet;

import java.sql.SQLException;

public final class NekoLobby extends JavaPlugin implements Listener {
    // 存储玩家双击空格时间的Map

    private Map<Player, Long> lastSpacePress = new HashMap<>();

    // 存储隐藏玩家的集合

    private Set<Player> hiddenPlayers = new HashSet<>();

    // 存储玩家上一个位置的Map

    private Map<Player, Location> lastLocation = new HashMap<>();

    // 存储玩家隐身功能冷却时间的Map

    private Map<Player, Long> invisibilityCooldown = new HashMap<>();

    // 存储玩家加入时间的Map

    private Map<Player, Long> playerJoinTime = new HashMap<>();

    // 存储玩家总游戏时间的Map

    private Map<Player, Long> playerTotalPlayTime = new HashMap<>();

    // 数据库连接

    private Connection authmeConnection;

    private Connection levelConnection;

    private Connection bedwarsConnection;

    private Connection thepitConnection;



    @Override

    public void onEnable() {
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby] 插件已启动!");
        getServer().getPluginManager().registerEvents(this, this);
        lockTimeToDay();
        saveDefaultConfig(); // 保存默认配置文件
        
        // 初始化数据库连接
        initializeDatabaseConnections();
    }

    @Override
    public void onDisable() {
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 插件已关闭!");
        
        // 关闭数据库连接
        closeDatabaseConnections();
    }

    /**
     * 初始化数据库连接
     */
    private void initializeDatabaseConnections() {
        try {
            // 加载MySQL驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // 获取配置
            FileConfiguration config = getConfig();
            
            // 获取通用数据库连接信息
            String host = config.getString("database.host", "localhost");
            int port = config.getInt("database.port", 3306);
            String username = config.getString("database.username", "root");
            String password = config.getString("database.password", "password");
            
            // 初始化Authme数据库连接（硬编码数据库名）
            String authmeUrl = "jdbc:mysql://" + host + ":" + port + "/authme" + 
                              "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            authmeConnection = DriverManager.getConnection(authmeUrl, username, password);
            
            // 初始化Level数据库连接（硬编码数据库名）
            String levelUrl = "jdbc:mysql://" + host + ":" + port + "/neko_level" + 
                             "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            levelConnection = DriverManager.getConnection(levelUrl, username, password);
            
            // 初始化Bedwars数据库连接（硬编码数据库名）
            String bedwarsUrl = "jdbc:mysql://" + host + ":" + port + "/nekobedwars" + 
                               "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            bedwarsConnection = DriverManager.getConnection(bedwarsUrl, username, password);
            
            // 初始化Thypit数据库连接（硬编码数据库名）
            String thepitUrl = "jdbc:mysql://" + host + ":" + port + "/thepit" + 
                              "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            thepitConnection = DriverManager.getConnection(thepitUrl, username, password);
            
            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby] 数据库连接初始化成功!");
        } catch (Exception e) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 数据库连接初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 关闭数据库连接
     */
    private void closeDatabaseConnections() {
        try {
            if (authmeConnection != null && !authmeConnection.isClosed()) {
                authmeConnection.close();
            }
            if (levelConnection != null && !levelConnection.isClosed()) {
                levelConnection.close();
            }
            if (bedwarsConnection != null && !bedwarsConnection.isClosed()) {
                bedwarsConnection.close();
            }
            if (thepitConnection != null && !thepitConnection.isClosed()) {
                thepitConnection.close();
            }
            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby] 数据库连接已关闭!");

        } catch (SQLException e) {

            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 关闭数据库连接时出错: " + e.getMessage());

            e.printStackTrace();

        }

    }

    

    /**

     * 从Authme表获取用户基本信息

     */

    private Map<String, Object> getPlayerAuthInfo(String playerName) {

        Map<String, Object> authInfo = new HashMap<>();

        if (authmeConnection == null) return authInfo;

        

        try {

            // 使用大小写不敏感的查询

            String query = "SELECT username, lastlogin, regdate, email FROM authme WHERE LOWER(username) = LOWER(?)";

            PreparedStatement stmt = authmeConnection.prepareStatement(query);

            stmt.setString(1, playerName);

            ResultSet rs = stmt.executeQuery();

            

            if (rs.next()) {

                authInfo.put("username", rs.getString("username"));

                authInfo.put("lastlogin", rs.getLong("lastlogin"));

                authInfo.put("regdate", rs.getLong("regdate"));

                authInfo.put("email", rs.getString("email"));

            }

            

            rs.close();

            stmt.close();

        } catch (SQLException e) {

            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 查询Authme数据时出错: " + e.getMessage());

        }

        

        return authInfo;

    }

    

    /**

     * 从player_level表获取等级经验信息

     */

    private Map<String, Object> getPlayerLevelInfo(String playerName) {

        Map<String, Object> levelInfo = new HashMap<>();

        if (levelConnection == null) return levelInfo;

        

        try {

            String query = "SELECT name, level, experience FROM player_level WHERE name = ?";

            PreparedStatement stmt = levelConnection.prepareStatement(query);

            stmt.setString(1, playerName);

            ResultSet rs = stmt.executeQuery();

            

            if (rs.next()) {

                levelInfo.put("name", rs.getString("name"));

                levelInfo.put("level", rs.getInt("level"));

                levelInfo.put("experience", rs.getInt("experience"));

            }

            

            rs.close();

            stmt.close();

        } catch (SQLException e) {

            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 查询等级数据时出错: " + e.getMessage());

        }

        

        return levelInfo;

    }

    

    /**

     * 从bw_stats_players表获取Bedwars统计数据

     */

    private Map<String, Object> getPlayerBedwarsStats(String playerName) {

        Map<String, Object> bedwarsStats = new HashMap<>();

        if (bedwarsConnection == null) return bedwarsStats;

        

        try {

            String query = "SELECT name, kills, wins, scord, loses, deaths FROM bw_stats_players WHERE name = ?";

            PreparedStatement stmt = bedwarsConnection.prepareStatement(query);

            stmt.setString(1, playerName);

            ResultSet rs = stmt.executeQuery();

            

            if (rs.next()) {

                bedwarsStats.put("name", rs.getString("name"));

                bedwarsStats.put("kills", rs.getInt("kills"));

                bedwarsStats.put("wins", rs.getInt("wins"));

                bedwarsStats.put("scord", rs.getInt("scord"));

                bedwarsStats.put("loses", rs.getInt("loses"));

                bedwarsStats.put("deaths", rs.getInt("deaths"));

            }

            

            rs.close();

            stmt.close();

        } catch (SQLException e) {

            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 查询Bedwars数据时出错: " + e.getMessage());

        }

        

        return bedwarsStats;

    }

    

    /**

     * 从天坑乱斗数据库获取统计数据

     */

    private Map<String, Object> getPlayerThypitStats(String playerName) {
        Map<String, Object> thepitStats = new HashMap<>();
        if (thepitConnection == null) return thepitStats;
        
        try {
            // 查询ThePitStats表获取统计数据
            String query = "SELECT KILLS, DEATHS, ASSISTS, DAMAGE_DEALT, DAMAGE_TAKEN FROM ThePitStats WHERE Player = ?";
            PreparedStatement stmt = thepitConnection.prepareStatement(query);
            stmt.setString(1, playerName);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                thepitStats.put("kills", rs.getInt("KILLS"));
                thepitStats.put("deaths", rs.getInt("DEATHS"));
                thepitStats.put("assists", rs.getInt("ASSISTS"));
                thepitStats.put("damage_dealt", rs.getInt("DAMAGE_DEALT"));
                thepitStats.put("damage_taken", rs.getInt("DAMAGE_TAKEN"));
            } else {
                // 如果没有找到记录，设置默认值
                thepitStats.put("kills", 0);
                thepitStats.put("deaths", 0);
                thepitStats.put("assists", 0);
                thepitStats.put("damage_dealt", 0);
                thepitStats.put("damage_taken", 0);
            }
            
            rs.close();
            stmt.close();
            
            // 查询ThePitProfiles表获取等级信息
            String profileQuery = "SELECT Level FROM ThePitProfiles WHERE Player = ?";
            PreparedStatement profileStmt = thepitConnection.prepareStatement(profileQuery);
            profileStmt.setString(1, playerName);
            ResultSet profileRs = profileStmt.executeQuery();
            
            if (profileRs.next()) {
                thepitStats.put("level", profileRs.getInt("Level"));
            } else {
                thepitStats.put("level", 1);
            }
            
            profileRs.close();
            profileStmt.close();
        } catch (Exception e) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 查询天坑乱斗数据时出错: " + e.getMessage());
        }
        
        return thepitStats;
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
        } else if (command.getName().equalsIgnoreCase("setrange")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "只有玩家可以设置活动范围!");
                return true;
            }
            
            Player player = (Player) sender;
            if (!player.hasPermission("nekospawn.setrange")) {
                player.sendMessage(ChatColor.RED + "你没有权限设置活动范围!");
                return true;
            }
            
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "用法: /setrange <point1|point2>");
                return true;
            }
            
            Location loc = player.getLocation();
            String point = args[0].toLowerCase();
            
            if (point.equals("point1") || point.equals("point2")) {
                getConfig().set("activity-range." + point + ".x", loc.getX());
                getConfig().set("activity-range." + point + ".z", loc.getZ());
                saveConfig();
                player.sendMessage(ChatColor.GREEN + "活动范围 " + point + " 已设置为当前坐标: X=" + loc.getX() + ", Z=" + loc.getZ());
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "无效的点名称。请使用 point1 或 point2");
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
        
        // 记录玩家加入时间
        playerJoinTime.put(player, System.currentTimeMillis());
        // 初始化玩家总游戏时间（如果之前没有记录）
        if (!playerTotalPlayTime.containsKey(player)) {
            playerTotalPlayTime.put(player, 0L);
        }
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

            // 只取消与方块相关的交互，避免影响物品使用

            if (e.getClickedBlock() != null) {

                e.setCancelled(true);

            }

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

                

                // 检查冷却时间，防止快速连续触发

                long currentTime = System.currentTimeMillis();

                Long lastUse = invisibilityCooldown.get(p);

                if (lastUse != null && currentTime - lastUse < 500) { // 500毫秒冷却

                    // 在冷却期间，直接取消事件

                    e.setCancelled(true);

                    return;

                }

                

                // 移除权限检查，所有人都可以使用

                togglePlayerVisibility(p, item);

                e.setCancelled(true);

                

                // 设置冷却时间

                invisibilityCooldown.put(p, currentTime);

            }

        }

    }

    private void openPlayerProfileGUI(Player p) {
        // 创建个人档案GUI
        Inventory profileGUI = Bukkit.createInventory(null, 54, ChatColor.BLUE + "个人档案");
        
        // 获取玩家名称
        String playerName = p.getName();
        
        // 从数据库获取玩家信息
        Map<String, Object> authInfo = getPlayerAuthInfo(playerName);
        Map<String, Object> levelInfo = getPlayerLevelInfo(playerName);
        Map<String, Object> bedwarsStats = getPlayerBedwarsStats(playerName);
        Map<String, Object> thepitStats = getPlayerThypitStats(playerName);
        
        // 计算玩家游戏时间
        long totalPlayTime = playerTotalPlayTime.getOrDefault(p, 0L);
        Long joinTime = playerJoinTime.get(p);
        if (joinTime != null) {
            // 加上当前会话的时间
            totalPlayTime += System.currentTimeMillis() - joinTime;
        }
        
        long totalHours = totalPlayTime / (1000 * 60 * 60);
        long totalMinutes = (totalPlayTime / (1000 * 60)) % 60;
        
        // 获取Authme信息
        String email = (String) authInfo.getOrDefault("email", "未设置");
        long lastLogin = (Long) authInfo.getOrDefault("lastlogin", 0L);
        long regDate = (Long) authInfo.getOrDefault("regdate", 0L);
        
        // 格式化时间
        String lastLoginStr = lastLogin > 0 ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(lastLogin * 1000)) : "未知";
        String regDateStr = regDate > 0 ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(regDate * 1000)) : "未知";
        
        // 获取等级信息
        int level = (Integer) levelInfo.getOrDefault("level", 1);
        int experience = (Integer) levelInfo.getOrDefault("experience", 0);
        
        // 获取Bedwars统计信息
        int kills = (Integer) bedwarsStats.getOrDefault("kills", 0);
        int wins = (Integer) bedwarsStats.getOrDefault("wins", 0);
        int score = (Integer) bedwarsStats.getOrDefault("scord", 0);
        int loses = (Integer) bedwarsStats.getOrDefault("loses", 0);
        int deaths = (Integer) bedwarsStats.getOrDefault("deaths", 0);
        
        // 计算Bedwars K/D比率
        double kdRatio = deaths > 0 ? (double) kills / deaths : kills;
        // 计算Bedwars W/L比率
        double wlRatio = loses > 0 ? (double) wins / loses : wins;
        
        // 玩家头像
        ItemStack playerHead = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta headMeta = (SkullMeta) playerHead.getItemMeta();
        headMeta.setOwner(p.getName());
        headMeta.setDisplayName(ChatColor.YELLOW + p.getName());
        
        // 添加玩家信息到Lore
        List<String> headLore = new ArrayList<>();
        headLore.add(ChatColor.GRAY + "等级: " + ChatColor.GREEN + level);
        headLore.add(ChatColor.GRAY + "在线时间: " + ChatColor.GREEN + totalHours + "小时 " + totalMinutes + "分钟");
        headLore.add(ChatColor.GRAY + "最后登录: " + ChatColor.GREEN + lastLoginStr);
        headLore.add("");
        headLore.add(ChatColor.GOLD + "▶ 点击查看详细信息");
        headMeta.setLore(headLore);
        playerHead.setItemMeta(headMeta);
        
        // 装饰性玻璃板 - 蓝色边框
        ItemStack blueGlassPane = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 11);
        ItemMeta blueGlassMeta = blueGlassPane.getItemMeta();
        blueGlassMeta.setDisplayName(" ");
        blueGlassPane.setItemMeta(blueGlassMeta);
        
        // 装饰性玻璃板 - 灰色背景
        ItemStack grayGlassPane = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta grayGlassMeta = grayGlassPane.getItemMeta();
        grayGlassMeta.setDisplayName(" ");
        grayGlassPane.setItemMeta(grayGlassMeta);
        
        // 填充背景
        for (int i = 0; i < 54; i++) {
            profileGUI.setItem(i, grayGlassPane.clone());
        }
        
        // 设置蓝色边框
        int[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53};
        for (int slot : borderSlots) {
            profileGUI.setItem(slot, blueGlassPane.clone());
        }
        
        // 设置玩家头像在中心位置
        profileGUI.setItem(13, playerHead);
        
        // 玩家统计信息
        ItemStack statsItem = new ItemStack(Material.BOOK);
        ItemMeta statsMeta = statsItem.getItemMeta();
        statsMeta.setDisplayName(ChatColor.GREEN + "统计信息");
        List<String> statsLore = new ArrayList<>();
        statsLore.add(ChatColor.GRAY + "注册时间: " + ChatColor.YELLOW + regDateStr);
        statsLore.add(ChatColor.GRAY + "邮箱: " + ChatColor.YELLOW + email);
        statsLore.add(ChatColor.GRAY + "游戏时间: " + ChatColor.YELLOW + totalHours + "小时 " + totalMinutes + "分钟");
        statsLore.add("");
        statsLore.add(ChatColor.GOLD + "总计游戏时间: " + ChatColor.YELLOW + totalHours + "小时");
        statsMeta.setLore(statsLore);
        statsItem.setItemMeta(statsMeta);
        profileGUI.setItem(31, statsItem);
        
        // 等级信息
        ItemStack levelItem = new ItemStack(Material.EXP_BOTTLE);
        ItemMeta levelMeta = levelItem.getItemMeta();
        levelMeta.setDisplayName(ChatColor.AQUA + "等级信息");
        List<String> levelLore = new ArrayList<>();
        levelLore.add(ChatColor.GRAY + "当前等级: " + ChatColor.GREEN + level);
        levelLore.add(ChatColor.GRAY + "经验值: " + ChatColor.GREEN + experience);
        levelMeta.setLore(levelLore);
        levelItem.setItemMeta(levelMeta);
        profileGUI.setItem(29, levelItem);
        
        // Bedwars统计
        ItemStack bedwarsItem = new ItemStack(Material.BED);
        ItemMeta bedwarsMeta = bedwarsItem.getItemMeta();
        bedwarsMeta.setDisplayName(ChatColor.RED + "Bedwars统计");
        List<String> bedwarsLore = new ArrayList<>();
        bedwarsLore.add(ChatColor.GRAY + "击杀数: " + ChatColor.GREEN + kills);
        bedwarsLore.add(ChatColor.GRAY + "死亡数: " + ChatColor.GREEN + deaths);
        bedwarsLore.add(ChatColor.GRAY + "K/D比率: " + ChatColor.GREEN + String.format("%.2f", kdRatio));
        bedwarsLore.add(ChatColor.GRAY + "胜利数: " + ChatColor.GREEN + wins);
        bedwarsLore.add(ChatColor.GRAY + "失败数: " + ChatColor.GREEN + loses);
        bedwarsLore.add(ChatColor.GRAY + "W/L比率: " + ChatColor.GREEN + String.format("%.2f", wlRatio));
        bedwarsLore.add(ChatColor.GRAY + "总分数: " + ChatColor.GREEN + score);
        bedwarsMeta.setLore(bedwarsLore);
        bedwarsItem.setItemMeta(bedwarsMeta);
        profileGUI.setItem(33, bedwarsItem);
        
        // 天坑乱斗统计
        int pitKills = (Integer) thepitStats.getOrDefault("kills", 0);
        int pitDeaths = (Integer) thepitStats.getOrDefault("deaths", 0);
        int pitAssists = (Integer) thepitStats.getOrDefault("assists", 0);
        int pitDamageDealt = (Integer) thepitStats.getOrDefault("damage_dealt", 0);
        int pitDamageTaken = (Integer) thepitStats.getOrDefault("damage_taken", 0);
        int pitLevel = (Integer) thepitStats.getOrDefault("level", 1);
        
        double pitKdRatio = pitDeaths > 0 ? (double) pitKills / pitDeaths : pitKills;
        
        ItemStack thepitItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta thepitMeta = thepitItem.getItemMeta();
        thepitMeta.setDisplayName(ChatColor.GOLD + "天坑乱斗统计");
        List<String> thepitLore = new ArrayList<>();
        thepitLore.add(ChatColor.GRAY + "等级: " + ChatColor.GREEN + pitLevel);
        thepitLore.add(ChatColor.GRAY + "击杀数: " + ChatColor.GREEN + pitKills);
        thepitLore.add(ChatColor.GRAY + "死亡数: " + ChatColor.GREEN + pitDeaths);
        thepitLore.add(ChatColor.GRAY + "助攻数: " + ChatColor.GREEN + pitAssists);
        thepitLore.add(ChatColor.GRAY + "K/D比率: " + ChatColor.GREEN + String.format("%.2f", pitKdRatio));
        thepitLore.add(ChatColor.GRAY + "造成伤害: " + ChatColor.GREEN + pitDamageDealt);
        thepitLore.add(ChatColor.GRAY + "受到伤害: " + ChatColor.GREEN + pitDamageTaken);
        thepitMeta.setLore(thepitLore);
        thepitItem.setItemMeta(thepitMeta);
        profileGUI.setItem(41, thepitItem);
        
        // 打开GUI
        p.openInventory(profileGUI);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        // 清除玩家数据
        lastSpacePress.remove(player);
        hiddenPlayers.remove(player);
        lastLocation.remove(player); // 清除玩家上一个位置数据
        invisibilityCooldown.remove(player); // 清除冷却时间数据
        
        // 更新玩家总游戏时间
        Long joinTime = playerJoinTime.get(player);
        if (joinTime != null) {
            long playTime = System.currentTimeMillis() - joinTime;
            long totalPlayTime = playerTotalPlayTime.getOrDefault(player, 0L) + playTime;
            playerTotalPlayTime.put(player, totalPlayTime);
            playerJoinTime.remove(player); // 清除加入时间数据
        }
        
        e.setQuitMessage(null);
    }
    
    // 检查玩家是否在活动范围内
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        FileConfiguration config = getConfig();
        
        // 检查Y坐标是否小于等于0，强制拉回出生点
        Location currentLocation = player.getLocation();
        if (currentLocation.getY() <= 0) {
            // 拉回出生点
            if (config.contains("spawn.world")) {
                String worldName = config.getString("spawn.world");
                double spawnX = config.getDouble("spawn.x");
                double spawnY = config.getDouble("spawn.y");
                double spawnZ = config.getDouble("spawn.z");
                float yaw = (float) config.getDouble("spawn.yaw");
                float pitch = (float) config.getDouble("spawn.pitch");
                
                Location spawnLocation = new Location(getServer().getWorld(worldName), spawnX, spawnY, spawnZ, yaw, pitch);
                player.teleport(spawnLocation);
                player.sendMessage(ChatColor.RED + "你不允许在这个范围之外");
            }
            return;
        }
        
        // 获取活动范围的两个点坐标
        double point1X = config.getDouble("activity-range.point1.x", 0);
        double point1Z = config.getDouble("activity-range.point1.z", 0);
        double point2X = config.getDouble("activity-range.point2.x", 0);
        double point2Z = config.getDouble("activity-range.point2.z", 0);
        
        // 检查是否设置了活动范围（默认值为0表示不限制）
        if (point1X != 0 || point1Z != 0 || point2X != 0 || point2Z != 0) {
            // 计算活动范围的边界
            double minX = Math.min(point1X, point2X);
            double maxX = Math.max(point1X, point2X);
            double minZ = Math.min(point1Z, point2Z);
            double maxZ = Math.max(point1Z, point2Z);
            
            double x = currentLocation.getX();
            double z = currentLocation.getZ();
            
            // 检查玩家是否在活动范围内
            if (x < minX || x > maxX || z < minZ || z > maxZ) {
                // 检查是否有上一个位置记录
                if (lastLocation.containsKey(player)) {
                    Location lastLoc = lastLocation.get(player);
                    // 确保上一个位置在活动范围内
                    double lastX = lastLoc.getX();
                    double lastZ = lastLoc.getZ();
                    if (lastX >= minX && lastX <= maxX && lastZ >= minZ && lastZ <= maxZ) {
                        player.teleport(lastLoc);
                        player.sendMessage(ChatColor.RED + "你不允许在这个范围之外");
                    } else {
                        // 如果上一个位置也不在范围内，则拉回出生点
                        if (config.contains("spawn.world")) {
                            String worldName = config.getString("spawn.world");
                            double spawnX = config.getDouble("spawn.x");
                            double spawnY = config.getDouble("spawn.y");
                            double spawnZ = config.getDouble("spawn.z");
                            float yaw = (float) config.getDouble("spawn.yaw");
                            float pitch = (float) config.getDouble("spawn.pitch");
                            
                            Location spawnLocation = new Location(getServer().getWorld(worldName), spawnX, spawnY, spawnZ, yaw, pitch);
                            player.teleport(spawnLocation);
                            player.sendMessage(ChatColor.RED + "你不允许在这个范围之外");
                        }
                    }
                } else {
                    // 没有上一个位置记录，拉回出生点
                    if (config.contains("spawn.world")) {
                        String worldName = config.getString("spawn.world");
                        double spawnX = config.getDouble("spawn.x");
                        double spawnY = config.getDouble("spawn.y");
                        double spawnZ = config.getDouble("spawn.z");
                        float yaw = (float) config.getDouble("spawn.yaw");
                        float pitch = (float) config.getDouble("spawn.pitch");
                        
                        Location spawnLocation = new Location(getServer().getWorld(worldName), spawnX, spawnY, spawnZ, yaw, pitch);
                        player.teleport(spawnLocation);
                        player.sendMessage(ChatColor.RED + "你不允许在这个范围之外");
                    }
                }
            } else {
                // 玩家在活动范围内，更新上一个位置
                lastLocation.put(player, currentLocation);
            }
        }
        // 如果活动范围未设置（都为0），则不限制玩家活动
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

        Player player = e.getPlayer();

        // 如果玩家是创造模式，则允许丢弃物品

        if (player.getGameMode().name().equals("CREATIVE")) {

            return;

        }

        e.setCancelled(true);

    }



    @EventHandler

    public void onInventoryClick(InventoryClickEvent e) {

        Player player = (Player) e.getWhoClicked();

        // 如果玩家是创造模式，则允许移动物品

        if (player.getGameMode().name().equals("CREATIVE")) {

            return;

        }

        // 检查是否是个人档案GUI，如果是则允许交互

        if (e.getInventory().getTitle().equals(ChatColor.BLUE + "个人档案")) {

            // 处理个人档案GUI中的交互

            handleProfileGUIInteraction(e);

            return;

        }

        e.setCancelled(true);

    }



    private void handleProfileGUIInteraction(InventoryClickEvent e) {

        e.setCancelled(true); // 防止玩家拿取物品

        Player player = (Player) e.getWhoClicked();

        ItemStack clickedItem = e.getCurrentItem();

        

        // 如果点击的是空槽位或装饰性物品，则不处理

        if (clickedItem == null || clickedItem.getType() == Material.AIR || 

            clickedItem.getType() == Material.STAINED_GLASS_PANE) {

            return;

        }

        

        // 如果点击的是玩家头像，则显示更多详细信息

        if (clickedItem.getType() == Material.SKULL_ITEM) {

            player.sendMessage(ChatColor.GREEN + "玩家详细信息:");

            player.sendMessage(ChatColor.GRAY + "  名称: " + ChatColor.YELLOW + player.getName());

            player.sendMessage(ChatColor.GRAY + "  UUID: " + ChatColor.YELLOW + player.getUniqueId());

            player.sendMessage(ChatColor.GRAY + "  等级: " + ChatColor.YELLOW + "1");

            player.closeInventory(); // 关闭GUI

            return;

        }

        

        // 如果点击的是统计信息书本

        if (clickedItem.getType() == Material.BOOK) {

            player.sendMessage(ChatColor.GREEN + "统计信息:");

            player.sendMessage(ChatColor.GRAY + "  游戏时间: " + ChatColor.YELLOW + "0小时");

            player.sendMessage(ChatColor.GRAY + "  在线时间: " + ChatColor.YELLOW + "0分钟");

            player.sendMessage(ChatColor.GRAY + "  加入时间: " + ChatColor.YELLOW + "今天");

            player.closeInventory(); // 关闭GUI

            return;

        }

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
