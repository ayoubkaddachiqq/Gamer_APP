package tn.esprit.entities;

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
        return libelle;
    }
}
