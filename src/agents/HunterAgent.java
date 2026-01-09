package agents;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import model.MessageType;

public class HunterAgent extends VillagerAgent {

    @Override
    protected void setup() {
        super.setup();
        this.myRole = model.Role.HUNTER;
    }

    protected void processMessage(MessageType messageType, String content, String sender) { //temporario para n dar erro
        super.processMessage(messageType, content, sender);
        if (messageType == MessageType.HUNTER_KILL) {
            System.out.println("9999999999");
            handleHunter(sender);
        }
    }

    private void handleHunter(String sender) {
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
            kill.addReceiver(new AID(sender, AID.ISLOCALNAME));
            send(kill);
        }
    }
}
