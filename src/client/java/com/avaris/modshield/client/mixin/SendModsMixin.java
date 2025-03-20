package com.avaris.modshield.client.mixin;

import com.avaris.modshield.client.ModShieldClient;
import net.minecraft.network.packet.c2s.login.EnterConfigurationC2SPacket;
import net.minecraft.network.packet.s2c.config.DynamicRegistriesS2CPacket;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DynamicRegistriesS2CPacket.class)
public class SendModsMixin {
    @Inject(method = "apply(Lnet/minecraft/network/listener/ClientConfigurationPacketListener;)V",at = @At("HEAD"))
    void onSendConfigurations(CallbackInfo ci){
        ModShieldClient.sendMods();
    }
}
