package com.jinxi.scoreboard;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
// import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

public class MobItemScorerPlugin extends JavaPlugin {

    private static final String OBJ_NAME = "mobScore";

    @Override
    public void onEnable() {
        getLogger().info("MobItemScorer enabling...");

        // 创建或获取主 scoreboard 上的 objective
        ScoreboardManager mgr = Bukkit.getScoreboardManager();
        if (mgr == null) {
            getLogger().severe("ScoreboardManager is null!");
            return;
        }
        Scoreboard main = mgr.getMainScoreboard();
        Objective obj = main.getObjective(OBJ_NAME);
        if (obj == null) {
            // 注册新 objective，display name 为 "Mob Score"
            obj = main.registerNewObjective(OBJ_NAME, "dummy", Component.text("Mob Score"));
        }
        // 把它设置为全服侧边栏（所有玩家可见）
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // 在 console 打印当前 objective
        getLogger().info("Objective '" + OBJ_NAME + "' ready. DisplaySlot: " + obj.getDisplaySlot());

        // 注册命令处理器（在 plugin.yml 中声明）
        this.getCommand("mobscore").setExecutor(this);

        getLogger().info("MobItemScorer enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MobItemScorer disabled.");
    }

    // /mobscore add <player> <amount>
    // /mobscore set <player> <amount>
    // /mobscore get <player>
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("mobscore")) return false;
        if (args.length < 2) {
            sender.sendMessage("Usage: /mobscore <add|set|get> <player> [amount]");
            return true;
        }

        String action = args[0].toLowerCase();
        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null && !action.equals("get")) {
            sender.sendMessage("Player not online: " + targetName);
            return true;
        }

        ScoreboardManager mgr = Bukkit.getScoreboardManager();
        if (mgr == null) {
            sender.sendMessage("ScoreboardManager unavailable.");
            return true;
        }
        Scoreboard main = mgr.getMainScoreboard();
        Objective obj = main.getObjective(OBJ_NAME);
        if (obj == null) {
            sender.sendMessage("Objective not found.");
            return true;
        }

        switch (action) {
            case "add":
                if (args.length < 3) {
                    sender.sendMessage("Usage: /mobscore add <player> <amount>");
                    return true;
                }
                int add;
                try { add = Integer.parseInt(args[2]); } catch (NumberFormatException e) {
                    sender.sendMessage("Amount must be integer.");
                    return true;
                }
                Score sAdd = obj.getScore(target.getName());
                sAdd.setScore(sAdd.getScore() + add);
                sender.sendMessage("Added " + add + " to " + target.getName() + ". Now: " + sAdd.getScore());
                if (target.isOnline()) target.sendMessage("§a+ " + add + " points");
                return true;

            case "set":
                if (args.length < 3) {
                    sender.sendMessage("Usage: /mobscore set <player> <amount>");
                    return true;
                }
                int val;
                try { val = Integer.parseInt(args[2]); } catch (NumberFormatException e) {
                    sender.sendMessage("Amount must be integer.");
                    return true;
                }
                Score sSet = obj.getScore(target.getName());
                sSet.setScore(val);
                sender.sendMessage("Set " + target.getName() + " to " + val);
                if (target != null && target.isOnline()) target.sendMessage("§eYour score set to " + val);
                return true;

            case "get":
                // allow getting offline via scoreboard too (scoreboard only stores names present)
                if (args.length < 2) {
                    sender.sendMessage("Usage: /mobscore get <player>");
                    return true;
                }
                // try online player first
                if (target != null) {
                    int score = obj.getScore(target.getName()).getScore();
                    sender.sendMessage(target.getName() + " has " + score + " points.");
                    return true;
                } else {
                    // if offline, we can still query the scoreboard by name (if present)
                    Score sc = obj.getScore(targetName);
                    sender.sendMessage(targetName + " has " + sc.getScore() + " points (may be 0).");
                    return true;
                }

            default:
                sender.sendMessage("Unknown action. Use add/set/get.");
                return true;
        }
    }
}
