package agents;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import model.*;

import java.util.*;

/**
 * Game Master:
 * - atribui roles
 * - controla fases
 * - valida mensagens
 * - faz logging
 */
public class GameMasterAgent extends Agent {

    private Map<String, Role> roles = new HashMap<>();
    private GamePhase phase = GamePhase.NIGHT;
    private List<String> alivePlayers = new ArrayList<>();

    @Override
    protected void setup() {
        Object[] args = getArguments();
        String[] players = (String[]) args[0];

        alivePlayers.addAll(Arrays.asList(players));
        assignRoles(players);

        broadcastSystem("Game started. Night phase.");
        nextPhase();
    }

    private void assignRoles(String[] players) {
        List<Role> pool = new ArrayList<>();
        pool.add(Role.WEREWOLF);
        pool.add(Role.SEER);
        pool.add(Role.DOCTOR);
        pool.add(Role.HUNTER);

        while (pool.size() < players.length) {
            pool.add(Role.VILLAGER);
        }

        Collections.shuffle(pool);

        for (int i = 0; i < players.length; i++) {
            roles.put(players[i], pool.get(i));
            sendRole(players[i], pool.get(i));
        }
    }

    private void sendRole(String player, Role role) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID(player, AID.ISLOCALNAME));
        msg.setContent("ROLE:" + role);
        send(msg);
    }

    private void broadcastSystem(String text) {
        for (String p : alivePlayers) {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(new AID(p, AID.ISLOCALNAME));
            msg.setContent("SYSTEM:" + text);
            send(msg);
        }
    }

    private void nextPhase() {
        switch (phase) {
            case NIGHT -> phase = GamePhase.DAY;
            case DAY -> phase = GamePhase.VOTING;
            case VOTING -> phase = GamePhase.NIGHT;
            case ENDED -> System.exit(0);
            default -> throw new IllegalArgumentException("Unexpected value: " + phase);
        }
        broadcastSystem("Phase changed to " + phase);
    }
}
