package dev.emortal.minestom.lobby.util;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

import java.util.ArrayList;
import java.util.List;

public class SphereUtil {

    public static List<Point> getBlocksInSphere(double radius) {
        ArrayList<Point> list = new ArrayList<>();

        for (double x = -radius; x <= radius; x++) {
            for (double y = -radius; y <= radius; y++) {
                for (double z = -radius; z <= radius; z++) {
                    if ((x * x) + (y * y) + (z * z) > radius * radius) continue;

                    list.add(new Vec(x, y, z));
                }
            }
        }

        return list;
    }

}
