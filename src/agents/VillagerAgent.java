package agents;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import model.MessageType;

import java.util.*;

public class VillagerAgent extends AbstractPlayerAgent {
    protected Map<String, List<String>> acusations = new HashMap<>();
    protected Map<String, List<String>> trustsMessages = new HashMap<>();
    protected String currSeer = "";
    protected String currDoctor = "";
    protected String currWerewolf = "";
    protected List<String> wasProtected = new ArrayList<>();

    @Override
    protected void setup() {
        super.setup();
        this.myRole = model.Role.VILLAGER;
    }

    protected void handleAccusation(String content, String sender) {
        Random random = new Random();
        content = content.trim();
        String[] parts = content.split(" ", 2);
        if (parts.length < 2) return; // Formato inválido

        String accusedAgent = parts[0];
        if (!acusations.containsKey(sender)) acusations.put(sender, new ArrayList<>());

        acusations.get(sender).add(accusedAgent);
        String reason = parts[1]; // por enquanto n tem uso
        double senderTrust = super.trust.get(sender);
        double accusedTrust = super.trust.get(accusedAgent);

        if (senderTrust > 0.5 && accusedTrust >= 0.5)
            super.updateTrust(accusedAgent, -0.1);  //se o acusador é confiável e o acusado também
        else if (senderTrust > 0.5)
            super.updateTrust(accusedAgent, -0.2);  //se o acusador é confiável e o acusado não é
        else if (accusedTrust >= 0.5)
            super.updateTrust(sender, -0.2);    //se o acusador não é confiável e o acusado é
        else {
            double deceptionChance = random.nextDouble(-0.2, 0.1);
            super.updateTrust(accusedAgent, deceptionChance); // se o acusador e o acusado não são confiáveis
            deceptionChance = random.nextDouble(-0.2, 0.1);
            super.updateTrust(sender, deceptionChance);
        }
    }

    protected void handleTrust(String content, String sender) {
        Random random = new Random();
        content = content.trim();
        String[] parts = content.split(" ", 2);
        if (parts.length < 2) return; // Formato inválido

        String trustedAgent = parts[0];
        if (!trustsMessages.containsKey(sender)) trustsMessages.put(sender, new ArrayList<>());

        trustsMessages.get(sender).add(trustedAgent);
        String reason = parts[1]; // por enquanto n tem uso
        double senderTrust = super.trust.get(sender);
        double trustedTrust = super.trust.get(trustedAgent);

        if (senderTrust > 0.5 && trustedTrust < 0.5)
            super.updateTrust(trustedAgent, 0.15);
        else if (senderTrust > 0.5)
            super.updateTrust(trustedAgent, 0.3);
        else {
            double deceptionChance = random.nextDouble(-0.2, 0.1);
            super.updateTrust(trustedAgent, deceptionChance);
        }
    }

    protected void handleRoleClaim(String content, String sender) {
        Random random = new Random();
        content = content.trim();
        String[] parts = content.split(" ", 2);
        if (parts.length < 2) return; // Formato inválido

        String Role = parts[0];
        String reason = parts[1]; // por enquanto n tem uso
        double senderTrust = super.trust.get(sender);
        switch (Role) {
            case "DOCTOR" -> {
                if (senderTrust > 0.5)
                    super.updateTrust(sender, 0.4); // se o confiador é confiável
                else if (senderTrust == 0.5) {
                    double deceptionChance = random.nextDouble(-0.3, 0.3);
                    super.updateTrust(sender, deceptionChance); // se o confiador não é confiável
                } else
                    super.updateTrust(sender, -0.3); // se o confiador é suspeito
            }
            case "VILLAGER" -> {
                if (senderTrust > 0.5)
                    super.updateTrust(sender, 0.2); // se o confiador é confiável
                else if (senderTrust == 0.5) {
                    double deceptionChance = random.nextDouble(-0.1, 0.1);
                    super.updateTrust(sender, deceptionChance); // se o confiador não é confiável
                } else
                    super.updateTrust(sender, -0.1); // se o confiador é suspeito
            }
            case "SEER" -> {
                if (senderTrust > 0.5)
                    super.updateTrust(sender, 0.5); // se o confiador é confiável
                else if (senderTrust == 0.5) {
                    double deceptionChance = random.nextDouble(-0.2, 0.4);
                    super.updateTrust(sender, deceptionChance); // se o confiador não é confiável // se o confiador não é confiável
                } else
                    super.updateTrust(sender, -0.3); // se o confiador é suspeito
            }
            case "WEREWOLF" -> {
                double deceptionChance = random.nextDouble(-1.0, 0.4);
                super.updateTrust(sender, deceptionChance); // qualquer um que diga que é lobisomem é suspeito, mas pode ser uma mentira para despistar
            }
            case "HUNTER" -> {
                if (senderTrust > 0.5)
                    super.updateTrust(sender, 0.5); // se o confiador é confiável e tem medo do hunter lhe focar
                else if (senderTrust == 0.5) {
                    double deceptionChance = random.nextDouble(-0.2, 0.3);
                    super.updateTrust(sender, deceptionChance); // se o confiador não é confiável
                } else
                    super.updateTrust(sender, -0.3); // se o confiador é suspeito
            }
        }
    }

    // escolhe o agente com menor trust (exclui a si próprio) e envia o voto ao GameMaster
    protected void handleVote(String content, String sender) {
        String target = null;
        double minTrust = Double.POSITIVE_INFINITY;
        for (Map.Entry<String, Double> e : super.trust.entrySet()) {
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
            vote.addReceiver(new AID(gameMasterAddr, AID.ISLOCALNAME));
            send(vote);
        }
    }

    @Override
    protected void handleSystem(String content) {
        super.handleSystem(content);
        if (content.contains("protected by doctor")) {
            String protectedAgent = content
                    .replace("The Player:", "")
                    .replace("is Dead but was protected by doctor", "")
                    .trim();
            wasProtected.add(protectedAgent);
        }
        if (content.contains("Day phase started.")) {
            reactToAmbient();
            discuss();
        }
    }


    // 30% de chance de acusar alguém, 20% de chance de revelar o seu papel, 20% de chance de dizer em quem confia mais, 30% de ficar calado
    protected void discuss() {
        if (currWerewolf != null && !currWerewolf.isEmpty())
            acuse(currWerewolf);
        else if (currSeer != null && !currSeer.isEmpty())
            trust(currSeer);
        else if (currDoctor != null && !currDoctor.isEmpty())
            trust(currDoctor);
        else {
            Random random = new Random();
            double decision = random.nextDouble();

            if (decision < 0.3) acuse();
            else if (decision < 0.5) revealRole();
            else if (decision < 0.7) trustSomeone();
        }
    }

    //diz em quem confia mais
    protected void trustSomeone() {
        String target = null;
        double maxTrust = Double.NEGATIVE_INFINITY;
        for (Map.Entry<String, Double> e : super.trust.entrySet()) {
            String name = e.getKey();
            Double t = e.getValue();
            if (name.equals(getLocalName()) || t == null) continue;
            if (t > maxTrust) {
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

    // acusa o agente em quem menos confia
    protected void acuse() {
        String target = null;
        double minTrust = Double.POSITIVE_INFINITY;
        for (Map.Entry<String, Double> e : super.trust.entrySet()) {
            String name = e.getKey();
            Double t = e.getValue();
            if (name.equals(getLocalName()) || t == null) continue;
            if (t < minTrust) {
                minTrust = t;
                target = name;
            }
        }
        if (target != null) acuse(target); // modularização da função
    }

    protected void acuse(String target) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setConversationId(MessageType.ACCUSATION.name());
        msg.setContent(target);
        for (String player : super.trust.keySet())
            if (!player.equals(getLocalName()))
                msg.addReceiver(new AID(player, AID.ISLOCALNAME));
        send(msg);
    }

    protected void trust(String target) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setConversationId(MessageType.TRUST.name());
        msg.setContent(target);
        for (String player : super.trust.keySet())
            if (!player.equals(getLocalName()))
                msg.addReceiver(new AID(player, AID.ISLOCALNAME));
        send(msg);
    }

    // revela o seu papel
    protected void revealRole() {
        String role = decideRole();
        ACLMessage roleClaim = new ACLMessage(ACLMessage.INFORM);
        roleClaim.setConversationId(MessageType.ROLE_CLAIM.name());
        roleClaim.setContent(role + " Revealing role");
        for (String p : super.trust.keySet())
            if (!p.equals(getLocalName()))
                roleClaim.addReceiver(new AID(p, AID.ISLOCALNAME));
        send(roleClaim);
    }

    protected String decideRole() {
        Random random = new Random();
        double decision = random.nextDouble();
        String role = "VILLAGER";

        if (decision < 0.05) role = "WEREWOLF"; // mentira
        else if (decision < 0.6) role = "VILLAGER";
        else if (decision < 0.7) role = "DOCTOR";
        else if (decision < 0.8) role = "SEER";
        else if (decision < 0.9) role = "HUNTER";

        return role;
    }

    protected void reactToAmbient() {
        for (Map.Entry<String, Map<model.Role, Double>> entry : super.beliefs.entrySet()) {
            String playerName = entry.getKey();
            Map<model.Role, Double> roleProbs = entry.getValue();

            // Pular a si próprio
            if (playerName.equals(getLocalName())) continue;

            double trustLevel = super.trust.getOrDefault(playerName, 0.5);

            // Se foi identificado como werewolf antes
            if (acusations.containsKey(playerName))
                for (String acusees : acusations.get(playerName))
                    if (super.wasWerewolf.contains(acusees)) {
                        super.updateBelief(playerName, model.Role.SEER, 1); // quase certeza
                        if (this.myRole != model.Role.WEREWOLF) {
                            super.updateTrust(playerName, 0.5);
                        }
                    }

            if (trustsMessages.containsKey(playerName))
                for (String trusteds : trustsMessages.get(playerName))
                    if (this.wasProtected.contains(trusteds)) {
                        super.updateBelief(playerName, model.Role.DOCTOR, 1); // quase certeza
                        if (this.myRole != model.Role.WEREWOLF) {
                            super.updateTrust(playerName, 0.5);
                        }
                    }

            // Baseado em trust
            if (trustLevel > 0.7 && this.myRole != model.Role.WEREWOLF) {
                // Alta confiança: mais provável ser DOCTOR, SEER ou VILLAGER
                super.updateBelief(playerName, model.Role.DOCTOR, roleProbs.get(model.Role.DOCTOR) + 0.10);
                super.updateBelief(playerName, model.Role.SEER, roleProbs.get(model.Role.SEER) + 0.10);
                super.updateBelief(playerName, model.Role.WEREWOLF, roleProbs.get(model.Role.WEREWOLF) - 0.15);
                super.updateBelief(playerName, model.Role.WEREWOLF, roleProbs.get(model.Role.HUNTER) - 0.10);
            } else if (trustLevel < 0.3 && this.myRole != model.Role.WEREWOLF) {
                // Baixa confiança: mais provável ser WEREWOLF
                super.updateBelief(playerName, model.Role.WEREWOLF, roleProbs.get(model.Role.WEREWOLF) + 0.30);
                super.updateBelief(playerName, model.Role.VILLAGER, roleProbs.get(model.Role.HUNTER) + 0.10);
                super.updateBelief(playerName, model.Role.DOCTOR, roleProbs.get(model.Role.DOCTOR) - 0.10);
                super.updateBelief(playerName, model.Role.SEER, roleProbs.get(model.Role.SEER) - 0.10);
            } else if (this.myRole != model.Role.WEREWOLF) {
                // Confiança média: ligeira inclinação para VILLAGER
                super.updateBelief(playerName, model.Role.VILLAGER, roleProbs.get(model.Role.VILLAGER) + 0.05);
                super.updateBelief(playerName, model.Role.WEREWOLF, roleProbs.get(model.Role.WEREWOLF) - 0.05);
            }

            // Se confia e foi mencionado como confiável em trusts
            if (trustsMessages.containsValue(playerName) && trustLevel > 0.5 && this.myRole != model.Role.WEREWOLF) {
                super.updateBelief(playerName, model.Role.VILLAGER, roleProbs.get(model.Role.VILLAGER) + 0.05);
            }
            if (this.myRole == model.Role.WEREWOLF) {
                super.updateBelief(playerName, model.Role.VILLAGER, roleProbs.get(model.Role.VILLAGER) + 0.10);
            }
        }

        // Identifica o jogador com maior probabilidade de ser DOCTOR e SEER
        String maxDoctor = null;
        double maxDoctorProb = 0.0;
        String maxSeer = null;
        double maxSeerProb = 0.0;
        String maxWerewolf = null;
        double maxWerewolfProb = 0.0;

        for (Map.Entry<String, Map<model.Role, Double>> entry : super.beliefs.entrySet()) {
            String playerName = entry.getKey();
            Map<model.Role, Double> roleProbs = entry.getValue();

            if (playerName.equals(getLocalName())) continue;

            double doctorProb = roleProbs.get(model.Role.DOCTOR);
            double seerProb = roleProbs.get(model.Role.SEER);
            double werewolfProb = roleProbs.get(model.Role.WEREWOLF);

            if (doctorProb > maxDoctorProb) {
                maxDoctorProb = doctorProb;
                maxDoctor = playerName;
            }

            if (seerProb > maxSeerProb) {
                maxSeerProb = seerProb;
                maxSeer = playerName;
            }

            if (werewolfProb > maxWerewolfProb) {
                maxWerewolfProb = werewolfProb;
                maxWerewolf = playerName;
            }
        }

        this.currDoctor = (maxDoctor != null && maxDoctorProb > 0.7) ? maxDoctor : "";
        this.currSeer = (maxSeer != null && maxSeerProb > 0.7) ? maxSeer : "";
        this.currWerewolf = (maxWerewolf != null && maxWerewolfProb > 0.7) ? maxWerewolf : "";
    }
}