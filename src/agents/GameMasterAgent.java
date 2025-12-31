package agents;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import model.*;

import java.util.*;

/**
 * Game Master:
 * - controla fases
 * - valida mensagens
 * - faz logging
 */
public class GameMasterAgent extends Agent {

    private GamePhase phase = GamePhase.NIGHT;
    private List<String> alivePlayers = new ArrayList<>();

    // map de roles obtidos por DF
    private Map<String, Role> playerRoles = new HashMap<>();

    /**
     * Pergunta a todos os agentes do DF com service type "player" qual é o seu
     * role.
     * Envia um REQUEST com conversationId "role-query" e espera replies INFORM com
     * "ROLE:<ROLE_NAME>".
     * 
     * @param timeoutMs tempo total (ms) para aguardar respostas
     * @return mapa jogadorLocalName -> Role (somente respostas válidas)
     */
    public Map<String, Role> queryRolesFromDF(long timeoutMs) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("player");
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length == 0)
                return Collections.emptyMap();

            ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
            req.setConversationId("role-query");
            req.setContent("ROLE_REQUEST");
            for (DFAgentDescription dfd : result) {
                req.addReceiver(dfd.getName());
            }
            send(req);

            long deadline = System.currentTimeMillis() + timeoutMs;
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("role-query"));

            while (System.currentTimeMillis() < deadline) {
                long wait = deadline - System.currentTimeMillis();
                ACLMessage reply = blockingReceive(mt, wait);
                if (reply == null)
                    break;
                String sender = reply.getSender().getLocalName();
                String content = reply.getContent();
                if (content != null && content.startsWith("ROLE:")) {
                    try {
                        Role r = Role.valueOf(content.substring(5));
                        playerRoles.put(sender, r);
                    } catch (IllegalArgumentException ignored) {
                        /* resposta inválida */ }
                }
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        return new HashMap<>(playerRoles);
    }

    @Override
    protected void setup() {
        // Registrar o GameMaster no DF para que players possam localizá-lo
        DFAgentDescription myDesc = new DFAgentDescription();
        myDesc.setName(getAID());
        ServiceDescription mySd = new ServiceDescription();
        mySd.setType("gamemaster");
        mySd.setName("GameMaster");
        myDesc.addServices(mySd);
        try {
            DFService.register(this, myDesc);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        // Buscar agentes "player" registrados no DF
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("player");
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            String[] players = new String[result.length];
            for (int i = 0; i < result.length; i++) {
                players[i] = result[i].getName().getLocalName();
            }

            alivePlayers.addAll(Arrays.asList(players));
            playerRoles = queryRolesFromDF(5000);

            broadcastSystem("Game started. Night phase.");
            nextPhase();
        } catch (FIPAException e) {
            e.printStackTrace();
            broadcastSystem("No players found in DF. Game cannot start.");
        }
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
