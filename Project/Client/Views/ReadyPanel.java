package Project.Client.Views;

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

        //need to add section for player leaderboard, which should display under ready button
        String[] cnames = {"Name", "ID", "Points"};
        //String[][] data = {{"test","1","1"},
        //{"test","2","0"}};
        
        JTable leaderboard = new JTable();
        this.add(leaderboard);

        //fill table with players in room
        
    }
}