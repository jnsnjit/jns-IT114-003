package Project.Client.Views;

import java.awt.Choice;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;

import Project.Client.Client;

public class ReadyPanel extends JPanel {
    public ReadyPanel() {
        JButton readyButton = new JButton();
        readyButton.setText("Ready");
        readyButton.addActionListener(l -> {
            try {
                Client.INSTANCE.sendReady();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
        this.add(readyButton);

        //milestone4, adding away button to be next to ready button.
        JButton awayButton = new JButton();
        awayButton.setText("Away");
        awayButton.addActionListener(l -> {
            try {
                Client.INSTANCE.sendAway();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
        this.add(awayButton);

        //milestone4, adding button to able button cooldown feature for game
        JButton ChoiceCooldownsButton = new JButton();
        ChoiceCooldownsButton.setText("Enable 10 Second Cooldown for RPS commands");
        ChoiceCooldownsButton.addActionListener(l -> {
            try {
                Client.INSTANCE.sendCooldownModifier();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
        this.add(ChoiceCooldownsButton);
    }
}