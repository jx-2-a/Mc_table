package com.jinxi.scoreboard;

import com.jinxi.scoreboard.style_board.table_type;
import com.jinxi.scoreboard.data_center.PlayerStats;
import com.jinxi.scoreboard.listener.ScoreListener;

// import net.kyori.adventure.text.Component;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
// import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
// import org.bukkit.scoreboard.*;



public class MobItemScorerPlugin extends JavaPlugin {
    // 全局缓存：每个玩家一个 PlayerStats
    public final Map<UUID, PlayerStats> playerStatsMap = new ConcurrentHashMap<>();
    public table_type tableType; // 声明实例变量

    @Override
    public void onEnable() {
        getLogger().info("计分板 启动中...");
        this.saveDefaultConfig();
        loadOnlinePlayersStats();                // 启动时读取 player_stats.yml
        getLogger().info("配置文件 已加载！");
        // 注册唯一监听器
        getServer().getPluginManager().registerEvents(new ScoreListener(this), this);
        // 启动计分板
        tableType = new table_type(this);
        getLogger().info("计分板 加载完成！");
    }

    @Override
    public void onDisable() {
        saveAllStats();                // 关闭时保存数据
        getLogger().info("§c计分板插件已关闭！数据已保存。");
    }

// 读取在线玩家数据（启动时调用）
public void loadOnlinePlayersStats() {
    File file = new File(getDataFolder(), "players.yml");
    if (!file.exists()) return;

    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
    
    int loadedCount = 0;
    
    // 只加载当前在线玩家的数据
    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
        UUID uuid = onlinePlayer.getUniqueId();
        String uuidStr = uuid.toString();
        
        if (cfg.contains(uuidStr)) {
            try {
                var sec = cfg.getConfigurationSection(uuidStr);
                PlayerStats stats = new PlayerStats();

                stats.addDamageScore(sec.getDouble("damage", 0));
                stats.addKillScore(sec.getInt("kills", 0));
                stats.addItemScore(sec.getInt("items", 0));
                stats.setVisible(sec.getBoolean("visible", true));

                playerStatsMap.put(uuid, stats);
                loadedCount++;
                
            } catch (Exception e) {
                getLogger().warning("加载玩家 " + onlinePlayer.getName() + " 数据失败: " + e.getMessage());
            }
        } else {
            // 在线玩家但没有数据文件，创建新数据
            playerStatsMap.put(uuid, new PlayerStats());
        }
    }
    
    getLogger().info("§a已加载 " + loadedCount + " 个在线玩家的数据！");
}

// 动态加载指定玩家的数据（玩家加入时调用）
public void loadPlayerStats(UUID playerId) {
    File file = new File(getDataFolder(), "players.yml");
    if (!file.exists()) {
        // 文件不存在，创建新数据
        playerStatsMap.put(playerId, new PlayerStats());
        return;
    }

    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
    String uuidStr = playerId.toString();
    
    if (cfg.contains(uuidStr)) {
        try {
            var sec = cfg.getConfigurationSection(uuidStr);
            PlayerStats stats = new PlayerStats();

            stats.addDamageScore(sec.getDouble("damage", 0));
            stats.addKillScore(sec.getInt("kills", 0));
            stats.addItemScore(sec.getInt("items", 0));
            stats.setVisible(sec.getBoolean("visible", true));

            playerStatsMap.put(playerId, stats);
            getLogger().info("已动态加载玩家 " + playerId + " 的数据");
            
        } catch (Exception e) {
            getLogger().warning("动态加载玩家 " + playerId + " 数据失败: " + e.getMessage());
            // 加载失败，创建新数据
            playerStatsMap.put(playerId, new PlayerStats());
        }
    } else {
        // 玩家没有数据文件，创建新数据
        playerStatsMap.put(playerId, new PlayerStats());
    }
    }

// 保存所有玩家数据（关闭时或定时调用）
public void saveAllStats() {
    YamlConfiguration cfg = new YamlConfiguration();
    for (Map.Entry<UUID, PlayerStats> e : playerStatsMap.entrySet()) {
        String path = e.getKey().toString();
        PlayerStats stats = e.getValue();
        cfg.set(path + ".damage", stats.getDamageScore());
        cfg.set(path + ".kills", stats.getKillScore());
        cfg.set(path + ".items", stats.getItemScore());
        cfg.set(path + ".visible", stats.isVisible());  // 保存开关
    }
    try {
        cfg.save(new File(getDataFolder(), "players.yml"));
        getLogger().severe("保存玩家数据成功！");
    } catch (IOException ex) {
        getLogger().severe("保存玩家数据失败！");
    }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
        sender.sendMessage("§c此命令只能由玩家使用！");
        return true;
    }

    if (!command.getName().equalsIgnoreCase("score_table")) {
        return false;
    }

    if (args.length == 0) {
        player.sendMessage("§c使用方法: §f/score_table <on|off|rule>");
        return true;
    }

    String action = args[0].toLowerCase();

    return switch (action) {
        case "clean_all" -> {
                // 检查权限
                if (!player.hasPermission("scoreboard.admin")) {
                    player.sendMessage("§c你没有权限执行此命令！");
                    yield true;
                }
                
                // 遍历所有玩家的统计数据并重置
                int resetCount = 0;
                for (PlayerStats stats : playerStatsMap.values()) {
                    stats.clean_all();
                    resetCount++;
                }
                
                player.sendMessage("§a已重置 " + resetCount + " 名玩家的所有计分数据！");
                yield true;
            }
        case "open" -> handleopen();
        case "close" -> handleclose();
        case "on" -> handleOn(player);
        case "off" -> handleOff(player);
        case "rule" -> handleRule(player);
        case "rule_kill" ->handlerule_kill(player);
        case "rule_damage" -> handlerule_damage(player);
        case "rule_item" -> handlerule_item(player);
        default -> {
            player.sendMessage("§c未知参数！使用: on, off, rule, rule_kill, rule_damage, rule_item");
            yield true;
        }
    };
    
    }
    private boolean handleopen() {
        tableType.toggleEnabled(true); // 立即刷新
        return true;
    }

    private boolean handleclose() {
        tableType.toggleEnabled(false); // 立即隐藏
        return true;
    }
    private boolean handlerule_kill(Player player) {
        player.sendMessage("§e§l=== 击杀计分表 ===");
        player.sendMessage("§c杀一个你就知道了！");
        player.sendMessage("§a===================");
        return true;
    }
    private boolean handlerule_damage(Player player) {
        player.sendMessage("§e§l=== 伤害计分表 ===");
        player.sendMessage("§造成的绝对伤害！");
        player.sendMessage("§a===================");
        return true;
    }
    private boolean handlerule_item(Player player) {
        player.sendMessage("§e§l=== 伤害计分表 ===");
        player.sendMessage("§计分的会播报的，拿就知道了！");
        player.sendMessage("§a===================");
        return true;
    }

    private boolean handleOn(Player player) {
        PlayerStats stats = getStats(player);
        stats.setVisible(true);
        player.sendMessage("§a计分板已开启！");
        tableType.updateForPlayer(player); // 立即刷新
        return true;
    }

    private boolean handleOff(Player player) {
        PlayerStats stats = getStats(player);
        stats.setVisible(false);
        player.sendMessage("§c计分板已关闭！");
        tableType.hideForPlayer(player); // 立即隐藏
        return true;
    }

    private boolean handleRule(Player player) {
        player.sendMessage("§e§l=== 计分表简介 ===");
        player.sendMessage("§c第一项伤害分: §f" + "玩家造成伤害时获得" + "具体使用rule_damage命令查看");
        player.sendMessage("§e第二项击杀分: §f" + "玩家造成击杀时获得" + "具体使用rule_kill命令查看");
        player.sendMessage("§b第三项物品分: §f" + "玩家获取物品时获得" + "具体使用rule_item命令查看");
        player.sendMessage("§6总分 = 伤害 + 击杀 + 物品");
        player.sendMessage("§a===================");
        return true;
    }
    // 在 MobItemScorerPlugin.java 中添加这个方法
    public PlayerStats getStats(Player player) {
        return playerStatsMap.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerStats());
    }
    // 必须有这个方法！（测试用）
    public Map<UUID, PlayerStats> getPlayerStatsMap() {
        return playerStatsMap;
    }
     // 检查玩家是否可见
    public boolean isPlayerVisible(Player player) {
        PlayerStats stats = playerStatsMap.get(player.getUniqueId());
        return stats != null && stats.isVisible();
    }


}
