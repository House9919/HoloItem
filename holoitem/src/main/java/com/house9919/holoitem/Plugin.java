/*
TODO:
  -Örökké lehessen ütni, ne tűnjön el.        -- DONE
  -X HP left helyett legyen egy progress bar  -- DONE
  -MySQL mentési rendszer                     -- DONE
  -Parancs törlésre                           -- DONE
  -UUID lecserélése sima id-re                -- DONE
  -Kerüljön lejjebb a szöveg
  -Csak admin tudja használni
*/

package com.house9919.holoitem;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
//import org.bukkit.event.player.PlayerInteractEntityEvent;
//import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

/*
 * holoitem java plugin
 */
public class Plugin extends JavaPlugin implements Listener
{
  private Map<ArmorStand, Integer> woodHologramHP = new HashMap<>();
  private Map<ArmorStand, Integer> stoneHologramHP = new HashMap<>();

  private TextRepository textRepository;
  private ArmorStandManager armorStandManager;

    @Override
    public void onEnable() {
        // Initialize the text repository with your MySQL connection details
        textRepository = new TextRepository("jdbc:mysql://141.144.241.111:3306/s6_mrpg", "House9919", "102030");
        armorStandManager = new ArmorStandManager();
        Bukkit.getPluginManager().registerEvents(this, this);

        getCommand("ch").setExecutor(new CreateHologramCommand(this));
        getCommand("dh").setExecutor(new DeleteHologramCommand(this, textRepository));
    }

    public ArmorStandManager getArmorStandManager() {
        return armorStandManager;
    }

    public class CreateHologramCommand implements CommandExecutor {
      private final Plugin plugin;
      private int armorStandCounter;

      public CreateHologramCommand(Plugin plugin) {
          this.plugin = plugin;
          this.armorStandCounter = 1;
      }

      @Override
      public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
          if (command.getName().equalsIgnoreCase("ch")) {
              // Check if the sender is a player
              if (!(sender instanceof Player)) {
                  sender.sendMessage(ChatColor.RED + "Only players can use this command!");
                  return true;
              }

              // Check if the command has the correct number of arguments
              if (args.length != 1) {
                  sender.sendMessage(ChatColor.DARK_AQUA + "Usage: " + ChatColor.WHITE + "/ch [wood/stone]");
                  return true;
              }

              // Check if the arguments are correct
              String textType = args[0];
              if (!textType.equals("wood") && !textType.equals("stone")) {
                  sender.sendMessage(ChatColor.DARK_RED + "Invalid arguments! " + ChatColor.WHITE + "Use 'wood' or 'stone' as a valid argument!");
                  return true;
              }

              // Get the player and their location
              Player player = (Player) sender;
              Location location = player.getLocation();

              String worldName = player.getWorld().getName();

              // Generate the armor stand ID
              String armorStandId = String.valueOf(armorStandCounter);
              armorStandCounter++;

              // Execute the asynchronous task
              new CreateHologramTask(plugin, player, textType, worldName, location, armorStandId, textRepository).runTaskAsynchronously(plugin);

              return true;
          }
          return false;
      }
}

    public class DeleteHologramCommand implements CommandExecutor
    {
        private final Plugin plugin;
      private final TextRepository textRepository;

      public DeleteHologramCommand(Plugin plugin, TextRepository textRepository) {
          this.plugin = plugin;
          this.textRepository = textRepository;
      }

      @Override
      public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
          if (args.length != 1) {
              sender.sendMessage(ChatColor.RED + "Usage: /dh [hologramID]");
              return true;
          }

          String armorStandId = args[0];

          // Execute the asynchronous task
          new DeleteHologramTask(sender, armorStandId).runTaskAsynchronously(plugin);

          return true;
      }

      private class DeleteHologramTask extends BukkitRunnable {
          private final CommandSender sender;
          private final String armorStandId;

          public DeleteHologramTask(CommandSender sender, String armorStandId) {
              this.sender = sender;
              this.armorStandId = armorStandId;
          }

          @Override
          public void run() {
              // Remove the armor stand from the game
              Bukkit.getScheduler().runTask(plugin, () -> {
                  ArmorStand armorStand = plugin.getArmorStandManager().getArmorStandById(armorStandId);

                  if (armorStand != null) {
                      armorStand.remove();
                      plugin.getArmorStandManager().removeArmorStand(armorStandId);
                  }

                  // Delete the armor stand data from the database
                  boolean success = textRepository.deleteArmorStandData(armorStandId, plugin);

                  // Send a message to the player asynchronously
                  Bukkit.getScheduler().runTask(plugin, () -> {
                      if (success) {
                          sender.sendMessage(ChatColor.GREEN + "Armor stand deleted successfully!");
                      } else {
                          sender.sendMessage(ChatColor.RED + "Failed to delete armor stand!");
                      }
                  });
              });
          }
      }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractAtEntityEvent event) {

        if (event.getRightClicked().getType() == EntityType.ARMOR_STAND) {

            ArmorStand armorStand = (ArmorStand) event.getRightClicked();
            Player player = event.getPlayer();
            ItemStack itemInHand = player.getInventory().getItemInMainHand();

            if (getTextTypeFromDatabase(armorStandId) != null) {
                String textType = armorStand.getMetadata("textint").get(0).asString();

                if (textType.equals("wood")) {

                    if (itemInHand.getType().toString().endsWith("_AXE")) {

                        // Get the player's damage
                        int damage = getAxeDamage(itemInHand.getType());

                        // Get the current HP of the wood
                        //int currentHP = woodHologramHP.getOrDefault(player, 100)
                        int currentHP = woodHologramHP.getOrDefault(armorStand, 100);

                        currentHP -= damage;

                        if (currentHP <= 0)
                        {
                          giveWood(player, 1);
                          currentHP = 100;
                          updateHologramText(armorStand, currentHP, textType);
                        }

                        // Update the current HP for the wood hologram
                        woodHologramHP.put(armorStand, currentHP);

                        updateHologramText(armorStand, currentHP, textType);
                        }
                    }

                 else if (textType.equals("stone")) {

                    if (itemInHand.getType().toString().endsWith("_PICKAXE")) {

                        // Get the player's damage
                        int damage = getPickaxeDamage(itemInHand.getType());

                        // Get the current HP of the wood
                        //int currentHP = woodHologramHP.getOrDefault(player, 100)
                        int currentHP = stoneHologramHP.getOrDefault(armorStand, 100);

                        currentHP -= damage;

                        if (currentHP <= 0)
                        {
                          giveStone(player);
                          currentHP = 100;
                          updateHologramText(armorStand, currentHP, textType);
                        }

                        // Update the current HP for the wood hologram
                        stoneHologramHP.put(armorStand, currentHP);

                        updateHologramText(armorStand, currentHP, textType);
                    }
                }
            }
        }
    }

    private class CreateHologramTask extends BukkitRunnable
    {
      private final Plugin plugin;
      private final Player player;
      private final String textType;
      private final String worldName;
      private final Location location;
      private final String armorStandId;
      private final TextRepository textRepository;

      public CreateHologramTask(Plugin plugin, Player player, String textType, String worldName, Location location, String armorStandId, TextRepository textRepository) {
          this.plugin = plugin;
          this.player = player;
          this.textType = textType;
          this.worldName = worldName;
          this.location = location;
          this.armorStandId = armorStandId;
          this.textRepository = textRepository;
      }

      @Override
      public void run() {
          Bukkit.getScheduler().runTask(plugin, () -> {
              // Save the armor stand data in the database
              textRepository.saveArmorStandData(armorStandId, textType, worldName, location.getX(), location.getY(), location.getZ());

              // Create the hologram
              createHologram(location, "<<< Click below to get " + ChatColor.BOLD + textType + ChatColor.RESET + " >>>", textType);
          });

          Bukkit.getScheduler().runTask(plugin, () -> {
              // Send a message to the player
              player.sendMessage(ChatColor.GREEN + "Hologram successfully created with a type of: " + ChatColor.BOLD + textType);
          });
      }
    }

    private void createHologram(Location originalLocation, String text, String textType) {
        final Location location = originalLocation.clone();
        final ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        armorStand.setCustomNameVisible(true);
        armorStand.setGravity(false);
        armorStand.setVisible(false);
        armorStand.setCustomName(text);
        armorStand.setMetadata("textint", new FixedMetadataValue(this, textType));

        // Update the hologram position every tick
        new BukkitRunnable() {
            @Override
            public void run() {
                if (armorStand.isDead()) {
                    cancel();
                    return;
                }

                // Adjust the position of the hologram
                Vector offset = new Vector(0, 0, 0); // 0, 0.25, 0
                Location newLocation = location.clone().add(offset);
                armorStand.teleport(newLocation);
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    private void updateHologramText(ArmorStand armorStand, int currentHP, String textType) {

      double progress = (double) currentHP / 100.0; // Calculate a progress percentage
      int progressBarLength = 10; /// Define the length of the progress bar
      int filledBarLength = (int) (progressBarLength * progress); // Calculate the length of the filled portion

      StringBuilder progressBar = new StringBuilder();
      for(int i = 0; i < filledBarLength; i++)
      {
        progressBar.append(ChatColor.GREEN + "\u25AE" + ChatColor.RESET);
      }
      for(int i = filledBarLength; i < progressBarLength; i++) {
        progressBar.append(ChatColor.GREEN + "\u25AF" + ChatColor.RESET);
      }

      armorStand.setCustomName("<<< Click below to get " + ChatColor.BOLD + textType + " ( " +  progressBar + ChatColor.BOLD  + " ) " + ChatColor.RESET + ">>>");
    }

    private void giveWood(Player player, int amount) {
      ItemStack woodItem = new ItemStack(Material.OAK_LOG, amount);

      // Check if the player already has wood in their inventory
      for (ItemStack item : player.getInventory().getContents()) {
          if (item != null && item.getType() == Material.OAK_LOG) {
              int currentAmount = item.getAmount();
              int maxStack = item.getMaxStackSize();

              // Check if there is room to stack more wood
              if (currentAmount < maxStack) {
                  int remainingSpace = maxStack - currentAmount;

                  if (remainingSpace >= amount) {
                      // The remaining space is enough to stack all the wood
                      item.setAmount(currentAmount + amount);
                      return;
                  } else {
                      // Fill the remaining space and continue to the next stack
                      item.setAmount(maxStack);
                      amount -= remainingSpace;
                  }
              }
          }
      }

      // If no existing stack was found or there was no room in existing stacks, add a new stack to the inventory
      if (amount > 0) {
          player.getInventory().addItem(woodItem);
      }
    }

    private void giveStone(Player player) {
        player.getInventory().addItem(new ItemStack(Material.STONE));
    }

    /*private void removeHologram(ArmorStand armorStand) {
        armorStand.remove();
    }*/

    private int getAxeDamage(Material axeMaterial)
    {
      if (axeMaterial == Material.WOODEN_AXE)
      {
        return 10;
      }
      else if (axeMaterial == Material.STONE_AXE)
      {
        return 15;
      }
      else if (axeMaterial == Material.GOLDEN_AXE)
      {
        return 20;
      }
      else if (axeMaterial == Material.IRON_AXE)
      {
        return 25;
      }
      else if (axeMaterial == Material.DIAMOND_AXE)
      {
        return 30;
      }

      // If there was no axe found
      return 0;
    }

      private int getPickaxeDamage(Material pickaxeMaterial)
      {
        if (pickaxeMaterial == Material.WOODEN_PICKAXE)
        {
          return 10;
        }
        else if (pickaxeMaterial == Material.STONE_PICKAXE)
        {
          return 15;
        }
        else if (pickaxeMaterial == Material.GOLDEN_PICKAXE)
        {
          return 20;
        }
        else if (pickaxeMaterial == Material.IRON_PICKAXE)
        {
          return 25;
        }
        else if (pickaxeMaterial == Material.DIAMOND_PICKAXE)
        {
          return 30;
        }
        
        // If there was no pickaxe found
        return 0;
      }

    /*private ArmorStand findHologramAtLocation(Location location) {
        for (ArmorStand armorStand : location.getWorld().getEntitiesByClass(ArmorStand.class)) {
            if (armorStand.hasMetadata("textint") && armorStand.getLocation().distanceSquared(location) < 0.5) {
                return armorStand;
            }
        }
        return null;
    }*/
}
