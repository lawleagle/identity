package draylar.identity.network;

import draylar.identity.Identity;
import draylar.identity.cca.FavoriteIdentitiesComponent;
import draylar.identity.registry.Components;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.registry.Registry;

public class ServerNetworking implements NetworkHandler {

    public static void init() {
        registerIdentityRequestPacketHandler();
        registerFavoritePacketHandler();
    }

    private static void registerIdentityRequestPacketHandler() {
        ServerSidePacketRegistry.INSTANCE.register(IDENTITY_REQUEST, (context, packet) -> {
            EntityType<?> type = Registry.ENTITY_TYPE.get(packet.readIdentifier());

            // Ensure player has permission to switch identities
            if (Identity.CONFIG.enableSwaps || context.getPlayer().hasPermissionLevel(3)) {
                if (type.equals(EntityType.PLAYER)) {
                    Components.CURRENT_IDENTITY.get(context.getPlayer()).setIdentity(null);
                } else {
                    Components.CURRENT_IDENTITY.get(context.getPlayer()).setIdentity((LivingEntity) type.create(context.getPlayer().world));
                }

                // Refresh player dimensions
                context.getPlayer().calculateDimensions();
            }
        });
    }

    private static void registerFavoritePacketHandler() {
        ServerSidePacketRegistry.INSTANCE.register(FAVORITE_UPDATE, (context, packet) -> {
            EntityType<?> type = Registry.ENTITY_TYPE.get(packet.readIdentifier());
            boolean favorite = packet.readBoolean();
            PlayerEntity player = context.getPlayer();

            if(favorite) {
                Components.FAVORITE_IDENTITIES.get(player).favorite(type);
            } else {
                Components.FAVORITE_IDENTITIES.get(player).unfavorite(type);
            }
        });
    }

    public static void updateClientConfig(PlayerEntity player) {
        PacketByteBuf packet = new PacketByteBuf(Unpooled.buffer());
        packet.writeBoolean(Identity.CONFIG.enableClientSwapMenu);
        ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, CAN_OPEN_MENU, packet);
    }

    private ServerNetworking() {
        // NO-OP
    }
}
