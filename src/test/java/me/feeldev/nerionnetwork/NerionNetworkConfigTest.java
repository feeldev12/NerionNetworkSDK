package me.feeldev.nerionnetwork;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NerionNetworkConfigTest {

    @Test
    void modsUseConfigNerionsdk(@TempDir File gameDir) {
        File file = NerionNetworkConfig.defaultCredentialFile(gameDir);

        assertEquals(new File(gameDir, "config/nerionsdk/launcher-api-credential.txt"), file);
    }

    @Test
    void bukkitAndPaperServersUsePluginsNerionSdk(@TempDir File serverDir) {
        assertTrue(new File(serverDir, "plugins").mkdir());

        File file = NerionNetworkConfig.defaultCredentialFile(serverDir);

        assertEquals(new File(serverDir, "plugins/NerionSDK/launcher-api-credential.txt"), file);
    }

    @Test
    void builderDefaultsToTheWorkingDirectoryFile() {
        NerionNetworkConfig config = NerionNetworkConfig.builder().build();

        assertEquals(NerionNetworkConfig.defaultCredentialFile(new File(".")), config.getCredentialFile());
    }

    @Test
    void anExplicitFileWins() {
        File custom = new File("somewhere/launcher-api-credential.txt");

        assertEquals(custom, NerionNetworkConfig.builder().credentialFile(custom).build().getCredentialFile());
    }
}
