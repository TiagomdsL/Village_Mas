package agents;

public class HunterAgent extends VillagerAgent {

    @Override
    protected void setup() {
        super.setup();
        this.myRole = model.Role.HUNTER;
    }

    @Override
    protected void decideAction() {
        // Se morrer, elimina jogador suspeito
    }
}
