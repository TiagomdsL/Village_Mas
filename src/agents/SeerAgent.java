package agents;

public class SeerAgent extends VillagerAgent {

    @Override
    protected void setup() {
        super.setup();
        this.myRole = model.Role.SEER;
    }

    @Override
    protected void decideAction() {
        // Escolhe jogador com maior incerteza
        // Atualiza crença para role confirmada
    }
}
