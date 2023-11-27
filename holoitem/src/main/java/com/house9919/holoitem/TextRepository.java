package com.house9919.holoitem;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.ChatColor;

public class TextRepository {
    private final String url;
    private final String username;
    private final String password;

    public TextRepository(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;

        testConnection();
    }

    public void saveArmorStandData(String armorStandId, String textType, String worldName, double x, double y, double z) {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement("INSERT INTO hi_armors (id, text_type, world, x, y, z) VALUES (?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, armorStandId);
            statement.setString(2, textType);
            statement.setString(3, worldName);
            statement.setDouble(4, x);
            statement.setDouble(5, y);
            statement.setDouble(6, z);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getTextTypeFromDatabase(String armorStandId) {
        try (Connection connection = DriverManager.getConnection(url, username, password);
            PreparedStatement statement = connection.prepareStatement("SELECT textType FROM hi_armors WHERE id = ?")) {

            statement.setString(1, armorStandId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString("textType");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean deleteArmorStandData(String armorStandId, Plugin plugin) {
        
        Location location = getArmorStandLocationFromDatabase(armorStandId);

        if (location == null) {
            return false; // Armor stand not found in the database
        }

        // Remove armor stand from the game
        removeArmorStandFromGame(location);

        try (Connection connection = DriverManager.getConnection(url, username, password);
            PreparedStatement statement = connection.prepareStatement("DELETE FROM hi_armors WHERE id = ?")) {

            statement.setString(1, armorStandId);
            statement.executeUpdate();

            return true;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false; // Database error occurred
    }

    private void testConnection() {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            System.out.println(ChatColor.GREEN + "Successfully connected to the MySQL database.");
        } catch (SQLException e) {
            System.out.println(ChatColor.RED + "Failed to connect to the MySQL database.");
            e.printStackTrace();
        }
    }

    private Location getArmorStandLocationFromDatabase(String armorStandId) {
    try (Connection connection = DriverManager.getConnection(url, username, password);
         PreparedStatement statement = connection.prepareStatement("SELECT world, x, y, z FROM hi_armors WHERE id = ?")) {

        statement.setString(1, armorStandId);
        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            double x = resultSet.getDouble("x");
            double y = resultSet.getDouble("y");
            double z = resultSet.getDouble("z");
            Bukkit.getLogger().info(Double.toString(x) + " | " + Double.toString(y) + " | " + Double.toString(z));
            String worldName = resultSet.getString("world"); // Replace with the appropriate world name

            World world = Bukkit.getWorld(worldName);
            return new Location(world, x, y, z);
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }

    return null;
    }

    private void removeArmorStandFromGame(Location location) {
        // Execute the command to remove the armor stand at the given location
        //String command = "minecraft:kill @e[type=minecraft:armor_stand,x=" + location.getX() + ",y=" + location.getY() + ",z=" + location.getZ() + ",distance=..1]";
        String command = "minecraft:execute in " + location.getWorld() + " run kill @e[type=minecraft:armor_stand,x=" + location.getX() + ",y=" + location.getY() + ",z=" + location.getZ() + ",distance=..1]";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        System.out.println(location.getWorld());
    }
}
