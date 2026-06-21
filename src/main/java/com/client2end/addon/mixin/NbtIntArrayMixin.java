package com.client2end.addon.mixin;

import com.client2end.addon.modules.OOMCrash;
import net.minecraft.nbt.NbtIntArray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.DataOutput;

@Mixin(NbtIntArray.class)
public class NbtIntArrayMixin {
    @Inject(method = "write(Ljava/io/DataOutput;)V", at = @At("HEAD"), cancellable = true)
    private void onWrite(DataOutput output, CallbackInfo ci) throws Exception {
        if (OOMCrash.isArmed()) {
            output.writeInt(OOMCrash.getArraySize());
            ci.cancel();
        }
    }
}
