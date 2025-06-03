package com.example.monmistic;

import java.util.HashMap;
import java.util.Map;

// Canvis afegits per la implementació de Trie
class Node {
    Map<Character, Node> fills;
    Zona zonaData; // Aquí emmagatzemarem l'objecte Zona complet
    boolean isFinalClau;

    public Node() {
        fills = new HashMap<>();
        isFinalClau = false;
    }
}
