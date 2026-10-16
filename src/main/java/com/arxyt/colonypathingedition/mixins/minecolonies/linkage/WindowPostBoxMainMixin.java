package com.arxyt.colonypathingedition.mixins.minecolonies.linkage;
// fleet 26.1.2: javac 25 numbers lambdas per enclosing method, the filter predicate is lambda$updateResources$0 (was $1/$4 under javac 21).

import com.arxyt.colonypathingedition.core.manager.LinkageManager;
import com.minecolonies.core.client.gui.WindowPostBoxMain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = WindowPostBoxMain.class, remap = false)
public abstract class WindowPostBoxMainMixin {

    @Redirect(method = "lambda$updateResources$0", at = @At(value = "INVOKE", target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"), remap = false)
    private boolean updateResources$contain(String instance, CharSequence s) {
        return LinkageManager.match(instance, s);
    }
}
