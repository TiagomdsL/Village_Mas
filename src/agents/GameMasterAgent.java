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
            allPlayers = new ArrayList<>(),
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
            registerState(new AfternoonPhaseBehaviour(myAgent, 2000), GamePhase.AFTERNOON.toString());


            registerTransition(GamePhase.SETUP.toString(), GamePhase.AFTERNOON.toString(), 0);
            registerTransition(GamePhase.AFTERNOON.toString(), GamePhase.ENDED.toString(), 1);

        }


    }

    private class SetupPlayersBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            queryRolesFromDF(2000);
        }

        @Override
        public int onEnd() {
            System.out.println(playerRoles);
            broadcastSystem("Init game");
            return 0;
        }
    }

    private class AfternoonPhaseBehaviour extends TickerBehaviour {
        private int transition = 0;

        public AfternoonPhaseBehaviour(Agent a, long period) {
            super(a, period);
        }

        @Override
        public void onTick() {
            broadcastSystem("Afternoon phase started.");

            if ( !deadPlayers.isEmpty()) {
                String playerDead = deadPlayers.removeFirst();
                //System.out.println( playerRoles.remove(playerDead));
                killPlayer(playerDead);
                broadcastSystem("The xerife kill the Player: " + playerDead);
            } else {
                broadcastSystem("No players were killed by the xerife.");
            }
            transition = isEnded();
            broadcastAlivePlayers();
            stop();
        }

        @Override
        public int onEnd() {
            System.out.println("Entrou");
            return transition;
        }
    }

    private class EndedBehaviour extends OneShotBehaviour {
        @Override
        public void action() {

            broadcastSystem("Game ended.");

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
                        System.out.println(sender);
                    } catch (IllegalArgumentException ignored) {
                        /* resposta inválida */
                    }
                }
            }
            allPlayers.addAll(Arrays.asList(players));
//            broadcastSystem("Game started. Night phase.");
//            nextPhase();

        } catch (FIPAException e) {
            e.printStackTrace();
        }

        return new HashMap<>(playerRoles);
    }

    private void broadcastSystem(String text) {
        for (String p : playerRoles.keySet()) {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(new AID(p, AID.ISLOCALNAME));
            msg.setContent(text);
            send(msg);
        }
        System.out.println("SYSTEM: " + text);
    }

    private void broadcastAlivePlayers() {
        String players = String.join(", ",  playerRoles.keySet());
        broadcastSystem("Players vivos:" + players);
        System.out.println(players);
    }


    private void killPlayer(String player) {
            playerRoles.remove(player);
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(new AID(player, AID.ISLOCALNAME));
            msg.setContent("You are dead.");
            send(msg);

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

        return  (werewolves == 0 || villagers == 0) ? 1 : 0;
    }

}
