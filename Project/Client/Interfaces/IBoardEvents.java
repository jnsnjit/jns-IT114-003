package Project.Client.Interfaces;

import java.util.List;

import Project.Common.LeaderboardRecord;

public interface IBoardEvents extends IGameEvents{
    void onRecieveLeaderboard(List<LeaderboardRecord> board);
}