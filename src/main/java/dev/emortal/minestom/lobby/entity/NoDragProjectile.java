package dev.emortal.minestom.lobby.entity;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityProjectile;
import net.minestom.server.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NoDragProjectile extends EntityProjectile {
    public NoDragProjectile(@Nullable Entity shooter, @NotNull EntityType entityType) {
        super(shooter, entityType);
        setNoGravity(true);
    }

    @Override
    protected void updateVelocity(boolean wasOnGround, boolean flying, Pos positionBeforeMove, Vec newVelocity) {
        this.velocity = newVelocity.mul(MinecraftServer.TICK_PER_SECOND).apply(Vec.Operator.EPSILON);
    }
}
