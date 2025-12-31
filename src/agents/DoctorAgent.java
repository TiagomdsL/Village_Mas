package agents;

public class DoctorAgent extends VillagerAgent {

    @Override
    protected void setup() {
        super.setup();
        this.myRole = model.Role.DOCTOR;
    }

    @Override
    protected void decideAction() {
        // Protege jogador com maior confiança
    }
}
