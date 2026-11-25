package com.studentudp.client;

import com.studentudp.common.DbConfigDTO;
import com.studentudp.common.UiTheme;
import java.awt.CardLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * 3-Step Client Application using CardLayout:
 * 1) Connect Server UDP -> 2) Configure MySQL -> 3) Student Management.
 */
public class ClientFrame extends JFrame {

    private static final String CARD_CONNECT = "CONNECT_SERVER";
    private static final String CARD_DB_CONFIG = "DB_CONFIG";
    private static final String CARD_STUDENT_MANAGER = "STUDENT_MANAGER";

    private final UdpClientEngine clientEngine = new UdpClientEngine();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardContainer = new JPanel(cardLayout);

    private final ConnectServerPanel connectServerPanel;
    private final DbConfigPanel dbConfigPanel;
    private final StudentManagerPanel studentManagerPanel;

    public ClientFrame() {
        super("UDP Student Management - Client");

        this.connectServerPanel = new ConnectServerPanel(clientEngine, (host, port) -> handleServerConnected());

        this.dbConfigPanel = new DbConfigPanel(clientEngine, new DbConfigPanel.DbConfigListener() {
            @Override
            public void onBackToConnect() {
                cardLayout.show(cardContainer, CARD_CONNECT);
            }

            @Override
            public void onDbConnected(DbConfigDTO config) {
                handleDbConnected(config);
            }
        });

        this.studentManagerPanel = new StudentManagerPanel(clientEngine, new StudentManagerPanel.NavigationListener() {
            @Override
            public void onChangeDb() {
                handleServerConnected();
            }

            @Override
            public void onDisconnect() {
                cardLayout.show(cardContainer, CARD_CONNECT);
            }
        });

        initUi();
    }

    private void handleServerConnected() {
        dbConfigPanel.refreshServerLabel();
        cardLayout.show(cardContainer, CARD_DB_CONFIG);
    }

    private void handleDbConnected(DbConfigDTO config) {
        studentManagerPanel.onEnterStep(config);
        cardLayout.show(cardContainer, CARD_STUDENT_MANAGER);
    }

    private void initUi() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(960, 620));
        setPreferredSize(new Dimension(1060, 660));

        cardContainer.setBackground(UiTheme.BG_WINDOW);
        cardContainer.add(connectServerPanel, CARD_CONNECT);
        cardContainer.add(dbConfigPanel, CARD_DB_CONFIG);
        cardContainer.add(studentManagerPanel, CARD_STUDENT_MANAGER);

        setContentPane(cardContainer);
        cardLayout.show(cardContainer, CARD_CONNECT);

        pack();
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        UiTheme.install();
        SwingUtilities.invokeLater(() -> {
            ClientFrame frame = new ClientFrame();
            frame.setVisible(true);
        });
    }
}
