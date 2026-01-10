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
                .setSeerRatio(0.1)
                .setDoctorRatio(0.1)
                .setHunterRatio(0.1)
                .setMessageLimit(2)
                .setMaxRounds(20)
                .setSeed(42);

        launcher.startGame();
    }
}
