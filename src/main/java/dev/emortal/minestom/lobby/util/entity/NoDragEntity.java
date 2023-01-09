package dev.emortal.minestom.lobby.util.entity;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public class NoDragEntity extends Entity {
    public NoDragEntity(@NotNull EntityType entityType) {
        super(entityType);
        this.setNoGravity(true);
    }

    @Override
    protected void updateVelocity(boolean wasOnGround, boolean flying, Pos positionBeforeMove, Vec newVelocity) {
        this.velocity = newVelocity.mul(MinecraftServer.TICK_PER_SECOND).apply(Vec.Operator.EPSILON);
    }
}
