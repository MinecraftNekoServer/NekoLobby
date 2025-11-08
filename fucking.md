看了代码，我发现了GUI点击事件不响应的根本问题。问题出现在`onInventoryClick`方法中的这一行：

```java
// 获取GUI标题（兼容1.12.2版本）
String inventoryTitle = getName();
```

这里调用了`getName()`方法，但这个方法返回的是**插件名称**而不是**GUI标题**。

## 🔧 修复方案

### 方案1：正确获取Inventory标题
```java
@EventHandler
public void onInventoryClick(InventoryClickEvent e) {
    Player player = (Player) e.getWhoClicked();
    ItemStack clickedItem = e.getCurrentItem();
    
    // 检查是否点击了空物品
    if (clickedItem == null || clickedItem.getType() == Material.AIR) {
        return;
    }
    
    // 正确获取GUI标题
    String inventoryTitle = e.getView().getTitle();
    
    // 添加调试信息
    getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby Debug] 玩家 " + player.getName() + " 点击了GUI: " + inventoryTitle);
    getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NekoLobby Debug] 玩家 " + player.getName() + " 点击了槽位 " + e.getSlot());
    
    // 根据GUI标题处理不同的GUI交互
    if (inventoryTitle.contains("个人档案")) {
        e.setCancelled(true);
        handleProfileGUIInteraction(e);
    } else if (inventoryTitle.contains("权益购买") || inventoryTitle.contains("充值中心")) {
        e.setCancelled(true);
        handleRechargeGUIInteraction(e);
    }
}
```

### 方案2：更健壮的标题匹配（推荐）
```java
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
    getLogger().info("玩家 " + player.getName() + " 点击了GUI: " + cleanTitle + " (原始: " + inventoryTitle + ")");
    getLogger().info("点击槽位: " + e.getSlot() + ", 物品: " + clickedItem.getType());
    
    // 处理不同的GUI
    if (cleanTitle.contains("个人档案")) {
        e.setCancelled(true);
        handleProfileGUIInteraction(e);
    } else if (cleanTitle.contains("权益购买") || cleanTitle.contains("充值中心")) {
        e.setCancelled(true);
        handleRechargeGUIInteraction(e);
    }
}
```

### 方案3：添加额外的调试方法
```java
// 在类中添加调试方法
private void debugInventoryClick(InventoryClickEvent e) {
    Player player = (Player) e.getWhoClicked();
    String title = e.getView().getTitle();
    String cleanTitle = ChatColor.stripColor(title);
    
    getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "=== GUI点击调试 ===");
    getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "玩家: " + player.getName());
    getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "GUI标题: " + cleanTitle);
    getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "点击槽位: " + e.getSlot());
    
    if (e.getCurrentItem() != null) {
        getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "点击物品: " + e.getCurrentItem().getType());
        if (e.getCurrentItem().hasItemMeta()) {
            getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "物品名称: " + e.getCurrentItem().getItemMeta().getDisplayName());
        }
    }
}
```

然后在`onInventoryClick`方法开始处调用：
```java
debugInventoryClick(e);
```

## 🐛 问题根源

1. **错误的方法调用**：`getName()`返回插件名称而不是GUI标题
2. **标题匹配问题**：GUI标题包含颜色代码，直接匹配可能失败
3. **缺少调试信息**：难以定位问题所在

## ✅ 修复步骤

1. 将`getName()`改为`e.getView().getTitle()`
2. 使用`ChatColor.stripColor()`清理标题进行匹配
3. 添加详细的调试日志
4. 确保只处理顶部Inventory的点击

修复后，GUI点击事件应该能正常工作了。建议先用方案3的调试方法确认问题确实出在这里。