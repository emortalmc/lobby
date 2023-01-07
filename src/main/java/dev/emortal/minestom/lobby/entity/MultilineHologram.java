package dev.emortal.minestom.lobby.entity;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.other.ArmorStandMeta;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MultilineHologram {
    private @NotNull List<Entity> entities = new ArrayList<>();

    private @NotNull List<Component> names;

    public MultilineHologram(@NotNull List<Component> names) {
        this.names = names;
    }

    public void setInstance(@NotNull Instance instance, @NotNull Pos position) {
        int i = 0;
        for (Component name : this.names) {
            Entity entity = new NoPhysicsEntity(EntityType.ARMOR_STAND);
            ArmorStandMeta meta = (ArmorStandMeta) entity.getEntityMeta();
            meta.setNotifyAboutChanges(false);
            //meta.radius = 0f
            meta.setSmall(true);
            meta.setHasNoBasePlate(true);
            meta.setMarker(true);
            meta.setInvisible(true);
            meta.setCustomNameVisible(true);
            meta.setCustomName(name);
            meta.setNotifyAboutChanges(true);

            entity.setInstance(instance, position.add(0.0, 0.5 + (0.30 * (names.size() - i)), 0.0));

            entities.add(entity);

            i++;
        }
    }

    public void remove() {
        for (Entity entity : entities) {
            entity.remove();
        }
    }

    public void setLine(int index, Component newName) {
        if (this.names.size() > index) this.names.set(index, newName);
        if (this.entities.size() > index) this.entities.get(index).getEntityMeta().setCustomName(newName);
    }
    public int size() {
        return this.names.size();
    }

}
