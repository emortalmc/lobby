package dev.emortal.minestom.lobby.util.entity;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public class NoPhysicsEntity extends Entity {
    public NoPhysicsEntity(@NotNull EntityType entityType) {
        super(entityType);

        this.hasPhysics = false;
        this.setNoGravity(true);
    }

    @Override
    protected void updateVelocity(boolean wasOnGround, boolean flying, Pos positionBeforeMove, Vec newVelocity) {

    }
}
