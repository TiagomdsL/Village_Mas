package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import model.GamePhase;
import model.MessageType;
import model.Role;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Game Master:
 * - controla fases
 * - valida mensagens
 * - faz logging
 */
public class GameMasterAgent extends Agent {
    private final List<String>
            toKill = new ArrayList<>(),
            protectedPlayers = new ArrayList<>(),
            werewolvesPlayers = new ArrayList<>();

    // lista de players a morrer (votação dos werewolfes)
    private final Map<String, Integer> toDiePlayers = new HashMap<>();

    // map de roles obtidos por DF
    private final Map<String, Role> playerRoles = new HashMap<>();

    JSONObject gameState = new JSONObject();
    JSONArray eventLog = new JSONArray();
    private int round = 1;


    @Override
    protected void setup() {
        gameState.put("events", eventLog);
        addBehaviour(new GamePhaseBehaviour());
    }

    private class GamePhaseBehaviour extends FSMBehaviour {
        public GamePhaseBehaviour() {
            registerFirstState(new SetupPlayersBehaviour(), GamePhase.SETUP.toString());
            registerState(new DayPhaseBehaviour(myAgent, 1000), GamePhase.Day.toString());
            registerState(new NightPhaseBehaviour(myAgent, 1000), GamePhase.NIGHT.toString());
            registerLastState(new EndedBehaviour(), GamePhase.ENDED.toString());

            registerTransition(GamePhase.SETUP.toString(), GamePhase.NIGHT.toString(), 0);

            registerTransition(GamePhase.Day.toString(), GamePhase.NIGHT.toString(), 0);
            registerTransition(GamePhase.Day.toString(), GamePhase.ENDED.toString(), 1);

            registerTransition(GamePhase.NIGHT.toString(), GamePhase.Day.toString(), 0);
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

    private class DayPhaseBehaviour extends TickerBehaviour {
        private int ticks = 0;
        private boolean votingStarted = true;

        public DayPhaseBehaviour(Agent a, long period) {
            super(a, period);
        }

        @Override
        public void onStart() {
            super.onStart();
            ticks = 0;
            broadcastSystem("Day phase started.", MessageType.SYSTEM);
        }

        @Override
        public void onTick() {
            //tempo para discutirem
            ticks++;
            if (ticks >= 2 && ticks < 5) {
                if (votingStarted) {
                    broadcastSystem("Start Voting", MessageType.VOTE);
                    votingStarted = false;
                }
                ACLMessage msg = receive();
                while ((msg = receive()) != null) {
                    String sender = msg.getSender().getLocalName();
                    String content = msg.getContent();
                    String convId = msg.getConversationId();
                    mensage_type(convId, sender, content);
                }

            } else if (ticks >= 5) {
                votingStarted = true;
                stop();
            }
        }

        @Override
        public int onEnd() {
            killByVote("DAY");

            broadcastSystem("Day phase ended.", MessageType.SYSTEM);
            GamePhaseBehaviour fsm = (GamePhaseBehaviour) getParent();
            fsm.registerState(new NightPhaseBehaviour(myAgent, 1000), GamePhase.NIGHT.toString()); // para garantir que a noite seja a próxima fase devido aos tickes após a primeira noite
            return isEnded();
        }
    }

    private class NightPhaseBehaviour extends TickerBehaviour {
        private int ticks = 0;
        private ACLMessage msg;

        public NightPhaseBehaviour(Agent a, long period) {
            super(a, period);
        }

        // manda requests as roles especiais que estes conseguem agir a noite (no caso hunter, doctor, werewolf e seer)
        @Override
        public void onStart() {
            super.onStart();
            ticks = 0;
            broadcastSystem("Night phase started.", MessageType.SYSTEM);
            for (String p : playerRoles.keySet()) {
                Role r = playerRoles.get(p);
                if (r == Role.WEREWOLF) {
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(new AID(p, AID.ISLOCALNAME));
                    msg.setContent("It's night. Choose someone to kill.");
                    msg.setConversationId(MessageType.WEREWOLF_ATTACK.toString());
                    send(msg);
                } else if (r == Role.DOCTOR) {
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(new AID(p, AID.ISLOCALNAME));
                    msg.setContent("It's night. Choose someone to protect.");
                    msg.setConversationId(MessageType.DOCTOR_PROTECT.toString());
                    send(msg);
                } else if (r == Role.SEER) {
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
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

            while ((msg = receive()) != null) {
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
            killByVote("NIGHT");

            broadcastSystem("Night phase ended.", MessageType.SYSTEM);
            GamePhaseBehaviour fsm = (GamePhaseBehaviour) getParent();
            fsm.registerState(new DayPhaseBehaviour(myAgent, 1000), GamePhase.Day.toString()); // para garantir que o dia seja a próxima fase devido aos tickes após o primeir dia

            round++;
            return isEnded();
        }
    }

    private class EndedBehaviour extends OneShotBehaviour {
        @Override
        public void action() {

            System.out.println(gameState.toString(2));
            saveGameStateToFile();

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
     */
    private void queryRolesFromDF(long timeoutMs) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("player");
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length == 0)
                return;

            ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
            req.setConversationId(MessageType.ROLE_QUERY.toString());
            req.setContent("ROLE_REQUEST");
            for (DFAgentDescription dfd : result) {
                req.addReceiver(dfd.getName());
            }
            send(req);

            long deadline = System.currentTimeMillis() + timeoutMs;
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE),
                    MessageTemplate.MatchConversationId(MessageType.ROLE_QUERY.toString()));

            while (System.currentTimeMillis() < deadline) {
                long wait = deadline - System.currentTimeMillis();
                ACLMessage reply = blockingReceive(mt, wait); // gamemaster recolhe as subscrições para participar do jogo
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
//        System.out.println(players);

    }

    private void broadcastWerewolvesPlayers() {
        for (String p : playerRoles.keySet()) {
            Role r = playerRoles.get(p);
            if (r == Role.WEREWOLF) {
                werewolvesPlayers.add(p);
            }
        }
        String werewolves = String.join(", ", werewolvesPlayers);
        for (String p : werewolvesPlayers) {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(new AID(p, AID.ISLOCALNAME));
            msg.setContent("Werewolves players:" + werewolves);
            msg.setConversationId(MessageType.WEREWOLF_PLAYERS.toString());
            send(msg);
        }
    }


    // verificação se houve players mortos
    private void killPlayers() {
        if (!protectedPlayers.isEmpty()) {
            protectedPlayersJson("NIGHT");
            for (String p : protectedPlayers) {
                broadcastSystem("Player " + p + " was protected by the Doctor.", MessageType.SYSTEM);
                toKill.remove(p);
                System.out.println("Foi protegido um player");
            }
        }
        protectedPlayers.clear();

        List<String> deadPlayers = new ArrayList<>(toKill); //auxiliar por causa do hunter
        toKill.clear();

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
                broadcastSystem("The Player: " + player + " is Dead. " + playerRoles.get(player), MessageType.SYSTEM);
                playerRoles.remove(player);
            }
            deadPlayers.clear();
        } else {
            broadcastSystem("No one died this round.", MessageType.SYSTEM);
        }

        if (!toKill.isEmpty()) killPlayers(); // recursividade para tratar as mortes causadas pelo hunter
    }

    // mata o jogador com mais votos, na votação dos werewolfes ou dos villagers
    private void killByVote(String phase) {
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
                toKill.add(mostVotedPlayer);
            }
        }
        toDiePlayers.clear();
        killPlayersJson(phase);
        killPlayers();
        alivePlayersJson(phase);
    }

    // verificação da condição de fim de jogo
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
        } else if (convId.equals(MessageType.HUNTER_KILL.toString())) { // o hunter matou alguém, adicionar à lista de mortos
            System.out.println("Adicionando jogador a lista de mortos pelo hunter: " + content);
            hunterJson(sender, content);
            toKill.add(content);
        } else if (convId.equals(MessageType.VOTE.toString())) {
            toDiePlayers.put(content, toDiePlayers.getOrDefault(content, 0) + 1); // lista da votação
//            System.out.println("Voto recebido de " + sender + " para " + content);
        }
    }

    private void killPlayersJson(String phase) {
        JSONArray deadArray = new JSONArray();
        for (String p : toKill) {
            JSONObject playerObj = new JSONObject();
            playerObj.put("name", p);
            playerObj.put("role", playerRoles.get(p).toString());
            deadArray.put(playerObj);
        }
        logEvent(phase, "DEAD_PLAYERS", deadArray);
    }

    private void alivePlayersJson(String phase) {
        JSONArray aliveArray = new JSONArray();
        for (String p : playerRoles.keySet()) {
            JSONObject playerObj = new JSONObject();
            playerObj.put("name", p);
            playerObj.put("role", playerRoles.get(p).toString());
            aliveArray.put(playerObj);
        }
        logEvent(phase, "ALIVE_PLAYERS", aliveArray);
    }

    private void protectedPlayersJson(String phase) {
        JSONArray protectedArray = new JSONArray();
        for (String p : protectedPlayers) {
            JSONObject playerObj = new JSONObject();
            playerObj.put("name", p);
            playerObj.put("role", playerRoles.get(p).toString());
            protectedArray.put(playerObj);
        }
        logEvent(phase, "PROTECTED_PLAYERS", protectedArray);
    }

    private void hunterJson(String hunter, String target) {
        JSONArray hunterArray = new JSONArray();
        JSONObject actionObj = new JSONObject();
        actionObj.put("hunter", hunter);
        actionObj.put("target", target);
        hunterArray.put(actionObj);
        logEvent("NIGHT", "HUNTER_KILL", hunterArray);
    }

    private void logEvent(String phase, String type, JSONArray data) {
        JSONObject event = new JSONObject();
        event.put("round", round);
        event.put("phase", phase);
        event.put("type", type);
        event.put("data", data);

        eventLog.put(event);

        gameState.put("events", eventLog);

        System.out.println(event.toString());
    }



    private void saveGameStateToFile() {
        try {
            Path path = Paths.get("game_state.json");
            Files.write(
                    path,
                    gameState.toString(2).getBytes(StandardCharsets.UTF_8)
            );
            System.out.println("Json guardado " + path.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
