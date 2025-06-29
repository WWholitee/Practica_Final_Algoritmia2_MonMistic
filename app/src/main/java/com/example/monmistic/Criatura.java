package com.example.monmistic;

import java.util.Objects;

public class Criatura implements Comparable<Criatura> { // Implementem Comparable
    private int id;
    private String nom;
    private Genere genere; // Vapordrac, Focguard, Tornadrac, Aiguard
    private int especie; // Número aleatori de 1 a 8
    private float x,y;


    public Criatura(int id, Genere genere, int especie, float x, float y) {
        this.id = id;
        this.genere = genere;
        this.especie = especie;
        this.x = x;
        this.y = y;
        this.nom =  genere.getName() + especie + "_" + id;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public Genere getGenere() {
        return genere;
    }

    public int getEspecie() {
        return especie;
    }

    public float getX(){
        return x;
    }

    public float getY(){
        return y;
    }

    public void setX(float newX){
        this.x = newX;
    }

    public void setY(float newY){
        this.y = newY;
    }

    /**
     * Compara aquesta criatura amb una altra per a l'ordenació.
     * Les criatures s'ordenen pel seu 'nom' generat.
     *
     * @param other L'altra criatura amb la qual comparar.
     * @return Un valor negatiu si aquesta criatura és "menor" que l'altra,
     * zero si són "iguals", o un valor positiu si és "major".
     */
    @Override
    public int compareTo(Criatura other) {
        // Utilitzem l'atribut 'nom' per a l'ordenació, ja que és un String i ja el tenim.
        // Aquest 'nom' ja inclou el gènere, l'espècie i la ID, fent-lo únic i útil per a l'ordenació.
        return this.nom.compareTo(other.getNom());
    }

    /**
     * Indica si algun altre objecte és "igual a" aquest.
     * Dues criatures es consideren iguals si tenen la mateixa 'id'.
     * Això és crucial per a la unicitat de les claus en Mapes o conjunts.
     *
     * @param o L'objecte amb el qual comparar.
     * @return true si aquest objecte és el mateix que l'argument o; false altrament.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Criatura criatura = (Criatura) o;
        // La unicitat de la criatura es defineix per la seva ID
        return id == criatura.id;
    }

    /**
     * Retorna un valor de codi hash per a l'objecte.
     * Consistent amb equals(), el codi hash es basa en la 'id'.
     *
     * @return Un valor de codi hash per a aquest objecte.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}