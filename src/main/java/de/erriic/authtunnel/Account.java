package de.erriic.authtunnel;

import java.util.UUID;

public record Account(UUID uuid, String name, Type type) {

    public boolean equals(Account other) {
        return this.uuid.equals(other.uuid) && this.name.equals(other.name) && this.type == other.type;
    }

    public boolean filter(String filter) {
        String filterLower = filter.toLowerCase().trim();
        return this.name.toLowerCase().contains(filterLower) || this.uuid.toString().toLowerCase().contains(filterLower) || this.type.toString().toLowerCase().contains(filterLower);
    }

    public enum Type {
        LOCAL, REMOTE
    }
}
