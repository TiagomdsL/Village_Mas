package agents;

import jade.core.Agent;
import util.Logger;

public class LoggerAgent extends Agent {
    @Override
    protected void setup() {
        Logger.log(getLocalName() + " started.");
    }
}