package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import model.MessageType;
import model.Role;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classe base para todos os jogadores.
 * Implementa crenças probabilísticas e confiança.
 */
public abstract class AbstractPlayerAgent extends Agent {

    protected Role myRole;

    // P(role | evidence) para cada jogador
    protected Map<String, Map<Role, Double>> beliefs = new HashMap<>();

    // Métrica de confiança [0,1]
    protected Map<String, Double> trust = new HashMap<>();


    @Override
    protected void setup() {
        System.out.println(getLocalName() + " started.");

        // Registrar este agente no DF como service type "player"
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("player");
        sd.setName("Player");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = myAgent.receive();
                if (msg == null) {
                    block();
                    return;
                }

                MessageType type = null;
                try {
                    type = MessageType.valueOf(msg.getConversationId());
                    System.out.println(type);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (type == null) return;
                processMessage(type, msg.getContent(), msg.getSender().getLocalName());


            }
        });
    }

    private void respondToRoleQuery(String sender) {
        System.out.println( getLocalName() + " respondendo role query: " + myRole.name());
        ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
        reply.addReceiver(new AID(sender, AID.ISLOCALNAME));
        reply.setPerformative(ACLMessage.INFORM);
        reply.setConversationId(MessageType.ROLE_QUERY.toString());
        reply.setContent("ROLE:" + myRole.name());
        send(reply);
    }

    protected void initBeliefs(String[] players) {
        for (String p : players) {
            Map<Role, Double> probs = new HashMap<>();
            for (Role r : Role.values()) {
                probs.put(r, 1.0 / Role.values().length);
            }
            beliefs.put(p, probs);
            trust.put(p, 0.5); // neutro
        }
    }

    protected void updateTrust(String player, double delta) {
        trust.put(player, Math.max(0, Math.min(1, trust.get(player) + delta)));
    }

    protected void checkAlivePlayers(String content) {


        if (!content.startsWith("Players vivos:")) {
            return;
        }

        String playersStr = content.replace("Players vivos:", "").trim();


        if (!playersStr.isEmpty()) {
            String[] players = playersStr.split("\\s*,\\s*");
            initBeliefs(players);
        }

        System.out.println(getLocalName() + "Alive players updated: " + trust);

    }


    protected void processMessage(MessageType messageType, String content, String sender) { //temporario para n dar erro
        switch (messageType) {
            case ACCUSATION:
                handleAccusation(content, sender);
                break;
            case TRUST:
                handleTrust(content, sender);
                break;
            case ROLE_CLAIM:
                handleRoleClaim(content, sender);
                break;
            case VOTE:
                handleVote(content, sender);
                break;
            case ROLE_QUERY:
                respondToRoleQuery(sender);
                break;
            case KILL_NOTIFICATION:
                System.out.println(getLocalName() + " received death notice.");
                doDelete();
                break;
            case ALIVE_PLAYERS:
                checkAlivePlayers(content);
                break;

            case SYSTEM:
                handleSystem(content, sender);
                break;
            default:
                System.out.println(messageType);
                break;
        }
    }
    protected abstract void handleAccusation(String content, String sender); // Processar acusação
    protected abstract void handleRoleClaim(String content, String sender); // Processar revelação de papel
    protected abstract void handleVote(String content, String sender);      // Processar voto
    protected abstract void handleSystem(String content, String sender);    // Processar mensagem do sistema
    protected abstract void handleTrust(String content, String sender);     // Processar confiança

    protected abstract void decideAction();

}
