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
import java.util.stream.Collectors;

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
    private final TableFormatter tableFormatter;
    // 缓存每行的 Team，避免重复创建
    private final Map<Integer, Team> lineTeams = new HashMap<>();
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

        
        this.tableFormatter = new TableFormatter();

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

    private void updateTable() {
    clearScores();

    // Step 1: 提交所有暂存数据
    for (PlayerStats stats : plugin.playerStatsMap.values()) {
        stats.commitTempData();
    }

    // Step 2: 构建 allRows（表头 + 分隔线 + 数据）
    List<String[]> allRows = new ArrayList<>();

    // 数据行
    for (Map.Entry<UUID, PlayerStats> e : plugin.playerStatsMap.entrySet()) {
        Player p = Bukkit.getPlayer(e.getKey());
        PlayerStats ps = e.getValue();
        if (p == null || !ps.isVisible()) continue;

        allRows.add(new String[]{
            truncatePlayerName(p.getName(), 50),
            String.valueOf((int) ps.getDamageScore()),
            String.valueOf(ps.getKillScore()),
            String.valueOf(ps.getItemScore()),
            String.valueOf(ps.getTotalScore())
        });
    }
    // Step 3: 更新 formatter
    tableFormatter.updateData(allRows.toArray(new String[0][]));
    // Step 4: 取得 **完整已对齐矩阵**
    List<String[]> matrix = tableFormatter.getFormattedMatrix();
    // Step 1: 过滤出玩家数据行（最后一列是数字）
    List<String[]> playerRows = new ArrayList<>();
    for (String[] row : matrix) {
        if (row.length == 0) continue;
        String scoreStr = row[row.length - 1].trim();
        if (scoreStr.matches("\\d+")) {  // 是纯数字
            playerRows.add(row);
        }
    }

    // Step 2: 按总分降序排序（高分在前）
    playerRows.sort((a, b) -> {
        int scoreA = Integer.parseInt(a[a.length - 1].trim());
        int scoreB = Integer.parseInt(b[b.length - 1].trim());
        return Integer.compare(scoreB, scoreA);  // 降序
    });

    // Step 3: 写入 scoreboard（最多 16 行，从上到下）
    int physicalLine = 15;  // 从最顶行开始
    for (int i = 0; i < Math.min(playerRows.size(), 16); i++) {
        String[] row = playerRows.get(i);

        // 提取总分作为 score
        int totalScore = Integer.parseInt(row[row.length - 1].trim());

        // 内容部分（去掉最后一列）
        String[] content = Arrays.copyOf(row, row.length - 1);
        String fullLine = String.join(" | ", content);

        // 写入：physicalLine 决定位置，totalScore 决定排序
        setLineWithScore(physicalLine--, fullLine, totalScore);
    }
}
    private String truncatePlayerName(String name, int maxWidth) {
        if (name.length() <= maxWidth) return name;
        return name.substring(0, maxWidth - 3) + "...";
    }
    private static final String[] LINE_ENTRIES = {
        "§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7",
        "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f"
    };
    private void setLineWithScore(int physicalLine, String text, int score) {
    if (physicalLine < 0 || physicalLine > 15) return;

    String entry = LINE_ENTRIES[physicalLine];

    Team team = lineTeams.computeIfAbsent(physicalLine, l -> {
        String teamName = "sb" + l;
        Team t = scoreboard.getTeam(teamName);
        if (t == null) {
            t = scoreboard.registerNewTeam(teamName);
        }
        t.addEntry(entry);
        return t;
    });

    // 设置文本
    team.setPrefix(text);
    team.setSuffix("");
    

    // 关键：用真实总分作为 score（高分在上！）
    objective.getScore(entry).setScore(score);
}

    // =============================
    // 清空所有行（只清除 entry，不删 team）
    // =============================
private void clearScores() {
    // 清除所有使用过的entry的分数
    for (String entry : scoreboard.getEntries()) {
        scoreboard.resetScores(entry);
    }

}

    // 🔧 公开方法：切换启用状态
    public void toggleEnabled(boolean isues) {
        setEnabled(isues);
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
        // 确保 Objective 显示在侧边栏
    if (objective != null) {
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }
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

    private class TableFormatter {
    private int[] colWidths;
    private int numCols;
    private String[][] currentRows;

    private static final String PADDING = " ";

    public TableFormatter() {}

   public void updateData(String[][] allRows) {
    if (allRows == null || allRows.length == 0) {
        currentRows = null; colWidths = null; numCols = 0;
        return;
    }
    this.currentRows = allRows;
    this.numCols = allRows[0].length;

    colWidths = new int[numCols];
    for (String[] row : allRows) {
        for (int i = 0; i < Math.min(row.length, numCols); i++) {
            colWidths[i] = Math.max(colWidths[i], row[i].length());
        }
    }
}

// 格式化行数据（应用补零对齐）
public String[] getFormattedRow(int index) {
    if (currentRows == null || index < 0 || index >= currentRows.length) {
        return new String[0];
    }
    String[] src = currentRows[index];
    String[] out = new String[numCols];

    for (int i = 0; i < numCols; i++) {
        String val = i < src.length ? src[i] : "";
        
        // 对第2、3、4列的数字进行补零处理
        if (i >= 1 && i <= 3 && isNumeric(val)) {
            // 直接使用colWidths[i]作为目标长度进行补零
            val = padNumberWithZero(val, colWidths[i]);
        }
        
        int pad = colWidths[i] - val.length();
        out[i] = val + PADDING.repeat(Math.max(0, pad));
    }
    return out;
}

// 判断是否为数字
private boolean isNumeric(String str) {
    if (str == null || str.trim().isEmpty()) return false;
    return str.matches("\\d+");
}

// 数字补零
private String padNumberWithZero(String number, int targetLength) {
    if (number.length() >= targetLength) return number;
    return "0".repeat(targetLength - number.length()) + number;
}

    /* -------------------------------------------------------------
       3. 返回 **完整矩阵** List<String[]>（每行是一个已对齐的 String[]）
       ------------------------------------------------------------- */
    public List<String[]> getFormattedMatrix() {
        List<String[]> matrix = new ArrayList<>();
        if (currentRows == null) return matrix;

        for (int i = 0; i < currentRows.length; i++) {
            matrix.add(getFormattedRow(i));
        }
        return matrix;
    }
}
}
