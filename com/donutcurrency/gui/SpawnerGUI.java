package com.donutcurrency.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.block.CreatureSpawner;

import com.donutcurrency.DonutCurrencyPlugin;
import com.donutcurrency.managers.EconomyManager;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class SpawnerGUI implements Listener {
    private DonutCurrencyPlugin plugin;
    private Player player;
    private EconomyManager economyManager;
    private Map<String, Double> spawnerPrices;
    private Map<String, EntityType> spawnerTypes;

    public SpawnerGUI(DonutCurrencyPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.economyManager = plugin.getEconomyManager();
        this.spawnerPrices = new HashMap<>();
        this.spawnerTypes = new HashMap<>();

        // Initialize spawner prices from config
        initializeSpawners();

        // Register listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void initializeSpawners() {
        // Get prices from config or set defaults
        double zombiePrice = plugin.getConfig().getDouble("spawners.zombie-price", 50000.0);
        double skeletonPrice = plugin.getConfig().getDouble("spawners.skeleton-price", 55000.0);
        double creeperPrice = plugin.getConfig().getDouble("spawners.creeper-price", 60000.0);

        spawnerPrices.put("Zombie", zombiePrice);
        spawnerPrices.put("Skeleton", skeletonPrice);
        spawnerPrices.put("Creeper", creeperPrice);

        spawnerTypes.put("Zombie", EntityType.ZOMBIE);
        spawnerTypes.put("Skeleton", EntityType.SKELETON);
        spawnerTypes.put("Creeper", EntityType.CREEPER);
    }

    public void openShop() {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Spawner Shop" + ChatColor.DARK_GRAY + "]");

        // Fill background
        ItemStack background = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bgMeta = background.getItemMeta();
        bgMeta.setDisplayName(" ");
        background.setItemMeta(bgMeta);

        for (int i = 0; i < 27; i++) {
            if (i != 10 && i != 12 && i != 14) {
                inv.setItem(i, background);
            }
        }

        // Add spawner items
        inv.setItem(10, createSpawnerItem("Zombie", spawnerPrices.get("Zombie"), Material.ZOMBIE_SPAWN_EGG));
        inv.setItem(12, createSpawnerItem("Skeleton", spawnerPrices.get("Skeleton"), Material.SKELETON_SPAWN_EGG));
        inv.setItem(14, createSpawnerItem("Creeper", spawnerPrices.get("Creeper"), Material.CREEPER_SPAWN_EGG));

        player.openInventory(inv);
    }

    private ItemStack createSpawnerItem(String type, double price, Material eggMaterial) {
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();

        List<String> lore = new ArrayList<>();
        String currencyName = plugin.getConfig().getString("currency.name", "Dong");
        String currencySymbol = plugin.getConfig().getString("currency.symbol", "🪙");

        meta.setDisplayName(ChatColor.GOLD + "Lồng " + type);
        lore.add(ChatColor.GRAY + "Giá: " + ChatColor.YELLOW + price + " " + currencySymbol + " " + currencyName);
        lore.add("");
        lore.add(ChatColor.GREEN + "Click để mua");

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getName().contains("Spawner Shop")) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();

            String spawnerType = null;
            switch (slot) {
                case 10:
                    spawnerType = "Zombie";
                    break;
                case 12:
                    spawnerType = "Skeleton";
                    break;
                case 14:
                    spawnerType = "Creeper";
                    break;
                default:
                    return;
            }

            buySpawner(player, spawnerType);
        }
    }

    private void buySpawner(Player player, String type) {
        double price = spawnerPrices.get(type);
        double balance = economyManager.getBalance(player.getUniqueId());
        String currencyName = plugin.getConfig().getString("currency.name", "Dong");
        String currencySymbol = plugin.getConfig().getString("currency.symbol", "🪙");

        if (balance < price) {
            player.sendMessage(ChatColor.RED + "Bạn không đủ tiền! Cần: " + ChatColor.YELLOW + price + " " + currencySymbol + " " + currencyName + ChatColor.RED + ", Hiện có: " + ChatColor.YELLOW + balance);
            return;
        }

        // Deduct money
        economyManager.deductBalance(player.getUniqueId(), price);

        // Create spawner item
        ItemStack spawner = createSpawnerBlockItem(type);

        // Give to player
        player.getInventory().addItem(spawner);

        // Send message
        player.sendMessage(ChatColor.GREEN + "✓ Bạn đã mua lồng " + type + " thành công!");
        player.sendMessage(ChatColor.GRAY + "Đã trừ: " + ChatColor.YELLOW + price + " " + currencySymbol + " " + currencyName);
        player.sendMessage(ChatColor.GRAY + "Số dư hiện tại: " + ChatColor.YELLOW + economyManager.getBalance(player.getUniqueId()));

        // Update action bar
        player.sendActionBar(ChatColor.GREEN + "Mua thành công lồng " + type);
    }

    private ItemStack createSpawnerBlockItem(String type) {
        ItemStack spawner = new ItemStack(Material.SPAWNER);
        BlockStateMeta meta = (BlockStateMeta) spawner.getItemMeta();

        // Set spawner type
        CreatureSpawner state = (CreatureSpawner) meta.getBlockState();
        state.setSpawnedType(spawnerTypes.get(type));
        state.update();

        meta.setBlockState(state);
        meta.setDisplayName(ChatColor.GOLD + "Lồng " + type);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Loại: " + ChatColor.YELLOW + type);
        lore.add(ChatColor.GRAY + "Đặt xuống để tạo lồng " + type);

        meta.setLore(lore);
        spawner.setItemMeta(meta);

        return spawner;
    }
}
