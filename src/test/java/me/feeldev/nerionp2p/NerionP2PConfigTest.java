package me.feeldev.nerionp2p;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NerionP2PConfigTest {

    @Test
    void modsUseConfigNerionsdk(@TempDir File gameDir) {
        File file = NerionP2PConfig.defaultCredentialFile(gameDir);

        assertEquals(new File(gameDir, "config/nerionsdk/launcher-api-credential.txt"), file);
    }

    @Test
    void bukkitAndPaperServersUsePluginsNerionSdk(@TempDir File serverDir) {
        assertTrue(new File(serverDir, "plugins").mkdir());

        File file = NerionP2PConfig.defaultCredentialFile(serverDir);

        assertEquals(new File(serverDir, "plugins/NerionSDK/launcher-api-credential.txt"), file);
    }

    @Test
    void builderDefaultsToTheWorkingDirectoryFile() {
        NerionP2PConfig config = NerionP2PConfig.builder().build();

        assertEquals(NerionP2PConfig.defaultCredentialFile(new File(".")), config.getCredentialFile());
    }

    @Test
    void anExplicitFileWins() {
        File custom = new File("somewhere/launcher-api-credential.txt");

        assertEquals(custom, NerionP2PConfig.builder().credentialFile(custom).build().getCredentialFile());
    }
}
