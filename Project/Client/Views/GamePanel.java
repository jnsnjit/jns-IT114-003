package Project.Client.Views;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import Project.Client.CardView;
import Project.Client.Client;
import Project.Client.Interfaces.IBoardEvents;
import Project.Client.Interfaces.ICardControls;
import Project.Client.Interfaces.IPhaseEvent;
import Project.Client.Interfaces.IRoomEvents;
import Project.Common.Constants;
import Project.Common.LeaderboardRecord;
import Project.Common.Phase;
import Project.Common.TimedEvent;

public class GamePanel extends JPanel implements IRoomEvents, IPhaseEvent, IBoardEvents {

    private JPanel playPanel;
    private JPanel scorePanel;
    private JTable scoreTable;
    private boolean canClick = true;
    private CardLayout cardLayout;
    public TimedEvent buttonTimer = null;
    private static final String READY_PANEL = "READY";
    private static final String PLAY_PANEL = "PLAY";//example panel for this lesson
    private static final String BOARD_PANEL = "LEADERBOARD";
    JPanel buttonPanel = new JPanel();

    public GamePanel(ICardControls controls) {
        super(new BorderLayout());

        // Create the three buttons for either rock, paper, or scissors and add them to a panel
        JButton rockButton = new JButton("Rock");
        rockButton.addActionListener(event->{
            try {
                buttonCooldown("rock");
                buttonTimer = Client.INSTANCE.cooldown ? new TimedEvent(10, () -> buttonCooldown(true, "rock")) : null;
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
        buttonPanel.add(rockButton);

        JButton paperButton = new JButton("Paper");
        paperButton.addActionListener(event ->{
            try{
                buttonCooldown("paper");
                buttonTimer = Client.INSTANCE.cooldown ? new TimedEvent(10, () -> buttonCooldown(true, "paper")) : null;
            } catch (Exception e) {
                // auto catch problems
                e.printStackTrace();
            }
        });
        buttonPanel.add(paperButton);

        JButton scissorsButton = new JButton("Scissors");
        scissorsButton.addActionListener(event ->{
            try{
                buttonCooldown("scissors");
                buttonTimer = Client.INSTANCE.cooldown ? new TimedEvent(10, () -> buttonCooldown(true,"scissors")) : null;
            } catch (Exception e) {
                // auto catch problems
                e.printStackTrace();
            }
        });
        buttonPanel.add(scissorsButton);

        JPanel gameContainer = new JPanel(new CardLayout());
        cardLayout = (CardLayout) gameContainer.getLayout();
        this.setName(CardView.GAME_SCREEN.name());
        Client.INSTANCE.addCallback(this);

        ReadyPanel readyPanel = new ReadyPanel();
        readyPanel.setName(READY_PANEL);
        gameContainer.add(READY_PANEL, readyPanel);

        playPanel = new JPanel();
        playPanel.setName(PLAY_PANEL);
        playPanel.add(buttonPanel);
        gameContainer.add(PLAY_PANEL, playPanel);

        scorePanel = new JPanel(new BorderLayout());
        scorePanel.setName(BOARD_PANEL);
        scoreTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(scoreTable);
        scorePanel.add(scrollPane, BorderLayout.CENTER);
        gameContainer.add(BOARD_PANEL, scorePanel);

        GameEventsPanel gameEventsPanel = new GameEventsPanel();
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, gameContainer, gameEventsPanel);
        splitPane.setResizeWeight(0.7);


        playPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                splitPane.setDividerLocation(0.7);
            }
        });

        playPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                playPanel.revalidate();
                playPanel.repaint();
            }
        });

        this.add(splitPane, BorderLayout.CENTER);
        controls.addPanel(CardView.CHAT_GAME_SCREEN.name(), this);
        setVisible(false);
    }

    

    @Override
    public void onRoomAction(long clientId, String clientName, String roomName, boolean isJoin) {
        if (Constants.LOBBY.equals(roomName) && isJoin) {
            setVisible(false);
            revalidate();
            repaint();
        }
    }

    @Override
    public void onReceivePhase(Phase phase) {
        System.out.println("Received phase: " + phase.name());
        if (!isVisible()) {
            setVisible(true);
            getParent().revalidate();
            getParent().repaint();
            System.out.println("GamePanel visible");
        }
        if (phase == Phase.READY) {
            cardLayout.show(playPanel.getParent(), READY_PANEL);
            buttonPanel.setVisible(false);
        } else if (phase == Phase.MAKE_CHOICE) {
            cardLayout.show(playPanel.getParent(), PLAY_PANEL);
            buttonPanel.setVisible(true);
        }
    }
    
    @Override
    public void onReceiveRoomList(List<String> rooms, String message) {
        // Not used here, but needs to be defined due to interface
    }
    public void onRecieveLeaderboard(List<LeaderboardRecord> lbr){
        // add table creation here
        DefaultTableModel dfm = new DefaultTableModel(new String[] { "Rank", "Player", "Score" }, 0);
        scoreTable.setModel(dfm);
        //sort table by player points
        for (int i = 0; i < lbr.size() - 1; i++) {
            for (int j = 0; j < lbr.size() - i - 1; j++) {
                if ((lbr.get(j)).getPoints() < (lbr.get(j + 1)).getPoints()) {
                    // Swap elements
                    LeaderboardRecord temp = (lbr.get(j));
                    lbr.set(j, lbr.get(j + 1));
                    lbr.set(j + 1, temp);
                }
            }
            //System.out.println("After iteration " + (i + 1) + ": " + numbers);
        }
        int rank = 1;
        for (LeaderboardRecord row : lbr) {
            row.setRank(rank);
            dfm.addRow(new Object[]{row.getRank(), row.getName(),row.getPoints()});
            rank++;
        }
        cardLayout.show(playPanel.getParent(), BOARD_PANEL);
    }
    //milestone 4 stuff, button that stops UI from submiting multiple choices within a timeframe, happens client side.
    public void buttonCooldown(boolean canClick, String choice){
        canClick = true;
        buttonCooldown(choice);
    }
    public void buttonCooldown(String choice){
        if(Client.INSTANCE.cooldown){
            if(canClick){
                canClick = false;
                Client.INSTANCE.sendChoice(choice);
                //Client.INSTANCE.sendMessage("Button is now on a ten second cooldown");
                Client.INSTANCE.clientSideGameEvent("Button is now on a ten second cooldown");
            }
        }else{
            Client.INSTANCE.sendChoice(choice);
        }
    }
}