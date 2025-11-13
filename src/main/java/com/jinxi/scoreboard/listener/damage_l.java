package com.jinxi.scoreboard.listener;

import com.jinxi.scoreboard.MobItemScorerPlugin;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class damage_l implements Listener {
    
    private final MobItemScorerPlugin plugin;
    
    public MobDamageListener(MobItemScorerPlugin plugin) {
        this.plugin = plugin;
    }
    
    // 🔥 实时计算对怪物伤害
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Monster)) return;
        
        // 累加最终伤害（已减防）
        double damage = event.getFinalDamage();
        double total = plugin.getPlayerDamage(player) + damage;
        plugin.setPlayerDamage(player, total);
        
        // 可选：玩家头顶伤害数字
        // player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
        //     new TextComponent("§c-%.0f".formatted(damage)));
    }
    
    // 💀 怪物死亡 +1 击杀
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player killer)) return;
        if (!(event.getEntity() instanceof Monster)) return;
        
        int kills = plugin.getPlayerKills(killer) + 1;
        plugin.setPlayerKills(killer, kills);
        
        killer.sendMessage("§a+1 击杀！ §e当前: " + kills);
    }
}