# Marketplace Project Summary

## 1. Project Goal

This project is a Java marketplace management application connected to a MySQL database.

The main domain objects are:

- `Produit`
- `Panier`
- `ProduitPanier`

The project is now organized with a layered architecture so the code is easier to understand, test, and extend.

---

## 2. What Was Found In The Original Project

At the beginning, the project contained:

- a minimal `Main` class
- two entity classes: `Produit` and `Panier`
- an empty `crud` service class
- one database helper class with hardcoded credentials
- no real DAO layer
- no real service layer
- no relation table model between `produit` and `panier`
- inconsistent package naming such as `entities`, `services`, and `utiles`

The original structure was too limited for a clean CRUD marketplace project.

---

## 3. What Was Done

The project was refactored into a cleaner beginner-friendly structure using:

- Java
- JDBC
- MySQL
- Maven
- JavaFX

### Main improvements

1. Cleaned the package structure
2. Added proper model classes
3. Added DAO interfaces
4. Added JDBC DAO implementations
5. Added service classes for business logic
6. Added a database configuration file
7. Added a professional SQL schema
8. Added a console entry point for testing CRUD
9. Added method descriptions to important functions
10. Added full product update support in the console
11. Added a JavaFX graphical interface
12. Added product search, category tree filtering, and sorting
13. Added panier interface actions for adding, updating, removing, and clearing panier lines
14. Fixed editable quantity handling in the JavaFX panier update button

---

## 4. Final Package Structure

The project now uses this structure:

```text
tn.esprit
tn.esprit.model
tn.esprit.dao
tn.esprit.dao.impl
tn.esprit.service
tn.esprit.util.config
tn.esprit.util.exception
```

### Meaning of each package

- `tn.esprit`
  - contains the application entry points:
    - `Main`
    - `MarketplaceApp`

- `tn.esprit.model`
  - contains the domain classes representing database/business objects

- `tn.esprit.dao`
  - contains DAO interfaces

- `tn.esprit.dao.impl`
  - contains JDBC implementations of the DAO interfaces

- `tn.esprit.service`
  - contains business logic and validation

- `tn.esprit.util.config`
  - contains database connection configuration

- `tn.esprit.util.exception`
  - contains custom runtime exceptions for data access errors

---

## 5. Database Design

The database used by the project is:

- `marketplace_db`

The connection is configured in:

- `src/main/resources/db.properties`

### Tables

#### A. `produit`

Stores product information.

Important fields:

- `id`
  - primary key
- `nom`
  - product name
- `description`
  - optional description
- `prix`
  - product price
- `stock`
  - available quantity
- `categorie`
  - product category
- `actif`
  - indicates whether the product is active
- `created_at`
  - creation time
- `updated_at`
  - last update time

#### B. `panier`

Stores cart information.

Important fields:

- `id`
  - primary key
- `reference`
  - business reference for the cart
- `statut`
  - cart state such as `ACTIF`
- `total`
  - synchronized total amount
- `created_at`
  - creation time
- `updated_at`
  - last update time

#### C. `produit_panier`

Relation table between carts and products.

Important fields:

- `panier_id`
  - foreign key to `panier`
- `produit_id`
  - foreign key to `produit`
- `quantite`
  - how many units of that product are in the cart
- `prix_unitaire`
  - product price at the time it was added
- `sous_total`
  - `quantite * prix_unitaire`
- `added_at`
  - line creation time

### Why `produit_panier` is important

One `panier` can contain many `produit`.
One `produit` can appear in many `panier`.

That is a many-to-many relation.

Because this relation also needs extra information like quantity and subtotal, a simple foreign key is not enough.
That is why a dedicated relation table was created.

---

## 6. SQL File

The full SQL schema is located in:

- `src/main/resources/sql/marketplace_schema.sql`

This file:

1. creates the database if needed
2. creates the tables in the correct order
3. creates constraints and indexes
4. inserts sample data
5. updates panier totals

---

## 7. Model Classes

### `Produit`

File:

- `src/main/java/tn/esprit/model/Produit.java`

Represents a product.

Contains:

- private fields
- constructors
- getters/setters
- `toString`
- `equals/hashCode`

### `Panier`

File:

- `src/main/java/tn/esprit/model/Panier.java`

Represents a cart.

Contains:

- cart header data
- total
- timestamps
- list of `ProduitPanier` lines

### `ProduitPanier`

File:

- `src/main/java/tn/esprit/model/ProduitPanier.java`

Represents one line of a cart.

Contains:

- `panierId`
- `produitId`
- `quantite`
- `prixUnitaire`
- `sousTotal`
- optional embedded `Produit`

---

## 8. DAO Layer

### What is DAO?

DAO means `Data Access Object`.

A DAO is responsible for:

- sending SQL queries
- reading data from the database
- inserting data into the database
- updating data in the database
- deleting data from the database

DAO classes should focus on database access only.

They should not contain business decisions like:

- checking if stock is enough for a cart
- deciding how total is calculated
- generating business rules

### DAO files in this project

- `src/main/java/tn/esprit/dao/ProduitDao.java`
- `src/main/java/tn/esprit/dao/PanierDao.java`
- `src/main/java/tn/esprit/dao/ProduitPanierDao.java`

### DAO implementation files

- `src/main/java/tn/esprit/dao/impl/ProduitDaoImpl.java`
- `src/main/java/tn/esprit/dao/impl/PanierDaoImpl.java`
- `src/main/java/tn/esprit/dao/impl/ProduitPanierDaoImpl.java`

### CRUD methods in DAO layer

#### `ProduitDaoImpl`

- `createProduit`
- `getProduitById`
- `getAllProduits`
- `updateProduit`
- `deleteProduit`
- `updateStock`
- `getProduitsActifs`

#### `PanierDaoImpl`

- `createPanier`
- `getPanierById`
- `getAllPaniers`
- `updatePanier`
- `deletePanier`
- `updateTotalPanier`

#### `ProduitPanierDaoImpl`

- `addProduitToPanier`
- `updateQuantiteProduitDansPanier`
- `removeProduitFromPanier`
- `getProduitsByPanierId`
- `clearPanier`
- `existsProduitInPanier`
- `getProduitDansPanier`

---

## 9. Service Layer

### What is a service?

A service is responsible for business logic.

It uses DAO classes to talk to the database, but it adds rules and decisions.

Examples of business logic:

- validate product price
- prevent negative stock
- check if requested quantity exceeds stock
- calculate line subtotal
- calculate panier total
- generate panier reference
- remove line if quantity becomes zero

### Difference Between DAO And Service

#### DAO

DAO answers this question:

> How do I read or write data in the database?

Examples:

- `INSERT INTO produit ...`
- `SELECT * FROM panier WHERE id = ?`
- `UPDATE produit_panier SET quantite = ?`

DAO is technical and SQL-oriented.

#### Service

Service answers this question:

> What should the application do according to business rules?

Examples:

- can this product be added to the cart?
- is the stock sufficient?
- what should the subtotal be?
- should a line be removed if quantity becomes zero?
- how should the total be recalculated?

Service is business-oriented.

### Simple example

If you want to add a product to a cart:

#### DAO part

The DAO only knows how to do:

- insert row into `produit_panier`
- update `panier.total`

#### Service part

The service decides:

- load the product
- verify it exists
- verify it is active
- verify stock is enough
- get current price
- calculate subtotal
- insert or update the line
- recalculate the cart total

### Service files in this project

- `src/main/java/tn/esprit/service/ProduitService.java`
- `src/main/java/tn/esprit/service/PanierService.java`

### Important service methods

#### `ProduitService`

- `addProduit`
- `updateProduit`
- `deleteProduit`
- `listProduits`
- `getProduitById`
- `validateProduit`
- `checkStockAvailable`

#### `PanierService`

- `createPanier`
- `listPaniers`
- `addProduitToPanier`
- `removeProduitFromPanier`
- `updateQuantite`
- `getPanierDetails`
- `calculateTotal`
- `clearPanier`

---

## 10. Database Connection Utility

Files:

- `src/main/java/tn/esprit/util/config/DatabaseConnection.java`
- `src/main/resources/db.properties`

### What it does

This utility centralizes the database connection logic.

Instead of writing username/password directly in code everywhere, the application now reads them from:

- `db.properties`

### Current configuration

- database: `marketplace_db`
- host: `localhost`
- port: `3306`
- username: `root`

---

## 11. Main Entry Point

File:

- `src/main/java/tn/esprit/Main.java`

This file is the console-based test entry point.

It lets you test:

- create product
- update product
- list products
- create panier
- add product to panier
- update quantity in panier
- remove line from panier
- display panier details
- clear panier

### Current menu

```text
1. Create produit
2. Update produit
3. List produits
4. Create panier
5. Add produit to panier
6. Update produit quantity in panier
7. Remove produit from panier
8. Display panier details
9. Clear panier
0. Exit
```

---

## 12. JavaFX Interface

File:

- `src/main/java/tn/esprit/MarketplaceApp.java`

This file is the graphical interface of the marketplace application.

It uses JavaFX and connects to the same service layer as the console application.

That means the interface does not write SQL directly.
It calls:

- `ProduitService`
- `PanierService`

This keeps the same clean architecture:

- UI = screens and buttons
- Service = business rules
- DAO = database access

### How to run the interface

The Maven JavaFX plugin is now configured to launch:

- `tn.esprit.MarketplaceApp`

From IntelliJ, you can run:

- `MarketplaceApp`

From the terminal, if Java and Maven are installed and available in `PATH`, you can run:

```text
mvn javafx:run
```

### Interface layout

The interface is divided into two main parts:

#### A. Products area

This area lets the user manage products.

It contains:

- a product table
- a product form
- create product button
- update product button
- delete product button
- clear form button
- search field
- category tree
- sort selector

#### B. Panier area

This area lets the user manage carts.

It contains:

- a panier selector
- a reference field for creating a new panier
- a panier line table
- a quantity spinner
- add selected product button
- update quantity button
- remove line button
- clear panier button
- total label

---

## 13. Product Search, Tree Filter, And Sort

The JavaFX interface now has better product navigation tools.

### Search bar

The search bar filters products by:

- product name
- product category
- product description

Example:

If the user types `clavier`, the table shows products whose name, category, or description contains that value.

### Category tree

The category tree is built from the categories found in the product list.

It has:

- root item: `All`
- one child item for each product category

When the user selects:

- `All`
  - all products are shown
- a specific category
  - only products from that category are shown

This is useful when there are many products and the user wants to browse by category.

### Sort selector

The sort selector lets the user order the product table.

Current sort options:

- `Name A-Z`
- `Name Z-A`
- `Price Low-High`
- `Price High-Low`
- `Stock Low-High`
- `Stock High-Low`

This sorting happens in the JavaFX interface after loading products from the service.

---

## 14. JavaFX Product Management

The product section of the interface supports:

1. selecting a product from the table
2. displaying its data in the form
3. creating a new product
4. updating the selected product
5. deleting the selected product
6. clearing the form

The form fields are:

- `nom`
- `description`
- `prix`
- `stock`
- `categorie`
- `actif`

When the user clicks `Create`, the interface creates a `Produit` object and sends it to:

- `ProduitService.addProduit`

When the user clicks `Update`, the interface updates the selected `Produit` through:

- `ProduitService.updateProduit`

When the user clicks `Delete`, the interface deletes the selected product through:

- `ProduitService.deleteProduit`

---

## 15. JavaFX Panier Management

The panier section of the interface supports:

1. creating a new panier
2. selecting an existing panier
3. adding the selected product to the selected panier
4. updating the quantity of a product already inside a panier
5. removing one panier line
6. clearing all lines from a panier
7. displaying the panier total

### Creating a panier

The user can type a custom reference.

If the reference is empty, the service generates one automatically.

This logic is handled by:

- `PanierService.createPanier`

### Adding a product to a panier

The user selects:

- one product from the product table
- one panier from the panier selector
- a quantity from the spinner

Then the interface calls:

- `PanierService.addProduitToPanier`

The service checks:

- that the panier exists
- that the product exists
- that the product is active
- that stock is sufficient
- whether the product is already in the panier

If the product already exists in the panier, the service increases the existing quantity.

### Updating quantity

The user selects one panier line, changes the quantity spinner, and clicks:

- `Update Quantity`

The interface calls:

- `PanierService.updateQuantite`

If the new quantity is `0` or less, the service removes the line.

### Quantity spinner fix

The quantity spinner is editable.

In JavaFX, when a spinner is editable, the typed text is not always committed before a button click.

Example:

1. the line quantity is `2`
2. the user types `5`
3. the user immediately clicks `Update Quantity`

Without a fix, JavaFX can still send the old value `2`.

This was fixed by adding a helper method in `MarketplaceApp`:

- `readQuantiteSpinner`

This method reads the text inside the spinner editor, converts it to an integer, stores it back into the spinner value factory, and then returns the correct quantity.

The same helper is used by:

- `Add Selected Product`
- `Update Quantity`

---

## 16. JavaFX Styling

File:

- `src/main/resources/marketplace.css`

This stylesheet controls the visual style of the JavaFX interface.

It styles:

- application background
- header
- section titles
- forms
- tables
- tree view
- buttons
- danger buttons
- total label

The goal is to make the interface cleaner and easier to read without changing the backend logic.

---

## 17. Product Update Feature

Originally, product update logic existed only in the backend layers.

That means:

- DAO update existed
- service update existed
- but the console app had no menu option for it

This was fixed by adding:

- `ProduitService.getProduitById`
- `Main.updateProduit`
- optional input helpers so the user can press Enter to keep current values

Now you can fully update a product from the console.

---

## 18. Files Created Or Updated

### Main application

- `src/main/java/tn/esprit/Main.java`
- `src/main/java/tn/esprit/MarketplaceApp.java`

### Model layer

- `src/main/java/tn/esprit/model/Produit.java`
- `src/main/java/tn/esprit/model/Panier.java`
- `src/main/java/tn/esprit/model/ProduitPanier.java`

### DAO interfaces

- `src/main/java/tn/esprit/dao/ProduitDao.java`
- `src/main/java/tn/esprit/dao/PanierDao.java`
- `src/main/java/tn/esprit/dao/ProduitPanierDao.java`

### DAO implementations

- `src/main/java/tn/esprit/dao/impl/ProduitDaoImpl.java`
- `src/main/java/tn/esprit/dao/impl/PanierDaoImpl.java`
- `src/main/java/tn/esprit/dao/impl/ProduitPanierDaoImpl.java`

### Services

- `src/main/java/tn/esprit/service/ProduitService.java`
- `src/main/java/tn/esprit/service/PanierService.java`

### Utilities

- `src/main/java/tn/esprit/util/config/DatabaseConnection.java`
- `src/main/java/tn/esprit/util/exception/DataAccessException.java`

### Resources

- `src/main/resources/db.properties`
- `src/main/resources/marketplace.css`
- `src/main/resources/sql/marketplace_schema.sql`

### Maven configuration

- `pom.xml`

### Documentation

- `PROJECT_SUMMARY.md`

---

## 19. Dependencies

The Maven file contains:

- MySQL Connector/J
- JavaFX dependencies
- JavaFX Maven plugin

File:

- `pom.xml`

The JavaFX Maven plugin is configured with:

- `tn.esprit.MarketplaceApp`

This means the graphical interface is the default JavaFX launch target.

---

## 20. What Was Tested

A full CRUD flow was executed against the local MySQL database.

The tested flow included:

1. create product
2. list product
3. create panier
4. add product to panier
5. update quantity
6. remove product from panier
7. display panier
8. clear panier

The backend logic worked with the live database.

### Current verification note

The latest JavaFX changes were reviewed from the source code.

In the current shell environment, these commands were not available:

- `java`
- `javac`
- `mvn`

Because of that, the JavaFX interface could not be compiled or launched from this shell.

It should be tested from IntelliJ or from a terminal where Java and Maven are available in `PATH`.

---

## 21. How To Test Now

1. Start MySQL or Wamp
2. Make sure `marketplace_db` exists
3. Make sure `db.properties` has the correct credentials
4. Run `MarketplaceApp.java` from IntelliJ
5. Use the JavaFX interface

Alternative console test:

1. Run `Main.java`
2. Use the console menu options

Alternative Maven launch:

```text
mvn javafx:run
```

If needed, you can verify data in phpMyAdmin in:

- `produit`
- `panier`
- `produit_panier`

---

## 22. Possible Next Improvements

Good next steps for this project would be:

1. add transaction management for panier operations
2. add unit tests
3. add better form validation messages in the JavaFX interface
4. add confirmation before deleting products used by existing panier lines
5. add a dashboard view with product count, panier count, and total sales amount
6. add DTOs or controllers if the UI becomes larger
7. move database password out of `db.properties` before sharing the project

---

## 23. Final Summary

The project is now much cleaner than the original version.

It now has:

- proper layers
- proper SQL schema
- reusable JDBC access
- business logic separation
- better naming
- configurable database connection
- a testable console application
- a JavaFX graphical interface
- product search
- category tree filtering
- product sorting
- panier line management from the interface
- working CRUD structure for `Produit`, `Panier`, and `ProduitPanier`

The most important architectural lesson in this project is:

- DAO = database access
- Service = business logic

That separation makes the project easier to maintain and easier to grow into a larger JavaFX application later.
