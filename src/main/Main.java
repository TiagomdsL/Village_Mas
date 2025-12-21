package main;

import launcher.GameLauncher;

/**
 * Classe de entrada do sistema.
 * Responsável apenas por arrancar o jogo
 * com uma configuração específica.
 */
public class Main {

    public static void main(String[] args) {

        // Configuração padrão (demo)
        GameLauncher launcher = new GameLauncher()
                .setNumPlayers(10)
                .setNumWerewolves(2)
                .enableSeer(true)
                .enableDoctor(true)
                .enableHunter(true)
                .setMessageLimit(2) // comunicação normal
                .setMaxRounds(20)
                .setSeed(42);

        launcher.startGame();
    }
}
