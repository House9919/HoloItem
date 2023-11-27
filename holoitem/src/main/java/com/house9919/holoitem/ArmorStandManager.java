package com.house9919.holoitem;

import org.bukkit.entity.ArmorStand;
import java.util.HashMap;
import java.util.Map;

public class ArmorStandManager {
    private final Map<String, ArmorStand> armorStands;

    public ArmorStandManager() {
        this.armorStands = new HashMap<>();
    }

    public void addArmorStand(String id, ArmorStand armorStand) {
        armorStands.put(id, armorStand);
    }

    public void removeArmorStand(String id) {
        armorStands.remove(id);
    }

    public ArmorStand getArmorStandById(String id) {
        return armorStands.get(id);
    }
}
