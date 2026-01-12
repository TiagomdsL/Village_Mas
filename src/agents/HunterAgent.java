package agents;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import model.MessageType;

import java.util.Map;
import java.util.Random;

public class HunterAgent extends VillagerAgent {

    @Override
    protected void setup() {
        super.setup();
        this.myRole = model.Role.HUNTER;
    }

    protected void processMessage(MessageType messageType, String content, String sender) { //temporario para n dar erro
        super.processMessage(messageType, content, sender);
        if (messageType == MessageType.HUNTER_KILL) { // antes de morrer, ele mata alguém
            handleHunter(sender);
            doDelete();
        }
    }

    // mata o agente que menos confia
    private void handleHunter(String sender) {
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
            ACLMessage kill = new ACLMessage(ACLMessage.INFORM);
            kill.setConversationId(MessageType.HUNTER_KILL.name());
            kill.setContent(target);
            kill.addReceiver(new AID(sender, AID.ISLOCALNAME));
            send(kill);
        }
    }

    @Override
    protected String decideRole() {
        Random random = new Random();
        double decision = random.nextDouble();
        String role = "Hunter";
        if (decision < 0.4)
            role = "WEREWOLF"; // jogada arriscada os werewolfs não o focam mas os aldeões podem expulsá-lo
        else if (decision < 0.5) role = "VILLAGER";
        else if (decision < 0.6) role = "DOCTOR";
        else if (decision < 0.7) role = "SEER";
        else role = "HUNTER";  // hunter é seguro pois ninguem o quer atacar

        return role;
    }
}
