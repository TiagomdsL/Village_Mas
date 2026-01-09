package agents;

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

        addBehaviour(new ListenerBehavior());
    }

    public class ListenerBehavior extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                if( msg.getConversationId().equals(MessageType.ROLE_QUERY.toString()) ){
                    respondToRoleQuery(msg);
                }
                else if( msg.getConversationId().equals(MessageType.KILL_NOTIFICATION.toString())){
                    dyingPlayer(msg);
                }
                else if( msg.getConversationId().equals(MessageType.ALIVE_PLAYERS.toString())){
                    checkAlivePlayers(msg);
                }
                else if ( msg.getConversationId().equals(MessageType.SYSTEM.toString()) ) {
                    String content = msg.getContent();
                    if( content.contains("is Dead")){
                        try {
                            String deadPlayer = content
                                    .replace("The Player:", "")
                                    .replace("is Dead.", "")
                                    .trim();

                            // Remover das estruturas internas
                            trust.remove(deadPlayer);
                            beliefs.remove(deadPlayer);

                            System.out.println(
                                    getLocalName() + " removed dead player from trust/beliefs: " + deadPlayer + " bc is dead."
                            );

                        } catch (Exception e) {
                            System.err.println(
                                    getLocalName() + " failed to parse death message: " + content
                            );
                        }
                    }
                }

            } else {
                block();
            }
        }
    }

    private void respondToRoleQuery(ACLMessage msg) {
        System.out.println( getLocalName() + " respondendo role query: " + myRole.name());
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setConversationId("role-query");
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

    protected void checkAlivePlayers(ACLMessage msg) {
        String content = msg.getContent();
        if (content == null) {
            return;
        }

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

    protected void dyingPlayer(ACLMessage msg) {
        String content = msg.getContent();
        if (content.contains("You are dead.")) {
            System.out.println(getLocalName() + " received death notice.");
            doDelete();
        }
    }

    protected abstract void processMessage(MessageType messageType, String content, String sender);
    protected abstract void handleAccusation(String content, String sender); // Processar acusação
    protected abstract void handleRoleClaim(String content, String sender); // Processar revelação de papel
    protected abstract void handleVote(String content, String sender);      // Processar voto
    protected abstract void handleSystem(String content, String sender);    // Processar mensagem do sistema
    protected abstract void handleTrust(String content, String sender);     // Processar confiança

    protected abstract void decideAction();

}
