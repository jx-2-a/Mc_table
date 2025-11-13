package com.jinxi.scoreboard.style_board;

import com.jinxi.scoreboard.MobItemScorerPlugin;
import com.jinxi.scoreboard.data_center.PlayerStats;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.*;

public class table_type {
    private static table_type instance;
    private final MobItemScorerPlugin plugin;
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final String OBJ_NAME;
    private boolean visible;         // 控制显示/隐藏
    private int updateInterval;        // 更新间隔(ticks)
    private boolean enabled = true; // 默认启用
    private BukkitRunnable updaterTask;
    
    public table_type(MobItemScorerPlugin plugin)  {
        this.plugin = plugin;
        instance = this;
        plugin.getLogger().info("§a计分板 table_type 已创建！");
        // 从配置文件读取值
        this.OBJ_NAME = plugin.getConfig().getString("scoreboard.objective_name", "default_stats");
        this.visible = plugin.getConfig().getBoolean("scoreboard.visible", true);
        this.updateInterval = plugin.getConfig().getInt("scoreboard.update_interval", 20);
        String displayName = plugin.getConfig().getString("scoreboard.title", "&d&l✦ &5计分榜 &d&l✦");
        

        ScoreboardManager mgr = Bukkit.getScoreboardManager();
        this.scoreboard = mgr.getMainScoreboard();

        // 转换颜色代码 (& → §)
        displayName = ChatColor.translateAlternateColorCodes('&', displayName);
        // 创建/获取 Objective
        // 使用临时变量
        Objective tempObjective = scoreboard.getObjective(OBJ_NAME);
        if (tempObjective == null) {
            tempObjective = scoreboard.registerNewObjective(OBJ_NAME, "dummy", displayName);
        }
        // 一次性赋值给 final 字段
        this.objective = tempObjective;
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        plugin.getLogger().info("§a计分板表格已加载！");
        startUpdater(); // 启动定时刷新
    }
    public static table_type getInstance() { return instance; }

    // 启动每秒刷新
    private void startUpdater() {
        // 先关闭已有任务
        stopUpdater();
        
        updaterTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (visible) {
                    updateTable();
                }
            }
        };
        updaterTask.runTaskTimer(plugin, updateInterval, updateInterval);
    }
    // 关闭定时刷新任务
    private void stopUpdater() {
        if (updaterTask != null) {
            updaterTask.cancel();
            updaterTask = null;
        }
    }
    
    // 🔥 核心：更新整个表格
    private void updateTable() {
        clearScores(); // 清空旧数据
        
        // 表头
        setLine(15, "§6§l玩家 §7§l| §c§l伤害 §7§l| §e§l击杀 §7§l| §a§l物品 §7§l| §b§l总分");
        setLine(14, "§7§m-----------------------------------");
        
        // 读取内存数据并排序（总分降序）
        List<Map.Entry<UUID, PlayerStats>> list = new ArrayList<>(plugin.playerStatsMap.entrySet());
        list.removeIf(e -> Bukkit.getPlayer(e.getKey()) == null); // 只显示在线玩家
        list.sort((a, b) -> Integer.compare(b.getValue().getTotalScore(), a.getValue().getTotalScore()));

        int rank = 13;
        for (Map.Entry<UUID, PlayerStats> e : list) {
            if (rank < 3) break; // 最多 10 条记录（留 2 行给底部）

            Player p = Bukkit.getPlayer(e.getKey());
            String name = p != null ? p.getName() : "离线玩家";
            PlayerStats ps = e.getValue();

            String line = formatLine(
                    name,
                    (int) ps.getDamageScore(),
                    ps.getKillScore(),
                    ps.getItemScore(),
                    ps.getTotalScore()
            );
            setLine(rank--, line);
        }
        // 底部信息
        setLine(2, "§7§m---------------------");
        setLine(1, "§a总在线: §f" + Bukkit.getOnlinePlayers().size());
    }
    
    private static final int NAME_WIDTH = 8;
    private static final int DMG_WIDTH = 5;
    private static final int KILL_WIDTH = 4;
    private static final int ITEM_WIDTH = 4;
    private static final int TOTAL_WIDTH = 5;

    private String formatLine(String name, int dmg, int kill, int item, int total) {
        // 更智能的名字截断（保留可见字符）
        String formattedName = ChatColor.GREEN + formatPlayerName(name, NAME_WIDTH);
        
        String dmgStr  = ChatColor.RED    + String.format("%" + DMG_WIDTH + "d", dmg);
        String killStr = ChatColor.YELLOW + String.format("%" + KILL_WIDTH + "d", kill);
        String itemStr = ChatColor.AQUA   + String.format("%" + ITEM_WIDTH + "d", item);
        String totalStr= ChatColor.GOLD   + String.format("%" + TOTAL_WIDTH + "d", total);

        return String.join(" §7| ", formattedName, dmgStr, killStr, itemStr, totalStr);
    }

    private String formatPlayerName(String name, int width) {
        // 移除颜色代码后计算真实长度
        String plainName = ChatColor.stripColor(name);
        if (plainName.length() > width) {
            return plainName.substring(0, width - 1) + "…";
        }
        return String.format("%-" + width + "s", plainName);
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
    

    // 🔧 公开方法：切换启用状态
    public void toggleEnabled() {
        setEnabled(!this.enabled);
    }

    public boolean isEnabled() {
        // 🔧 公开方法：检查是否启用
        return this.enabled;
    }
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        
        this.enabled = enabled;
        
        if (!enabled) {
            // 完全隐藏计分板
            hideCompletely();
            // 可选：停止更新任务
            stopUpdater();
        } else {
            showCompletely();
            // 可选：重启更新任务
            startUpdater();
        }
    }
    public void hideCompletely() {
        // 移除侧边栏显示
        if (objective != null) {
            objective.setDisplaySlot(null);
        }
        // 更新状态
        this.visible = false;
        
        plugin.getLogger().info("§c计分板已关闭");
    }
    public void showCompletely() {
        // 打开侧边栏显示
        if (objective != null) {
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            }
        // 更新状态
        this.visible = true;
        plugin.getLogger().info("§c计分板已开启");
    }
    public boolean isVisible() {
        return visible;
    }
    public void updateForPlayer(Player player) {
        // 🔧 公开方法：为单个玩家更新计分板
        if (!visible) {
            return; // 全局禁用，直接返回
        }
        
        // 直接启用玩家的计分板
        player.setScoreboard(this.scoreboard);
        plugin.getLogger().info("已为玩家 " + player.getName() + " 启用计分板");
    }
    public void hideForPlayer(Player player) {
        Scoreboard board = player.getScoreboard();
        Objective obj = board.getObjective(DisplaySlot.SIDEBAR);
        if (obj != null) {
            obj.setDisplaySlot(null);
        }
        plugin.getLogger().info("已为玩家 " + player.getName() + " 关闭计分板");
    }
}