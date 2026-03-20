package com.lythroo.wavelength.mixin;

import net.minecraft.entity.EntityCollisionHandler;
import com.lythroo.wavelength.common.data.DimensionGateManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndPortalBlock.class)
public class MixinEndPortalBlock {

    @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    private void wavelength$blockEndPortal(BlockState state, World world,
                                           BlockPos pos, Entity entity,
                                           EntityCollisionHandler collisionHandler,
                                           boolean someFlag,
                                           CallbackInfo ci) {
        if (world.isClient()) return;
        if (DimensionGateManager.isEndOpen()) return;
        if (!(entity instanceof PlayerEntity)) return;

        ci.cancel();
        if (entity instanceof ServerPlayerEntity sp) {
            sp.sendMessage(Text.literal("§c⚠ The End is currently closed by the server."), true);
        }
    }
}