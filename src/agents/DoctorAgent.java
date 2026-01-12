package agents;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import model.MessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class DoctorAgent extends VillagerAgent {

    @Override
    protected void setup() {
        super.setup();
        this.myRole = model.Role.DOCTOR;
    }

    protected void processMessage(MessageType messageType, String content, String sender) { //temporario para n dar erro
        super.processMessage(messageType, content, sender);
        if (messageType == MessageType.DOCTOR_PROTECT)
            handleDoctor(sender);
    }


    private void handleDoctor(String sender) {
        String protectedAgent;
        List<String> candidates = new ArrayList<>();
        for (Map.Entry<String, Double> entry : super.trust.entrySet()) {
            String agent = entry.getKey();
            double trustValue = entry.getValue();
            if (!agent.equals(getLocalName()) && trustValue > 0.5)
                candidates.add(agent);

        }

        if (candidates.isEmpty()) {
            // fallback: escolhe qualquer outro agente ao acaso (mesmo que trust <= 0.5)
            List<String> allOthers = new ArrayList<>();
            for (String a : super.trust.keySet())
                if (!a.equals(getLocalName()))
                    allOthers.add(a);

            if (allOthers.isEmpty())
                protectedAgent = null;
            else {
                int idx = ThreadLocalRandom.current().nextInt(allOthers.size());
                protectedAgent = allOthers.get(idx);
            }
        } else {
            int idx = ThreadLocalRandom.current().nextInt(candidates.size());
            protectedAgent = candidates.get(idx);
        }

        if (protectedAgent != null) {
            ACLMessage protectMsg = new ACLMessage(ACLMessage.INFORM);
            protectMsg.setConversationId(MessageType.DOCTOR_PROTECT.name());
            protectMsg.setContent(protectedAgent);
            protectMsg.addReceiver(new AID(sender, AID.ISLOCALNAME)); // para o GameMaster e ser escalavel
            send(protectMsg);
            System.out.println("Doctor " + getLocalName() + " is protecting " + protectedAgent);
        }
    }

    @Override
    protected String decideRole() {
        Random random = new Random();
        double decision = random.nextDouble();
        String role = "Doctor";
        if (decision < 0.1)
            role = "WEREWOLF";  // jogada arriscada os werewolfs não o focam mas os aldeões podem expulsá-lo
        else if (decision < 0.7) role = "VILLAGER";
        else if (decision < 0.8) role = "DOCTOR";   // doctor n se quer revelar pois n é seguro
        else if (decision < 0.9) role = "SEER";
        else role = "HUNTER";

        return role;
    }
}
