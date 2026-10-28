package com.elyra.economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public class Main extends JavaPlugin implements Listener {

    private final String GUI_TITLE = ChatColor.DARK_GREEN + "Put items here to SELL";

    @Override
    public void onEnable() {
        getLogger().info("ElyraEconomy (Advanced Buy/Sell/Pay) 1.8.8 enabled!");
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("ElyraEconomy disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by players!");
            return true;
        }

        Player player = (Player) sender;

        // ==========================================
        // 1. PAY COMMAND LOGIC
        // ==========================================
        if (cmd.getName().equalsIgnoreCase("pay")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Correct usage: /pay <player> <amount>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "That player is currently offline!");
                return true;
            }
            try {
                double amount = Double.parseDouble(args[1]);
                if (amount <= 0) {
                    player.sendMessage(ChatColor.RED + "Amount must be greater than 0!");
                    return true;
                }
                // Balance system connection placeholder
                player.sendMessage(ChatColor.GREEN + "You sent " + ChatColor.YELLOW + target.getName() + ChatColor.GREEN + " " + ChatColor.GOLD + "$" + amount + ChatColor.GREEN + "!");
                target.sendMessage(ChatColor.GREEN + "You received " + ChatColor.GOLD + "$" + amount + ChatColor.GREEN + " from " + ChatColor.YELLOW + player.getName() + ChatColor.GREEN + "!");
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Please provide a valid numeric amount!");
            }
            return true;
        }

        // ==========================================
        // 2. BUY COMMAND LOGIC
        // ==========================================
        if (cmd.getName().equalsIgnoreCase("buy")) {
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Correct usage: /buy <item> [amount]");
                return true;
            }
            player.sendMessage(ChatColor.YELLOW + "The Buy GUI/Shop feature is coming soon!");
            return true;
        }

        // ==========================================
        // 3. SELL COMMAND LOGIC (ALL FEATURES)
        // ==========================================
        if (cmd.getName().equalsIgnoreCase("sell")) {
            
            // Feature A: /sell (Empty command -> Opens Chest GUI)
            if (args.length == 0) {
                Inventory sellGui = Bukkit.createInventory(null, 27, GUI_TITLE);
                player.openInventory(sellGui);
                player.sendMessage(ChatColor.YELLOW + "Place items inside the chest and close it to sell them.");
                return true;
            }

            String subCommand = args[0].toLowerCase();

            // Feature B: /sell hand
            if (subCommand.equalsIgnoreCase("hand")) {
                ItemStack item = player.getItemInHand();
                if (item == null || item.getType() == Material.AIR) {
                    player.sendMessage(ChatColor.RED + "You are not holding any item in your hand!");
                    return true;
                }
                double price = getItemPrice(item.getType());
                if (price <= 0) {
                    player.sendMessage(ChatColor.RED + "This item cannot be sold to the server shop!");
                    return true;
                }
                double total = price * item.getAmount();
                player.setItemInHand(new ItemStack(Material.AIR));
                player.sendMessage(ChatColor.GREEN + "Hand item sold successfully! Earnings: " + ChatColor.GOLD + "$" + total);
                return true;
            }

            // Feature C: /sell inventory
            if (subCommand.equalsIgnoreCase("inventory")) {
                double totalEarnings = 0;
                ItemStack[] contents = player.getInventory().getContents();
                for (int i = 0; i < contents.length; i++) {
                    ItemStack item = contents[i];
                    if (item != null && item.getType() != Material.AIR) {
                        double price = getItemPrice(item.getType());
                        if (price > 0) {
                            totalEarnings += (price * item.getAmount());
                            player.getInventory().setItem(i, new ItemStack(Material.AIR));
                        }
                    }
                }
                if (totalEarnings > 0) {
                    player.sendMessage(ChatColor.GREEN + "All sellable items from your inventory have been sold! Total: " + ChatColor.GOLD + "$" + totalEarnings);
                } else {
                    player.sendMessage(ChatColor.RED + "No sellable items were found in your inventory!");
                }
                return true;
            }

            // Feature D: /sell <item> [amount]
            Material mat = Material.matchMaterial(subCommand.toUpperCase());
            if (mat == null) {
                player.sendMessage(ChatColor.RED + "Invalid item name! Example: /sell diamond 64");
                return true;
            }
            double price = getItemPrice(mat);
            if (price <= 0) {
                player.sendMessage(ChatColor.RED + "This specific item cannot be sold!");
                return true;
            }
            int amountToSell = 1;
            if (args.length >= 2) {
                try {
                    amountToSell = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Please enter a valid numeric amount!");
                    return true;
                }
            }
            if (player.getInventory().containsAtLeast(new ItemStack(mat), amountToSell)) {
                player.getInventory().removeItem(new ItemStack(mat, amountToSell));
                player.sendMessage(ChatColor.GREEN + "You successfully sold " + ChatColor.YELLOW + amountToSell + "x " + mat.name() + ChatColor.GREEN + "! Total Earnings: " + ChatColor.GOLD + "$" + (price * amountToSell));
            } else {
                player.sendMessage(ChatColor.RED + "You do not have enough items in your inventory!");
            }
            return true;
        }

        return false;
    }

    // ==========================================
    // CHEST GUI REMOVE / CLOSE EVENT (AUTO SELL)
    // ==========================================
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // String check compatible with older 1.8 Spigot API variants
        if (!event.getInventory().getTitle().equals(GUI_TITLE)) return;
        
        Player player = (Player) event.getPlayer();
        double totalGuiEarnings = 0;
        Inventory inv = event.getInventory();

        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                double price = getItemPrice(item.getType());
                if (price > 0) {
                    totalGuiEarnings += (price * item.getAmount());
                } else {
                    // Return non-sellable items back safely
                    HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(item);
                    if (!leftOver.isEmpty()) {
                        // Drop on ground if player inventory is full
                        for (ItemStack dropItem : leftOver.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), dropItem);
                        }
                    }
                }
            }
        }
        
        if (totalGuiEarnings > 0) {
            player.sendMessage(ChatColor.GREEN + "Items inside the chest GUI were sold! Total Earnings: " + ChatColor.GOLD + "$" + totalGuiEarnings);
        }
    }

    private double getItemPrice(Material material) {
        return getConfig().getDouble("prices." + material.name(), 0.0);
    }
                                   }
                          
