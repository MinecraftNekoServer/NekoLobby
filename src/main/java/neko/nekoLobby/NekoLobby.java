package neko.nekoLobby;

import neko.nekoLobby.QRCodeMapRenderer;
import neko.nekoLobby.ZPayUtil;
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
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.regex.Pattern;

// LuckPerms API
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.types.InheritanceNode;

// PlaceholderAPI
import me.clip.placeholderapi.PlaceholderAPI;

// HTTP服务器相关
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

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
    // 数据库连接信息
    private String dbHost;
    private int dbPort;
    private String dbUsername;
    private String dbPassword;
    // LuckPerms API
    private LuckPerms luckPerms;
    private boolean placeholderAPIEnabled;

    // Z-Pay支付工具


    private ZPayUtil zPayUtil;


    private String zPayPid;


    private String zPayKey;


    private String zPayNotifyUrl;


    private int zPayHttpPort; // HTTP服务器端口


    // HTTP服务器

    private HttpServer httpServer;

    private ExecutorService httpExecutor;

    @Override
    public void onEnable() {
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby] 插件已启动!");
        getServer().getPluginManager().registerEvents(this, this);
        lockTimeToDay();
        saveDefaultConfig(); // 保存默认配置文件

        // 初始化数据库连接
        initializeDatabaseConnections();

        // 初始化LuckPerms API
        try {
            luckPerms = LuckPermsProvider.get();
            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby] LuckPerms API 已连接!");
        } catch (Exception e) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 无法连接到 LuckPerms API: " + e.getMessage());
        }

        // 初始化Z-Pay支付配置
        initializeZPayConfig();

        // 检查PlaceholderAPI是否启用
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholderAPIEnabled = true;
            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby] PlaceholderAPI 已连接!");
        } else {
            placeholderAPIEnabled = false;
            getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "[NekoLobby] PlaceholderAPI 未安装或未启用，部分功能可能受限!");
        }

        // 启动HTTP服务器以处理支付回调
        startHttpServer();
    }


    /**
     * 创建Z-Pay支付订单
     */

    private void createZPayOrder(Player player) {

        if (zPayUtil == null) {

            player.sendMessage(ChatColor.RED + "支付系统未初始化，请联系管理员！");

            return;

        }

        // 生成订单号
        String orderNo = zPayUtil.generateOrderNo();
        String playerName = player.getName();
        String subject = "梦幻次元-VIP权益(1个月)";
        String amount = "15.00"; // 15元
        String type = "alipay"; // 默认使用支付宝，也可以是wxpay
        String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "127.0.0.1";
        String param = "player:" + playerName; // 附加参数，包含玩家名称用于后续识别

        // 创建支付订单并获取二维码URL
        String qrCodeUrl = zPayUtil.getPaymentQRCodeUrl(orderNo, subject, amount, type, ip, param);

        if (qrCodeUrl != null && !qrCodeUrl.isEmpty()) {
            // 成功获取二维码URL，显示在地图上
            player.sendMessage(ChatColor.GREEN + "VIP订单创建成功啦。杂鱼！");
            player.sendMessage(ChatColor.YELLOW + "正在生成支付二维码。杂鱼！");
            player.closeInventory(); // 关闭GUI

            // 在单独的线程中处理二维码地图渲染，避免阻塞主线程
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                QRCodeMapRenderer.showQRCodeOnMap(player, qrCodeUrl);
            });
        } else {

            player.sendMessage(ChatColor.RED + "真是杂鱼呢。订单创建失败咯");
        }
    }

    /**
     * 创建Z-Pay支付订单
     */

    private void createZPayOrder(Player player, String type) {

        if (zPayUtil == null) {

            player.sendMessage(ChatColor.RED + "支付系统未初始化，请联系管理员！");

            return;

        }

        // 生成订单号
        String orderNo = zPayUtil.generateOrderNo();
        String playerName = player.getName();
        String subject = "梦幻次元-VIP权益(1个月)";
        String amount = "15.00"; // 15元
        String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "127.0.0.1";
        String param = "player:" + playerName; // 附加参数，包含玩家名称用于后续识别

        // 创建支付订单并获取二维码URL
        String qrCodeUrl = zPayUtil.getPaymentQRCodeUrl(orderNo, subject, amount, type, ip, param);

        if (qrCodeUrl != null && !qrCodeUrl.isEmpty()) {
            // 成功获取二维码URL，显示在地图上
            player.sendMessage(ChatColor.GREEN + "VIP订单创建成功啦，杂鱼！");
            player.sendMessage(ChatColor.YELLOW + "正在生成支付二维码。杂鱼！");
            player.closeInventory(); // 关闭GUI

            // 在单独的线程中处理二维码地图渲染，避免阻塞主线程
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                QRCodeMapRenderer.showQRCodeOnMap(player, qrCodeUrl);
            });
        } else {
            player.sendMessage(ChatColor.RED + "真是杂鱼呢。订单创建失败咯");
            // 添加更多详细错误信息
            String errorDetails = zPayUtil.getLastError();
            if (errorDetails != null && !errorDetails.isEmpty()) {
                player.sendMessage(ChatColor.RED + "错误详情: " + errorDetails);
            }
        }
    }

    @Override

    public void onDisable() {

        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 插件已关闭!");

        // 关闭HTTP服务器
        stopHttpServer();

        // 关闭数据库连接
        closeDatabaseConnections();
    }

    /**
     * 初始化数据库连接信息
     */
    private void initializeDatabaseConnections() {
        try {
            // 加载MySQL驱动
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 获取配置
            FileConfiguration config = getConfig();

            // 保存数据库连接信息
            dbHost = config.getString("database.host", "localhost");
            dbPort = config.getInt("database.port", 3306);
            dbUsername = config.getString("database.username", "root");
            dbPassword = config.getString("database.password", "wcjs123");

            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby] 数据库连接信息初始化成功!");

            // 创建待处理VIP购买表
            createPendingVipPurchasesTable();
        } catch (Exception e) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 数据库连接信息初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建待处理VIP购买表
     */
    private void createPendingVipPurchasesTable() {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = createDatabaseConnection("neko_level");
            String query = "CREATE TABLE IF NOT EXISTS pending_vip_purchases (" +
                    "player_name VARCHAR(50) PRIMARY KEY, " +
                    "purchase_time BIGINT NOT NULL)";
            stmt = conn.prepareStatement(query);
            stmt.executeUpdate();
            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby] 待处理VIP购买表创建成功!");
        } catch (SQLException e) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 创建待处理VIP购买表时出错: " + e.getMessage());
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 关闭数据库资源时出错: " + e.getMessage());
            }
        }
    }

    /**
     * 关闭数据库连接（现已改为按需连接，此方法仅作提示用）
     */
    private void closeDatabaseConnections() {
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby] 数据库连接信息已清理!");
    }

    /**
     * 初始化Z-Pay支付配置
     */

    private void initializeZPayConfig() {

        try {

            FileConfiguration config = getConfig();


            // 读取Z-Pay配置

            zPayPid = config.getString("zpay.pid", "");

            zPayKey = config.getString("zpay.key", "");

            zPayNotifyUrl = config.getString("zpay.notify_url", "");

            zPayHttpPort = config.getInt("zpay.http_port", 8080); // 默认端口8080


            // 检查配置是否完整

            if (zPayPid.isEmpty() || zPayKey.isEmpty() || zPayNotifyUrl.isEmpty()) {

                getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] Z-Pay支付配置不完整，请检查config.yml文件!");

                return;

            }


            // 创建ZPayUtil实例


            zPayUtil = new ZPayUtil(zPayPid, zPayKey, zPayNotifyUrl);

            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby] Z-Pay支付系统初始化成功!");

        } catch (Exception e) {

            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] Z-Pay支付配置初始化失败: " + e.getMessage());

            e.printStackTrace();

        }

    }

    /**
     * 启动HTTP服务器以处理支付回调
     */

    private void startHttpServer() {

        try {

            // 使用配置文件中指定的端口

            int port = zPayHttpPort;


            httpServer = HttpServer.create(new InetSocketAddress(port), 0);

            httpServer.createContext("/zpay/notify", new PaymentNotificationHandler());

            httpExecutor = Executors.newCachedThreadPool();

            httpServer.setExecutor(httpExecutor);

            httpServer.start();

            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby] HTTP服务器已启动，端口: " + port);

        } catch (Exception e) {

            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 启动HTTP服务器失败: " + e.getMessage());

            e.printStackTrace();

        }

    }

    /**
     * 关闭HTTP服务器
     */
    private void stopHttpServer() {
        if (httpServer != null) {
            httpServer.stop(0);
            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby] HTTP服务器已关闭");
        }
        if (httpExecutor != null) {
            httpExecutor.shutdown();
        }
    }

    /**
     * 支付通知处理器
     */
    class PaymentNotificationHandler implements HttpHandler {

        @Override

        public void handle(HttpExchange exchange) throws IOException {

            if ("GET".equals(exchange.getRequestMethod()) || "POST".equals(exchange.getRequestMethod())) {

                // 解析URL参数（GET请求参数在URL中，POST请求也可能有URL参数）

                Map<String, String> params = parseQueryString(exchange.getRequestURI().getRawQuery());


                // 如果是POST请求，也要读取POST数据

                if ("POST".equals(exchange.getRequestMethod())) {

                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(exchange.getRequestBody()));

                    String line;

                    StringBuilder requestBody = new StringBuilder();

                    while ((line = reader.readLine()) != null) {

                        requestBody.append(line);

                    }

                    reader.close();


                    // 解析POST参数并合并到params中

                    Map<String, String> postParams = parseQueryString(requestBody.toString());

                    params.putAll(postParams);

                }


                try {

                    // 验证签名

                    if (zPayUtil.verifySign(new HashMap<>(params))) {

                        // 处理支付成功

                        String tradeNo = params.get("trade_no");

                        String outTradeNo = params.get("out_trade_no");

                        String param = params.get("param");

                        // 根据文档，使用trade_status参数来判断支付状态

                        String status = params.get("trade_status");


                        // 根据文档，只有TRADE_SUCCESS是成功状态


                        if ("TRADE_SUCCESS".equals(status)) {


                            // 提取玩家名称


                            if (param != null && param.startsWith("player:")) {


                                String playerName = param.substring(7); // 去掉"player:"前缀


                                // 获取商品名称来判断是VIP还是MVP购买

                                String subject = params.get("subject");


                                // 设置玩家权限


                                Player player = Bukkit.getPlayer(playerName);


                                if (player != null && player.isOnline()) {


                                    // 在主线程中设置权限


                                    Bukkit.getScheduler().runTask(NekoLobby.this, () -> {


                                        if (subject != null && subject.contains("MVP")) {

                                            // 设置MVP权限

                                            setPlayerMvpGroup(player);


                                            // 发送丰富的支付成功消息

                                            player.sendMessage("");

                                            player.sendMessage(ChatColor.GOLD + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");

                                            player.sendMessage(ChatColor.GREEN + "🎉 支付成功！感谢您的支持！🎉");

                                            player.sendMessage(ChatColor.AQUA + "您的MVP权益已自动激活！");

                                            player.sendMessage(ChatColor.YELLOW + "有效期: 一个月");

                                            player.sendMessage(ChatColor.LIGHT_PURPLE + "享受MVP特权，祝您游戏愉快！");

                                            player.sendMessage(ChatColor.GOLD + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");

                                            player.sendMessage("");


                                            // 向所有在线玩家广播

                                            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {

                                                if (onlinePlayer != null && onlinePlayer.isOnline()) {

                                                    onlinePlayer.sendMessage("");

                                                    onlinePlayer.sendMessage(ChatColor.GOLD + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");

                                                    onlinePlayer.sendMessage(ChatColor.AQUA + "📢 服务器公告");

                                                    onlinePlayer.sendMessage(ChatColor.YELLOW + playerName + " 杂鱼购买了MVP权益！");

                                                    onlinePlayer.sendMessage(ChatColor.GREEN + "感谢支持，祝游戏愉快！");

                                                    onlinePlayer.sendMessage(ChatColor.GOLD + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");

                                                    onlinePlayer.sendMessage("");

                                                }

                                            }


                                            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby] 玩家 " + playerName + " 的MVP权限已自动激活");


                                        } else {

                                            // 设置VIP权限

                                            setPlayerVipGroup(player);


                                            // 发送丰富的支付成功消息

                                            player.sendMessage("");

                                            player.sendMessage(ChatColor.GOLD + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");

                                            player.sendMessage(ChatColor.GREEN + "🎉 支付成功！感谢您的支持！🎉");

                                            player.sendMessage(ChatColor.AQUA + "您的VIP权益已自动激活！");

                                            player.sendMessage(ChatColor.YELLOW + "有效期: 一个月");

                                            player.sendMessage(ChatColor.LIGHT_PURPLE + "享受VIP特权，祝您游戏愉快！");

                                            player.sendMessage(ChatColor.GOLD + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");

                                            player.sendMessage("");


                                            // 向所有在线玩家广播

                                            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {

                                                if (onlinePlayer != null && onlinePlayer.isOnline()) {

                                                    onlinePlayer.sendMessage("");

                                                    onlinePlayer.sendMessage(ChatColor.GOLD + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");

                                                    onlinePlayer.sendMessage(ChatColor.AQUA + "📢 服务器公告");

                                                    onlinePlayer.sendMessage(ChatColor.YELLOW + playerName + " 杂鱼购买了VIP权益！");

                                                    onlinePlayer.sendMessage(ChatColor.GREEN + "感谢支持，祝游戏愉快！");

                                                    onlinePlayer.sendMessage(ChatColor.GOLD + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");

                                                    onlinePlayer.sendMessage("");

                                                }

                                            }


                                            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby] 玩家 " + playerName + " 的VIP权限已自动激活");


                                        }


                                    });


                                } else {


                                    // 玩家不在线，记录到数据库，等待玩家上线时处理

                                    if (subject != null && subject.contains("MVP")) {

                                        handleOfflineMvpPurchase(playerName);

                                    } else {

                                        handleOfflineVipPurchase(playerName);

                                    }


                                }


                            } else {


                                getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "[NekoLobby] 无法解析支付参数: " + param);


                            }


                        } else {


                            // 非支付成功状态，记录日志


                            getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "[NekoLobby] 收到非成功支付状态的回调: " + status + ", 参数: " + params);


                        }


                        // 发送成功响应 - 确保返回纯文本"success"字符串

                        String response = "success";

                        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");

                        exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);

                        OutputStream os = exchange.getResponseBody();

                        os.write(response.getBytes("UTF-8"));

                        os.close();

                    } else {

                        // 验证失败

                        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 支付回调签名验证失败，参数: " + params);

                        String response = "fail";

                        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");

                        exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);

                        OutputStream os = exchange.getResponseBody();

                        os.write(response.getBytes("UTF-8"));

                        os.close();

                    }

                } catch (Exception e) {

                    getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 处理支付回调时出错: " + e.getMessage());

                    e.printStackTrace();

                    String response = "error";

                    exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");

                    exchange.sendResponseHeaders(500, response.getBytes("UTF-8").length);

                    OutputStream os = exchange.getResponseBody();

                    os.write(response.getBytes("UTF-8"));

                    os.close();

                }

            } else {

                // 不支持的方法

                String response = "错误的请求方法！";

                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");

                exchange.sendResponseHeaders(405, response.getBytes("UTF-8").length);

                OutputStream os = exchange.getResponseBody();

                os.write(response.getBytes("UTF-8"));

                os.close();

            }

        }

    }

    /**
     * 解析查询字符串
     */
    private Map<String, String> parseQueryString(String query) {
        Map<String, String> result = new HashMap<>();
        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx > 0) {
                    try {
                        String key = java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                        String value = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                        result.put(key, value);
                    } catch (Exception e) {
                        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 解析查询字符串时出错: " + e.getMessage());
                    }
                } else if (!pair.isEmpty()) {
                    try {
                        String key = java.net.URLDecoder.decode(pair, "UTF-8");
                        result.put(key, "");
                    } catch (Exception e) {
                        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 解析查询字符串时出错: " + e.getMessage());
                    }
                }
            }
        }
        return result;
    }

    /**
     * 获取玩家的权限组名称
     */

    private String getPlayerGroup(Player player) {

        if (luckPerms == null) return "未知";


        try {

            User user = luckPerms.getUserManager().getUser(player.getUniqueId());

            if (user == null) return "默认";


            String primaryGroup = user.getPrimaryGroup();

            if (primaryGroup == null || primaryGroup.isEmpty()) return "默认";


            // 如果PlaceholderAPI可用，则解析权限组中的占位符并转换颜色代码

            if (placeholderAPIEnabled) {

                primaryGroup = PlaceholderAPI.setPlaceholders(player, primaryGroup);

            }
            // 解析颜色代码

            primaryGroup = ChatColor.translateAlternateColorCodes('&', primaryGroup);


            return primaryGroup;

        } catch (Exception e) {

            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 获取玩家权限组时出错: " + e.getMessage());

            return "默认";

        }

    }

    /**
     * 获取玩家的称号 (使用suffix.0)
     */
    private String getPlayerPrefix(Player player) {
        if (luckPerms == null) return "暂时还没有称号喵~";

        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) return "暂时还没有称号喵~";

            // 使用MetaData获取后缀suffix.0
            net.luckperms.api.cacheddata.CachedMetaData metaData = user.getCachedData().getMetaData();
            if (metaData != null) {
                // 获取所有后缀
                java.util.SortedMap<Integer, String> suffixes = metaData.getSuffixes();
                if (suffixes != null && !suffixes.isEmpty()) {
                    // 查找suffix.0 (优先级为0的后缀)
                    String suffix = suffixes.get(0);
                    if (suffix != null && !suffix.isEmpty()) {
                        // getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby] 玩家 " + player.getName() + " 的称号(suffix.0): " + suffix);

                        // 如果PlaceholderAPI可用，则解析后缀中的占位符并转换颜色代码
                        if (placeholderAPIEnabled) {
                            suffix = PlaceholderAPI.setPlaceholders(player, suffix);
                        }
                        // 解析颜色代码
                        suffix = ChatColor.translateAlternateColorCodes('&', suffix);

                        return suffix;
                    }
                }
            }

            getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "[NekoLobby] 玩家 " + player.getName() + " 没有找到称号");
            return "暂时还没有称号喵~";
        } catch (Exception e) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 获取玩家称号时出错: " + e.getMessage());
            e.printStackTrace();
            return "暂时还没有称号喵~";
        }
    }

    /**
     * 建立数据库连接的辅助方法
     */

    private Connection createDatabaseConnection(String databaseName) throws SQLException {

        String url = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + databaseName +

                "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

        int maxRetries = 3;

        int retryCount = 0;

        SQLException lastException = null;


        while (retryCount < maxRetries) {

            try {

                return DriverManager.getConnection(url, dbUsername, dbPassword);

            } catch (SQLException e) {

                lastException = e;

                retryCount++;

                getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "[NekoLobby] 数据库连接失败，正在重试 (" + retryCount + "/" + maxRetries + "): " + e.getMessage());

                // 等待一段时间后重试

                try {

                    Thread.sleep(1000 * retryCount); // 递增等待时间

                } catch (InterruptedException ie) {

                    Thread.currentThread().interrupt();

                    throw new SQLException("数据库连接被中断", ie);

                }

            }

        }

        // 所有重试都失败了，抛出最后一次异常

        throw lastException;

    }


    /**
     * 从Authme表获取用户基本信息
     */

    private Map<String, Object> getPlayerAuthInfo(String playerName) {

        Map<String, Object> authInfo = new HashMap<>();

        Connection conn = null;

        PreparedStatement stmt = null;

        ResultSet rs = null;


        try {

            conn = createDatabaseConnection("authme");

            // 使用大小写不敏感的查询

            String query = "SELECT username, lastlogin, regdate, email FROM authme WHERE LOWER(username) = LOWER(?)";
            stmt = conn.prepareStatement(query);

            stmt.setString(1, playerName);

            rs = stmt.executeQuery();


            if (rs.next()) {

                authInfo.put("username", rs.getString("username"));

                authInfo.put("lastlogin", rs.getLong("lastlogin"));

                authInfo.put("regdate", rs.getLong("regdate"));

                authInfo.put("email", rs.getString("email"));

            }

        } catch (SQLException e) {

            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 查询Authme数据时出错: " + e.getMessage());

        } finally {

            // 确保资源被释放

            try {

                if (rs != null) rs.close();

                if (stmt != null) stmt.close();

                if (conn != null) conn.close();

            } catch (SQLException e) {

                getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 关闭Authme数据库资源时出错: " + e.getMessage());

            }

        }


        return authInfo;

    }

    private Map<String, Object> getPlayerLevelInfo(String playerName) {

        Map<String, Object> levelInfo = new HashMap<>();

        Connection conn = null;

        PreparedStatement stmt = null;

        ResultSet rs = null;


        try {

            conn = createDatabaseConnection("neko_level");

            String query = "SELECT name, level, experience, cat_food FROM player_levels WHERE name = ?";

            stmt = conn.prepareStatement(query);

            stmt.setString(1, playerName);

            rs = stmt.executeQuery();


            if (rs.next()) {

                levelInfo.put("name", rs.getString("name"));

                levelInfo.put("level", rs.getInt("level"));

                levelInfo.put("experience", rs.getInt("experience"));

                levelInfo.put("cat_food", rs.getInt("cat_food"));

            }

        } catch (SQLException e) {

            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 查询等级数据时出错: " + e.getMessage());

        } finally {

            // 确保资源被释放

            try {

                if (rs != null) rs.close();

                if (stmt != null) stmt.close();

                if (conn != null) conn.close();

            } catch (SQLException e) {

                getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 关闭等级数据库资源时出错: " + e.getMessage());

            }

        }


        return levelInfo;

    }

    /**
     * 从bw_stats_players表获取Bedwars统计数据
     */

    private Map<String, Object> getPlayerBedwarsStats(String playerName) {

        Map<String, Object> bedwarsStats = new HashMap<>();

        Connection conn = null;

        PreparedStatement stmt = null;

        ResultSet rs = null;


        try {

            conn = createDatabaseConnection("nekobedwars");

            String query = "SELECT name, kills, wins, score, loses, deaths FROM bw_stats_players WHERE name = ?";

            stmt = conn.prepareStatement(query);

            stmt.setString(1, playerName);

            rs = stmt.executeQuery();


            if (rs.next()) {

                bedwarsStats.put("name", rs.getString("name"));

                bedwarsStats.put("kills", rs.getInt("kills"));

                bedwarsStats.put("wins", rs.getInt("wins"));

                bedwarsStats.put("score", rs.getInt("score"));

                bedwarsStats.put("loses", rs.getInt("loses"));

                bedwarsStats.put("deaths", rs.getInt("deaths"));

            }

        } catch (SQLException e) {

            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 查询Bedwars数据时出错: " + e.getMessage());

        } finally {

            // 确保资源被释放

            try {

                if (rs != null) rs.close();

                if (stmt != null) stmt.close();

                if (conn != null) conn.close();

            } catch (SQLException e) {

                getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 关闭Bedwars数据库资源时出错: " + e.getMessage());

            }

        }


        return bedwarsStats;

    }

    /**
     * 从天坑乱斗数据库获取统计数据
     */

    private Map<String, Object> getPlayerThypitStats(String playerName) {

        Map<String, Object> thepitStats = new HashMap<>();

        Connection conn = null;

        PreparedStatement stmt = null;

        ResultSet rs = null;

        PreparedStatement profileStmt = null;

        ResultSet profileRs = null;


        try {

            conn = createDatabaseConnection("thepit");

            // 查询ThePitStats表获取统计数据

            String query = "SELECT KILLS, DEATHS, ASSISTS, DAMAGE_DEALT, DAMAGE_TAKEN FROM ThePitStats WHERE Player = ?";

            stmt = conn.prepareStatement(query);

            stmt.setString(1, playerName);

            rs = stmt.executeQuery();


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


            // 查询ThePitProfiles表获取等级信息

            String profileQuery = "SELECT Level FROM ThePitProfiles WHERE Player = ?";

            profileStmt = conn.prepareStatement(profileQuery);

            profileStmt.setString(1, playerName);

            profileRs = profileStmt.executeQuery();


            if (profileRs.next()) {

                thepitStats.put("level", profileRs.getInt("Level"));

            } else {

                thepitStats.put("level", 1);

            }

        } catch (Exception e) {

            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 查询天坑乱斗数据时出错: " + e.getMessage());

        } finally {

            // 确保资源被释放

            try {

                if (rs != null) rs.close();

                if (stmt != null) stmt.close();

                if (profileRs != null) profileRs.close();

                if (profileStmt != null) profileStmt.close();

                if (conn != null) conn.close();

            } catch (SQLException e) {

                getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 关闭天坑乱斗数据库资源时出错: " + e.getMessage());

            }

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


            String playerName = args[1];

            Player targetPlayer = Bukkit.getPlayer(playerName);


            if (targetPlayer != null) {

                if (args.length >= 3 && args[2].equalsIgnoreCase("mvp")) {

                    // 给玩家设置MVP权限组

                    setPlayerMvpGroup(targetPlayer);

                    sender.sendMessage(ChatColor.GREEN + "已为玩家 " + playerName + " 设置MVP权限组！");

                    targetPlayer.sendMessage(ChatColor.GREEN + "MVP权益购买已处理！");

                } else {

                    // 给玩家设置VIP权限组

                    setPlayerVipGroup(targetPlayer);

                    sender.sendMessage(ChatColor.GREEN + "已为玩家 " + playerName + " 设置VIP权限组！");

                    targetPlayer.sendMessage(ChatColor.GREEN + "VIP权益购买已处理！");

                }

            } else {

                // 尝试离线设置

                sender.sendMessage(ChatColor.YELLOW + "玩家不在线，正在处理离线设置...");

                // 对于离线玩家，我们可以记录到数据库或其他地方，待玩家上线时处理

                if (args.length >= 3 && args[2].equalsIgnoreCase("mvp")) {

                    handleOfflineMvpPurchase(playerName);

                    sender.sendMessage(ChatColor.GREEN + "已记录 " + playerName + " 的MVP购买，下次上线时激活！");

                } else {

                    handleOfflineVipPurchase(playerName);

                    sender.sendMessage(ChatColor.GREEN + "已记录 " + playerName + " 的VIP购买，下次上线时激活！");

                }

            }
        return false;
    }


    @EventHandler

    public void onPlayerJoin(PlayerJoinEvent event) throws SQLException {
        Player player = event.getPlayer();
        FileConfiguration config = getConfig();

        if (config.contains("spawn.world")) {
            String worldName = config.getString("spawn.world");
            World world = getServer().getWorld(worldName);

            // 检查世界是否存在
            if (world == null) {
                getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 错误: 世界 '" + worldName + "' 不存在!");
                player.sendMessage(ChatColor.RED + "出生点世界不存在，请联系管理员!");
                return; // 不进行传送
            }

            double x = config.getDouble("spawn.x");
            double y = config.getDouble("spawn.y");
            double z = config.getDouble("spawn.z");
            float yaw = (float) config.getDouble("spawn.yaw");
            float pitch = (float) config.getDouble("spawn.pitch");

            Location spawnLocation = new Location(world, x, y, z, yaw, pitch);
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


        // 权益购买/充值（第九个格子，索引8）

        Material emeraldMat = Material.matchMaterial("EMERALD");

        ItemStack rechargeItem = emeraldMat != null ?

                new ItemStack(emeraldMat) :

                new ItemStack(Material.matchMaterial("EMERALD"));

        ItemMeta rechargeMeta = rechargeItem.getItemMeta();

        rechargeMeta.setDisplayName(ChatColor.AQUA + "✦ " + ChatColor.BOLD + "权益购买/充值" + ChatColor.AQUA + " ✦");

        List<String> rechargeLore = new ArrayList<>();

        rechargeLore.add(ChatColor.WHITE + "✿ " + ChatColor.GREEN + "点击打开权益中心" + ChatColor.WHITE + " ✿");

        rechargeLore.add("");

        rechargeLore.add(ChatColor.YELLOW + "❁ " + ChatColor.ITALIC + "点击购买/充值权益" + ChatColor.YELLOW + " ❁");

        rechargeMeta.setLore(rechargeLore);

        rechargeItem.setItemMeta(rechargeMeta);


        inv.setItem(8, rechargeItem);


        // 检查是否有待处理的VIP购买

        checkPendingVipPurchases(player);

        // 检查是否有待处理的MVP购买

        checkPendingMvpPurchases(player);


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
            // player.sendMessage(ChatColor.RED + "你没有权限放置方块!");
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
            // 只处理与方块相关的交互，避免影响物品使用
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
                return;
            }


            // 处理权益购买/充值物品


            Material emeraldMat = Material.matchMaterial("EMERALD");


            if ((emeraldMat != null && item.getType() == emeraldMat) || item.getType() == Material.matchMaterial("EMERALD")) {


                if (p.getInventory().getHeldItemSlot() == 8) { // 第九个格子（索引8）


                    // 打开权益购买/充值界面


                    openRechargeGUI(p);


                    e.setCancelled(true);


                    return;


                }


            }

        }

    }

    private void openPlayerProfileGUI(Player p) {
        // 创建个人档案GUI - 二次元风格
        Inventory profileGUI = Bukkit.createInventory(null, 54, ChatColor.LIGHT_PURPLE + "✿ " + ChatColor.BOLD + "个人档案" + ChatColor.LIGHT_PURPLE + " ✿");

        // 获取玩家名称

        String playerName = p.getName();


        // 从数据库获取玩家信息 (实时获取最新数据)

        Map<String, Object> authInfo = getPlayerAuthInfo(playerName);

        Map<String, Object> levelInfo = getPlayerLevelInfo(playerName);

        Map<String, Object> bedwarsStats = getPlayerBedwarsStats(playerName);

        Map<String, Object> thepitStats = getPlayerThypitStats(playerName);


        // 实时获取权限组和称号 (确保使用最新数据)

        String group = getPlayerGroup(p);

        String prefix = getPlayerPrefix(p);

        // 获取Authme信息
        String email = (String) authInfo.getOrDefault("email", "未设置");
        long lastLogin = (Long) authInfo.getOrDefault("lastlogin", 0L);
        long regDate = (Long) authInfo.getOrDefault("regdate", 0L);

        // 格式化时间
        String lastLoginStr = lastLogin > 0 ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(lastLogin)) : "未知";
        String regDateStr = regDate > 0 ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(regDate)) : "未知";

        // 获取等级信息
        int level = (Integer) levelInfo.getOrDefault("level", 1);
        int experience = (Integer) levelInfo.getOrDefault("experience", 0);

        // 获取Bedwars统计信息
        int kills = (Integer) bedwarsStats.getOrDefault("kills", 0);
        int wins = (Integer) bedwarsStats.getOrDefault("wins", 0);
        int score = (Integer) bedwarsStats.getOrDefault("score", 0);
        int loses = (Integer) bedwarsStats.getOrDefault("loses", 0);
        int deaths = (Integer) bedwarsStats.getOrDefault("deaths", 0);

        // 计算Bedwars K/D比率
        double kdRatio = deaths > 0 ? (double) kills / deaths : kills;
        // 计算Bedwars W/L比率
        double wlRatio = loses > 0 ? (double) wins / loses : wins;

        // 天坑乱斗统计
        int pitKills = (Integer) thepitStats.getOrDefault("kills", 0);
        int pitDeaths = (Integer) thepitStats.getOrDefault("deaths", 0);
        int pitAssists = (Integer) thepitStats.getOrDefault("assists", 0);
        int pitDamageDealt = (Integer) thepitStats.getOrDefault("damage_dealt", 0);
        int pitDamageTaken = (Integer) thepitStats.getOrDefault("damage_taken", 0);
        int pitLevel = (Integer) thepitStats.getOrDefault("level", 1);

        double pitKdRatio = pitDeaths > 0 ? (double) pitKills / pitDeaths : pitKills;

        // 玩家头像 - 二次元风格
        Material playerHeadMat = Material.matchMaterial("SKULL_ITEM");
        ItemStack playerHead = playerHeadMat != null ?
                new ItemStack(playerHeadMat, 1, (short) 3) :
                new ItemStack(Material.matchMaterial("SKULL_ITEM"), 1, (short) 3);
        SkullMeta headMeta = (SkullMeta) playerHead.getItemMeta();
        if (playerHeadMat != null) {
            // 1.12.2及以下版本
            headMeta.setOwner(p.getName());
        } else {
            // 1.13及以上版本
            headMeta.setOwningPlayer(p);
        }
        headMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "★ " + ChatColor.BOLD + playerName + ChatColor.LIGHT_PURPLE + " ★");

        // 添加玩家信息到Lore - 二次元风格

        List<String> headLore = new ArrayList<>();

        headLore.add(ChatColor.WHITE + "✿ " + ChatColor.AQUA + "权限组: " + ChatColor.YELLOW + group + ChatColor.WHITE + " ✿");

        headLore.add(ChatColor.WHITE + "✿ " + ChatColor.GOLD + "称号: " + ChatColor.RESET + prefix + ChatColor.WHITE + " ✿");

        headLore.add(ChatColor.WHITE + "✿ " + ChatColor.GOLD + "猫粮: " + ChatColor.LIGHT_PURPLE + (Integer) levelInfo.getOrDefault("cat_food", 0) + ChatColor.WHITE + " ✿");

        headLore.add(ChatColor.WHITE + "✿ " + ChatColor.AQUA + "等级: " + ChatColor.YELLOW + level + ChatColor.WHITE + " ✿");

        headLore.add(ChatColor.WHITE + "✿ " + ChatColor.AQUA + "最后登录: " + ChatColor.GREEN + lastLoginStr + ChatColor.WHITE + " ✿");

        headLore.add("");

        headLore.add(ChatColor.LIGHT_PURPLE + "❀ " + ChatColor.BOLD + "点击查看详情" + ChatColor.LIGHT_PURPLE + " ❀");

        headMeta.setLore(headLore);
        playerHead.setItemMeta(headMeta);

        // 装饰性玻璃板 - 粉色边框 (二次元风格)
        Material pinkGlassMat = Material.matchMaterial("STAINED_GLASS_PANE");
        ItemStack pinkGlassPane = pinkGlassMat != null ?
                new ItemStack(pinkGlassMat, 1, (short) 6) :
                new ItemStack(Material.matchMaterial("STAINED_GLASS_PANE"), 1, (short) 6);
        ItemMeta pinkGlassMeta = pinkGlassPane.getItemMeta();
        pinkGlassMeta.setDisplayName(ChatColor.WHITE + "❀");
        pinkGlassPane.setItemMeta(pinkGlassMeta);

        // 装饰性玻璃板 - 淡紫色背景 (二次元风格)
        Material purpleGlassMat = Material.matchMaterial("STAINED_GLASS_PANE");
        ItemStack purpleGlassPane = purpleGlassMat != null ?
                new ItemStack(purpleGlassMat, 1, (short) 2) :
                new ItemStack(Material.matchMaterial("STAINED_GLASS_PANE"), 1, (short) 2);
        ItemMeta purpleGlassMeta = purpleGlassPane.getItemMeta();
        purpleGlassMeta.setDisplayName(ChatColor.WHITE + "✿");
        purpleGlassPane.setItemMeta(purpleGlassMeta);

        // 装饰性玻璃板 - 白色装饰 (二次元风格)
        Material whiteGlassMat = Material.matchMaterial("STAINED_GLASS_PANE");
        ItemStack whiteGlassPane = whiteGlassMat != null ?
                new ItemStack(whiteGlassMat, 1, (short) 0) :
                new ItemStack(Material.matchMaterial("STAINED_GLASS_PANE"), 1, (short) 0);
        ItemMeta whiteGlassMeta = whiteGlassPane.getItemMeta();
        whiteGlassMeta.setDisplayName(ChatColor.WHITE + "❁");
        whiteGlassPane.setItemMeta(whiteGlassMeta);

        // 填充背景 - 使用淡紫色玻璃板
        for (int i = 0; i < 54; i++) {
            profileGUI.setItem(i, purpleGlassPane.clone());
        }

        // 设置粉色边框
        int[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53};
        for (int slot : borderSlots) {
            profileGUI.setItem(slot, pinkGlassPane.clone());
        }

        // 添加装饰点 - 创建更美观的布局
        int[] decorationSlots = {10, 11, 12, 14, 15, 16, 19, 20, 21, 23, 24, 25, 28, 29, 30, 32, 33, 34, 37, 38, 40, 42, 43};
        for (int slot : decorationSlots) {
            profileGUI.setItem(slot, whiteGlassPane.clone());
        }

        // 设置玩家头像在顶部中心位置
        profileGUI.setItem(4, playerHead);

        // 玩家统计信息 - 二次元风格 (第二行中间)
        Material bookMat = Material.matchMaterial("BOOK");
        ItemStack statsItem = bookMat != null ?
                new ItemStack(bookMat) :
                new ItemStack(Material.matchMaterial("BOOK"));
        ItemMeta statsMeta = statsItem.getItemMeta();
        statsMeta.setDisplayName(ChatColor.AQUA + "✉ " + ChatColor.BOLD + "基础信息" + ChatColor.AQUA + " ✉");
        List<String> statsLore = new ArrayList<>();
        statsLore.add(ChatColor.WHITE + "✿ " + ChatColor.GOLD + "注册时间: " + ChatColor.YELLOW + regDateStr + ChatColor.WHITE + " ✿");
        statsLore.add(ChatColor.WHITE + "✿ " + ChatColor.GOLD + "邮箱地址: " + ChatColor.YELLOW + email + ChatColor.WHITE + " ✿");
        statsLore.add("");
        statsLore.add(ChatColor.LIGHT_PURPLE + "❁ " + ChatColor.ITALIC + "玩家基本信息" + ChatColor.LIGHT_PURPLE + " ❁");
        statsMeta.setLore(statsLore);
        statsItem.setItemMeta(statsMeta);
        profileGUI.setItem(22, statsItem);

        // 等级信息 - 二次元风格 (第二行右侧)
        Material expBottleMat = Material.matchMaterial("EXP_BOTTLE");
        ItemStack levelItem = expBottleMat != null ?
                new ItemStack(expBottleMat) :
                new ItemStack(Material.matchMaterial("EXP_BOTTLE"));
        ItemMeta levelMeta = levelItem.getItemMeta();

        levelMeta.setDisplayName(ChatColor.GREEN + "✧ " + ChatColor.BOLD + "等级信息" + ChatColor.GREEN + " ✧");

        List<String> levelLore = new ArrayList<>();

        levelLore.add(ChatColor.WHITE + "✿ " + ChatColor.GOLD + "当前等级: " + ChatColor.LIGHT_PURPLE + level + ChatColor.WHITE + " ✿");

        levelLore.add(ChatColor.WHITE + "✿ " + ChatColor.GOLD + "经验值: " + ChatColor.LIGHT_PURPLE + experience + ChatColor.WHITE + " ✿");

        levelLore.add("");

        levelLore.add(ChatColor.AQUA + "❁ " + ChatColor.ITALIC + "经验值成长记录" + ChatColor.AQUA + " ❁");

        levelMeta.setLore(levelLore);

        levelItem.setItemMeta(levelMeta);

        profileGUI.setItem(24, levelItem);

        // Bedwars统计 - 二次元风格 (第三行左侧)
        Material bedMat = Material.matchMaterial("BED");
        ItemStack bedwarsItem = bedMat != null ?
                new ItemStack(bedMat) :
                new ItemStack(Material.matchMaterial("BED"));
        ItemMeta bedwarsMeta = bedwarsItem.getItemMeta();
        bedwarsMeta.setDisplayName(ChatColor.RED + "⚔ " + ChatColor.BOLD + "起床战争" + ChatColor.RED + " ⚔");
        List<String> bedwarsLore = new ArrayList<>();
        bedwarsLore.add(ChatColor.WHITE + "✿ " + ChatColor.GOLD + "击杀数: " + ChatColor.GREEN + kills + ChatColor.WHITE + " ✿");
        bedwarsLore.add(ChatColor.WHITE + "✿ " + ChatColor.GOLD + "死亡数: " + ChatColor.RED + deaths + ChatColor.WHITE + " ✿");
        bedwarsLore.add(ChatColor.WHITE + "✿ " + ChatColor.GOLD + "K/D比率: " + (kdRatio >= 1.0 ? ChatColor.GREEN : ChatColor.RED) + String.format("%.2f", kdRatio) + ChatColor.WHITE + " ✿");
        bedwarsLore.add(ChatColor.WHITE + "✿ " + ChatColor.GOLD + "胜利数: " + ChatColor.GREEN + wins + ChatColor.WHITE + " ✿");
        bedwarsLore.add(ChatColor.WHITE + "✿ " + ChatColor.GOLD + "失败数: " + ChatColor.RED + loses + ChatColor.WHITE + " ✿");
        bedwarsLore.add(ChatColor.WHITE + "✿ " + ChatColor.GOLD + "W/L比率: " + (wlRatio >= 1.0 ? ChatColor.GREEN : ChatColor.RED) + String.format("%.2f", wlRatio) + ChatColor.WHITE + " ✿");
        bedwarsLore.add(ChatColor.WHITE + "✿ " + ChatColor.GOLD + "总分数: " + ChatColor.AQUA + score + ChatColor.WHITE + " ✿");
        bedwarsLore.add("");
        bedwarsLore.add(ChatColor.LIGHT_PURPLE + "❁ " + ChatColor.ITALIC + "战斗数据统计" + ChatColor.LIGHT_PURPLE + " ❁");
        bedwarsMeta.setLore(bedwarsLore);
        bedwarsItem.setItemMeta(bedwarsMeta);
        profileGUI.setItem(30, bedwarsItem);

        // 天坑乱斗统计 - 二次元风格 (第三行右侧)
        Material diamondSwordMat = Material.matchMaterial("DIAMOND_SWORD");
        ItemStack thepitItem = diamondSwordMat != null ?
                new ItemStack(diamondSwordMat) :
                new ItemStack(Material.matchMaterial("DIAMOND_SWORD"));
        ItemMeta thepitMeta = thepitItem.getItemMeta();
        thepitMeta.setDisplayName(ChatColor.GOLD + "⚔ " + ChatColor.BOLD + "天坑乱斗" + ChatColor.GOLD + " ⚔");
        List<String> thepitLore = new ArrayList<>();
        thepitLore.add(ChatColor.WHITE + "✿ " + ChatColor.GOLD + "等级: " + ChatColor.LIGHT_PURPLE + pitLevel + ChatColor.WHITE + " ✿");
        thepitLore.add(ChatColor.WHITE + "✿ " + ChatColor.GOLD + "击杀数: " + ChatColor.GREEN + pitKills + ChatColor.WHITE + " ✿");
        thepitLore.add(ChatColor.WHITE + "✿ " + ChatColor.GOLD + "死亡数: " + ChatColor.RED + pitDeaths + ChatColor.WHITE + " ✿");
        thepitLore.add(ChatColor.WHITE + "✿ " + ChatColor.GOLD + "助攻数: " + ChatColor.AQUA + pitAssists + ChatColor.WHITE + " ✿");
        thepitLore.add(ChatColor.WHITE + "✿ " + ChatColor.GOLD + "K/D比率: " + (pitKdRatio >= 1.0 ? ChatColor.GREEN : ChatColor.RED) + String.format("%.2f", pitKdRatio) + ChatColor.WHITE + " ✿");
        thepitLore.add(ChatColor.WHITE + "✿ " + ChatColor.GOLD + "造成伤害: " + ChatColor.GREEN + pitDamageDealt + ChatColor.WHITE + " ✿");
        thepitLore.add(ChatColor.WHITE + "✿ " + ChatColor.GOLD + "受到伤害: " + ChatColor.RED + pitDamageTaken + ChatColor.WHITE + " ✿");
        thepitLore.add("");
        thepitLore.add(ChatColor.YELLOW + "❁ " + ChatColor.ITALIC + "竞技场战斗记录" + ChatColor.YELLOW + " ❁");
        thepitMeta.setLore(thepitLore);
        thepitItem.setItemMeta(thepitMeta);
        profileGUI.setItem(32, thepitItem);


        // 打开GUI

        p.openInventory(profileGUI);

    }

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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        // 基础检查
        if (!(e.getWhoClicked() instanceof Player)) return;

        Player player = (Player) e.getWhoClicked();
        Inventory clickedInventory = e.getClickedInventory();

        // 确保点击的是顶部Inventory（不是玩家自己的背包）
        if (clickedInventory == null || !clickedInventory.equals(e.getView().getTopInventory())) {
            return;
        }

        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        String inventoryTitle = e.getView().getTitle();

        // 使用颜色代码清理后的标题进行匹配
        String cleanTitle = ChatColor.stripColor(inventoryTitle);

        // 调试信息
        // getLogger().info("玩家 " + player.getName() + " 点击了GUI: " + cleanTitle + " (原始: " + inventoryTitle + ")");
        // getLogger().info("点击槽位: " + e.getSlot() + ", 物品: " + clickedItem.getType());

        // 处理不同的GUI
        if (cleanTitle.contains("个人档案")) {
            e.setCancelled(true);
            handleProfileGUIInteraction(e);
        } else if (cleanTitle.contains("权益购买") || cleanTitle.contains("充值中心")) {
            e.setCancelled(true);
            handleRechargeGUIInteraction(e);
        }
    }

    private void handleProfileGUIInteraction(InventoryClickEvent e) {


        e.setCancelled(true); // 防止玩家拿取物品


        Player player = (Player) e.getWhoClicked();

        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby Debug] Handling profile GUI interaction for player " + player.getName());


        ItemStack clickedItem = e.getCurrentItem();

        // 添加点击位置调试信息
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby Debug] Player " + player.getName() + " clicked slot " + e.getSlot());


        // 获取玩家数据用于显示 (实时获取最新数据)

        String playerName = player.getName();

        Map<String, Object> authInfo = getPlayerAuthInfo(playerName);

        long regDate = (Long) authInfo.getOrDefault("regdate", 0L);

        String regDateStr = regDate > 0 ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(regDate)) : "未知";

        String email = (String) authInfo.getOrDefault("email", "未设置");


        // 获取等级信息 (实时获取最新数据)

        Map<String, Object> levelInfo = getPlayerLevelInfo(playerName);

        int level = (Integer) levelInfo.getOrDefault("level", 1);

        int experience = (Integer) levelInfo.getOrDefault("experience", 0);


        // 获取Bedwars统计信息 (实时获取最新数据)

        Map<String, Object> bedwarsStats = getPlayerBedwarsStats(playerName);

        int kills = (Integer) bedwarsStats.getOrDefault("kills", 0);

        int wins = (Integer) bedwarsStats.getOrDefault("wins", 0);

        int score = (Integer) bedwarsStats.getOrDefault("score", 0);

        int loses = (Integer) bedwarsStats.getOrDefault("loses", 0);

        int deaths = (Integer) bedwarsStats.getOrDefault("deaths", 0);

        // 如果点击的是空槽位或装饰性物品，则不处理
        Material glassPaneMat = Material.matchMaterial("STAINED_GLASS_PANE");
        boolean isGlassPane = glassPaneMat != null ?

                clickedItem.getType() == glassPaneMat :

                (clickedItem.getType() == Material.matchMaterial("STAINED_GLASS_PANE") && (clickedItem.getDurability() == 0 || clickedItem.getDurability() == 2 || clickedItem.getDurability() == 3 || clickedItem.getDurability() == 6 || clickedItem.getDurability() == 7 || clickedItem.getDurability() == 8 || clickedItem.getDurability() == 9 || clickedItem.getDurability() == 15));

        if (clickedItem == null || clickedItem.getType() == Material.AIR || isGlassPane) {

            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby Debug] Player " + player.getName() + " clicked empty slot or glass pane");

            return;

        }


        // 如果点击的是玩家头像，则显示更多详细信息 - 二次元风格

        Material skullMat = Material.matchMaterial("SKULL_ITEM");

        if ((skullMat != null && clickedItem.getType() == skullMat) || clickedItem.getType() == Material.matchMaterial("SKULL_ITEM")) {

            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby Debug] Player " + player.getName() + " clicked player head item");

            player.sendMessage(ChatColor.LIGHT_PURPLE + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");

            player.sendMessage(ChatColor.AQUA + "                   玩家详细信息");

            player.sendMessage(ChatColor.LIGHT_PURPLE + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");

            player.sendMessage(ChatColor.YELLOW + "  ✿ 名称: " + ChatColor.WHITE + player.getName());

            player.sendMessage(ChatColor.YELLOW + "  ✿ UUID: " + ChatColor.WHITE + player.getUniqueId());

            player.sendMessage(ChatColor.YELLOW + "  ✿ 等级: " + ChatColor.WHITE + level);

            player.sendMessage(ChatColor.LIGHT_PURPLE + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");

            return;

        }

        // 如果点击的是统计信息书本 - 二次元风格

        Material bookMat = Material.matchMaterial("BOOK");

        if ((bookMat != null && clickedItem.getType() == bookMat) || clickedItem.getType() == Material.matchMaterial("BOOK")) {

            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby Debug] Player " + player.getName() + " clicked book item");

            player.sendMessage(ChatColor.AQUA + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");

            player.sendMessage(ChatColor.GOLD + "                   基础信息");

            player.sendMessage(ChatColor.AQUA + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");

            player.sendMessage(ChatColor.YELLOW + "  ✿ 注册时间: " + ChatColor.WHITE + regDateStr);

            player.sendMessage(ChatColor.YELLOW + "  ✿ 邮箱地址: " + ChatColor.WHITE + email);

            player.sendMessage(ChatColor.AQUA + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");

            return;

        }

        // 如果点击的是等级信息 - 二次元风格


        Material expBottleMat = Material.matchMaterial("EXP_BOTTLE");


        if ((expBottleMat != null && clickedItem.getType() == expBottleMat) || clickedItem.getType() == Material.matchMaterial("EXP_BOTTLE")) {

            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby Debug] Player " + player.getName() + " clicked exp bottle item");


            player.sendMessage(ChatColor.GREEN + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");


            player.sendMessage(ChatColor.LIGHT_PURPLE + "                   等级信息");


            player.sendMessage(ChatColor.GREEN + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");


            player.sendMessage(ChatColor.YELLOW + "  ✿ 当前等级: " + ChatColor.WHITE + level);


            player.sendMessage(ChatColor.YELLOW + "  ✿ 经验值: " + ChatColor.WHITE + experience);


            player.sendMessage(ChatColor.GREEN + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");

            return;

        }

        // 如果点击的是Bedwars统计 - 二次元风格

        Material bedMat = Material.matchMaterial("BED");

        if ((bedMat != null && clickedItem.getType() == bedMat) || clickedItem.getType() == Material.matchMaterial("BED")) {

            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby Debug] Player " + player.getName() + " clicked bed item");

            player.sendMessage(ChatColor.RED + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");

            player.sendMessage(ChatColor.GOLD + "                   起床战争");

            player.sendMessage(ChatColor.RED + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");

            player.sendMessage(ChatColor.YELLOW + "  ✿ 击杀数: " + ChatColor.WHITE + kills);

            player.sendMessage(ChatColor.YELLOW + "  ✿ 死亡数: " + ChatColor.WHITE + deaths);

            player.sendMessage(ChatColor.YELLOW + "  ✿ 胜利数: " + ChatColor.WHITE + wins);

            player.sendMessage(ChatColor.YELLOW + "  ✿ 失败数: " + ChatColor.WHITE + loses);

            player.sendMessage(ChatColor.YELLOW + "  ✿ 总分数: " + ChatColor.WHITE + score);

            player.sendMessage(ChatColor.RED + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");

            return;
        }


        // 如果点击的是天坑乱斗统计 - 二次元风格


        Material diamondSwordMat = Material.matchMaterial("DIAMOND_SWORD");

        if ((diamondSwordMat != null && clickedItem.getType() == diamondSwordMat) || clickedItem.getType() == Material.matchMaterial("DIAMOND_SWORD")) {

            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby Debug] Player " + player.getName() + " clicked diamond sword item");

            // 实时获取天坑乱斗统计信息


            Map<String, Object> thepitStats = getPlayerThypitStats(playerName);


            int pitKills = (Integer) thepitStats.getOrDefault("kills", 0);

            int pitDeaths = (Integer) thepitStats.getOrDefault("deaths", 0);

            int pitAssists = (Integer) thepitStats.getOrDefault("assists", 0);

            int pitLevel = (Integer) thepitStats.getOrDefault("level", 1);

            player.sendMessage(ChatColor.GOLD + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");

            player.sendMessage(ChatColor.LIGHT_PURPLE + "                   天坑乱斗");

            player.sendMessage(ChatColor.GOLD + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");

            player.sendMessage(ChatColor.YELLOW + "  ✿ 等级: " + ChatColor.WHITE + pitLevel);

            player.sendMessage(ChatColor.YELLOW + "  ✿ 击杀数: " + ChatColor.WHITE + pitKills);

            player.sendMessage(ChatColor.YELLOW + "  ✿ 死亡数: " + ChatColor.WHITE + pitDeaths);

            player.sendMessage(ChatColor.YELLOW + "  ✿ 助攻数: " + ChatColor.WHITE + pitAssists);

            player.sendMessage(ChatColor.GOLD + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");

            return;
        }

        // 如果没有匹配任何项目，输出调试信息
        // getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby Debug] Player " + player.getName() + " clicked unknown item: " + clickedItem.getType().name());

    }

    /**
     * 处理权益购买GUI中的点击事件
     */


    private void handleRechargeGUIInteractionOld(InventoryClickEvent e) {


        e.setCancelled(true); // 防止玩家拿取物品


        Player player = (Player) e.getWhoClicked();

        // getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby Debug] 处理玩家 " + player.getName() + " 的权益购买GUI交互");


        ItemStack clickedItem = e.getCurrentItem();


        // 添加点击位置调试信息

        // getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby Debug] 玩家 " + player.getName() + " 点击了槽位 " + e.getSlot());


        // 获取玩家名称

        String playerName = player.getName();

        // 从数据库获取玩家信息

        Map<String, Object> levelInfo = getPlayerLevelInfo(playerName);

        int catFood = (Integer) levelInfo.getOrDefault("cat_food", 0);


        // 如果点击的是空槽位或装饰性物品，则不处理

        Material glassPaneMat = Material.matchMaterial("STAINED_GLASS_PANE");

        boolean isGlassPane = glassPaneMat != null ?

                clickedItem.getType() == glassPaneMat :

                clickedItem.getType() == Material.matchMaterial("STAINED_GLASS_PANE") ||

                        clickedItem.getType() == Material.matchMaterial("STAINED_GLASS_PANE") ||

                        clickedItem.getType() == Material.matchMaterial("STAINED_GLASS_PANE") ||

                        clickedItem.getType() == Material.matchMaterial("STAINED_GLASS_PANE") ||

                        clickedItem.getType() == Material.matchMaterial("STAINED_GLASS_PANE");


        if (clickedItem == null || clickedItem.getType() == Material.AIR || isGlassPane) {

            return;

        }


        // 如果点击的是VIP权益选项

        Material diamondMat = Material.matchMaterial("DIAMOND");

        if ((diamondMat != null && clickedItem.getType() == diamondMat) || clickedItem.getType() == Material.matchMaterial("DIAMOND")) {

            // 检查玩家是否有足够的猫粮

            if (catFood < 300) {

                player.sendMessage(ChatColor.RED + "猫粮不足！购买VIP权益需要300猫粮。");

                player.closeInventory(); // 关闭GUI

                return;

            }


            // 扣除猫粮

            deductCatFood(player, 300);

            // 设置玩家VIP权限组

            setPlayerVipGroup(player);

            player.sendMessage(ChatColor.GREEN + "VIP权益购买成功！");

            player.closeInventory(); // 关闭GUI

            return;

        }


        // 支付宝VIP购买（槽位22）
        if (e.getSlot() == 22) { // VIP权益选项（现金支付）
            player.sendMessage(ChatColor.YELLOW + "正在为您生成支付二维码...");
            // 创建Z-Pay支付订单
            createZPayOrder(player);
            return;
        }

        // 如果点击的是玩家信息头颅，刷新GUI（槽位49）
        if (e.getSlot() == 49) {
            Material playerHeadMat = Material.matchMaterial("SKULL_ITEM");
            if ((playerHeadMat != null && clickedItem.getType() == playerHeadMat) || clickedItem.getType() == Material.matchMaterial("SKULL_ITEM")) {
                // getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby Debug] 玩家 " + player.getName() + " 点击了玩家头颅项目");
                // 重新打开GUI以刷新信息
                openRechargeGUI(player);
                return;
            }

        }

        // 现金支付VIP购买（槽位22）


        if (e.getSlot() == 22) { // VIP权益选项（现金支付） {
            player.sendMessage(ChatColor.YELLOW + "正在为您生成Z-Pay支付二维码...");
            // 创建Z-Pay支付订单
            createZPayOrder(player);
            return;
        }


        // 如果点击的是玩家信息头颅，刷新GUI（槽位49）

        if (e.getSlot() == 49) {

            Material playerHeadMat = Material.matchMaterial("SKULL_ITEM");

            if ((playerHeadMat != null && clickedItem.getType() == playerHeadMat) || clickedItem.getType() == Material.matchMaterial("SKULL_ITEM")) {

                // getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby Debug] 玩家 " + player.getName() + " 点击了玩家头颅项目");

                // 重新打开GUI以刷新信息

                openRechargeGUI(player);

                return;

            }

        }


        // 如果没有匹配任何项目，输出调试信息

        // getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby Debug] 玩家 " + player.getName() + " 点击了未知项目: " + clickedItem.getType().name() + " (槽位: " + e.getSlot() + ")");

    }


    @EventHandler

    public void onPlayerQuit(PlayerQuitEvent e) {

        Player player = e.getPlayer();

        // 清除玩家数据

        lastSpacePress.remove(player);

        hiddenPlayers.remove(player);

        lastLocation.remove(player); // 清除玩家上一个位置数据

        invisibilityCooldown.remove(player); // 清除冷却时间数据

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
                player.sendMessage(ChatColor.RED + "杂鱼你要干什么！不允许出去！");
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
                        player.sendMessage(ChatColor.RED + "杂鱼你要干什么！不允许出去！");
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
                            player.sendMessage(ChatColor.RED + "杂鱼你要干什么！不允许出去！");
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
                        player.sendMessage(ChatColor.RED + "杂鱼你要干什么！不允许出去！");
                    }
                }
            } else {
                // 玩家在活动范围内，更新上一个位置
                lastLocation.put(player, currentLocation);
            }
        }
        // 如果活动范围未设置（都为0），则不限制玩家活动
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

        // 只玩家按住空格键时触发（潜行）
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

    /**
     * 扣除玩家猫粮
     */

    private void deductCatFood(Player player, int amount) {

        Connection conn = null;

        PreparedStatement stmt = null;


        try {

            conn = createDatabaseConnection("neko_level");

            String query = "UPDATE player_levels SET cat_food = cat_food - ? WHERE name = ?";

            stmt = conn.prepareStatement(query);

            stmt.setInt(1, amount);

            stmt.setString(2, player.getName());

            stmt.executeUpdate();

        } catch (SQLException e) {

            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 扣除玩家猫粮时出错: " + e.getMessage());

        } finally {

            // 确保资源被释放

            try {

                if (stmt != null) stmt.close();

                if (conn != null) conn.close();

            } catch (SQLException e) {

                getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 关闭数据库资源时出错: " + e.getMessage());

            }

        }

    }


    /**
     * 设置玩家VIP权限组
     */

    private void setPlayerVipGroup(Player player) {

        if (luckPerms == null) {

            player.sendMessage(ChatColor.RED + "权限系统未初始化，无法设置VIP权限组！");

            return;

        }


        try {

            // 获取用户

            User user = luckPerms.getUserManager().getUser(player.getUniqueId());

            if (user == null) {

                player.sendMessage(ChatColor.RED + "无法获取用户信息！");

                return;

            }


            // 创建继承节点（将用户添加到VIP组）

            InheritanceNode node = InheritanceNode.builder("vip")

                    .expiry(30, java.util.concurrent.TimeUnit.DAYS) // 设置30天有效期

                    .build();


            // 构建修改任务并应用

            luckPerms.getUserManager().modifyUser(player.getUniqueId(), userEditor -> {

                // 使用传统方式移除之前的VIP权限（如果存在）

                // 先收集要保留的节点

                java.util.List<net.luckperms.api.node.Node> nodesToKeep = new java.util.ArrayList<>();

                for (net.luckperms.api.node.Node n : userEditor.data().toCollection()) {

                    if (!(n instanceof InheritanceNode &&

                            ((InheritanceNode) n).getGroupName().equals("vip"))) {

                        nodesToKeep.add(n);

                    }
                }
                // 清空所有节点并重新添加保留的节点
                userEditor.data().clear();
                for (net.luckperms.api.node.Node n : nodesToKeep) {
                    userEditor.data().add(n);
                }
                // 添加新的VIP权限
                userEditor.data().add(node);
            }).thenRun(() -> {
                // 异步操作完成后，在主线程发送消息
                getServer().getScheduler().runTask(this, () -> {
                    player.sendMessage(ChatColor.GREEN + "您的VIP权限已成功设置，有效期为一个月！");
                });
            }).exceptionally(throwable -> {
                getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 设置玩家VIP权限组时出错: " + throwable.getMessage());
                player.sendMessage(ChatColor.RED + "设置VIP权限时出现错误，请联系管理员！");
                return null;
            });
        } catch (Exception e) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 设置玩家VIP权限组时出错: " + e.getMessage());
            player.sendMessage(ChatColor.RED + "设置VIP权限时出现错误，请联系管理员！");
        }
    }

    /**
     * 设置玩家MVP权限组
     */

    private void setPlayerMvpGroup(Player player) {

        if (luckPerms == null) {

            player.sendMessage(ChatColor.RED + "权限系统未初始化，无法设置MVP权限组！");

            return;

        }


        try {

            // 获取用户

            User user = luckPerms.getUserManager().getUser(player.getUniqueId());

            if (user == null) {

                player.sendMessage(ChatColor.RED + "无法获取用户信息！");

                return;

            }


            // 创建继承节点（将用户添加到MVP组）

            InheritanceNode node = InheritanceNode.builder("mvp")

                    .expiry(30, java.util.concurrent.TimeUnit.DAYS) // 设置30天有效期

                    .build();


            // 构建修改任务并应用

            luckPerms.getUserManager().modifyUser(player.getUniqueId(), userEditor -> {

                // 使用传统方式移除之前的MVP权限（如果存在）

                // 先收集要保留的节点

                java.util.List<net.luckperms.api.node.Node> nodesToKeep = new java.util.ArrayList<>();

                for (net.luckperms.api.node.Node n : userEditor.data().toCollection()) {

                    if (!(n instanceof InheritanceNode &&

                            ((InheritanceNode) n).getGroupName().equals("mvp"))) {

                        nodesToKeep.add(n);

                    }
                }
                // 清空所有节点并重新添加保留的节点
                userEditor.data().clear();
                for (net.luckperms.api.node.Node n : nodesToKeep) {
                    userEditor.data().add(n);
                }
                // 添加新的MVP权限
                userEditor.data().add(node);
            }).thenRun(() -> {
                // 异步操作完成后，在主线程发送消息
                getServer().getScheduler().runTask(this, () -> {
                    player.sendMessage(ChatColor.GREEN + "您的MVP权限已成功设置，有效期为一个月！");
                });
            }).exceptionally(throwable -> {
                getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 设置玩家MVP权限组时出错: " + throwable.getMessage());
                player.sendMessage(ChatColor.RED + "设置MVP权限时出现错误，请联系管理员！");
                return null;
            });
        } catch (Exception e) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 设置玩家MVP权限组时出错: " + e.getMessage());
            player.sendMessage(ChatColor.RED + "设置MVP权限时出现错误，请联系管理员！");
        }
    }

    /**
     * 检查玩家是否拥有VIP权限
     */

    private boolean hasVipPermission(Player player) {
        if (luckPerms == null) {
            return false;
        }

        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                return false;
            }
            // 检查用户是否拥有VIP组的继承权限且未过期
            return user.getInheritedGroups(user.getQueryOptions()).stream()
                    .anyMatch(group -> group.getName().equalsIgnoreCase("vip"));
        } catch (Exception e) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 检查玩家VIP权限时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 延长玩家VIP权限组的有效期
     */

    private void extendVipGroup(Player player) {
        if (luckPerms == null) {
            player.sendMessage(ChatColor.RED + "权限系统未初始化，无法延长VIP权限组！");
            return;
        }
        try {
            // 获取用户
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                player.sendMessage(ChatColor.RED + "无法获取用户信息！");
                return;
            }
            // 创建新的继承节点（将用户添加到VIP组，延长30天有效期）
            InheritanceNode node = InheritanceNode.builder("vip")
                    .expiry(30, java.util.concurrent.TimeUnit.DAYS) // 延长30天有效期
                    .build();
            // 构建修改任务并应用

            luckPerms.getUserManager().modifyUser(player.getUniqueId(), userEditor -> {

                // 使用传统方式移除之前的VIP权限（如果存在）
                // 先收集要保留的节点
                java.util.List<net.luckperms.api.node.Node> nodesToKeep = new java.util.ArrayList<>();
                for (net.luckperms.api.node.Node n : userEditor.data().toCollection()) {
                    if (!(n instanceof InheritanceNode &&
                            ((InheritanceNode) n).getGroupName().equals("vip"))) {
                        nodesToKeep.add(n);
                    }
                }
                // 清空所有节点并重新添加保留的节点
                userEditor.data().clear();
                for (net.luckperms.api.node.Node n : nodesToKeep) {
                    userEditor.data().add(n);
                }
                // 添加新的VIP权限（延长有效期）
                userEditor.data().add(node);
            }).thenRun(() -> {
                // 异步操作完成后，在主线程发送消息
                getServer().getScheduler().runTask(this, () -> {
                    player.sendMessage(ChatColor.GREEN + "您的VIP权限已成功续期，有效期延长一个月！");
                });
            }).exceptionally(throwable -> {
                getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 延长玩家VIP权限组时出错: " + throwable.getMessage());
                player.sendMessage(ChatColor.RED + "延长VIP权限时出现错误，请联系管理员！");
                return null;
            });
        } catch (Exception e) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 延长玩家VIP权限组时出错: " + e.getMessage());
            player.sendMessage(ChatColor.RED + "延长VIP权限时出现错误，请联系管理员！");
        }
    }

    /**
     * 处理离线VIP购买
     */
    private void handleOfflineVipPurchase(String playerName) {
        // 记录到数据库中，待玩家上线时处理
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = createDatabaseConnection("neko_level");
            String query = "INSERT INTO pending_vip_purchases (player_name, purchase_time) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE purchase_time = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, playerName);
            stmt.setLong(2, System.currentTimeMillis());
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 记录离线VIP购买时出错: " + e.getMessage());
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 关闭数据库资源时出错: " + e.getMessage());
            }
        }
    }

    /**
     * 处理支付成功的VIP购买（不扣除猫粮）
     */

    private void handlePaidVipPurchase(Player player) {
        // 直接设置VIP权限组，不扣除猫粮
        setPlayerVipGroup(player);
        player.sendMessage(ChatColor.GREEN + "支付成功啦！杂鱼的VIP激活成功啦。有效期一个月！");
    }

    /**
     * 处理支付成功的MVP购买（不扣除猫粮）
     */

    private void handlePaidMvpPurchase(Player player) {
        // 直接设置MVP权限组，不扣除猫粮
        setPlayerMvpGroup(player);
        player.sendMessage(ChatColor.GREEN + "支付成功啦！杂鱼的MVP激活成功啦。有效期一个月！");
    }

    /**
     * 处理离线MVP购买
     */

    private void handleOfflineMvpPurchase(String playerName) {
        // 记录到数据库中，待玩家上线时处理
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = createDatabaseConnection("neko_level");
            String query = "INSERT INTO pending_vip_purchases (player_name, purchase_time) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE purchase_time = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, playerName);
            stmt.setLong(2, System.currentTimeMillis());
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 记录离线MVP购买时出错: " + e.getMessage());
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 关闭数据库资源时出错: " + e.getMessage());
            }
        }
    }

    /**
     * 检查并处理待处理的VIP购买
     */
    private void checkPendingVipPurchases(Player player) throws SQLException {
        String playerName = player.getName();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = createDatabaseConnection("neko_level");
            String query = "SELECT player_name FROM pending_vip_purchases WHERE player_name = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, playerName);
            rs = stmt.executeQuery();

            if (rs.next()) {
                // 有待处理的VIP购买
                setPlayerVipGroup(player);
                player.sendMessage(ChatColor.GREEN + "杂鱼！你还知道回来呢。你买完VIP怎么跑了");

                // 删除记录
                PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM pending_vip_purchases WHERE player_name = ?");
                deleteStmt.setString(1, playerName);
                deleteStmt.executeUpdate();
                deleteStmt.close();
            }
        } catch (SQLException e) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 检查待处理VIP购买时出错: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 关闭数据库资源时出错: " + e.getMessage());
            }
        }
    }

    /**
     * 检查并处理待处理的MVP购买
     */

    private void checkPendingMvpPurchases(Player player) {
        String playerName = player.getName();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = createDatabaseConnection("neko_level");
            String query = "SELECT player_name FROM pending_vip_purchases WHERE player_name = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, playerName);
            rs = stmt.executeQuery();

            if (rs.next()) {
                // 有待处理的MVP购买
                setPlayerMvpGroup(player);
                player.sendMessage(ChatColor.GREEN + "杂鱼！你还知道回来呢。你买完MVP怎么跑了");

                // 删除记录
                PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM pending_vip_purchases WHERE player_name = ?");
                deleteStmt.setString(1, playerName);
                deleteStmt.executeUpdate();
                deleteStmt.close();
            }
        } catch (SQLException e) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 检查待处理MVP购买时出错: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NekoLobby] 关闭数据库资源时出错: " + e.getMessage());
            }
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

    /**
     * 打开权益购买/充值GUI界面
     */

    private void openRechargeGUI(Player p) {
        // 创建权益购买/充值GUI界面
        Inventory rechargeGUI = Bukkit.createInventory(null, 54, ChatColor.AQUA + "✦ " + ChatColor.BOLD + "权益购买/充值中心" + ChatColor.AQUA + " ✦");
        // 获取玩家名称
        String playerName = p.getName();
        // 从数据库获取玩家信息
        Map<String, Object> levelInfo = getPlayerLevelInfo(playerName);
        int catFood = (Integer) levelInfo.getOrDefault("cat_food", 0);

        // 装饰性玻璃板 - 蓝色边框
        Material blueGlassMat = Material.matchMaterial("STAINED_GLASS_PANE");
        ItemStack blueGlassPane = blueGlassMat != null ?
                new ItemStack(blueGlassMat, 1, (short) 3) :
                new ItemStack(Material.matchMaterial("STAINED_GLASS_PANE"), 1, (short) 3);
        ItemMeta blueGlassMeta = blueGlassPane.getItemMeta();
        blueGlassMeta.setDisplayName(ChatColor.BLUE + "权益中心");
        blueGlassPane.setItemMeta(blueGlassMeta);

        // 装饰性玻璃板 - 青色背景
        Material cyanGlassMat = Material.matchMaterial("STAINED_GLASS_PANE");
        ItemStack cyanGlassPane = cyanGlassMat != null ?
                new ItemStack(cyanGlassMat, 1, (short) 9) :
                new ItemStack(Material.matchMaterial("STAINED_GLASS_PANE"), 1, (short) 9);
        ItemMeta cyanGlassMeta = cyanGlassPane.getItemMeta();
        cyanGlassMeta.setDisplayName(ChatColor.AQUA + " ");
        cyanGlassPane.setItemMeta(cyanGlassMeta);

        // 填充背景
        for (int i = 0; i < 54; i++) {
            rechargeGUI.setItem(i, cyanGlassPane.clone());
        }

        // 设置边框
        int[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53};
        for (int slot : borderSlots) {
            rechargeGUI.setItem(slot, blueGlassPane.clone());
        }

        // VIP权益选项
        Material diamondMat = Material.matchMaterial("DIAMOND");
        ItemStack vipItem = diamondMat != null ?
                new ItemStack(diamondMat) :
                new ItemStack(Material.matchMaterial("DIAMOND"));
        ItemMeta vipMeta = vipItem.getItemMeta();
        vipMeta.setDisplayName(ChatColor.GOLD + "✦ " + ChatColor.BOLD + "VIP权益" + ChatColor.GOLD + " ✦");
        List<String> vipLore = new ArrayList<>();
        vipLore.add(ChatColor.WHITE + "✿ " + ChatColor.GREEN + "价格: " + ChatColor.RED + "300猫粮" + ChatColor.WHITE + " ✿");
        vipLore.add(ChatColor.WHITE + "✿ " + ChatColor.AQUA + "有效期: " + ChatColor.LIGHT_PURPLE + "一个月" + ChatColor.WHITE + " ✿");
        vipLore.add("");
        vipLore.add(ChatColor.YELLOW + "❁ " + ChatColor.ITALIC + "点击购买VIP权限" + ChatColor.YELLOW + " ❁");
        vipMeta.setLore(vipLore);
        vipItem.setItemMeta(vipMeta);
        rechargeGUI.setItem(20, vipItem);

        // VIP购买支付宝
        Material goldIngotMat = Material.matchMaterial("GOLD_INGOT");
        ItemStack payVipItem = goldIngotMat != null ?
                new ItemStack(goldIngotMat) :
                new ItemStack(Material.matchMaterial("GOLD_INGOT")); // Fallback
        ItemMeta payVipMeta = payVipItem.getItemMeta();
        payVipMeta.setDisplayName(ChatColor.GOLD + "✦ " + ChatColor.BOLD + "VIP权益 (支付宝)" + ChatColor.GOLD + " ✦");
        List<String> payVipLore = new ArrayList<>();
        payVipLore.add(ChatColor.WHITE + "✿ " + ChatColor.GREEN + "价格: " + ChatColor.RED + "15元" + ChatColor.WHITE + " ✿");
        payVipLore.add(ChatColor.WHITE + "✿ " + ChatColor.AQUA + "有效期: " + ChatColor.LIGHT_PURPLE + "一个月" + ChatColor.WHITE + " ✿");
        payVipLore.add("");
        payVipLore.add(ChatColor.YELLOW + "❁ " + ChatColor.ITALIC + "点击用支付宝购买VIP权限" + ChatColor.YELLOW + " ❁");
        payVipLore.add("");
        payVipLore.add(ChatColor.RED + "⚠ " + ChatColor.BOLD + "需要真实支付" + ChatColor.RED + " ⚠");
        payVipMeta.setLore(payVipLore);
        payVipItem.setItemMeta(payVipMeta);
        rechargeGUI.setItem(22, payVipItem);

        // VIP购买微信
        Material redstoneMat = Material.matchMaterial("REDSTONE");
        ItemStack wxpayVipItem = redstoneMat != null ?
                new ItemStack(redstoneMat) :
                new ItemStack(Material.matchMaterial("REDSTONE")); // Fallback
        ItemMeta wxpayVipMeta = wxpayVipItem.getItemMeta();
        wxpayVipMeta.setDisplayName(ChatColor.GREEN + "✦ " + ChatColor.BOLD + "VIP权益 (微信)" + ChatColor.GREEN + " ✦");
        List<String> wxpayVipLore = new ArrayList<>();
        wxpayVipLore.add(ChatColor.WHITE + "✿ " + ChatColor.GREEN + "价格: " + ChatColor.RED + "15元" + ChatColor.WHITE + " ✿");
        wxpayVipLore.add(ChatColor.WHITE + "✿ " + ChatColor.AQUA + "有效期: " + ChatColor.LIGHT_PURPLE + "一个月" + ChatColor.WHITE + " ✿");
        wxpayVipLore.add("");
        wxpayVipLore.add(ChatColor.YELLOW + "❁ " + ChatColor.ITALIC + "点击用微信购买VIP权限" + ChatColor.YELLOW + " ❁");
        wxpayVipLore.add("");
        wxpayVipLore.add(ChatColor.RED + "⚠ " + ChatColor.BOLD + "需要真实支付" + ChatColor.RED + " ⚠");
        wxpayVipMeta.setLore(wxpayVipLore);
        wxpayVipItem.setItemMeta(wxpayVipMeta);
        rechargeGUI.setItem(24, wxpayVipItem);

        // MVP权益选项
        Material emeraldMat = Material.matchMaterial("EMERALD");
        ItemStack mvpItem = emeraldMat != null ?
                new ItemStack(emeraldMat) :
                new ItemStack(Material.matchMaterial("EMERALD"));
        ItemMeta mvpMeta = mvpItem.getItemMeta();
        mvpMeta.setDisplayName(ChatColor.AQUA + "✦ " + ChatColor.BOLD + "MVP权益" + ChatColor.AQUA + " ✦");
        List<String> mvpLore = new ArrayList<>();
        mvpLore.add(ChatColor.WHITE + "✿ " + ChatColor.GREEN + "价格: " + ChatColor.RED + "600猫粮" + ChatColor.WHITE + " ✿");
        mvpLore.add(ChatColor.WHITE + "✿ " + ChatColor.AQUA + "有效期: " + ChatColor.LIGHT_PURPLE + "一个月" + ChatColor.WHITE + " ✿");
        mvpLore.add("");
        mvpLore.add(ChatColor.YELLOW + "❁ " + ChatColor.ITALIC + "点击购买MVP权限" + ChatColor.YELLOW + " ❁");
        mvpMeta.setLore(mvpLore);
        mvpItem.setItemMeta(mvpMeta);
        rechargeGUI.setItem(29, mvpItem);

        // MVP购买支付宝
        Material goldNuggetMat = Material.matchMaterial("GOLD_NUGGET");
        ItemStack payMvpItem = goldNuggetMat != null ?
                new ItemStack(goldNuggetMat) :
                new ItemStack(Material.matchMaterial("GOLD_NUGGET")); // Fallback
        ItemMeta payMvpMeta = payMvpItem.getItemMeta();
        payMvpMeta.setDisplayName(ChatColor.AQUA + "✦ " + ChatColor.BOLD + "MVP权益 (支付宝)" + ChatColor.AQUA + " ✦");
        List<String> payMvpLore = new ArrayList<>();
        payMvpLore.add(ChatColor.WHITE + "✿ " + ChatColor.GREEN + "价格: " + ChatColor.RED + "30元" + ChatColor.WHITE + " ✿");
        payMvpLore.add(ChatColor.WHITE + "✿ " + ChatColor.AQUA + "有效期: " + ChatColor.LIGHT_PURPLE + "一个月" + ChatColor.WHITE + " ✿");
        payMvpLore.add("");
        payMvpLore.add(ChatColor.YELLOW + "❁ " + ChatColor.ITALIC + "点击用支付宝购买MVP权限" + ChatColor.YELLOW + " ❁");
        payMvpLore.add("");
        payMvpLore.add(ChatColor.RED + "⚠ " + ChatColor.BOLD + "需要真实支付" + ChatColor.RED + " ⚠");
        payMvpMeta.setLore(payMvpLore);
        payMvpItem.setItemMeta(payMvpMeta);
        rechargeGUI.setItem(31, payMvpItem);

        // MVP购买微信
        Material glowstoneDustMat = Material.matchMaterial("GLOWSTONE_DUST");
        ItemStack wxpayMvpItem = glowstoneDustMat != null ?
                new ItemStack(glowstoneDustMat) :
                new ItemStack(Material.matchMaterial("GLOWSTONE_DUST")); // Fallback
        ItemMeta wxpayMvpMeta = wxpayMvpItem.getItemMeta();
        wxpayMvpMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "✦ " + ChatColor.BOLD + "MVP权益 (微信)" + ChatColor.LIGHT_PURPLE + " ✦");
        List<String> wxpayMvpLore = new ArrayList<>();
        wxpayMvpLore.add(ChatColor.WHITE + "✿ " + ChatColor.GREEN + "价格: " + ChatColor.RED + "30元" + ChatColor.WHITE + " ✿");
        wxpayMvpLore.add(ChatColor.WHITE + "✿ " + ChatColor.AQUA + "有效期: " + ChatColor.LIGHT_PURPLE + "一个月" + ChatColor.WHITE + " ✿");
        wxpayMvpLore.add("");
        wxpayMvpLore.add(ChatColor.YELLOW + "❁ " + ChatColor.ITALIC + "点击用微信购买MVP权限" + ChatColor.YELLOW + " ❁");
        wxpayMvpLore.add("");
        wxpayMvpLore.add(ChatColor.RED + "⚠ " + ChatColor.BOLD + "需要真实支付" + ChatColor.RED + " ⚠");
        wxpayMvpMeta.setLore(wxpayMvpLore);
        wxpayMvpItem.setItemMeta(wxpayMvpMeta);
        rechargeGUI.setItem(33, wxpayMvpItem);


        // 玩家信息显示
        Material playerHeadMat = Material.matchMaterial("SKULL_ITEM");
        ItemStack playerHead = playerHeadMat != null ?
                new ItemStack(playerHeadMat, 1, (short) 3) :
                new ItemStack(Material.matchMaterial("SKULL_ITEM"), 1, (short) 3);
        SkullMeta headMeta = (SkullMeta) playerHead.getItemMeta();
        if (playerHeadMat != null) {
            // 1.12.2及以下版本
            headMeta.setOwner(p.getName());
        } else {
            // 1.13及以上版本
            headMeta.setOwningPlayer(p);
        }
        headMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "★ " + ChatColor.BOLD + "杂鱼信息" + ChatColor.LIGHT_PURPLE + " ★");
        List<String> headLore = new ArrayList<>();
        headLore.add(ChatColor.WHITE + "✿ " + ChatColor.GOLD + "杂鱼: " + ChatColor.YELLOW + p.getName() + ChatColor.WHITE + " ✿");
        headLore.add(ChatColor.WHITE + "✿ " + ChatColor.GOLD + "杂鱼的猫粮: " + ChatColor.LIGHT_PURPLE + catFood + ChatColor.WHITE + " ✿");
        headLore.add("");
        headLore.add(ChatColor.LIGHT_PURPLE + "❀ " + ChatColor.BOLD + "点击刷新杂鱼的信息" + ChatColor.LIGHT_PURPLE + " ❁");
        headMeta.setLore(headLore);
        playerHead.setItemMeta(headMeta);
        rechargeGUI.setItem(49, playerHead);

        // 打开GUI
        p.openInventory(rechargeGUI);
    }

    /**
     * 处理权益购买GUI中的点击事件（更新版本）
     */

    private void handleRechargeGUIInteraction(InventoryClickEvent e) {
        e.setCancelled(true); // 防止玩家拿取物品
        Player player = (Player) e.getWhoClicked();
        // getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby Debug] 玩家 " + player.getName() + " 点击了权益购买GUI");
        ItemStack clickedItem = e.getCurrentItem();

        // 添加点击位置调试信息
        // getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby Debug] 玩家 " + player.getName() + " 点击了槽位 " + e.getSlot());

        // 获取玩家名称
        String playerName = player.getName();

        // 从数据库获取玩家信息
        Map<String, Object> levelInfo = getPlayerLevelInfo(playerName);
        int catFood = (Integer) levelInfo.getOrDefault("cat_food", 0);

        // 如果点击的是空槽位或装饰性物品，则不处理
        Material glassPaneMat = Material.matchMaterial("STAINED_GLASS_PANE");
        boolean isGlassPane = glassPaneMat != null ?
                clickedItem.getType() == glassPaneMat :
                clickedItem.getType() == Material.matchMaterial("STAINED_GLASS_PANE") ||
                        clickedItem.getType() == Material.matchMaterial("STAINED_GLASS_PANE") ||
                        clickedItem.getType() == Material.matchMaterial("STAINED_GLASS_PANE") ||
                        clickedItem.getType() == Material.matchMaterial("STAINED_GLASS_PANE") ||
                        clickedItem.getType() == Material.matchMaterial("STAINED_GLASS_PANE");
        if (clickedItem == null || clickedItem.getType() == Material.AIR || isGlassPane) {
            return;
        }
        // 使用槽位检测来处理VIP权益购买（槽位20）
        if (e.getSlot() == 20) { // VIP权益选项
            // 检查玩家是否有足够的猫粮
            if (catFood < 300) {
                player.sendMessage(ChatColor.RED + "杂鱼杂鱼！你的猫粮都不够呢！");
                return;
            }

            // 扣除猫粮
            deductCatFood(player, 300);
            // 设置玩家VIP权限组
            setPlayerVipGroup(player);
            player.sendMessage(ChatColor.GREEN + "真是大杂鱼呢，你的VIP购买成功了喔！");
            // 重新打开GUI以刷新信息
            openRechargeGUI(player);
            return;
        }

        // 支付宝支付VIP购买（槽位22）
        if (e.getSlot() == 22) { // 支付宝VIP权益选项（现金支付）
            player.sendMessage(ChatColor.YELLOW + "正在为您生成支付宝支付二维码...");
            // 创建Z-Pay支付宝支付订单
            createZPayOrder(player, "alipay");
            return;
        }

        // 微信支付VIP购买（槽位24）
        if (e.getSlot() == 24) { // 微信VIP权益选项（现金支付）
            player.sendMessage(ChatColor.YELLOW + "正在给你创建支付二维码呢杂鱼");
            // 创建Z-Pay微信支付订单
            createZPayOrder(player, "wxpay");
            return;
        }

        // 使用槽位检测来处理MVP权益购买（槽位29）
        if (e.getSlot() == 29) { // MVP权益选项
            // 检查玩家是否有足够的猫粮
            if (catFood < 600) {
                player.sendMessage(ChatColor.RED + "真是杂鱼，你的猫粮都不够就想买吗？");
                return;
            }

            // 扣除猫粮
            deductCatFood(player, 600);
            // 设置玩家MVP权限组
            setPlayerMvpGroup(player);
            player.sendMessage(ChatColor.GREEN + "杂鱼！MVP购买成功啦！");
            // 重新打开GUI以刷新信息
            openRechargeGUI(player);
            return;
        }

        // 支付宝支付MVP购买（槽位31）
        if (e.getSlot() == 31) { // 支付宝MVP权益选项（现金支付）
            player.sendMessage(ChatColor.YELLOW + "正在给你创建支付二维码呢杂鱼");

            // 创建Z-Pay支付宝支付订单
            String orderNo = zPayUtil.generateOrderNo();
            String subject = "梦幻次元-MVP权益(1个月)";
            String amount = "30.00"; // 30元
            String type = "alipay"; // 支付宝
            String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "127.0.0.1";
            String param = "player:" + player.getName(); // 附加参数，包含玩家名称用于后续识别

            // 创建支付订单并获取二维码URL
            String qrCodeUrl = zPayUtil.getPaymentQRCodeUrl(orderNo, subject, amount, type, ip, param);

            if (qrCodeUrl != null && !qrCodeUrl.isEmpty()) {
                // 成功获取二维码URL，显示在地图上
                player.sendMessage(ChatColor.GREEN + "MVP支付订单创建成功！");
                player.sendMessage(ChatColor.YELLOW + "正在生成支付二维码地图...");
                player.closeInventory(); // 关闭GUI

                // 在单独的线程中处理二维码地图渲染，避免阻塞主线程
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    QRCodeMapRenderer.showQRCodeOnMap(player, qrCodeUrl);
                });
            } else {
                player.sendMessage(ChatColor.RED + "真是杂鱼呢，订单创建失败了呢");

                // 添加更多详细错误信息
                String errorDetails = zPayUtil.getLastError();
                if (errorDetails != null && !errorDetails.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "错误详情: " + errorDetails);
                }
            }
            return;
        }

        // 微信支付MVP购买（槽位33）
        if (e.getSlot() == 33) { // 微信MVP权益选项（现金支付）
            player.sendMessage(ChatColor.YELLOW + "正在为您生成微信支付二维码...");
            // 创建Z-Pay微信支付订单
            String orderNo = zPayUtil.generateOrderNo();
            String subject = "梦幻次元-MVP权益(1个月)";
            String amount = "30.00"; // 30元
            String type = "wxpay"; // 微信
            String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "127.0.0.1";
            String param = "player:" + player.getName(); // 附加参数，包含玩家名称用于后续识别

            // 创建支付订单并获取二维码URL
            String qrCodeUrl = zPayUtil.getPaymentQRCodeUrl(orderNo, subject, amount, type, ip, param);
            if (qrCodeUrl != null && !qrCodeUrl.isEmpty()) {
                // 成功获取二维码URL，显示在地图上
                player.sendMessage(ChatColor.GREEN + "MVP支付订单创建成功！");
                player.sendMessage(ChatColor.YELLOW + "正在给你创建支付二维码呢杂鱼");
                player.closeInventory(); // 关闭GUI
                // 在单独的线程中处理二维码地图渲染，避免阻塞主线程
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    QRCodeMapRenderer.showQRCodeOnMap(player, qrCodeUrl);
                });
            } else {
                player.sendMessage(ChatColor.RED + "创建支付订单失败了喔杂鱼~");
                // 添加更多详细错误信息
                String errorDetails = zPayUtil.getLastError();
                if (errorDetails != null && !errorDetails.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "错误详情: " + errorDetails);
                }
            }
            return;
        }

        // 如果点击的是玩家信息头颅，刷新GUI（槽位49）
        if (e.getSlot() == 49) {
            Material playerHeadMat = Material.matchMaterial("SKULL_ITEM");
            if ((playerHeadMat != null && clickedItem.getType() == playerHeadMat) || clickedItem.getType() == Material.matchMaterial("SKULL_ITEM")) {
                // getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby Debug] 玩家 " + player.getName() + " 点击了玩家头颅项目");
                // 重新打开GUI以刷新信息
                openRechargeGUI(player);
                return;
            }
        }
    }
}