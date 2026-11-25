package com.studentudp.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studentudp.client.ClientFrame;
import com.studentudp.common.UiTheme;
import com.studentudp.server.ServerFrame;
import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class GuiSmokeTest {

    @Test
    void serverAndClientFramesConstructCleanlyOnEdt() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires non-headless display");

        SwingUtilities.invokeAndWait(() -> {
            UiTheme.install();
            ServerFrame serverFrame = new ServerFrame();
            ClientFrame clientFrame = new ClientFrame();
            try {
                assertTrue(serverFrame.getWidth() >= 1040);
                assertTrue(clientFrame.getWidth() >= 960);
            } finally {
                serverFrame.dispose();
                clientFrame.dispose();
            }
        });
    }
}
