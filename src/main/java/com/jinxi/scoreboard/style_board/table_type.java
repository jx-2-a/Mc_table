package com.jinxi.scoreboard.style_board;

import com.jinxi.scoreboard.MobItemScorerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.*;

public class table_type {
    
    private final MobItemScorerPlugin plugin;
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final String OBJ_NAME;
    private boolean visible;         // 控制显示/隐藏
    private boolean autoUpdate;      // 自动更新计分板
    private int updateInterval;        // 更新间隔(ticks)
    
    public table_type(MobItemScorerPlugin plugin)  {
        this.plugin = plugin;

        // 从配置文件读取值
        this.OBJ_NAME = plugin.getConfig().getString("scoreboard.objective_name", "default_stats");
        this.visible = plugin.getConfig().getBoolean("scoreboard.visible", true);
        this.autoUpdate = plugin.getConfig().getBoolean("scoreboard.auto_update", true);
        this.updateInterval = plugin.getConfig().getInt("scoreboard.update_interval", 20);
        String displayName = plugin.getConfig().getString("scoreboard.title", "&d&l✦ &5计分榜 &d&l✦");

        ScoreboardManager mgr = Bukkit.getScoreboardManager();
        this.scoreboard = mgr.getMainScoreboard();

        // 转换颜色代码 (& → §)
        displayName = ChatColor.translateAlternateColorCodes('&', displayName);
        // 创建/获取 Objective
        objective = scoreboard.getObjective(OBJ_NAME);
        if (objective == null) {
            objective = scoreboard.registerNewObjective(OBJ_NAME, "dummy", displayName);
        }
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        plugin.getLogger().info("§a计分板表格已加载！");
        startUpdater(); // 启动定时刷新
    }
    
    // 启动每秒刷新
    private void startUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (visible) {
                    updateTable();
                }
            }
        }.runTaskTimer(plugin, updateInterval, updateInterval); // 每秒更新一次
    }
    
    // 🔥 核心：更新整个表格
    private void updateTable() {
        clearScores(); // 清空旧数据
        
        // 表头 + 分隔线
        setLine(15, "§6§l玩家 §7§l| §c§l总伤害 §7§l| §e§l击杀");
        setLine(14, "§7§m---------------------");
        
        // 获取所有在线玩家，按总伤害降序排序
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.sort((p1, p2) -> Double.compare(
            plugin.getPlayerDamage(p2), plugin.getPlayerDamage(p1)));
        
        int rank = 13;
        for (Player p : players) {
            if (rank < 3) break; // 只显示前10名（留3行给底部）
            
            double damage = plugin.getPlayerDamage(p);
            int kills = plugin.getPlayerKills(p);
            
            // 格式化一行：玩家名(8格) | 伤害(6位) | 击杀(4位)
            String line = formatLine(p.getName(), (int)damage, kills);
            setLine(rank--, line);
        }
        
        // 底部信息
        setLine(2, "§7§m---------------------");
        setLine(1, "§a总在线: §f" + Bukkit.getOnlinePlayers().size());
    }
    
    // 🎨 格式化一行（完美对齐）
    private String formatLine(String name, int damage, int kills) {
        // 玩家名截取8字符 + 左对齐
        if (name.length() > 8) name = name.substring(0, 8);
        name = ChatColor.GREEN + String.format("%-8s", name);
        
        // 数字右对齐 + 颜色
        String dmgStr = ChatColor.RED + String.format("%6d", damage);
        String killStr = ChatColor.YELLOW + String.format("%4d", kills);
        
        return name + " §7| " + dmgStr + " §7| " + killStr;
    }
    
    // 设置第N行（score值控制顺序）
    private void setLine(int score, String text) {
        // 清理重复行
        for (String entry : scoreboard.getEntries()) {
            Score s = objective.getScore(entry);
            if (s.getScore() == score) {
                scoreboard.resetScores(entry);
            }
        }
        
        // 创建假玩家条目（长度限制32字符）
        String entryName = ChatColor.translateAlternateColorCodes('&', 
            "&" + score + text.substring(0, Math.min(28, text.length())));
        
        OfflinePlayer fakePlayer = Bukkit.getOfflinePlayer(entryName);
        objective.getScore(fakePlayer).setScore(score);
    }
    
    // 清空所有动态行
    private void clearScores() {
        scoreboard.getEntries().forEach(scoreboard::resetScores);
    }
    
    // 🔧 公开方法：开关表格
    public void toggleVisibility() {
        visible = !visible;
        if (!visible) {
            clearScores();
            objective.setDisplaySlot(null);
        } else {
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            updateTable();
        }
        plugin.getLogger().info("表格可见性: " + (visible ? "开启" : "关闭"));
    }
    
    public boolean isVisible() {
        return visible;
    }
}