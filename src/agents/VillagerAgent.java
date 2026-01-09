package agents;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import model.MessageType;
import model.Role;

public class VillagerAgent extends AbstractPlayerAgent {

    @Override
    protected void setup() {
        super.setup();
        this.myRole = model.Role.VILLAGER;
    }



    protected void processMessage(MessageType messageType, String content, String sender) { //temporario para n dar erro
        switch (messageType) {
            case ACCUSATION:
                handleAccusation(content, sender);
                break;
            case TRUST:
                handleTrust(content, sender);
                break;
            case ROLE_CLAIM:
                handleRoleClaim(content, sender);
                break;
            case VOTE:
                handleVote(content, sender);
                break;
            case SYSTEM:
                handleSystem(content, sender);
                break;
            default:
                break;
        }
    }

    protected void handleAccusation(String content, String sender) {
        content = content.trim();
        String[] parts = content.split(" ", 2);
        if (parts.length < 2) {
            return; // Formato inválido
        }
        String accusedAgent = parts[0];
        String reason = parts[1]; // por enquanto n tem uso
        double senderTrust = super.trust.get(sender);
        double accusedTrust = super.trust.get(accusedAgent);

        if (senderTrust > 0.5 && accusedTrust >= 0.5) { 
            super.updateTrust(accusedAgent, -0.1);//se o acusador é confiável e o acusado também
        } else if( senderTrust > 0.5 ) { 
            super.updateTrust(accusedAgent, -0.2);//se o acusador é confiável e o acusado não é
        } else if( accusedTrust >= 0.5 ) {                                                        
            super.updateTrust(accusedAgent, -0.2);//se o acusador não é confiável e o acusado é
        }
        else {
            super.updateTrust(accusedAgent, -0.1);//se nenhum dos dois é confiável
        }
    }

    protected void handleTrust(String content, String sender) {
        content = content.trim();
        String[] parts = content.split(" ", 2);
        if (parts.length < 2) {
            return; // Formato inválido
        }
        String trustedAgent = parts[0];
        String reason = parts[1]; // por enquanto n tem uso
        double senderTrust = super.trust.get(sender);
        double trustedTrust = super.trust.get(trustedAgent);
        if (senderTrust > 0.5 && trustedTrust <= 0.5) { 
            super.updateTrust(trustedAgent, 0.1);//se o confiador é confiável e o confiado também
        } else if( senderTrust > 0.5 ) { 
            super.updateTrust(trustedAgent, 0.2);//se o confiador é confiável e o confiado não é
        } else if( trustedTrust <= 0.5 ) {                                                        
            super.updateTrust(trustedAgent, 0.2);//se o confiador não é confiável e o confiado é
        }
        else {
            super.updateTrust(trustedAgent, 0.1);//se nenhum dos dois é confiável
        }
    }

    protected void handleRoleClaim(String content, String sender) {
        content = content.trim();
        String[] parts = content.split(" ", 2);
        if (parts.length < 2) {
            return; // Formato inválido
        }
        String Role = parts[0];
        String reason = parts[1]; // por enquanto n tem uso
        double senderTrust = super.trust.get(sender);
        if (Role.equals("DOCTOR")) {
            super.updateTrust(sender, -0.8); // obviamente mentira
        } else if (Role.equals("VILLAGER")) {
            if (senderTrust > 0.5) {
                super.updateTrust(sender, 0.2); // se o confiador é confiável 
            } else if (senderTrust == 0.5) {
                super.updateTrust(sender, 0.1); // se o confiador não é confiável 
            } else {
                super.updateTrust(sender, -0.1); // se o confiador é suspeito
            }
        }
    }

    protected void handleVote(String content, String sender) {
        decideAction();
    }

    protected void handleSystem(String content, String sender) {
        // TODO
    }

    @Override
    protected void decideAction() {
        // escolhe o agente com menor trust (exclui a si próprio) e envia VOTE ao GameMaster
        String target = null;
        double minTrust = Double.POSITIVE_INFINITY;
        for (java.util.Map.Entry<String, Double> e : super.trust.entrySet()) {
            String name = e.getKey();
            Double t = e.getValue();
            if (name.equals(getLocalName()) || t == null) continue;
            if (t < minTrust) {
                minTrust = t;
                target = name;
            }
        }
        if (target != null) {
            ACLMessage vote = new ACLMessage(ACLMessage.INFORM);
            vote.setConversationId(MessageType.VOTE.name());
            vote.setContent(target);
            vote.addReceiver(new jade.core.AID("GameMaster", jade.core.AID.ISLOCALNAME));
            send(vote);
        }
    }
}
