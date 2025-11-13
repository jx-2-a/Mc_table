// src/main/java/com/jinxi/scoreboard/listener/ScoreListener.java
package com.jinxi.scoreboard.listener;

import com.jinxi.scoreboard.MobItemScorerPlugin;
import com.jinxi.scoreboard.data_center.PlayerStats;
import com.jinxi.scoreboard.style_board.table_type;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
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
    stats.addTempKill(mobBonusScore); // 精英怪额外加分
    // plugin.getLogger().info(killer.getName() + " 击杀了 " + entityType + 
    //                            "，获得 " + mobBonusScore + " 分");
    String message = "§6⚔ §e" + killer.getName() + " §7击杀了 §b" + getMobDisplayName(entityType) + " §8| §a+" + mobBonusScore + "分";
    Bukkit.broadcastMessage(message);
}

    // 5. 玩家捡起物品 - 只记录珍贵物品
@EventHandler
public void onPickup(PlayerPickupItemEvent e) {
    Player player = e.getPlayer();
    ItemStack item = e.getItem().getItemStack();
    int value_ietm = isValuableItem(item);
    // 只记录珍贵物品
    if (value_ietm>0) {
        PlayerStats stats = plugin.getStats(player);
        stats.addTempItem(value_ietm* item.getAmount());
        
        // plugin.getLogger().info(player.getName() + " 捡起珍贵物品: " + item.getType() + " × " + item.getAmount() +
                    //    "，获得 " + (value_ietm * item.getAmount()) + " 分");
        String message = "§6⚔ §e" + player.getName() + " 捡起珍贵物品 §b" + getItemName(item) + " §8| §a+" + (value_ietm * item.getAmount()) + "分";
        Bukkit.broadcastMessage(message);
    }
}
// 判断是否为珍贵物品
private int isValuableItem(ItemStack item) {
    // 构建配置路径
    String path = "scoreboard.Material_value_map." + item.toString();
    
    // 从配置读取分数，如果不存在则返回0
    int score = plugin.getConfig().getInt(path, 0);
    
    return score;
}

public int getMobScore(EntityType entityType) {
    // 构建配置路径
    String path = "scoreboard.boss_scores." + entityType.toString();
    
    // 从配置读取分数，如果不存在则返回0
    int score = plugin.getConfig().getInt(path, 0);
    
    return score;
}
public String getMobDisplayName(EntityType entityType) {
    // 构建配置路径
    String path = "scoreboard.boss_names." + entityType.toString();
    
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
    String path = "scoreboard.Material_value_name." + entityType.toString();
    
    // 从配置读取显示名称，如果不存在则返回默认名称
    String displayName = plugin.getConfig().getString(path);
    
    if (displayName == null || displayName.isEmpty()) {
        // 如果没有配置，返回实体类型的默认名称
        return entityType.toString();
    }
    
    return displayName;
}
}