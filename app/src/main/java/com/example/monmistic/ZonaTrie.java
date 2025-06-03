package com.example.monmistic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Canvis afegits per la implementació de Trie
public class ZonaTrie {
    private Node root;

    public ZonaTrie() {
        root = new Node();
    }

    // Mètode per inserir una zona al Trie
    public void insert(String name, Zona zona) {
        Node current = root;
        // Convertir el nom a minúscules per cerques insensibles a majúscules
        for (char ch : name.toLowerCase().toCharArray()) {
            current.fills.putIfAbsent(ch, new Node());
            current = current.fills.get(ch);
        }
        current.isFinalClau = true;
        current.zonaData = zona; // Guarda la zona completa
    }

    // Mètode per buscar una zona pel seu nom complet
    public Zona search(String name) {
        Node current = root;
        // Convertir el nom a minúscules per cerques insensibles a majúscules
        for (char ch : name.toLowerCase().toCharArray()) {
            Node node = current.fills.get(ch);
            if (node == null) {
                return null; // No trobat
            }
            current = node;
        }
        return current.isFinalClau ? current.zonaData : null;
    }

    // Opcional: Mètode per obtenir suggeriments basats en un prefix (per autocompletat)
    public List<Zona> getSuggestions(String prefix) {
        List<Zona> suggestions = new ArrayList<>();
        Node current = root;
        for (char ch : prefix.toLowerCase().toCharArray()) {
            Node node = current.fills.get(ch);
            if (node == null) {
                return suggestions; // No hi ha coincidències
            }
            current = node;
        }
        findAllWords(current, suggestions);
        return suggestions;
    }

    private void findAllWords(Node node, List<Zona> list) {
        if (node.isFinalClau && node.zonaData != null) {
            list.add(node.zonaData);
        }
        for (Node child : node.fills.values()) {
            findAllWords(child, list);
        }
    }
}