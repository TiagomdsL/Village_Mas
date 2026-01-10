package agents;

import java.util.Random;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import model.MessageType;
import model.Role;

public class SeerAgent extends VillagerAgent {

    @Override
    protected void setup() {
        super.setup();
        this.myRole = model.Role.SEER;

    }

    protected void processMessage(MessageType messageType, String content, String sender) { //temporario para n dar erro
        super.processMessage(messageType, content, sender);
        if (messageType == MessageType.SEER_REVEAL) {
            handleSeer(sender);
        } else if (messageType == MessageType.SEER_RECEIVE) {
            handleSeerReceive(content, sender);
        }
    }

    private void handleSeer(String sender){
        String target = null;
        double minTrust = Double.POSITIVE_INFINITY;
        for (java.util.Map.Entry<String, Double> e : super.trust.entrySet()) {
            String name = e.getKey();
            double t = e.getValue();
            if (name.equals(getLocalName())) continue;
            if (t < minTrust) {
                minTrust = t;
                target = name;
            }
        }
        if (target != null) {
            ACLMessage reveal = new ACLMessage(ACLMessage.REQUEST); // requesita a role ao agente
            reveal.setConversationId(MessageType.SEER_REVEAL.name());
            reveal.setContent("I see you, but u are distracted.");
            reveal.addReceiver(new AID(target, AID.ISLOCALNAME)); // ao inves de ver o GameMaster, ve o agente em questao diretamente
            send(reveal);
        }
    }

    // altera o trust com base no content recebido ("AgentX ROLE")
    private void handleSeerReceive(String content, String sender) {
        if (content == null) return;
        content = content.trim();
        if (content.isEmpty()) return;
//        System.out.println("Ent a role deste é " + content);

        if (content.equals(Role.WEREWOLF.name())) {
            super.updateTrust(sender, 0.0);
        } else {
            super.updateTrust(sender, 1.0);
        }
    }
    
    @Override
    protected String decideRole() {
        Random random = new Random();
        double decision = random.nextDouble();
        String role = "SEER";
        if (decision < 0.1) {
            role = "WEREWOLF"; // jogada arriscada os werewolfs não o focam mas os aldeões podem expulsá-lo 
        }else if (decision < 0.5) {
            role = "VILLAGER";
        } else  if (decision < 0.6) {
            role = "DOCTOR";
        } else  if (decision < 0.9) {
            role = "SEER";
        } else  if (decision < 1) {
            role = "HUNTER";
        }
        return role;
    }
}
