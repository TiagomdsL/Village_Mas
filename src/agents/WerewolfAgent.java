package agents;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import model.MessageType;

public class WerewolfAgent extends VillagerAgent {
    @Override
    protected void setup() {
        super.setup();
        this.myRole = model.Role.WEREWOLF;
    }

    @Override
    protected void processMessage(MessageType messageType, String content, String sender) {
        super.processMessage(messageType, content, sender);
        if (messageType == MessageType.WEREWOLF_ATTACK) {
            handleWerewolfAttack(sender);
        }
    }

    // TODO foi gerado pelo copilot
    private void handleWerewolfAttack(String sender) {
        // Lógica para decidir o alvo do ataque dos lobisomens
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
            ACLMessage attack = new ACLMessage(ACLMessage.INFORM);
            attack.setConversationId(MessageType.WEREWOLF_ATTACK.name());
            attack.setContent(target);
            attack.addReceiver(new AID(sender, AID.ISLOCALNAME));
            send(attack);
        }
    }

    @Override
    protected void decideAction() {
        // Estratégia de engano:
        // - acusar aldeões com baixa confiança
        // - evitar acusar outros lobos
    }
}
