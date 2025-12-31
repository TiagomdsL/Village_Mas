package agents;

public class WerewolfAgent extends VillagerAgent {
    @Override
    protected void setup() {
        super.setup();
        this.myRole = model.Role.WEREWOLF;
    }

    @Override
    protected void decideAction() {
        // Estratégia de engano:
        // - acusar aldeões com baixa confiança
        // - evitar acusar outros lobos
    }
}
