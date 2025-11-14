// src/main/java/com/jinxi/scoreboard/listener/ScoreListener.java
package com.jinxi.scoreboard.listener;

import com.jinxi.scoreboard.MobItemScorerPlugin;
import com.jinxi.scoreboard.data_center.PlayerStats;
import com.jinxi.scoreboard.style_board.table_type;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class ScoreListener implements Listener {

    private final MobItemScorerPlugin plugin;

    public ScoreListener(MobItemScorerPlugin plugin) {
        this.plugin = plugin;
    }

    // 1. 玩家登录 → 恢复计分板显示
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID playerId = p.getUniqueId();
        // 从文件加载玩家的分数数据到内存
        plugin.loadPlayerStats(playerId);
        boolean visible = plugin.isPlayerVisible(p);
        if (visible) {
            plugin.tableType.updateForPlayer(p);
        } else {
            plugin.tableType.hideForPlayer(p);
        }
    }

    // 2. 玩家退出 → 可选清理
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // 1. 保存玩家的分数数据到文件
        plugin.saveAllStats();
        
        // 2. 从内存中清理数据（可选，为了节省内存）
        plugin.getPlayerStatsMap().remove(playerId);
        
        plugin.getLogger().info("已保存并清理玩家 " + player.getName() + " 的数据");
    }

    // 3. 玩家造成伤害
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player player)) return;
        if (e.getFinalDamage() <= 0) return;

        PlayerStats stats = plugin.getStats(player);
        stats.addTempDamage(e.getFinalDamage());
        
    }

    // 4. 玩家击杀实体
    @EventHandler
public void onKill(EntityDeathEvent e) {
    Player killer = e.getEntity().getKiller();
    if (killer == null) return;

    PlayerStats stats = plugin.getStats(killer);

    // 记录击杀的怪物类型
    EntityType entityType = e.getEntityType();
    int mobBonusScore = getMobScore(entityType);
    if (mobBonusScore>0) {
    stats.addTempKill(mobBonusScore); // 精英怪额外加分
    // plugin.getLogger().info(killer.getName() + " 击杀了 " + entityType + 
    //                            "，获得 " + mobBonusScore + " 分");
    String message = "§6⚔ §e" + killer.getName() + " §7击杀了 §b" + getMobDisplayName(entityType) + " §8| §a+" + mobBonusScore + "分";
    // Broadcast without using deprecated API
    Bukkit.getOnlinePlayers().forEach(pl -> pl.sendMessage(message));
}
}

    // 5. 玩家捡起物品 - 只记录珍贵物品
@EventHandler
public void onPickup(PlayerPickupItemEvent e) {
    Player player = e.getPlayer();
    ItemStack item = e.getItem().getItemStack();
    String materialName = item.getType().name();
    int count = item.getAmount();
    int value_ietm = isValuableItem(player,item);
    // 只记录珍贵物品
    if (value_ietm>0) {
        PlayerStats stats = plugin.getStats(player);
        stats.addTempItem(value_ietm* item.getAmount());
        
        // plugin.getLogger().info(player.getName() + " 捡起珍贵物品: " + item.getType() + " × " + item.getAmount() +
        //                "，获得 " + (value_ietm * item.getAmount()) + " 分");
        String message = "§6⚔ §e" + player.getName() + " 捡起珍贵物品 §b" + materialName + " × " + count + " §8| §a+" + (value_ietm * item.getAmount()) + "分";
        Bukkit.getOnlinePlayers().forEach(pl -> pl.sendMessage(message));
    }
}
private Map<UUID, Set<Long>> playerScoredItems = new HashMap<>();

private int isValuableItem(Player player, ItemStack item) {
    if (item == null) return -1;
    
    String materialName = item.getType().name();
    String path = "Material_value_map." + materialName;
    
    if (!plugin.getConfig().contains(path)) {
        return -1;
    }
    
    int score = plugin.getConfig().getInt(path, 0);
    
    // 获取物品创建时间戳（近似值）
    long itemTimestamp = getItemCreationTimestamp(item);
    UUID playerId = player.getUniqueId();
    
    // 检查玩家是否已经对这个物品计分过
    if (playerScoredItems.getOrDefault(playerId, new HashSet<>()).contains(itemTimestamp)) {
        return 0;
    }
    
    // 记录已计分
    playerScoredItems.computeIfAbsent(playerId, k -> new HashSet<>()).add(itemTimestamp);
    return score;
}

// 获取物品创建时间戳
private long getItemCreationTimestamp(ItemStack item) {
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "creation_time");
        
        if (container.has(key, PersistentDataType.LONG)) {
            return container.get(key, PersistentDataType.LONG);
        }
        
        // 如果没有时间戳，创建并保存
        long timestamp = System.currentTimeMillis();
        container.set(key, PersistentDataType.LONG, timestamp);
        item.setItemMeta(meta);
        return timestamp;
    }
    
    return System.currentTimeMillis();
}

public int getMobScore(EntityType entityType) {
    // 构建配置路径
    String path = "boss_scores." + entityType.toString();
    // 从配置读取分数，如果不存在则返回0
    int score = plugin.getConfig().getInt(path, 0);
    return score;
}
public String getMobDisplayName(EntityType entityType) {
    // 构建配置路径
    String path = "boss_names." + entityType.toString();
    
    // 从配置读取显示名称，如果不存在则返回默认名称
    String displayName = plugin.getConfig().getString(path);
    
    if (displayName == null || displayName.isEmpty()) {
        // 如果没有配置，返回实体类型的默认名称
        return entityType.toString();
    }
    
    return displayName;
}
public String getItemName(ItemStack entityType) {
    // 构建配置路径
    String path = "Material_value_name." + entityType.toString();
    
    // 从配置读取显示名称，如果不存在则返回默认名称
    String displayName = plugin.getConfig().getString(path);
    
    if (displayName == null || displayName.isEmpty()) {
        // 如果没有配置，返回实体类型的默认名称
        return entityType.toString();
    }
    
    return displayName;
}
}