package dev.emortal.minestom.lobby;

import dev.emortal.minestom.lobby.entity.NoDragProjectile;
import dev.emortal.minestom.lobby.npc.GameModeManager;
import dev.emortal.minestom.lobby.npc.PacketNPC;
import dev.emortal.minestom.lobby.util.SphereUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ExplosionPacket;
import net.minestom.server.timer.TaskSchedule;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class LobbyEvents {

    public static void registerEvents(EventNode<Event> eventNode, GameModeManager gmm) {
        eventNode.addListener(PlayerSpawnEvent.class, e -> {
            e.getPlayer().getInventory().setItemStack(0, ItemStack.of(Material.FIRE_CHARGE));
            e.getPlayer().getInventory().setItemStack(4, ItemStack.of(Material.COMPASS));

            e.getPlayer().setAllowFlying(true);

            for (PacketNPC npc : gmm.getNpcs()) {
                npc.addViewer(e.getPlayer());
            }
        });

        eventNode.addListener(PlayerUseItemEvent.class, e -> {
            if (e.getHand() != Player.Hand.MAIN) return;

            if (e.getPlayer().getItemInMainHand().material() == Material.FIRE_CHARGE) {
                e.setCancelled(true);

                Vec initialVelocity = e.getPlayer().getPosition().direction().mul(30.0);
                Entity fireball = new NoDragProjectile(e.getPlayer(), EntityType.FIREBALL);
                fireball.setVelocity(initialVelocity);

                fireball.setInstance(e.getInstance(), e.getPlayer().getPosition().add(0, e.getPlayer().getEyeHeight(), 0));
                fireball.scheduleRemove(10, ChronoUnit.SECONDS);
            }

            if (e.getPlayer().getItemInMainHand().material() == Material.COMPASS) {
                e.getPlayer().openInventory(gmm.getCompassInventory());
            }
        });

        eventNode.addListener(ProjectileCollideWithBlockEvent.class, e -> {
            if (e.getEntity().getEntityType() == EntityType.FIREBALL) {

                ThreadLocalRandom rand = ThreadLocalRandom.current();

                double radius = 3.0;
                if (rand.nextDouble() < 0.05) radius += 5;

                List<Point> blocks = SphereUtil.getBlocksInSphere(radius);

                Point pos = e.getEntity().getPosition();

                HashMap<Point, Block> originalState = new HashMap<>();
                AbsoluteBlockBatch batch = new AbsoluteBlockBatch();
                List<Point> filteredBlocks = new ArrayList<>();
                for (Point block : blocks) {
                    Point blockPos = block.add(pos);
                    Block currentBlock = e.getInstance().getBlock(blockPos);
                    if (currentBlock.compare(Block.AIR)) continue;

                    filteredBlocks.add(blockPos);

                    batch.setBlock(blockPos, Block.AIR);
                    originalState.put(blockPos, currentBlock);
                }

                Collections.shuffle(filteredBlocks);

                e.getInstance().sendGroupedPacket(new ExplosionPacket((float) pos.x(), (float) pos.y(), (float) pos.z(), 2f, new byte[]{}, 0f, 0f, 0f));
                batch.apply(e.getInstance(), null);

                e.getInstance().scheduler().submitTask(new Supplier<>() {
                    boolean firstRun = true;
                    int i = 0;

                    @Override
                    public TaskSchedule get() {
                        if (firstRun) {
                            firstRun = false;
                            return TaskSchedule.seconds(3);
                        }

                        for (int j = 0; j < 4; j++) {
                            if (i >= filteredBlocks.size()) return TaskSchedule.stop();

                            e.getInstance().setBlock(filteredBlocks.get(i), originalState.get(filteredBlocks.get(i)));
//                        instance.playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.BLOCK, 0.7f, 2f));

                            i++;
                        }


                        return TaskSchedule.nextTick();
                    }
                });

                e.getEntity().remove();
            }
        });

        eventNode.addListener(PlayerBlockBreakEvent.class, e -> e.setCancelled(true))
                .addListener(PlayerBlockPlaceEvent.class, e -> e.setCancelled(true));
    }

}
