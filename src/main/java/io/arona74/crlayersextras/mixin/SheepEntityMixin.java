package io.arona74.crlayersextras.mixin;

import io.arona74.crlayersextras.SheepGrassEatingHandler;
import net.minecraft.entity.passive.SheepEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SheepEntity.class)
public class SheepEntityMixin {

    @Inject(method = "onEatingGrass", at = @At("HEAD"))
    private void onEatingGrass(CallbackInfo ci) {
        SheepEntity sheep = (SheepEntity) (Object) this;
        SheepGrassEatingHandler.tryEatGrassLayer(sheep);
    }
}
