package com.example.monmistic;

import android.util.Log;

public class Zona {
    private String nomOficial;
    private int x1, y1, x2, y2;

    public Zona(String nomOficial, int x1, int y1, int x2, int y2) {
        this.nomOficial = nomOficial;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public int getY1() {
        return y1;
    }

    public int getX2() {
        return x2;
    }

    public int getY2() {
        return y2;
    }

    public int getX1() {
        return x1;
    }

    public String getNomOficial() {
        return nomOficial;
    }

    public boolean contains(int x, int y) {
        Log.d("ZONA: ", "x1: " + x1 + " x2: " + x2 + "y1: " + y1 + "y2: " + y2);
        return x >= x1 && x < x2 && y >= y1 && y < y2;
    }
}
