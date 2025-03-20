package com.avaris.modshield.mixin;

import com.avaris.modshield.ModShield;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Inject(method = "checkCanJoin",at = @At("RETURN"),cancellable = true)
    void onCanJoin(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Text> cir){
        if(cir.getReturnValue() != null){
            return;
        }
        Text ret = ModShield.canJoin(address,profile);
        if(ret != null){
            cir.setReturnValue(ret);
        }
    }
    @Inject(method = "onPlayerConnect", at = @At("RETURN"))
    void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci){
        ModShield.onPlayerConnect(connection,player,clientData);
    }
}
