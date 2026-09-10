package com.eme.guilds;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Guild {

    private final String name;
    private UUID ownerUuid;
    private final Set<UUID> members = new LinkedHashSet<>();
    private long lastSeenMessageId = 0; // do śledzenia postępu odbierania wiadomości z Discorda

    public Guild(String name, UUID ownerUuid) {
        this.name = name;
        this.ownerUuid = ownerUuid;
        this.members.add(ownerUuid);
    }

    public String getName() {
        return name;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public boolean isOwner(UUID uuid) {
        return ownerUuid.equals(uuid);
    }

    public long getLastSeenMessageId() {
        return lastSeenMessageId;
    }

    public void setLastSeenMessageId(long id) {
        this.lastSeenMessageId = id;
    }
}
