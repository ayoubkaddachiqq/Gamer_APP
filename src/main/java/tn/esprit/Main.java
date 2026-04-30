package tn.esprit;

import tn.esprit.model.Panier;
import tn.esprit.model.Produit;
import tn.esprit.model.ProduitPanier;
import tn.esprit.service.PanierService;
import tn.esprit.service.ProduitService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

public class Main {

    private final ProduitService produitService;
    private final PanierService panierService;
    private final Scanner scanner;

    public Main() {
        this.produitService = new ProduitService();
        this.panierService = new PanierService();
        this.scanner = new Scanner(System.in);
    }

    /**
     * Starts the console application entry point.
     */
    public static void main(String[] args) {
        new Main().start();
    }

    /**
     * Displays the main menu and dispatches user actions until exit.
     */
    private void start() {
        System.out.println("=== Marketplace Management Console ===");
        boolean running = true;

        while (running) {
            printMenu();
            int choice = readInt("Choose an option: ");

            try {
                switch (choice) {
                    case 1 -> createProduit();
                    case 2 -> updateProduit();
                    case 3 -> listProduits();
                    case 4 -> createPanier();
                    case 5 -> addProduitToPanier();
                    case 6 -> updateQuantitePanier();
                    case 7 -> removeProduitFromPanier();
                    case 8 -> displayPanierDetails();
                    case 9 -> clearPanier();
                    case 0 -> running = false;
                    default -> System.out.println("Unknown option.");
                }
            } catch (RuntimeException exception) {
                System.out.println("Operation failed: " + exception.getMessage());
            }
        }

        scanner.close();
        System.out.println("Application closed.");
    }

    /**
     * Prints the available console actions.
     */
    private void printMenu() {
        System.out.println();
        System.out.println("1. Create produit");
        System.out.println("2. Update produit");
        System.out.println("3. List produits");
        System.out.println("4. Create panier");
        System.out.println("5. Add produit to panier");
        System.out.println("6. Update produit quantity in panier");
        System.out.println("7. Remove produit from panier");
        System.out.println("8. Display panier details");
        System.out.println("9. Clear panier");
        System.out.println("0. Exit");
    }

    /**
     * Reads product data from the console and persists it.
     */
    private void createProduit() {
        String nom = readLine("Nom: ");
        String description = readLine("Description: ");
        BigDecimal prix = readBigDecimal("Prix: ");
        int stock = readInt("Stock: ");
        String categorie = readLine("Categorie: ");

        Produit produit = new Produit(nom, description, prix, stock, categorie, true);
        Produit created = produitService.addProduit(produit);
        System.out.println("Produit created with id " + created.getId());
    }

    /**
     * Loads an existing product and updates any field the user changes.
     */
    private void updateProduit() {
        int produitId = readInt("Produit id to update: ");
        Produit produit = produitService.getProduitById(produitId)
                .orElseThrow(() -> new IllegalArgumentException("Produit not found for id " + produitId + "."));

        System.out.println("Current produit: " + produit);
        System.out.println("Press Enter to keep the current value.");

        String nom = readOptionalLine("Nom [" + produit.getNom() + "]: ");
        String description = readOptionalLine("Description [" + nullToEmpty(produit.getDescription()) + "]: ");
        BigDecimal prix = readOptionalBigDecimal("Prix [" + produit.getPrix() + "]: ", produit.getPrix());
        int stock = readOptionalInt("Stock [" + produit.getStock() + "]: ", produit.getStock());
        String categorie = readOptionalLine("Categorie [" + nullToEmpty(produit.getCategorie()) + "]: ");
        Boolean actif = readOptionalBoolean("Actif [" + (produit.isActif() ? "yes" : "no") + "] (yes/no): ", produit.isActif());

        if (!nom.isBlank()) {
            produit.setNom(nom);
        }
        produit.setDescription(description.isBlank() ? produit.getDescription() : description);
        produit.setPrix(prix);
        produit.setStock(stock);
        produit.setCategorie(categorie.isBlank() ? produit.getCategorie() : categorie);
        produit.setActif(actif);

        Produit updated = produitService.updateProduit(produit);
        System.out.println("Produit updated: " + updated);
    }

    /**
     * Loads and prints all products from the database.
     */
    private void listProduits() {
        List<Produit> produits = produitService.listProduits();
        if (produits.isEmpty()) {
            System.out.println("No produit found.");
            return;
        }

        produits.forEach(System.out::println);
    }

    /**
     * Creates a new panier with an optional custom reference.
     */
    private void createPanier() {
        String reference = readLine("Reference (leave empty for auto): ");
        Panier panier = new Panier();
        panier.setReference(reference.isBlank() ? null : reference);
        Panier created = panierService.createPanier(panier);
        System.out.println("Panier created with id " + created.getId() + " and reference " + created.getReference());
    }

    /**
     * Adds a product line into an existing panier.
     */
    private void addProduitToPanier() {
        int panierId = readInt("Panier id: ");
        int produitId = readInt("Produit id: ");
        int quantite = readInt("Quantite: ");

        panierService.addProduitToPanier(panierId, produitId, quantite);
        displayPanierSummary(panierId);
    }

    /**
     * Updates the quantity of a product already present in a panier.
     */
    private void updateQuantitePanier() {
        int panierId = readInt("Panier id: ");
        int produitId = readInt("Produit id: ");
        int quantite = readInt("Nouvelle quantite (0 to remove): ");

        panierService.updateQuantite(panierId, produitId, quantite);
        displayPanierSummary(panierId);
    }

    /**
     * Removes a product line from a panier.
     */
    private void removeProduitFromPanier() {
        int panierId = readInt("Panier id: ");
        int produitId = readInt("Produit id: ");

        panierService.removeProduitFromPanier(panierId, produitId);
        displayPanierSummary(panierId);
    }

    /**
     * Displays one panier with its product lines and total.
     */
    private void displayPanierDetails() {
        int panierId = readInt("Panier id: ");
        displayPanierSummary(panierId);
    }

    /**
     * Removes all product lines from a panier and resets its total.
     */
    private void clearPanier() {
        int panierId = readInt("Panier id: ");
        panierService.clearPanier(panierId);
        System.out.println("Panier cleared.");
    }

    /**
     * Prints a panier summary including every line and the synchronized total.
     */
    private void displayPanierSummary(int panierId) {
        Panier panier = panierService.getPanierDetails(panierId);
        System.out.println(panier);
        for (ProduitPanier ligne : panier.getLignes()) {
            System.out.println("  " + ligne);
        }
        System.out.println("Total panier: " + panier.getTotal());
    }

    /**
     * Reads an integer safely from the console.
     */
    private int readInt(String label) {
        while (true) {
            System.out.print(label);
            String value = scanner.nextLine();
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    /**
     * Reads an integer but allows an empty value to keep the current one.
     */
    private int readOptionalInt(String label, int currentValue) {
        while (true) {
            System.out.print(label);
            String value = scanner.nextLine().trim();
            if (value.isEmpty()) {
                return currentValue;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    /**
     * Reads a decimal value safely from the console.
     */
    private BigDecimal readBigDecimal(String label) {
        while (true) {
            System.out.print(label);
            String value = scanner.nextLine();
            try {
                return new BigDecimal(value);
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a valid decimal number.");
            }
        }
    }

    /**
     * Reads a decimal value but allows an empty value to keep the current one.
     */
    private BigDecimal readOptionalBigDecimal(String label, BigDecimal currentValue) {
        while (true) {
            System.out.print(label);
            String value = scanner.nextLine().trim();
            if (value.isEmpty()) {
                return currentValue;
            }
            try {
                return new BigDecimal(value);
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a valid decimal number.");
            }
        }
    }

    /**
     * Reads one trimmed line from the console.
     */
    private String readLine(String label) {
        System.out.print(label);
        return scanner.nextLine().trim();
    }

    /**
     * Reads a line and allows an empty value.
     */
    private String readOptionalLine(String label) {
        System.out.print(label);
        return scanner.nextLine().trim();
    }

    /**
     * Reads a boolean value but allows an empty value to keep the current one.
     */
    private Boolean readOptionalBoolean(String label, boolean currentValue) {
        while (true) {
            System.out.print(label);
            String value = scanner.nextLine().trim();
            if (value.isEmpty()) {
                return currentValue;
            }
            if (value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("y") || value.equalsIgnoreCase("true")) {
                return true;
            }
            if (value.equalsIgnoreCase("no") || value.equalsIgnoreCase("n") || value.equalsIgnoreCase("false")) {
                return false;
            }
            System.out.println("Please answer yes or no.");
        }
    }

    /**
     * Converts null strings into an empty label for prompts.
     */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
