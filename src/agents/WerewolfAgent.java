package agents;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import model.MessageType;
import java.util.List;

public class WerewolfAgent extends VillagerAgent {
    private List<String> wolves;
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
        else if (messageType == MessageType.WEREWOLF_QUESTION) {
            
            handleWerewolfQuestion(sender);
        }
        else if (messageType == MessageType.WEREWOLF_ANSWER) {
            // Recebeu resposta de outro lobo sobre quem atacar
            handleWerewolfAnswer(content);
        }
    }

    //Pergunta aos outros lobos quem atacar
    private void werewolfQuestion() { 
        ACLMessage question = new ACLMessage(ACLMessage.INFORM);
        question.setConversationId(MessageType.WEREWOLF_QUESTION.name());
        question.setContent("Quem devemos atacar?");
        for (String wolf : wolves) {
            if (!wolf.equals(getLocalName())) {
                question.addReceiver(new AID(wolf, AID.ISLOCALNAME));
            }
        }
        send(question);
    }

    //Recebeu a pergunta de outro lobo sobre quem atacar e decide quem sugerir
    private void handleWerewolfQuestion(String sender) {
        String target = null;
        double minTrust = Double.POSITIVE_INFINITY;
        for (java.util.Map.Entry<String, Double> e : super.trust.entrySet()) {
            String name = e.getKey();
            double t = e.getValue();
            if (name.equals(getLocalName())) continue;
            if (t < minTrust && !this.wolves.contains(name)) {
                minTrust = t;
                target = name;
            }
        }
        if (target != null) {
            ACLMessage answer = new ACLMessage(ACLMessage.INFORM);
            answer.setConversationId(MessageType.WEREWOLF_ANSWER.name());
            answer.setContent(target);
            answer.addReceiver(new AID(sender, AID.ISLOCALNAME));
            send(answer);
        }
    }

    //Recebeu a resposta de outro lobo sobre quem atacar
    private void handleWerewolfAnswer(String content) {
        String target = content.trim();
        super.updateTrust(target, 1.0); 
        System.out.println(getLocalName() + " recebeu sugestão de ataque para: " + target);
        handleWerewolfAttack("GameMaster");
    }

    //Decide quem atacar com 50/50 de perguntar aos outros lobos ou escolher o mais confiável, se o target é sugestão ele envia o ataque ao GameMaster
    private void handleWerewolfAttack(String sender) { 
        String target = null;
        double maxTrust = Double.NEGATIVE_INFINITY;
        for (java.util.Map.Entry<String, Double> e : super.trust.entrySet()) {
            String name = e.getKey();
            double t = e.getValue();
            if (name.equals(getLocalName())) continue;
            if (t > maxTrust && !this.wolves.contains(name)) {
                maxTrust = t;
                target = name;
            }
        }
        double doIAskOthers = Math.random();
        double targetTrust = super.trust.get(target);
        if (target != null && (doIAskOthers < 0.5|| targetTrust == 1.0)) {
            ACLMessage attack = new ACLMessage(ACLMessage.INFORM);
            attack.setConversationId(MessageType.WEREWOLF_ATTACK.name());
            attack.setContent(target);
            attack.addReceiver(new AID(sender, AID.ISLOCALNAME));
            send(attack);
        } else {
            werewolfQuestion();
        }
    }

    @Override
    protected void decideAction() {
        String target = null;
        double minTrust = Double.POSITIVE_INFINITY;
        for (java.util.Map.Entry<String, Double> e : super.trust.entrySet()) {
            String name = e.getKey();
            Double t = e.getValue();
            if (name.equals(getLocalName()) || t == null || this.wolves.contains(name)) continue;
            if (t < minTrust && !this.wolves.contains(name)) {
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
