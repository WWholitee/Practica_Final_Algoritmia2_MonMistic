package com.example.monmistic;

import java.util.Random;

/**
 * Classe que representa una criatura al joc.
 */
public class Criatura {
    private int id;
    private String nom;
    private String genere; // Vapordrac, Focguard, Tornadrac, Aiguard
    private int especie; // Número aleatori de 1 a 8
    private float x,y;


    public Criatura(int id, String genere, int especie, float x, float y) {
        this.id = id;
        this.genere = genere;
        this.especie = especie;
        this.x = x;
        this.y = y;
        this.nom = genere.concat(String.valueOf(especie)).concat("_").concat(String.valueOf(id));
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getGenere() {
        return genere;
    }

    public int getEspecie() {
        return especie;
    }
}