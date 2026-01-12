package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import model.MessageType;
import model.Role;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classe base para todos os jogadores.
 * Implementa crenças probabilísticas e confiança.
 */
public abstract class AbstractPlayerAgent extends Agent {

    protected Role myRole;
    protected String gameMasterAddr;

    // P(role | evidence) para cada jogador
    protected Map<String, Map<Role, Double>> beliefs = new HashMap<>();

    // Métrica de confiança [0,1]
    protected Map<String, Double> trust = new HashMap<>();

    // Lista de mortos que já foram revelados como lobisomens
    protected List<String> wasWerewolf = new java.util.ArrayList<>();


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
                    type = MessageType.valueOf(msg.getConversationId()); // listener
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (type == null) return;
                processMessage(type, msg.getContent(), msg.getSender().getLocalName());

            }
        });
    }

    protected void respondToRoleQuery(String sender) {
//        System.out.println(getLocalName() + " respondendo role query: " + myRole.name());
        ACLMessage reply = new ACLMessage(ACLMessage.SUBSCRIBE); // subscreve ao gamemaster para participar do jogo, enviando a sua role
        reply.addReceiver(new AID(sender, AID.ISLOCALNAME));
        gameMasterAddr = sender;
        reply.setConversationId(MessageType.ROLE_QUERY.toString());
        reply.setContent("ROLE:" + myRole.name());
        send(reply);
    }

    protected void initBeliefs(String[] players) {
        for (String p : players) {
            Map<Role, Double> probs = new HashMap<>();
            for (Role r : Role.values())
                probs.put(r, 1.0 / Role.values().length);

            beliefs.put(p, probs);
            trust.put(p, 0.5); // neutro
        }
    }

    protected void updateTrust(String player, double delta) {
        trust.put(player, Math.max(0, Math.min(1, trust.get(player) + delta)));
    }

    protected void updateBelief(String player, Role role, double prob) {
        Map<Role, Double> probs = beliefs.get(player);
        if (probs == null) return;
        probs.put(role, prob);
        // normalizar
        double total = 0.0;
        for (double p : probs.values())
            total += p;
        for (Role r : probs.keySet())
            probs.put(r, probs.get(r) / total);
    }

    protected void checkAlivePlayers(String content) {
        if (!content.startsWith("Players vivos:")) return;

        String playersStr = content.replace("Players vivos:", "").trim();

        if (!playersStr.isEmpty()) {
            String[] players = playersStr.split("\\s*,\\s*");
            initBeliefs(players);
        }
    }


    // Gere as mensagens recebidas
    protected void processMessage(MessageType messageType, String content, String sender) {
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
                handleSystem(content);
                break;
            case SEER_REVEAL:
                detectedBySeer(sender);
                break;
            default:
                break;
        }
    }

    // da a role quando o seer o observa (recebe uma mensagem deste)
    private void detectedBySeer(String sender) {
        ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
        reply.addReceiver(new AID(sender, AID.ISLOCALNAME));
        reply.setPerformative(ACLMessage.INFORM);
        reply.setConversationId(MessageType.SEER_RECEIVE.name());
        reply.setContent(myRole.name());
        send(reply);
    }

    protected void handleSystem(String content) {
        if (content.contains("is Dead")) {
            try {
                String[] deadPlayer = content
                        .replace("The Player:", "")
                        .replace("is Dead", "")
                        .split("\\.");

                String deadPlayerStr = deadPlayer[0].trim();
                String roleStr = deadPlayer[1].trim();

                // remove o player morto das listas de confiança
                trust.remove(deadPlayerStr);
                beliefs.remove(deadPlayerStr);

                for (String p : trust.keySet()) {
                    if (!p.equals(deadPlayerStr)) {
                        if (!roleStr.equals(Role.WEREWOLF.name())) {
                            updateTrust(p, -0.15); // diminui a confiança em outros jogadores, causando panico
                        } else {
                            wasWerewolf.add(deadPlayerStr);
                            updateTrust(p, 0.15); // aumenta a confiança em outros jogadores, aliviando o medo
                        }
                    }
                }

            } catch (Exception e) {
                System.err.println(getLocalName() + " failed to parse death message: " + content);
            }
        }

    }    // Processar mensagem do sistema

    protected abstract void handleAccusation(String content, String sender); // Processa acusação
    protected abstract void handleRoleClaim(String content, String sender); // Processa revelação de papel
    protected abstract void handleVote(String content, String sender);      // Processa voto
    protected abstract void handleTrust(String content, String sender);     // Processa confiança
}
