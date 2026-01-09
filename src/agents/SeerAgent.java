package agents;

import jade.lang.acl.ACLMessage;
import model.MessageType;

public class SeerAgent extends VillagerAgent {

    @Override
    protected void setup() {
        super.setup();
        this.myRole = model.Role.SEER;
        addBehaviour(new jade.core.behaviours.CyclicBehaviour(this) {
            @Override
            public void action() {
                jade.lang.acl.ACLMessage msg = myAgent.receive();
                if (msg != null) {
                    try {
                        model.MessageType messageType = model.MessageType.valueOf(msg.getConversationId());
                        if (messageType == model.MessageType.SEER_REVEAL) {
                            handleSeer();
                        } else if (messageType == model.MessageType.SEER_RECEIVE) {
                            handleSeerReceive(msg.getContent());
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

    private void handleSeer(){
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
            ACLMessage reveal = new ACLMessage(ACLMessage.INFORM);
            reveal.setConversationId(MessageType.SEER_REVEAL.name());
            reveal.setContent(target);
            reveal.addReceiver(new jade.core.AID("GameMaster", jade.core.AID.ISLOCALNAME));
            send(reveal);
        }
    }

    // altera o trust com base no content recebido ("AgentX ROLE")
    private void handleSeerReceive(String content) {
        if (content == null) return;
        content = content.trim();
        if (content.isEmpty()) return;
        String[] parts = content.split("\\s+", 2);
        if (parts.length < 2) return;
        String agent = parts[0];
        String role = parts[1].trim().toUpperCase();

        if (role.equals("WEREWOLF")) {
            super.updateTrust(agent, 0.0);
        } else {
            super.updateTrust(agent, 1.0);
        }
    }

}
