package agents;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import model.MessageType;
import model.Role;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SeerAgent extends VillagerAgent {

    protected Map<String, String> seerResults = new HashMap<>();
    protected boolean needToAcuse = false;
    protected String accuseTarget = null;
    protected boolean needToProtect = false;
    protected String protectTarget = null;

    @Override
    protected void setup() {
        super.setup();
        this.myRole = model.Role.SEER;
    }

    protected void processMessage(MessageType messageType, String content, String sender) {
        super.processMessage(messageType, content, sender);
        if (messageType == MessageType.SEER_REVEAL)
            handleSeer(sender);
        else if (messageType == MessageType.SEER_RECEIVE)
            handleSeerReceive(content, sender);
    }

    // requisita a role ao agente que menos confia, para investigar
    private void handleSeer(String sender) {
        String target = null;
        double minTrust = Double.POSITIVE_INFINITY;
        for (Map.Entry<String, Double> e : super.trust.entrySet()) {
            String name = e.getKey();
            double t = e.getValue();
            if (name.equals(getLocalName())) continue;
            if (t < minTrust) {
                minTrust = t;
                target = name;
            }
        }
        if (target != null) {
            ACLMessage reveal = new ACLMessage(ACLMessage.REQUEST);
            reveal.setConversationId(MessageType.SEER_REVEAL.name());
            reveal.setContent("I see you, but u are distracted.");
            reveal.addReceiver(new AID(target, AID.ISLOCALNAME));
            send(reveal);
        }
    }

    // recebe a role e altera o trust com base no que recebeu
    private void handleSeerReceive(String content, String sender) {
        if (content == null) return;
        content = content.trim();
        if (content.isEmpty()) return;
        seerResults.put(sender, content);
        if (content.equals(Role.WEREWOLF.name())) {
            super.updateTrust(sender, -1.0);
            needToAcuse = true;
            accuseTarget = sender;
        } else if (content.equals(Role.DOCTOR.name())) {
            needToProtect = true;
            protectTarget = sender;
            super.updateTrust(sender, 1.0);
        } else super.updateTrust(sender, 1.0);

        ACLMessage ack = new ACLMessage(ACLMessage.INFORM); // confirma rececao da informacao
        ack.setConversationId(MessageType.SEER_REVEAL.name());
        ack.addReceiver(new AID(gameMasterAddr, AID.ISLOCALNAME));
        ack.setContent(sender);
        send(ack);
    }

    @Override
    protected void discuss() {
        // guarda as pessoas que sejam werewolfs ou doctors conhecidos para acusar e proteger respetivamente
        for (String player : super.trust.keySet())
            if (seerResults.containsKey(player) && seerResults.get(player).equals(Role.WEREWOLF.name())) {
                needToAcuse = true;
                accuseTarget = player;
            }

        for (String player : super.trust.keySet())
            if (seerResults.containsKey(player) && seerResults.get(player).equals(Role.DOCTOR.name())) {
                needToProtect = true;
                protectTarget = player;
            }

        if (needToAcuse && accuseTarget != null) {  // prioriza acusar werewolfs conhecidos
            acuse(accuseTarget);
            needToAcuse = false;
            accuseTarget = null;
        } else if (needToProtect && protectTarget != null) { // depois prioriza proteger doctors conhecidos
            trust(protectTarget);
            needToProtect = false;
            protectTarget = null;
        } else super.discuss();  // senao segue a discussao normal
    }


    @Override
    protected String decideRole() {
        Random random = new Random();
        double decision = random.nextDouble();
        String role = "SEER";

        if (decision < 0.1)
            role = "WEREWOLF"; // jogada arriscada onde os werewolfs não o focam, mas os aldeões podem expulsá-lo
        else if (decision < 0.5) role = "VILLAGER";
        else if (decision < 0.6) role = "DOCTOR";
        else if (decision < 0.9) role = "SEER";
        else role = "HUNTER";

        return role;
    }

    @Override
    protected void reactToAmbient() {
        super.reactToAmbient();
        for (String player : seerResults.keySet()) {
            String role = seerResults.get(player);
            super.updateBelief(player, Role.valueOf(role), 1.0);
        }
    }
}
