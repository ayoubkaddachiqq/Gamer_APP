package org.esprit.models;

public class TypeEvenement {
    private int id;
    private String libelle;

    public TypeEvenement() {}

    public TypeEvenement(int id, String libelle) {
        this.id = id;
        this.libelle = libelle;
    }

    public TypeEvenement(String libelle) {
        this.libelle = libelle;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    @Override
    public String toString() {
        // En JavaFX (ex: pour un ComboBox), c'est souvent cette méthode qui est affichée.
        // Donc on retourne le libelle directement, ou un format lisible.
        return libelle;
    }
}
