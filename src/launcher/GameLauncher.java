package launcher;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

import java.util.ArrayList;
import java.util.List;
import java.util.Random; // usar em casos futuros

/**
 * Classe responsável por lançar um jogo de The Village
 * com configurações totalmente customizáveis.
 */
public class GameLauncher {

    /* ================= CONFIGURAÇÕES DO JOGO ================= */

    // Jogadores
    private int numPlayers = 10;
    private int numWerewolves = 2;

    // Roles especiais
    private boolean enableSeer = true;
    private boolean enableDoctor = true;
    private boolean enableHunter = true;

    // Comunicação
    private int maxPublicMessagesPerDay = 2; // ex: 1 para comunicação restrita
    private boolean enforceMessageLimit = true;

    // Jogo
    private int maxRounds = 20;
    private boolean demoMode = false;

    // Reprodutibilidade
    private long randomSeed = System.currentTimeMillis();

    /* ========================================================== */

    public static void main(String[] args) {
        new GameLauncher().startGame();
    }

    /**
     * Método principal que inicia o jogo.
     */
    public void startGame() {
        try {
            Runtime rt = Runtime.instance();
            Profile profile = new ProfileImpl();
            profile.setParameter(Profile.GUI, "true");

            AgentContainer container = rt.createMainContainer(profile);

            // Criar agente de logging (sempre presente)
            AgentController logger = container.createNewAgent(
                    "Logger",
                    "agents.LoggerAgent",
                    null);
            logger.start();

            List<String> playerNames = generatePlayerNames();
            Object[] gmArgs = buildGameMasterArgs(playerNames);

            // Criar Game Master
            AgentController gm = container.createNewAgent(
                    "GameMaster",
                    "agents.GameMasterAgent",
                    gmArgs);
            gm.start();

            // Criar jogadores
            for (String player : playerNames) {
                AgentController agent = container.createNewAgent(
                        player,
                        "agents.VillagerAgent",
                        null);
                agent.start();
            }

            System.out.println("Game launched with configuration:");
            printConfiguration();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================= MÉTODOS AUXILIARES ================= */

    /**
     * Gera nomes dos jogadores.
     */
    private List<String> generatePlayerNames() {
        List<String> players = new ArrayList<>();
        for (int i = 1; i <= numPlayers; i++) {
            players.add("Player" + i);
        }
        return players;
    }

    /**
     * Constrói argumentos para o Game Master.
     */
    private Object[] buildGameMasterArgs(List<String> players) {
        return new Object[] {
                players.toArray(new String[0]),
                numWerewolves,
                enableSeer,
                enableDoctor,
                enableHunter,
                maxPublicMessagesPerDay,
                enforceMessageLimit,
                maxRounds,
                demoMode,
                randomSeed
        };
    }

    /**
     * Imprime configuração (útil para logs experimentais).
     */
    private void printConfiguration() {
        System.out.println("Players: " + numPlayers);
        System.out.println("Werewolves: " + numWerewolves);
        System.out.println("Seer enabled: " + enableSeer);
        System.out.println("Doctor enabled: " + enableDoctor);
        System.out.println("Hunter enabled: " + enableHunter);
        System.out.println("Message limit/day: " + maxPublicMessagesPerDay);
        System.out.println("Max rounds: " + maxRounds);
        System.out.println("Random seed: " + randomSeed);
        System.out.println("Demo mode: " + demoMode);
    }

    /* ================= SETTERS (para experimentos) ================= */

    public GameLauncher setNumPlayers(int n) {
        this.numPlayers = n;
        return this;
    }

    public GameLauncher setNumWerewolves(int n) {
        this.numWerewolves = n;
        return this;
    }

    public GameLauncher enableSeer(boolean v) {
        this.enableSeer = v;
        return this;
    }

    public GameLauncher enableDoctor(boolean v) {
        this.enableDoctor = v;
        return this;
    }

    public GameLauncher enableHunter(boolean v) {
        this.enableHunter = v;
        return this;
    }

    public GameLauncher setMessageLimit(int limit) {
        this.maxPublicMessagesPerDay = limit;
        return this;
    }

    public GameLauncher setMaxRounds(int rounds) {
        this.maxRounds = rounds;
        return this;
    }

    public GameLauncher setSeed(long seed) {
        this.randomSeed = seed;
        return this;
    }
}
