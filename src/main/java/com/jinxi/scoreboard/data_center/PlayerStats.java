package com.jinxi.scoreboard.data_center;  // ← 改成这个！

public class PlayerStats {
    private double damageScore = 0;
    private int killScore = 0;
    private int itemScore = 0;
    private int totalScore = 0;
    private boolean visible = true;  // 新增：个人开关

    // 暂存数据（本轮统计）
    private double tempDamage = 0;
    private int tempKills = 0;
    private int tempItems = 0;
    // 添加暂存方法
      public void addTempDamage(double amount) {
        this.tempDamage += amount;
    }
    
    public void addTempKill(int amount) {
        this.tempKills += amount;
    }
    
    public void addTempItem(int amount) {
        this.tempItems += amount;
    }
    public void clean_all() {
        this.tempDamage = 0;
        this.tempKills = 0;
        this.tempItems = 0;
        this.damageScore = 0;
        this.killScore = 0;
        this.itemScore = 0;
        this.totalScore = 0;
    }
    
    // 提交暂存数据到总数据
    public void commitTempData() {
        this.damageScore += tempDamage;
        this.killScore += tempKills;
        this.itemScore += tempItems;
        recalcTotal();
        // 归零暂存数据
        resetTempData();
    }
    
    // 重置暂存数据
    public void resetTempData() {
        this.tempDamage = 0;
        this.tempKills = 0;
        this.tempItems = 0;
    }
    
    // 获取暂存数据（用于显示）
    public double getTempDamage() { return tempDamage; }
    public int getTempKills() { return tempKills; }
    public int getTempItems() { return tempItems; }
    

    public synchronized double getDamageScore() { return damageScore; }
    public synchronized void addDamageScore(double v) {
        damageScore += v;
        recalcTotal();
    }

    public synchronized int getKillScore() { return killScore; }
    public synchronized void addKillScore(int v) {
        killScore += v;
        recalcTotal();
    }

    public synchronized int getItemScore() { return itemScore; }
    public synchronized void addItemScore(int v) {
        itemScore += v;
        recalcTotal();
    }

    public synchronized int getTotalScore() { return totalScore; }

    public synchronized boolean isVisible() { return visible; }
    public synchronized void setVisible(boolean visible) { this.visible = visible; }

    private void recalcTotal() {
        totalScore = (int) Math.round(damageScore * 1.0) + killScore * 1 + itemScore * 1;
    }
}