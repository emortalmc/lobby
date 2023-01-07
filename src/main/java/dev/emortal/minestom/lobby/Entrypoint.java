package dev.emortal.minestom.lobby;

import dev.emortal.minestom.core.MinestomServer;
import dev.emortal.minestom.core.module.chat.ChatModule;
import dev.emortal.minestom.core.module.core.CoreModule;
import dev.emortal.minestom.core.module.kubernetes.KubernetesModule;
import dev.emortal.minestom.core.module.permissions.PermissionModule;

public class Entrypoint {
    public static void main(String[] args) {
        new MinestomServer.Builder()
                .module(KubernetesModule.class, KubernetesModule::new)
                .module(CoreModule.class, CoreModule::new)
                .module(PermissionModule.class, PermissionModule::new)
                .module(ChatModule.class, ChatModule::new)
                .module(LobbyModule.class, LobbyModule::new)
                .build();
    }
}
