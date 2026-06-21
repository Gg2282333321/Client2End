package com.client2end.addon.modules;

import com.client2end.addon.Client2End;
import meteordevelopment.meteorclient.systems.modules.Module;

public class ModuleTestC2S extends Module {
    public ModuleTestC2S() {
        super(Client2End.CATEGORY, "test-c2s", "Test module for C2S packet handling");
    }
}