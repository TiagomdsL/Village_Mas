package agents;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import model.MessageType;

public class HunterAgent extends VillagerAgent {

    @Override
    protected void setup() {
        super.setup();
        this.myRole = model.Role.HUNTER;
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = myAgent.receive();
                if (msg != null) {
                    try {
                        MessageType messageType = MessageType.valueOf(msg.getConversationId());
                        if (messageType == MessageType.HUNTER_KILL) {

                            System.out.println("FFFFFFFFFFFF");
                            handleHunter();
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

    private void handleHunter(){
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
            ACLMessage kill = new ACLMessage(ACLMessage.INFORM);
            kill.setConversationId(MessageType.HUNTER_KILL.name());
            kill.setContent(target);
            kill.addReceiver(new jade.core.AID("GameMaster", jade.core.AID.ISLOCALNAME));
            send(kill);
        }
    }
}
