package org.pronze.hypixelify.arena;

import org.bukkit.entity.Player;
import org.pronze.hypixelify.Configurator;
import org.pronze.hypixelify.SBAHypixelify;
import org.pronze.hypixelify.data.GameStorage;
import org.pronze.hypixelify.message.Messages;
import org.pronze.hypixelify.scoreboard.ScoreBoard;
import org.screamingsandals.bedwars.Main;
import org.screamingsandals.bedwars.api.RunningTeam;
import org.screamingsandals.bedwars.api.events.BedwarsGameEndingEvent;
import org.screamingsandals.bedwars.api.events.BedwarsGameStartedEvent;
import org.screamingsandals.bedwars.api.events.BedwarsTargetBlockDestroyedEvent;
import org.screamingsandals.bedwars.api.game.Game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.screamingsandals.bedwars.lib.nms.title.Title.sendTitle;

public class Arena {


    public final double radius;
    private final Game mGame;
    private final ScoreBoard scoreBoard;
    private final GameStorage storage;
    public GameTask gameTask;


    public Arena(Game game) {
        radius = Math.pow(SBAHypixelify.getConfigurator().config.getInt("upgrades.trap-detection-range", 7), 2);
        this.mGame = game;
        storage = new GameStorage();
        scoreBoard = new ScoreBoard(this);
        gameTask = new GameTask(this);
    }

    public GameStorage getStorage() {
        return storage;
    }

    public Game getGame() {
        return this.mGame;
    }

    public ScoreBoard getScoreBoard() {
        return this.scoreBoard;
    }

    public void onTargetBlockDestroyed(BedwarsTargetBlockDestroyedEvent e) {
        for (Player p : e.getTeam().getConnectedPlayers()) {
            if (p != null && p.isOnline())
                sendTitle(p, Messages.message_bed_destroyed_title, Messages.message_bed_destroyed_subtitle, 0, 40, 20);
        }
    }

    public void onOver(BedwarsGameEndingEvent e) {
        final Game game = e.getGame();

        if (mGame.equals(game)) {
            try {
                if (scoreBoard != null)
                    scoreBoard.updateScoreboard();
                if (gameTask != null && !gameTask.isCancelled()) {
                    gameTask.cancel();
                    gameTask = null;
                }
            } catch (Throwable t){
                t.printStackTrace();
            }

            final RunningTeam winner = e.getWinningTeam();

            if (winner != null) {
                final Map<String, Integer> dataKills = new HashMap<>();
                game.getConnectedPlayers().forEach(player -> {
                    dataKills.put(player.getDisplayName(), Main.getPlayerStatisticsManager()
                            .getStatistic(player).getCurrentKills());
                });

                int kills_1 = 0;
                int kills_2 = 0;
                int kills_3 = 0;
                String kills_1_player = "none";
                String kills_2_player = "none";
                String kills_3_player = "none";

                for (String player : dataKills.keySet()) {
                    int k = dataKills.get(player);
                    if (k > 0 && k > kills_1) {
                        kills_1_player = player;
                        kills_1 = k;
                    }
                }
                for (String player : dataKills.keySet()) {
                    int k = dataKills.get(player);
                    if (k > kills_2 && k <= kills_1 && !player.equals(kills_1_player)) {
                        kills_2_player = player;
                        kills_2 = k;
                    }
                }
                for (String player : dataKills.keySet()) {
                    int k = dataKills.get(player);
                    if (k > kills_3 && k <= kills_2 && !player.equals(kills_1_player) &&
                            !player.equals(kills_2_player)) {
                        kills_3_player = player;
                        kills_3 = k;
                    }
                }

                final List<String> WinTeamPlayers = new ArrayList<>();

                winner.getConnectedPlayers().forEach(player -> WinTeamPlayers.add(player.getDisplayName()));

                winner.getConnectedPlayers().forEach(pl -> sendTitle(pl, "§6§lVICTORY!", "", 0, 90, 0));

                for (Player player : game.getConnectedPlayers()) {
                    for (String message : Configurator.overstats_message) {
                        if (message == null || message.isEmpty()) {
                            return;
                        }

                        player.sendMessage(message.replace("{color}", org.screamingsandals.bedwars.game.TeamColor.valueOf(winner.getColor().name()).chatColor.toString())
                                .replace("{win_team}", winner.getName())
                                .replace("{win_team_players}", WinTeamPlayers.toString())
                                .replace("{first_1_kills_player}", kills_1_player)
                                .replace("{first_2_kills_player}", kills_2_player)
                                .replace("{first_3_kills_player}", kills_3_player)
                                .replace("{first_1_kills}", String.valueOf(kills_1))
                                .replace("{first_2_kills}", String.valueOf(kills_2))
                                .replace("{first_3_kills}", String.valueOf(kills_3)));
                    }
                }
            }
        }
    }


    public void onGameStarted(BedwarsGameStartedEvent e) {
        final Game game = e.getGame();

        if (!game.equals(mGame)) return;

        game.getConnectedPlayers().forEach(player -> {
            Configurator.gamestart_message.forEach(message -> {
                if (message == null || message.isEmpty()) {
                    return;
                }

                player.sendMessage(message);
            });
        });

        mGame.getRunningTeams().forEach(t -> {
            storage.setTrap(t, false);
            storage.setPool(t, false);
            storage.setTargetBlockLocation(t);
            storage.setProtection(t.getName(), 0);
            storage.setSharpness(t.getName(), 0);
        });

    }


}
