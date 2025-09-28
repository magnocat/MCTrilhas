package com.magnocat.mctrilhas.ctf;

import com.magnocat.mctrilhas.ranks.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.Objects;
import java.util.UUID;

/**
 * Gerencia o placar (scoreboard) para uma partida de CTF.
 */
public class CTFScoreboard {

    private final CTFGame game;

    public CTFScoreboard(CTFGame game) {
        this.game = game;
    }

    /**
     * Cria e atribui um novo placar para um jogador.
     * @param player O jogador que receberá o placar.
     */
    public void createPlayerScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("ctf_sidebar", "dummy", ChatColor.GOLD + "" + ChatColor.BOLD + "Capture a Bandeira");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        setupScoreboardTeams(board);

        player.setScoreboard(board);
    }

    /**
     * Configura as equipes no placar para colorir os nomes dos jogadores.
     * @param board O placar a ser configurado.
     */
    private void setupScoreboardTeams(Scoreboard board) {
        TeamColor teamOneColor = game.getTeamOne();
        TeamColor teamTwoColor = game.getTeamTwo();

        // Garante que a equipe seja registrada apenas uma vez
        Team teamOne = board.getTeam(teamOneColor.name()) == null ? board.registerNewTeam(teamOneColor.name()) : board.getTeam(teamOneColor.name());
        teamOne.setColor(teamOneColor.getChatColor());
        teamOne.setPrefix(teamOneColor.getChatColor().toString());
        teamOne.setAllowFriendlyFire(false); // Impede dano entre membros da mesma equipe

        Team teamTwo = board.getTeam(teamTwoColor.name()) == null ? board.registerNewTeam(teamTwoColor.name()) : board.getTeam(teamTwoColor.name());
        teamTwo.setColor(teamTwoColor.getChatColor());
        teamTwo.setPrefix(teamTwoColor.getChatColor().toString());
        teamTwo.setAllowFriendlyFire(false);

        // Adiciona todos os jogadores da partida às suas respectivas equipes no placar
        for (Player p : game.getOnlinePlayers()) {
            TeamColor playerTeamColor = game.getPlayerTeamColor(p.getUniqueId());
            if (playerTeamColor == teamOneColor) {
                teamOne.addEntry(p.getName());
            } else if (playerTeamColor == teamTwoColor) {
                teamTwo.addEntry(p.getName());
            }
        }
    }

    /**
     * Atualiza o placar para todos os jogadores na partida.
     */
    public void updateForAllPlayers() {
        game.getTeams().values().forEach(team ->
                team.getPlayers().forEach(uuid -> {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        updateForPlayer(player);
                    }
                })
        );
    }

    private void updateForPlayer(Player player) {
        Scoreboard board = player.getScoreboard();
        Objective objective = board.getObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return;

        // Limpa as entradas antigas para evitar duplicatas
        board.getEntries().forEach(board::resetScores);

        TeamColor teamOneColor = game.getTeamOne();
        TeamColor teamTwoColor = game.getTeamTwo();
        CTFTeam teamOne = game.getTeams().get(teamOneColor);
        CTFTeam teamTwo = game.getTeams().get(teamTwoColor);
        CTFPlayerStats stats = game.getPlayerStats().get(player.getUniqueId());
        Rank playerRank = game.getPlugin().getPlayerDataManager().getRank(player.getUniqueId());

        objective.getScore(" ").setScore(12);
        objective.getScore(ChatColor.WHITE + "Tempo: " + ChatColor.GREEN + formatTime(game.getTimeRemaining())).setScore(11);
        objective.getScore("  ").setScore(10);
        objective.getScore(teamOneColor.getChatColor() + teamOneColor.getDisplayName() + ": " + ChatColor.WHITE + teamOne.getScore()).setScore(9);
        objective.getScore(teamTwoColor.getChatColor() + teamTwoColor.getDisplayName() + ": " + ChatColor.WHITE + teamTwo.getScore()).setScore(8);
        objective.getScore("   ").setScore(7);
        objective.getScore(getFlagStatusLine(teamOneColor)).setScore(6);
        objective.getScore(getFlagStatusLine(teamTwoColor)).setScore(5);
        objective.getScore("    ").setScore(4);
        if (playerRank != null) {
            objective.getScore(ChatColor.WHITE + "Ranque: " + playerRank.getColor() + playerRank.getDisplayName()).setScore(3);
        }
        if (stats != null) {
            objective.getScore(ChatColor.WHITE + "K/D: " + ChatColor.GREEN + stats.getKills() + "/" + stats.getDeaths()).setScore(2);
        }
        objective.getScore("     ").setScore(1);
        objective.getScore(ChatColor.YELLOW + "mc.magnocat.net").setScore(0);
    }

    private String getFlagStatusLine(TeamColor flagTeamColor) {
        CTFFlag flag = game.getFlags().get(flagTeamColor);
        String prefix = " " + flagTeamColor.getChatColor() + "Bandeira: ";

        return switch (flag.getState()) {
            case AT_BASE -> prefix + ChatColor.GREEN + "NA BASE";
            case CARRIED -> {
                UUID carrierUUID = flag.getCarrier();
                if (carrierUUID != null) {
                    Player carrier = Bukkit.getPlayer(carrierUUID);
                    if (carrier != null) {
                        TeamColor carrierTeamColor = game.getPlayerTeamColor(carrier.getUniqueId());
                        yield prefix + ChatColor.RED + "com " + carrierTeamColor.getChatColor() + carrier.getName();
                    }
                }
                yield prefix + ChatColor.RED + "CARREGADA"; // Fallback caso o jogador não seja encontrado
            }
            case DROPPED -> prefix + ChatColor.YELLOW + "CAÍDA";
        };
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public void destroyPlayerScoreboard(Player player) {
        player.setScoreboard(Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard());
    }
}