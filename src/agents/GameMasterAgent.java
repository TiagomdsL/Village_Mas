package agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
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
    private List<String>
            deadPlayers = new ArrayList<>(),
            protectedPlayers = new ArrayList<>();

    // map de roles obtidos por DF
    private Map<String, Role> playerRoles = new HashMap<>();


    @Override
    protected void setup() {

        deadPlayers.add("Player1"); // apenas para testes

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
        //playerRoles = queryRolesFromDF(5000);
        addBehaviour(new GamePhaseBehaviour());
    }

    private class GamePhaseBehaviour extends FSMBehaviour {
        //        private static final String
//                SETUP_PLAYERS = "SETUP_PLAYERS",
        public GamePhaseBehaviour() {
            registerFirstState(new SetupPlayersBehaviour(), GamePhase.SETUP.toString());
            registerState(new VotePhaseBehaviour(myAgent, 1), GamePhase.VOTING.toString());

            registerLastState(new EndedBehaviour(), GamePhase.ENDED.toString());


            registerTransition(GamePhase.SETUP.toString(), GamePhase.VOTING.toString(), 0);
            registerTransition(GamePhase.VOTING.toString(), GamePhase.ENDED.toString(), 1);

        }


    }

    private class SetupPlayersBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            queryRolesFromDF(2000);
        }

        @Override
        public int onEnd() {
//            System.out.println(playerRoles);
            broadcastSystem("Init game", MessageType.SYSTEM);
            broadcastAlivePlayers();
            return 0;
        }
    }

    private class VotePhaseBehaviour extends TickerBehaviour {

        private int ticks = 0;

        public VotePhaseBehaviour(Agent a, long period) {
            super(a, period);
        }

        @Override
        public void onStart() {
            super.onStart();
            broadcastSystem("Vote phase started.", MessageType.SYSTEM);
        }

        @Override
        public void onTick() {
            // TODO votação, onde recebe os votos via ACL, e verifica um treshold para matar alguem

            ticks++;
            if (ticks >= 2) {
                stop();
            }
        }

        @Override
        public int onEnd() {
            killPlayer();

            //broadcastAlivePlayers();
            System.out.println("Entrou");
            return isEnded();
        }
    }

    private class NightPhaseBehaviour extends TickerBehaviour {
        public NightPhaseBehaviour(Agent a, long period) {
            super(a, period);
        }

        @Override
        public void onStart() {
            super.onStart();
            broadcastSystem("Night phase started.", MessageType.SYSTEM);

        }

        @Override
        protected void onTick() {

        }

    }

    private class EndedBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            broadcastSystem("Game ended.", MessageType.SYSTEM);
            myAgent.doDelete();
        }
    }


    // ---------------------------- Metodos Auxiliares ----------------------------

    /**
     * Pergunta a todos os agentes do DF com service type "player" qual é o seu
     * role.
     * Envia um REQUEST com conversationId "role-query" e espera replies INFORM com
     * "ROLE:<ROLE_NAME>".
     *
     * @param timeoutMs tempo total (ms) para aguardar respostas
     * @return mapa jogadorLocalName -> Role (somente respostas válidas)
     */
    private Map<String, Role> queryRolesFromDF(long timeoutMs) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("player");
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length == 0)
                return Collections.emptyMap();

            ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
            req.setConversationId(MessageType.ROLE_QUERY.toString());
            req.setContent("ROLE_REQUEST");
            for (DFAgentDescription dfd : result) {
                req.addReceiver(dfd.getName());
            }
            send(req);

            long deadline = System.currentTimeMillis() + timeoutMs;
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("role-query"));

            String[] players = new String[result.length];

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
                        players[playerRoles.size() - 1] = sender;
                    } catch (IllegalArgumentException ignored) {
                        /* resposta inválida */
                    }
                }
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        return new HashMap<>(playerRoles);
    }

    private void broadcastSystem(String text, MessageType type) {
        for (String p : playerRoles.keySet()) {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(new AID(p, AID.ISLOCALNAME));
            msg.setContent(text);
            msg.setConversationId(type.toString());
            send(msg);
        }
        System.out.println("SYSTEM: " + text);
    }

    private void broadcastAlivePlayers() {
        String players = String.join(", ", playerRoles.keySet());
        broadcastSystem("Players vivos:" + players, MessageType.ALIVE_PLAYERS);
        System.out.println(players);
    }


    private void killPlayer() {
        if (!protectedPlayers.isEmpty()) {
            for (String p : protectedPlayers) {
                deadPlayers.remove(p);
            }
            protectedPlayers.clear();
        }

        if (!deadPlayers.isEmpty()) {
            for (String player : deadPlayers) {
                playerRoles.remove(player);
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(new AID(player, AID.ISLOCALNAME));
                msg.setContent("You are dead.");
                msg.setConversationId(MessageType.KILL_NOTIFICATION.toString());
                send(msg);
                broadcastSystem("The Player: " + player + " is Dead.", MessageType.SYSTEM);
            }
            deadPlayers.clear();
        } else {
            broadcastSystem("No one died this round.", MessageType.SYSTEM);
        }

    }

    private int isEnded() {
        int werewolves = 0, villagers = 0;

        for (Role role : playerRoles.values()) {
            if (role == Role.WEREWOLF) {
                werewolves++;
            } else if (role == Role.VILLAGER) {
                villagers++;
            }
        }

        return (werewolves == 0 || villagers == 0) ? 1 : 0;
    }

}
