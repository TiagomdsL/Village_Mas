package agents;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import model.Role;

public class VillagerAgent extends AbstractPlayerAgent {

    @Override
    protected void setup() {
        super.setup();
        this.myRole = model.Role.VILLAGER;

        addBehaviour(new jade.core.behaviours.CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                if (msg != null) {
                    handleMessage(msg.getContent());
                } else {
                    block();
                }
            }
        });
    }

    private void handleMessage(String content) {
        if (content.startsWith("ROLE:")) {
            myRole = Role.valueOf(content.split(":")[1]);
        }
    }

    @Override
    protected void decideAction() {
        // Aldeão simples: acusa quem tem menos confiança
    }
}
