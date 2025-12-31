package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import model.Role;

import java.util.HashMap;
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

        // Recebe REQUEST de role-query e responde com INFORM "ROLE:<ROLE_NAME>"
        addBehaviour(new CyclicBehaviour(this) {
            private final MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("role-query"));

            @Override
            public void action() {
                ACLMessage msg = myAgent.receive(mt);
                if (msg != null) {
                    String content = msg.getContent();
                    if ("ROLE_REQUEST".equals(content)) {
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setConversationId("role-query");
                        String roleStr = (myRole != null) ? myRole.name() : "UNASSIGNED";
                        reply.setContent("ROLE:" + roleStr);
                        myAgent.send(reply);
                    }
                } else {
                    block();
                }
            }
        });
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

    protected abstract void decideAction();
}
