package launcher;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Collections;

import model.Role;

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

            // Criar Game Master (sem args; GameMaster obtém players/roles via DF)
            AgentController gm = container.createNewAgent(
                    "GameMaster",
                    "agents.GameMasterAgent",
                    null);
            gm.start();

            // Gerar e atribuir roles localmente aqui para escolher a classe do agente a
            // criar
            List<Role> rolePool = buildRolePool(playerNames.size());
            Random rnd = new Random(randomSeed);
            Collections.shuffle(rolePool, rnd);

            for (int i = 0; i < playerNames.size(); i++) {
                String player = playerNames.get(i);
                Role role = rolePool.get(i);
                String className = classForRole(role);

                AgentController agent = container.createNewAgent(
                        player,
                        className,
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
     * Constrói pool de roles de acordo com as flags e número de lobisomens.
     */
    private List<Role> buildRolePool(int playersCount) {
        List<Role> pool = new ArrayList<>();

        // adicionar lobisomens
        int wolves = Math.min(numWerewolves, playersCount);
        for (int i = 0; i < wolves; i++)
            pool.add(Role.WEREWOLF);

        if (enableSeer)
            pool.add(Role.SEER);
        if (enableDoctor)
            pool.add(Role.DOCTOR);
        if (enableHunter)
            pool.add(Role.HUNTER);

        // preencher o resto com VILLAGER
        while (pool.size() < playersCount)
            pool.add(Role.VILLAGER);

        // se sobrar (menos jogadores que roles desejadas) aparar extras (remove do fim)
        while (pool.size() > playersCount)
            pool.remove(pool.size() - 1);

        return pool;
    }

    /**
     * Mapeia Role -> classe de agente (package 'agents').
     */
    private String classForRole(Role role) {
        return switch (role) {
            case WEREWOLF -> "agents.WerewolfAgent";
            case SEER -> "agents.SeerAgent";
            case DOCTOR -> "agents.DoctorAgent";
            case HUNTER -> "agents.HunterAgent";
            case VILLAGER -> "agents.VillagerAgent";
            default -> "agents.VillagerAgent";
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
        this.numWerewolves = Math.max(1, n / 5); // ajustar número de lobisomens
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
