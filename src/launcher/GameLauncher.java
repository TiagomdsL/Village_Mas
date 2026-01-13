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
    private int numPlayers = 20;
    private double werewolvesRatio = 0.3 + (Math.random() * 2 - 1) * 0.05;

    // Roles especiais
    private double seerRatio = 0.1 + (Math.random() * 2 - 1) * 0.03;
    private double doctorRatio = 0.1 + (Math.random() * 2 - 1) * 0.03;
    private double hunterRatio = 0.1 + (Math.random() * 2 - 1) * 0.03;

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
            profile.setParameter(Profile.GUI, "false");

            AgentContainer container = rt.createMainContainer(profile);


            List<String> playerNames = generatePlayerNames();
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

            // Criar Game Master (sem args; GameMaster obtém players/roles via DF)
            AgentController gm = container.createNewAgent(
                    "GameMaster",
                    "agents.GameMasterAgent",
                    null);
            gm.start();


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

        // adicionar as roles que não sejam villagers em si
        int wolves = (int) Math.floor(playersCount * werewolvesRatio);
        int seers = (int) Math.floor(playersCount * seerRatio);
        int doctors = (int) Math.floor(playersCount * doctorRatio);
        int hunters = (int) Math.floor(playersCount * hunterRatio);

        for (int i = 0; i < wolves; i++)
            pool.add(Role.WEREWOLF);

        for (int i = 0; i < seers; i++)
            pool.add(Role.SEER);

        for (int i = 0; i < doctors; i++)
            pool.add(Role.DOCTOR);

        for (int i = 0; i < hunters; i++)
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
        System.out.println("Werewolves: " + werewolvesRatio);
        System.out.println("Seer enabled: " + seerRatio);
        System.out.println("Doctor enabled: " + doctorRatio);
        System.out.println("Hunter enabled: " + hunterRatio);
        System.out.println("Message limit/day: " + maxPublicMessagesPerDay);
        System.out.println("Max rounds: " + maxRounds);
        System.out.println("Random seed: " + randomSeed);
        System.out.println("Demo mode: " + demoMode);
    }

    /* ================= SETTERS (para experimentos) ================= */

    public GameLauncher setNumPlayers(int n) {
        this.numPlayers = n;
        this.werewolvesRatio = Math.max(1, n / 5); // ajustar número de lobisomens
        return this;
    }

    public GameLauncher setSeerRatio(double r) {
        this.seerRatio = r + (Math.random() * 2 - 1) * 0.03;
        return this;
    }

    public GameLauncher setDoctorRatio(double r) {
        this.doctorRatio = r + (Math.random() * 2 - 1) * 0.03;
        return this;
    }

    public GameLauncher setHunterRatio(double r) {
        this.hunterRatio = r + (Math.random() * 2 - 1) * 0.03;
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
