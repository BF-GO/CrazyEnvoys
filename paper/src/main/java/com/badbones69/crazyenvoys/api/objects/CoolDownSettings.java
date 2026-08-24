package com.badbones69.crazyenvoys.api.objects;

import com.badbones69.crazyenvoys.Methods;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CoolDownSettings {

    private final Map<UUID, Calendar> cooldown = new ConcurrentHashMap<>();

    public void addCooldown(UUID uuid, String cooldownTimer) {
        this.cooldown.put(uuid, Methods.getTimeFromString(cooldownTimer));
    }

    public void removeCoolDown(UUID uuid) {
        this.cooldown.remove(uuid);
    }

    public Map<UUID, Calendar> getCooldown() {
        final Map<UUID, Calendar> snapshot = new HashMap<>();
        this.cooldown.forEach((uuid, calendar) -> snapshot.put(uuid, (Calendar) calendar.clone()));
        return Collections.unmodifiableMap(snapshot);
    }

    public void clearCoolDowns() {
        this.cooldown.clear();
    }
}
