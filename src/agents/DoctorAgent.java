package agents;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import model.MessageType;

import java.util.Map;

public class DoctorAgent extends VillagerAgent {

    @Override
    protected void setup() {
        super.setup();
        this.myRole = model.Role.DOCTOR;
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = myAgent.receive();
                if (msg != null) {
                    try {
                        MessageType messageType = MessageType.valueOf(msg.getConversationId());
                        if (messageType == MessageType.DOCTOR_PROTECT) {
                            handleDoctor();
                        } else {}
                    } catch (IllegalArgumentException e) {
                        // ConversationId não corresponde a MessageType -> ignorar
                    }
                } else {
                    block();
                }
            }
        });
    }

    
    private void handleDoctor() {
        String protectedAgent;
        java.util.List<String> candidates = new java.util.ArrayList<>();
        for (Map.Entry<String, Double> entry : super.trust.entrySet()) {
            String agent = entry.getKey();
            double trustValue = entry.getValue();
            if (!agent.equals(getLocalName()) && trustValue > 0.5) {
                candidates.add(agent);
            }
        }

        if (candidates.isEmpty()) {
            // fallback: escolhe qualquer outro agente ao acaso (mesmo que trust <= 0.5)
            java.util.List<String> allOthers = new java.util.ArrayList<>();
            for (String a : super.trust.keySet()) {
                if (!a.equals(getLocalName())) {
                    allOthers.add(a);
                }
            }
            if (allOthers.isEmpty()) {
                protectedAgent = null;
            } else {
                int idx = java.util.concurrent.ThreadLocalRandom.current().nextInt(allOthers.size());
                protectedAgent = allOthers.get(idx);
            }
        } else {
            int idx = java.util.concurrent.ThreadLocalRandom.current().nextInt(candidates.size());
            protectedAgent = candidates.get(idx);
        }

        if (protectedAgent != null) {
            ACLMessage protectMsg = new ACLMessage(ACLMessage.INFORM);
            protectMsg.setConversationId(MessageType.DOCTOR_PROTECT.name());
            protectMsg.setContent(protectedAgent);
            protectMsg.addReceiver(new jade.core.AID("GameMaster", jade.core.AID.ISLOCALNAME));
            send(protectMsg);
        }
    }
}
