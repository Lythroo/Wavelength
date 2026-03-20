package com.lythroo.wavelength.mixin;

import com.lythroo.wavelength.common.data.DimensionGateManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetherPortalBlock.class)
public class MixinNetherPortalBlock {

    @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    private void wavelength$blockNetherPortal(BlockState state, World world,
                                              BlockPos pos, Entity entity,
                                              EntityCollisionHandler collisionHandler,
                                              boolean someFlag,
                                              CallbackInfo ci) {
        if (world.isClient()) return;
        if (DimensionGateManager.isNetherOpen()) return;
        if (!(entity instanceof PlayerEntity)) return;

        ci.cancel();
        if (entity instanceof ServerPlayerEntity sp) {
            sp.sendMessage(Text.literal("§c⚠ The Nether is currently closed by the server."), true);
        }
    }
}