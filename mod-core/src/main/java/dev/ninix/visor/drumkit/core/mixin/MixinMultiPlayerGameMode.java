package dev.ninix.visor.drumkit.core.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {

    @Shadow @Final private Minecraft minecraft;

    private boolean isStickOnNoteBlock(BlockPos pos) {
        if (minecraft.player == null || minecraft.level == null) return false;

        boolean hasStick = minecraft.player.getMainHandItem().is(Items.STICK) ||
            minecraft.player.getOffhandItem().is(Items.STICK);
        boolean isNoteBlock = minecraft.level.getBlockState(pos).is(Blocks.NOTE_BLOCK);

        return hasStick && isNoteBlock;
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onStartDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (isStickOnNoteBlock(pos)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onContinueDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (isStickOnNoteBlock(pos)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (isStickOnNoteBlock(hitResult.getBlockPos())) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            cir.cancel();
        }
    }
}
