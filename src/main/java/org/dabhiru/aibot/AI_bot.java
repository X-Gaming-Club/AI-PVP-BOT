package org.dabhiru.aibot;

import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.enchantments.Enchantment;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.mcmonkey.sentinel.SentinelTrait;

import java.util.List;
import java.util.stream.Collectors;

public class AI_bot extends JavaPlugin implements Listener {
    NPCRegistry registry;
    SentinelTrait sentinel;
    //private static  int coins;
    double initialArmorPoints;
    double playerDamage;
    //NPC bot;
    double armorDurabilityPercentage;
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("spawnbot").setExecutor(new SpawnBotCommand());
        getCommand("equipbot").setExecutor(new EquipBotCommand());
        getCommand("startnpc").setExecutor(new StartNPCCommand());
        registry = CitizensAPI.getNPCRegistry();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public class SpawnBotCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                int numberOfBots = 1;

                if (args.length > 0) {
                    try {
                        numberOfBots = Integer.parseInt(args[0]);
                    } catch (NumberFormatException e) {
                        player.sendMessage("Invalid number format. Please enter a valid number.");
                        return false;
                    }
                }

                for (int i = 0; i < numberOfBots; i++) {
                    NPC npc = registry.createNPC(EntityType.PLAYER, "Bot");
                    npc.spawn(player.getLocation());
                    npc.addTrait(SentinelTrait.class);
                    SentinelTrait sentinel = npc.getOrAddTrait(SentinelTrait.class);
                    sentinel.setHealth(20);
                    sentinel.setInvincible(false);
                    //setNearestPlayerAsTarget(npc);
                }
                player.sendMessage(numberOfBots + " bot(s) spawned.");
                return true;
            }
            return false;
        }



    }


    public class EquipBotCommand implements CommandExecutor {
        private ItemStack createItemStack(String itemName, int enchantmentLevel) {
            Material material = Material.getMaterial(itemName);
            if (material == null) return null;

            ItemStack itemStack = new ItemStack(material);
            //ItemMeta meta = itemStack.getItemMeta();
            //if (meta != null) {
                for (Enchantment enchantment : Enchantment.values()) {
                    itemStack.addUnsafeEnchantment(enchantment, enchantmentLevel);
                }
                //itemStack.setItemMeta(meta);


            return itemStack;
        }
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length < 2) {
                sender.sendMessage("Usage: /equipbot <material> <level>");
                return false;
            }

            //Equipment equipment = npc.getOrAddTrait(Equipment.class);

            // Equip the bot with specified armor type

            String type = args[0].toUpperCase();
            int level;
            try {
                level = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage("Invalid level format. Please enter a valid number.");
                return false;
            }

            for (NPC npc : CitizensAPI.getNPCRegistry()) {
                if (npc.hasTrait(SentinelTrait.class)) {
                    Equipment equipment = npc.getOrAddTrait(Equipment.class);
                    ItemStack helmet = createItemStack(type + "_HELMET", level);
                    ItemStack chestplate = createItemStack(type + "_CHESTPLATE", level);
                    ItemStack leggings = createItemStack(type + "_LEGGINGS", level);
                    ItemStack boots = createItemStack(type + "_BOOTS", level);
                    ItemStack sword = createItemStack(type + "_SWORD", level);

                    equipment.set(Equipment.EquipmentSlot.HELMET, helmet);
                    equipment.set(Equipment.EquipmentSlot.CHESTPLATE, chestplate);
                    equipment.set(Equipment.EquipmentSlot.LEGGINGS, leggings);
                    equipment.set(Equipment.EquipmentSlot.BOOTS, boots);
                    equipment.set(Equipment.EquipmentSlot.HAND, sword);

                    //ItemStack item = new ItemStack(material);
                    sender.sendMessage("Bots equipped with " + type + " and enchantments at level " + level);
                }
            }

            return true;
        }
    }
    public class StartNPCCommand implements CommandExecutor {

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {

                startTargetingTask();
                sender.sendMessage("NPCs are now targeting the nearest players.");
                return true;
            }
            return false;
        }
    }
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        updateTargets(player);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player target = (Player) event.getEntity();
            Player attacker = (Player) event.getDamager();
            updateTargets(target, attacker);
        }
    }
    public Player getNearestPlayer(Location location) {
        List<Player> players = Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getWorld().equals(location.getWorld()))
                .collect(Collectors.toList());

        Player nearestPlayer = null;
        double nearestDistanceSquared = Double.MAX_VALUE;

        for (Player player : players) {
            double distanceSquared = player.getLocation().distanceSquared(location);
            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearestPlayer = player;
            }
        }

        return nearestPlayer;
    }

    private void updateTargets(Player... excludePlayers) {
 //       List<Player> excludeList = List.of(excludePlayers);
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (npc.hasTrait(SentinelTrait.class)) {
                SentinelTrait sentinel = npc.getOrAddTrait(SentinelTrait.class);
                Player nearestPlayer = getNearestPlayer(npc.getEntity().getLocation());

                if (nearestPlayer != null) {
                    //sentinel.targetingHelper.clearAllTargets();
                    //sentinel.targetingHelper.addTarget(nearestPlayer);
                    //sentinel.chaseTarget = nearestPlayer;
                    sentinel.addTarget(String.valueOf(nearestPlayer));
                    sentinel.faceLocation(nearestPlayer.getLocation().add(0,1.7,0));
                    npc.getNavigator().setTarget(nearestPlayer,true);
                }
            }
        }
    }

    private void setNearestPlayerAsTarget(NPC npc) {
        if (npc.hasTrait(SentinelTrait.class)) {
            SentinelTrait sentinel = npc.getOrAddTrait(SentinelTrait.class);
            Player nearestPlayer = getNearestPlayer(npc.getEntity().getLocation());

            if (nearestPlayer != null) {
                sentinel.addTarget(String.valueOf(nearestPlayer));
                sentinel.faceLocation(nearestPlayer.getLocation().add(0,1.7,0));
                npc.getNavigator().setTarget(nearestPlayer,true);
            }
        }
    }



    private void startTargetingTask() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                if (npc.hasTrait(SentinelTrait.class) && npc.isSpawned()) {
                    Entity entity = npc.getEntity();
                    if (entity != null && entity instanceof Player) {
                        Player botplayer = (Player) entity;
                        getLogger().info("name: "+botplayer.getName());
                        Location botLocation = botplayer.getLocation();
                        getLogger().info("Location: "+botLocation);
                        if (botLocation != null) {
                            Player nearestPlayer = getNearestPlayer(botLocation);
                            getLogger().info("Nearest Player: "+nearestPlayer);
                            if (nearestPlayer != null) {
                                SentinelTrait sentinel = npc.getOrAddTrait(SentinelTrait.class);
                                sentinel.chaseRange = 20000;
                                sentinel.attackRate = 1;
                                sentinel.range = 10;
                                sentinel.accuracy = 0;
                                sentinel.allowKnockback = true;
                                //sentinel.speed = 2.1;
                                sentinel.fightback = true;
                                sentinel.attackRate = 2;
                                sentinel.speed = 1.5;
                                sentinel.addTarget("event:pvp");
                                sentinel.addTarget("UUID:" + nearestPlayer.getUniqueId());
                                //sentinel.addTarget(String.valueOf(nearestPlayer));
                                sentinel.faceLocation(nearestPlayer.getLocation().add(0, 1.7, 0));
                                npc.getNavigator().setTarget(nearestPlayer, true);
                            }

                        } else {
                            Bukkit.getLogger().warning("Bot player location is null for NPC: " + npc.getName());
                        }
                    } else {
                        Bukkit.getLogger().warning("Bot entity is null or not a player for NPC: " + npc.getName());
                    }
                }
            }
        }, 0L, 20L * 15); // Runs every tick (20 ticks per second)
    }

    private double calculateNPCDamage(NPC npc) {
        Player botPlayer = (Player) npc.getEntity();
        ItemStack mainHandItem = botPlayer.getInventory().getItemInMainHand();
        double baseDamage = getItemDamage(mainHandItem);
        double enchantmentBonus = getWeaponEnchantmentBonus(mainHandItem);
        double playerStrengthBonus = getPlayerStrengthBonus(botPlayer);

        return baseDamage + enchantmentBonus + playerStrengthBonus;
        // 8+4+3.02=15.02
    }

    private void initializeNPCArmorAndHealth(NPC npc) {
        initialArmorPoints = calculateTotalArmorPoints(npc);
        SentinelTrait sentinel = npc.getOrAddTrait(SentinelTrait.class);
        sentinel.armor = initialArmorPoints;
        sentinel.damage=calculateNPCDamage(npc);
        sentinel.setHealth(20.0); // Setting initial health to 20
        getLogger().warning("Initial Armor: " + sentinel.armor);
        getLogger().warning("Initial Health: " + sentinel.health);
    }

    private double calculateTotalArmorPoints(NPC npc) {
        double armorPoints = 0;
        if (npc.hasTrait(Equipment.class)) {
            Equipment equipment = npc.getOrAddTrait(Equipment.class);
            armorPoints += getArmorPoints(equipment.get(Equipment.EquipmentSlot.HELMET));
            armorPoints += getArmorPoints(equipment.get(Equipment.EquipmentSlot.CHESTPLATE));
            armorPoints += getArmorPoints(equipment.get(Equipment.EquipmentSlot.LEGGINGS));
            armorPoints += getArmorPoints(equipment.get(Equipment.EquipmentSlot.BOOTS));
        }
        return armorPoints;
    }

    private double getArmorPoints(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return 0;
        }
        double baseArmorPoints = 0;
        Material material = item.getType();
        switch (material) {
            case NETHERITE_HELMET:
            case DIAMOND_HELMET:
                baseArmorPoints = 3;
                break;
            case IRON_HELMET:
                baseArmorPoints = 2;
                break;
            case NETHERITE_CHESTPLATE:
            case DIAMOND_CHESTPLATE:
                baseArmorPoints = 8;
                break;
            case IRON_CHESTPLATE:
                baseArmorPoints = 6;
                break;
            case NETHERITE_LEGGINGS:
            case DIAMOND_LEGGINGS:
                baseArmorPoints = 6;
                break;
            case IRON_LEGGINGS:
                baseArmorPoints = 5;
                break;
            case NETHERITE_BOOTS:
            case DIAMOND_BOOTS:
                baseArmorPoints = 3;
                break;
            case IRON_BOOTS:
                baseArmorPoints = 2;
                break;
            default:
                baseArmorPoints = 0;
                break;
        }
        double protectionBonus = getArmorProtectionBonus(item);
        return baseArmorPoints * (1 + protectionBonus);
    }

    private double getArmorProtectionBonus(ItemStack item) {
        double protectionBonus = 0;
        if (item.containsEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL)) {
            int level = item.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL);
            protectionBonus += level * 0.04; // Each level of Protection adds 4% protection
        }
        return protectionBonus;
    }

    private String generateDynamicPrompt(Player player, NPC npc, double effectiveDamage) {
        double playerHealth = player.getHealth();
        double npcHealth = sentinel.health;
        double playerDamage = -1 * effectiveDamage;
        double npcDamage = sentinel.damage;
        double totalDurability = 0;
        double currentDurability = 0;

        for (ItemStack armorPiece : player.getInventory().getArmorContents()) {
            if (armorPiece != null) {
                totalDurability += armorPiece.getType().getMaxDurability();
                currentDurability += (armorPiece.getType().getMaxDurability() - armorPiece.getDurability());
            }
        }
        armorDurabilityPercentage = (currentDurability / totalDurability) * 100;
        getLogger().warning("durability: "+armorDurabilityPercentage);
        String situationDescription;
        if (playerHealth < npcHealth && armorDurabilityPercentage < 25) {
            situationDescription = npc.getName() + " taunts " + player.getName() + " fiercely, mocking their low health and broken armor.";
        } else if (playerHealth > npcHealth || playerDamage > npcDamage) {
            situationDescription = npc.getName() + " challenges " + player.getName() + " as they dominate the fight with superior damage.";
        } else if (playerHealth == npcHealth || armorDurabilityPercentage > 75) {
            situationDescription = npc.getName() + " engages " + player.getName() + " in a close fight, acknowledging their strong armor.";
        } else if (npcDamage > playerDamage) {
            situationDescription = npc.getName() + " mocks " + player.getName() + " for their weaker attacks.";
        } else {
            situationDescription = npc.getName() + " fights " + player.getName() + " in an evenly matched battle.";
        }

        return "As the best Minecraft PvPer, situation is this " + situationDescription + " Player health: " + playerHealth + ", NPC health: " + npcHealth + ", Player damage: " + playerDamage + ", NPC damage: " + npcDamage + ", NPC armor: " + sentinel.armor + ". Provide a 10-word description with player and bot name of the intense, emotionally charged battle. without situation and in hinglish like a whatsapp message";
    }
    public void healNPC(NPC npc,Player player) {
        // SentinelTrait sentinel = npc.getOrAddTrait(SentinelTrait.class);
        if (player.getInventory().getHelmet() != null||player.getInventory().getBoots()!=null) {
            double healAmount = 3.0; // Amount of health restored
            double newHealth = Math.min(sentinel.health + healAmount, 20.0); // Max health is 20
            sentinel.setHealth(newHealth);
            // Optionally update NPC inventory to show it has food
            // Equipment equipment = npc.getOrAddTrait(Equipment.class);
            //equipment.set(Equipment.EquipmentSlot.HAND, new ItemStack(Material.COOKED_BEEF, 1));
            getLogger().info(npc.getName() + " eats food and heals to " + newHealth + " health.");
        }
    }
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!CitizensAPI.getNPCRegistry().isNPC(event.getEntity())) return;

        Player player = (Player) event.getDamager();
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getEntity());
        getLogger().info(npc.getName());

        playerDamage = calculatePlayerDamage(player);
        //SentinelTrait sentinel = npc.getOrAddTrait(SentinelTrait.class);
        double npcArmor = sentinel.armor;
        double protectionReduction = calculateProtectionReduction(npc);
        getLogger().info("protection reduction:" + protectionReduction);
        getLogger().info("armour" + sentinel.armor);
        getLogger().info("Player Damage" + playerDamage);
        double effectiveDamage = calculateEffectiveDamage(playerDamage, npcArmor, protectionReduction);
        getLogger().info("Effective Calculated Damage" + effectiveDamage);
        if (sentinel.health < 10.0) {
            healNPC(npc,player);
        }
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                String prompt = generateDynamicPrompt(player, npc, effectiveDamage);
                String response = openai.getChatResponse(prompt);

                // Decide the message to send based on health, damage, and armor durability
                String tauntMessage;
                if (player.getHealth() < sentinel.health && armorDurabilityPercentage < 25) {
                    tauntMessage = npc.getName() + " " + ChatColor.RED + response;
                } else if (player.getHealth() > sentinel.health && (-1 * effectiveDamage) > sentinel.damage) {
                    tauntMessage = npc.getName() + " " + ChatColor.YELLOW + response;
                } else if (player.getHealth() == sentinel.health && armorDurabilityPercentage > 75) {
                    tauntMessage = npc.getName() + " " + ChatColor.GREEN +response;
                } else if (sentinel.damage > (-1 * effectiveDamage)) {
                    tauntMessage = npc.getName() + " " + ChatColor.RED + response;
                } else {
                    tauntMessage = npc.getName() + " " + ChatColor.GRAY  + response;
                }

                // Send the message on the main server thread
                Bukkit.getScheduler().runTask(this, () -> {
                    player.sendMessage(tauntMessage);
                });
            } catch (Exception e) {
                getLogger().severe("Error getting response from OpenAI: " + e.getMessage());
                e.printStackTrace();
            }
        });

        applyDamageToNPC(npc, effectiveDamage);
        reduceNPCArmor(npc, effectiveDamage);
    }

    private double calculatePlayerDamage(Player player) {
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        double baseDamage = getItemDamage(mainHandItem);
        double enchantmentBonus = getWeaponEnchantmentBonus(mainHandItem);
        double playerStrengthBonus = getPlayerStrengthBonus(player);

        return baseDamage + enchantmentBonus + playerStrengthBonus;
        // 8+4+3.02=15.02
    }

    private double getItemDamage(ItemStack item) {
        if (item == null) {
            return 1; // Default damage for an empty hand
        }

        Material material = item.getType();
        switch (material) {
            case NETHERITE_SWORD: return 8;
            case DIAMOND_SWORD: return 7;
            case IRON_SWORD: return 6;
            case STONE_SWORD: return 5;
            case WOODEN_SWORD: return 4;
            case NETHERITE_AXE: return 10;
            case DIAMOND_AXE: return 9;
            case IRON_AXE: return 6;
            case STONE_AXE: return 4;
            case WOODEN_AXE: return 3;
            default: return 1; // Default damage for unknown items
        }
    }

    private double getWeaponEnchantmentBonus(ItemStack item) {
        if (item == null) return 0;

        double bonus = 0;
        if (item.containsEnchantment(Enchantment.DAMAGE_ALL)) {
            int level = item.getEnchantmentLevel(Enchantment.DAMAGE_ALL);
            bonus += 1 + (level - 1) * 0.5;
        }
        if (item.containsEnchantment(Enchantment.DAMAGE_UNDEAD)) {
            int level = item.getEnchantmentLevel(Enchantment.DAMAGE_UNDEAD);
            bonus += level * 2.5;
        }
        if (item.containsEnchantment(Enchantment.DAMAGE_ARTHROPODS)) {
            int level = item.getEnchantmentLevel(Enchantment.DAMAGE_ARTHROPODS);
            bonus += level * 2.5;
        }
        return bonus;
    }

    private double getPlayerStrengthBonus(Player player) {
        PotionEffect effect = player.getPotionEffect(PotionEffectType.INCREASE_DAMAGE);
        if (effect != null) {
            return 1.01 * (effect.getAmplifier() + 1); // Each level of Strength adds 3 damage
        }
        return 0;
    }

    private double calculateProtectionReduction(NPC npc) {
        double reduction = 0;
        if (npc.hasTrait(Equipment.class)) {
            Equipment equipment = npc.getOrAddTrait(Equipment.class);

            reduction += getArmorProtectionReduction(equipment.get(Equipment.EquipmentSlot.HELMET));
            reduction += getArmorProtectionReduction(equipment.get(Equipment.EquipmentSlot.CHESTPLATE));
            reduction += getArmorProtectionReduction(equipment.get(Equipment.EquipmentSlot.LEGGINGS));
            reduction += getArmorProtectionReduction(equipment.get(Equipment.EquipmentSlot.BOOTS));
        }
        return reduction;
    }
    private double getArmorProtectionReduction(ItemStack item) {
        double reduction = 0;
        if (item != null && item.getType() != Material.AIR) {
            for (Enchantment enchantment : item.getEnchantments().keySet()) {
                if (enchantment.equals(Enchantment.PROTECTION_ENVIRONMENTAL)) {
                    int level = item.getEnchantmentLevel(enchantment);
                    reduction += level * 0.04; // Each level of Protection adds 4% protection
                }
                // Add other protection enchantments as needed
            }

            // Reduce protection based on armor durability
            int maxDurability = item.getType().getMaxDurability();
            double durabilityRatio = (double) (maxDurability - item.getDurability()) / maxDurability;
            reduction *= durabilityRatio; // Reduce reduction based on durability
        }
        return reduction;
    }



    private double calculateEffectiveDamage(double baseDamage, double armor, double protectionReduction) {
        // Calculate armor reduction (maximum 80% reduction)
        double armorReduction = Math.min(20, armor) * 0.04; // 4% damage reduction per armor point, max 20 points

        // Calculate effective damage after armor reduction
        double damageAfterArmor = baseDamage * (1 - armorReduction);

        // Calculate effective damage after protection reduction
        double effectiveDamage = damageAfterArmor * (1 - protectionReduction);

        return -effectiveDamage;
    }


    private void applyDamageToNPC(NPC npc, double damage) {
        //SentinelTrait sentinel = npc.getOrAddTrait(SentinelTrait.class);
        double newHealth = Math.max(0, sentinel.health + damage); // Ensure health doesn't go below 0
        sentinel.setHealth(newHealth);

        getLogger().info("Health: " + newHealth);
    }


    private void reduceNPCArmor(NPC npc, double damage) {
        //SentinelTrait sentinel = npc.getOrAddTrait(SentinelTrait.class);
        double armorReduction = Math.min(sentinel.armor, damage * 0.05); // 5% of the damage is used to reduce the armor
        double newArmor = Math.max(0, sentinel.armor + armorReduction); // Update armor value

        // Calculate armor durability reduction
        double armorDurabilityReduction = Math.min(sentinel.armor - newArmor, 1); // Reduce by 1 point or the difference, whichever is smaller

        sentinel.armor = newArmor; // Update armor value

        // Optionally, you can also reduce the durability of each armor piece here
        if (npc.hasTrait(Equipment.class)) {
            Equipment equipment = npc.getOrAddTrait(Equipment.class);
            ItemStack[] armorPieces = {
                    equipment.get(Equipment.EquipmentSlot.HELMET),
                    equipment.get(Equipment.EquipmentSlot.CHESTPLATE),
                    equipment.get(Equipment.EquipmentSlot.LEGGINGS),
                    equipment.get(Equipment.EquipmentSlot.BOOTS)
            };

            for (ItemStack armorPiece : armorPieces) {
                if (armorPiece != null && armorPiece.getType() != Material.AIR) {
                    reduceArmorDurability(armorPiece, armorDurabilityReduction);
                }
            }
        }

        getLogger().info("New Armor: " + sentinel.armor);
    }

    // Method to reduce armor durability
    private void reduceArmorDurability(ItemStack armorPiece, double reduction) {
        int currentDurability = armorPiece.getDurability();
        int newDurability = Math.max(0, currentDurability + (int) Math.ceil(reduction));
        getLogger().info("New Durability: "+newDurability+"armour Piece:"+armorPiece);
        armorPiece.setDurability((short) newDurability);

    }
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (CitizensAPI.getNPCRegistry().isNPC(entity)) {
            NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
            npc.destroy();
        }
    }
}
