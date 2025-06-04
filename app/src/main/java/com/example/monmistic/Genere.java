package com.example.monmistic;

/**
 * Classe que emmagatzema els atributs específics de cada gènere de criatura.
 */
public class Genere {
    private float velocitat;
    private int colorDetector; // Color.BLACK, Color.GREEN, Color.RED, Color.BLUE
    private int puntsCompensacio;
    private float distanciaDeteccio; // Usada per calcular el factor de zoom

    /**
     * Constructor per crear un objecte d'atributs de criatura.
     *
     * @param velocitat La velocitat de desplaçament.
     * @param colorDetector El color amb què el detector identifica la criatura.
     * @param puntsCompensacio Els punts de compensació per pacte.
     * @param distanciaDeteccio La distància a la qual el sensor pot detectar la criatura.
     */
    public Genere(float velocitat, int colorDetector, int puntsCompensacio, float distanciaDeteccio) {
        this.velocitat = velocitat;
        this.colorDetector = colorDetector;
        this.puntsCompensacio = puntsCompensacio;
        this.distanciaDeteccio = distanciaDeteccio;
    }

    // Getters
    public float getVelocitat() {
        return velocitat;
    }

    public int getColorDetector() {
        return colorDetector;
    }

    public int getPuntsCompensacio() {
        return puntsCompensacio;
    }

    public float getDistanciaDeteccio() {
        return distanciaDeteccio;
    }
}
