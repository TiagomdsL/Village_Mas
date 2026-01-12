package agents;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import model.MessageType;

import java.util.*;

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
        // Recebeu a proposta para atacar alguém, decide atacar diretamente o seer ou doutor se o souber, senão, faz a votação entre werewolfs
        if (messageType == MessageType.WEREWOLF_ATTACK) {
            if (super.currSeer != null && super.trust.containsKey(super.currSeer))
                kill(super.currSeer);
            else if (super.currDoctor != null && super.trust.containsKey(super.currDoctor))
                kill(super.currDoctor);
            else handleWerewolfAttack(sender);

        } else if (messageType == MessageType.WEREWOLF_QUESTION) // Os werewolfs receberam a pergunta de outro werewolf sobre quem atacar
            handleWerewolfQuestion(sender);
        else if (messageType == MessageType.WEREWOLF_ANSWER)  // O werewolf que perguntou recebe a resposta de outro lobo sobre quem atacar
            handleWerewolfAnswer(content);
        else if (messageType == MessageType.WEREWOLF_PLAYERS) // Recebe a lista de werewolfs
            wereWolfList(content);
    }

    // Pergunta aos outros werewolfs quem atacar
    private void werewolfQuestion() {
        ACLMessage question = new ACLMessage(ACLMessage.REQUEST);
        question.setConversationId(MessageType.WEREWOLF_QUESTION.name());
        question.setContent("Quem devemos atacar?");
        for (String wolf : wolves)
            if (!wolf.equals(getLocalName()))
                question.addReceiver(new AID(wolf, AID.ISLOCALNAME));
        send(question);
    }

    //Recebeu a pergunta de outro werewolf sobre quem atacar e decide quem sugerir
    private void handleWerewolfQuestion(String sender) {
        String target = null;
        double minTrust = Double.POSITIVE_INFINITY;
        for (Map.Entry<String, Double> e : super.trust.entrySet()) {
            String name = e.getKey();
            double t = e.getValue();
            if (name.equals(getLocalName())) continue;
            if (t < minTrust && !this.wolves.contains(name)) {
                minTrust = t;
                target = name;
            }
        }
        if (target != null) {
            ACLMessage answer = new ACLMessage(ACLMessage.PROPAGATE); // propaga ao outro werewolf, o player que o werewolf tem menos confiança
            answer.setConversationId(MessageType.WEREWOLF_ANSWER.name());
            answer.setContent(target);
            answer.addReceiver(new AID(sender, AID.ISLOCALNAME));
            send(answer);
        }
    }

    //Recebeu a resposta de outro werewolf sobre quem atacar
    private void handleWerewolfAnswer(String content) {
        String target = content.trim();
        super.updateTrust(target, 1.0);
        System.out.println(getLocalName() + " recebeu sugestão de ataque para: " + target);
        handleWerewolfAttack(gameMasterAddr);
    }

    //Decide quem atacar com 50/50 de perguntar aos outros werewolfs ou escolher o mais confiável, se o target é sugestão ele envia o ataque ao GameMaster
    private void handleWerewolfAttack(String sender) {
        String target = null;
        double maxTrust = Double.NEGATIVE_INFINITY;
        for (Map.Entry<String, Double> e : super.trust.entrySet()) {
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
        if (target != null && (doIAskOthers < 0.5 || targetTrust == 1.0) && !wolves.contains(target)) {
            ACLMessage attack = new ACLMessage(ACLMessage.INFORM);
            attack.setConversationId(MessageType.WEREWOLF_ATTACK.name());
            attack.setContent(target);
            attack.addReceiver(new AID(sender, AID.ISLOCALNAME));
            send(attack);
        } else werewolfQuestion();
    }

    protected void kill(String target) {
        ACLMessage attack = new ACLMessage(ACLMessage.INFORM);
        attack.setConversationId(MessageType.WEREWOLF_ATTACK.name());
        attack.setContent(target);
        attack.addReceiver(new AID(gameMasterAddr, AID.ISLOCALNAME));
        send(attack);
    }

    @Override
    protected void handleVote(String content, String sender) {
        String target = null;
        double minTrust = Double.POSITIVE_INFINITY;
        for (Map.Entry<String, Double> e : super.trust.entrySet()) {
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
            vote.addReceiver(new AID(gameMasterAddr, AID.ISLOCALNAME));
            send(vote);
        }
    }

    protected void wereWolfList(String content) { // para saber os seus colegas
        if (content.contains("Werewolves players:")) {
            String[] parts = content.split(":");
            String wolvesList = parts[1].trim();
            this.wolves = new ArrayList<>(Arrays.asList(wolvesList.split(",")));
        }
    }

    @Override
    protected void handleRoleClaim(String content, String sender) {
        super.handleRoleClaim(content, sender);
        String[] parts = content.split(" ");
        if (parts.length >= 2) {
            String Role = parts[0];
            double senderTrust = super.trust.get(sender);

            if (Role.equals("VILLAGER") && !this.wolves.contains(sender))
                super.updateTrust(sender, -0.1); // não precisa matar um villager rápido
            else if (Role.equals("SEER") && !this.wolves.contains(sender))
                super.updateTrust(sender, -1); // prioridade maxima de matar
            else if (Role.equals("WEREWOLF") && !this.wolves.contains(sender))
                super.updateTrust(sender, 1); // qualquer um que diga que é lobisomem e n é, é conveniente
            else if (Role.equals("HUNTER"))
                super.updateTrust(sender, 0); // hunter é o menos prioritário para matar pois pode significar uma morte de werewolf
        }
    }

    // 50% de chance de acusar alguém, 20% de chance de revelar o seu papel, 20% de chance de dizer em quem confia mais, 10% de ficar calado
    @Override
    protected void discuss() {
        Random random = new Random();
        double decision = random.nextDouble();
        if (decision < 0.5) acuse();
        else if (decision < 0.7) revealRole();
        else if (decision < 0.9) trustSomeone();
    }

    //acusa o agente em quem menos confia, ignorando outros werewolfs
    @Override
    protected void acuse() {
        String target = null;
        double minTrust = Double.POSITIVE_INFINITY;
        for (Map.Entry<String, Double> e : super.trust.entrySet()) {
            String name = e.getKey();
            Double t = e.getValue();
            if (name.equals(getLocalName()) || t == null || this.wolves.contains(name)) continue;
            if (t < minTrust && !this.wolves.contains(name)) {
                minTrust = t;
                target = name;
            }
        }
        if (target != null) {
            ACLMessage accusation = new ACLMessage(ACLMessage.INFORM);
            accusation.setConversationId(MessageType.ACCUSATION.name());
            accusation.setContent(target + " is suspicious");
            for (String p : super.trust.keySet())
                if (!p.equals(getLocalName()))
                    accusation.addReceiver(new AID(p, AID.ISLOCALNAME));
            send(accusation);
        }
    }

    // diz em quem confia mais, apenas nos werewolfs outros werewolfs
    @Override
    protected void trustSomeone() {
        String target = null;
        double maxTrust = Double.NEGATIVE_INFINITY;
        for (Map.Entry<String, Double> e : super.trust.entrySet()) {
            String name = e.getKey();
            Double t = e.getValue();
            if (name.equals(getLocalName()) || t == null || this.wolves.contains(name)) continue;
            if (t > maxTrust && this.wolves.contains(name)) {
                maxTrust = t;
                target = name;
            }
        }
        if (target != null) {
            ACLMessage trustMsg = new ACLMessage(ACLMessage.INFORM);
            trustMsg.setConversationId(MessageType.TRUST.name());
            trustMsg.setContent(target + " is trustworthy");
            for (String p : super.trust.keySet())
                if (!p.equals(getLocalName()))
                    trustMsg.addReceiver(new AID(p, AID.ISLOCALNAME));
            send(trustMsg);
        }
    }

    @Override
    protected String decideRole() {
        Random random = new Random();
        double decision = random.nextDouble();
        String role = "WEREWOLF";

        if (decision < 0.02) role = "WEREWOLF"; // não tem motivo de se revelar
        else if (decision < 0.4) role = "VILLAGER";
        else if (decision < 0.5) role = "DOCTOR";
        else if (decision < 0.8) role = "SEER"; //seer é favorável para manipular os aldeões
        else role = "HUNTER";    // hunter é seguro, pois ninguém o quer atacar

        return role;
    }

    @Override
    protected void reactToAmbient() {
        super.reactToAmbient();

        // Coloca os outros werewolfs com 100% de certeza que são werewolfs
        for (String wolf : wolves)
            if (!wolf.equals(getLocalName()))
                super.updateBelief(wolf, model.Role.WEREWOLF, 1.0);

        if (super.trust.containsKey(super.currSeer))
            super.updateTrust(super.currSeer, -1.0); // se o seer está vivo, é prioridade máxima matar

        if (super.trust.containsKey(super.currDoctor))
            super.updateTrust(super.currDoctor, -0.5); // doctor é prioridade alta de matar
    }
}
