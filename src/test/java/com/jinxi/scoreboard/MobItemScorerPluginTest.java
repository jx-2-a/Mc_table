// src/test/java/com/jinxi/scoreboard/MobItemScorerPluginTest.java
package com.jinxi.scoreboard;

import com.jinxi.scoreboard.data_center.PlayerStats;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MobItemScorerPluginTest {

    private MobItemScorerPlugin plugin;
    private File dataFolder;
    private Player mockPlayer;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        System.out.println("=== 开始设置测试环境 ===");
        
        // 模拟 JavaPlugin
        plugin = mock(MobItemScorerPlugin.class, CALLS_REAL_METHODS);
        dataFolder = tempDir.toFile();
        System.out.println("临时数据文件夹: " + dataFolder.getAbsolutePath());

        // 模拟 getDataFolder()
        when(plugin.getDataFolder()).thenReturn(dataFolder);

        // 模拟 Player
        mockPlayer = mock(Player.class);
        UUID playerId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        System.out.println("模拟玩家 UUID: " + playerId);
        
        System.out.println("=== 测试环境设置完成 ===\n");
    }

    @Test
    void testGetStatsAndSaveLoad() {
        System.out.println("🧪 开始测试: testGetStatsAndSaveLoad - 数据保存和加载功能");
        
        // 1. 获取玩家数据（自动创建）
        System.out.println("1. 获取玩家数据（自动创建）");
        PlayerStats stats = plugin.getStats(mockPlayer);
        assertNotNull(stats, "获取的PlayerStats不应为null");
        System.out.println("   ✅ 成功获取PlayerStats实例");

        // 2. 加分
        System.out.println("2. 添加各类分数");
        System.out.println("   添加伤害分数: 100.5");
        stats.addDamageScore(100.5);
        System.out.println("   添加击杀分数: 3");
        stats.addKillScore(3);
        System.out.println("   添加物品分数: 10");
        stats.addItemScore(10);

        // 3. 验证总分
        System.out.println("3. 验证总分计算");
        double expectedTotal = 100.5 + (3 * 10) + (10 * 5); // 伤害 + 击杀*10 + 物品*5
        int actualTotal = stats.getTotalScore();
        System.out.println("   期望总分: " + expectedTotal + " → 整数: " + (int)expectedTotal);
        System.out.println("   实际总分: " + actualTotal);
        assertEquals((int)expectedTotal, actualTotal, "总分计算不正确");
        System.out.println("   ✅ 总分验证通过");

        // 4. 保存
        System.out.println("4. 保存所有数据到文件");
        plugin.saveAllStats();
        
        // 验证文件存在
        File dataFile = new File(dataFolder, "players.yml");
        boolean fileExists = dataFile.exists();
        System.out.println("   数据文件存在: " + fileExists + " (" + dataFile.getAbsolutePath() + ")");
        assertTrue(fileExists, "数据文件应该被创建");
        System.out.println("   ✅ 数据文件保存成功");

        // 5. 清空内存
        System.out.println("5. 清空内存中的数据");
        int beforeClearSize = plugin.getPlayerStatsMap().size();
        plugin.getPlayerStatsMap().clear();
        int afterClearSize = plugin.getPlayerStatsMap().size();
        System.out.println("   清空前数据量: " + beforeClearSize + " → 清空后: " + afterClearSize);
        assertEquals(0, afterClearSize, "清空后地图应该为空");
        System.out.println("   ✅ 内存数据清空成功");

        // 6. 重新加载
        System.out.println("6. 从文件重新加载数据");
        plugin.loadOnlinePlayersStats();
        int afterLoadSize = plugin.getPlayerStatsMap().size();
        System.out.println("   重新加载后数据量: " + afterLoadSize);
        System.out.println("   ✅ 数据加载完成");

        // 7. 验证数据恢复
        System.out.println("7. 验证数据完整性");
        PlayerStats loaded = plugin.getPlayerStatsMap().get(mockPlayer.getUniqueId());
        assertNotNull(loaded, "重新加载后应该能找到玩家数据");
        System.out.println("   ✅ 成功找到重新加载的玩家数据");
        
        // 验证具体数值
        System.out.println("   验证伤害分数: 期望=100.5, 实际=" + loaded.getDamageScore());
        assertEquals(100.5, loaded.getDamageScore(), 0.01, "伤害分数不匹配");
        
        System.out.println("   验证击杀分数: 期望=3, 实际=" + loaded.getKillScore());
        assertEquals(3, loaded.getKillScore(), "击杀分数不匹配");
        
        System.out.println("   验证物品分数: 期望=10, 实际=" + loaded.getItemScore());
        assertEquals(10, loaded.getItemScore(), "物品分数不匹配");
        
        System.out.println("   验证总分: 期望=" + (int)expectedTotal + ", 实际=" + loaded.getTotalScore());
        assertEquals((int)expectedTotal, loaded.getTotalScore(), "总分不匹配");
        
        System.out.println("   验证显示状态: 期望=true, 实际=" + loaded.isVisible());
        assertTrue(loaded.isVisible(), "默认显示状态应该为true");
        
        System.out.println("🎉 所有数据验证通过！\n");
    }

    @Test
    void testVisibilityToggleAndSave() {
        System.out.println("🧪 开始测试: testVisibilityToggleAndSave - 显示状态切换和保存");
        
        // 初始获取和设置
        System.out.println("1. 获取玩家数据并设置显示状态为false");
        PlayerStats stats = plugin.getStats(mockPlayer);
        stats.setVisible(false);
        System.out.println("   设置显示状态: " + stats.isVisible());
        
        // 保存
        System.out.println("2. 保存数据");
        plugin.saveAllStats();
        File dataFile = new File(dataFolder, "players.yml");
        System.out.println("   数据文件存在: " + dataFile.exists());
        
        // 清空并重新加载
        System.out.println("3. 清空内存并重新加载");
        plugin.getPlayerStatsMap().clear();
        plugin.loadOnlinePlayersStats();
        
        // 验证
        System.out.println("4. 验证显示状态持久化");
        PlayerStats loaded = plugin.getPlayerStatsMap().get(mockPlayer.getUniqueId());
        assertNotNull(loaded, "重新加载后应该能找到玩家数据");
        
        boolean isVisible = loaded.isVisible();
        System.out.println("   重新加载后的显示状态: " + isVisible);
        assertFalse(isVisible, "显示状态应该保持为false");
        
        System.out.println("🎉 显示状态持久化测试通过！\n");
    }
}