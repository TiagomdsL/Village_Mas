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
    private List<String>
            deadPlayers = new ArrayList<>(),
            protectedPlayers = new ArrayList<>(),
            werewolvesPlayers = new ArrayList<>();

    // lista de players a morrer (votação dos werewolfes)
    private Map<String, Integer> toDiePlayers = new HashMap<>();

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

        addBehaviour(new GamePhaseBehaviour());
    }

    private class GamePhaseBehaviour extends FSMBehaviour {
        public GamePhaseBehaviour() {
            registerFirstState(new SetupPlayersBehaviour(), GamePhase.SETUP.toString());
            registerState(new VotePhaseBehaviour(myAgent, 1000), GamePhase.VOTING.toString());
            registerState(new NightPhaseBehaviour(myAgent, 1000), GamePhase.NIGHT.toString());
            registerLastState(new EndedBehaviour(), GamePhase.ENDED.toString());

            registerTransition(GamePhase.SETUP.toString(), GamePhase.VOTING.toString(), 0);

            registerTransition(GamePhase.VOTING.toString(), GamePhase.NIGHT.toString(), 0);
            registerTransition(GamePhase.VOTING.toString(), GamePhase.ENDED.toString(), 1);
//
            registerTransition(GamePhase.NIGHT.toString(), GamePhase.VOTING.toString(), 0);
            registerTransition(GamePhase.NIGHT.toString(), GamePhase.ENDED.toString(), 1);

        }

    }

    // Busca os players e seus roles no DF
    private class SetupPlayersBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            queryRolesFromDF(2000);
        }

        @Override
        public int onEnd() {
            broadcastSystem("Init game", MessageType.SYSTEM);
            broadcastWerewolvesPlayers();
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
            ticks = 0;
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
            killPlayers();
            GamePhaseBehaviour fsm = (GamePhaseBehaviour) getParent();
            fsm.registerState(new NightPhaseBehaviour(myAgent, 1000), GamePhase.NIGHT.toString());
            return isEnded();
        }
    }

    private class NightPhaseBehaviour extends TickerBehaviour {
        private int ticks = 0;

        public NightPhaseBehaviour(Agent a, long period) {
            super(a, period);
        }

        // manda msg as roles especiais que estes conseguem agir a noite (no caso hunter, doctor, werewolf e seer)
        @Override
        public void onStart() {
            super.onStart();
            ticks = 0;
            broadcastSystem("Night phase started.", MessageType.SYSTEM);
            for (String p : playerRoles.keySet()) {
                Role r = playerRoles.get(p);
                if (r == Role.WEREWOLF) {
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.addReceiver(new AID(p, AID.ISLOCALNAME));
                    msg.setContent("It's night. Choose someone to kill.");
                    msg.setConversationId(MessageType.WEREWOLF_ATTACK.toString());
                    send(msg);
                } else if (r == Role.DOCTOR) {
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.addReceiver(new AID(p, AID.ISLOCALNAME));
                    msg.setContent("It's night. Choose someone to protect.");
                    msg.setConversationId(MessageType.DOCTOR_PROTECT.toString());
                    send(msg);
                } else if (r == Role.SEER) {
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.addReceiver(new AID(p, AID.ISLOCALNAME));
                    msg.setContent("revela");
                    msg.setConversationId(MessageType.SEER_REVEAL.toString());
                    send(msg);
                }
            }
        }

        @Override
        protected void onTick() {
            ticks++;

            ACLMessage msg = receive(); //blockingReceive();
            if (msg != null) {
                String sender = msg.getSender().getLocalName();
                String content = msg.getContent();
                String convId = msg.getConversationId();
                mensage_type(convId, sender, content);
            }
            if (ticks >= 5) {
                stop();
            }
        }

        @Override
        public int onEnd() {
            if (!toDiePlayers.isEmpty()) {
                String mostVotedPlayer = null;
                int maxVotes = -1;

                for (Map.Entry<String, Integer> entry : toDiePlayers.entrySet()) {
                    if (entry.getValue() > maxVotes) {
                        maxVotes = entry.getValue();
                        mostVotedPlayer = entry.getKey();
                    }
                }

                if (mostVotedPlayer != null) {
                    deadPlayers.add(mostVotedPlayer);
                }
            }
            toDiePlayers.clear();
            killPlayers();


            GamePhaseBehaviour fsm = (GamePhaseBehaviour) getParent();
            fsm.registerState(new VotePhaseBehaviour(myAgent, 1000), GamePhase.VOTING.toString());

            return isEnded();
        }
    }

    private class EndedBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            for (Role role : playerRoles.values()) {
                if (role == Role.WEREWOLF) {
                    broadcastSystem("Game ended, vitoria dos lobisomens.", MessageType.SYSTEM);
                    myAgent.doDelete();
                    return;
                }
            }
            broadcastSystem("Game ended, vitoria dos aldeões.", MessageType.SYSTEM);
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
                    MessageTemplate.MatchConversationId(MessageType.ROLE_QUERY.toString()));


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

    private void broadcastWerewolvesPlayers() {
        for (String p : playerRoles.keySet()) {
            Role r = playerRoles.get(p);
            if (r == Role.WEREWOLF) {
                werewolvesPlayers.add(p);
            }
        }
        String werewolves = String.join(", ", werewolvesPlayers);
        broadcastSystem("Werewolves players:" + werewolves, MessageType.WEREWOLF_PLAYERS);
        System.out.println(werewolves);
    }


    private void killPlayers() {
        if (!protectedPlayers.isEmpty()) {
            for (String p : protectedPlayers) {
                deadPlayers.remove(p);
                System.out.println("Foi protegido um player");
            }
        }
        protectedPlayers.clear();

        if (!deadPlayers.isEmpty()) {
            for (String player : deadPlayers) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(new AID(player, AID.ISLOCALNAME));
                msg.setContent("You are dead.");

                // se for um hunter, este será notificado para matar alguém
                if (playerRoles.get(player) == Role.HUNTER)
                    msg.setConversationId(MessageType.HUNTER_KILL.toString());
                else
                    msg.setConversationId(MessageType.KILL_NOTIFICATION.toString()); // se não for, notifica apenas a morte

                send(msg);
                playerRoles.remove(player);
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

        return (werewolves == 0 || villagers <= werewolves) ? 1 : 0;
    }

    private void mensage_type(String convId, String sender, String content) {
        if (convId.equals(MessageType.WEREWOLF_ATTACK.toString())) {
            toDiePlayers.put(content, toDiePlayers.getOrDefault(content, 0) + 1); // lista da votação
        } else if (convId.equals(MessageType.DOCTOR_PROTECT.toString())) {
            protectedPlayers.add(content);
        } else if (convId.equals(MessageType.HUNTER_KILL.toString())) {
            deadPlayers.add(content);
        }

    }

}
