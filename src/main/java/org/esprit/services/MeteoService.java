package org.esprit.services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MeteoService {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Map<String, Coordonnees> COORDONNEES_TUNISIE = creerCoordonneesTunisie();

    public CompletableFuture<ResultatMeteo> chargerMeteo(String lieu, LocalDate date) {
        if (lieu == null || lieu.isBlank() || date == null) {
            return CompletableFuture.completedFuture(
                    new ResultatMeteo("Meteo non verifiee", "Choisir lieu et date debut", "--")
            );
        }

        String villeMeteo = extraireVilleMeteo(lieu);
        String ville = URLEncoder.encode(villeMeteo, StandardCharsets.UTF_8);
        String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + ville
                + "&count=1&language=fr&format=json&countryCode=TN";
        HttpRequest geoRequest = HttpRequest.newBuilder(URI.create(geoUrl)).GET().build();

        return httpClient.sendAsync(geoRequest, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        Coordonnees fallback = trouverCoordonneesLocales(villeMeteo);
                        if (fallback != null) {
                            return chargerMeteoDepuisCoordonnees(fallback, date);
                        }
                        return CompletableFuture.completedFuture(
                                new ResultatMeteo("Meteo indisponible", "Service geocoding " + response.statusCode(), "!")
                        );
                    }
                    String body = response.body();
                    Double latitude = extraireNombre(body, "\"latitude\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
                    Double longitude = extraireNombre(body, "\"longitude\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
                    String nom = extraireTexte(body, "\"name\"\\s*:\\s*\"([^\"]+)\"");
                    if (latitude == null || longitude == null) {
                        Coordonnees fallback = trouverCoordonneesLocales(villeMeteo);
                        if (fallback != null) {
                            return chargerMeteoDepuisCoordonnees(fallback, date);
                        }
                        return CompletableFuture.completedFuture(
                                new ResultatMeteo("Lieu introuvable", "Essayez Tunis, Sfax, Sousse ou Monastir", "?")
                        );
                    }
                    return chargerMeteoDepuisCoordonnees(
                            new Coordonnees(nom == null ? villeMeteo : nom, latitude, longitude), date
                    );
                });
    }

    private CompletableFuture<ResultatMeteo> chargerMeteoDepuisCoordonnees(Coordonnees coordonnees, LocalDate date) {
        String meteoUrl = "https://api.open-meteo.com/v1/forecast?latitude=" + coordonnees.latitude
                + "&longitude=" + coordonnees.longitude
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
                + "&timezone=auto&start_date=" + date + "&end_date=" + date;
        HttpRequest meteoRequest = HttpRequest.newBuilder(URI.create(meteoUrl)).GET().build();
        return httpClient.sendAsync(meteoRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(meteoResponse -> {
                    if (meteoResponse.statusCode() < 200 || meteoResponse.statusCode() >= 300) {
                        return new ResultatMeteo(
                                "Meteo indisponible", "Service meteo " + meteoResponse.statusCode(), "!"
                        );
                    }
                    return formaterMeteo(meteoResponse.body(), coordonnees.nom, date);
                });
    }

    private String extraireVilleMeteo(String lieu) {
        String ville = lieu.split(",")[0].trim();
        ville = ville.replaceAll("(?i)\\b(centre ville|mall|gaming|arena|stade|salle|lac 2|lac)\\b", "").trim();
        return ville.isBlank() ? lieu.trim() : ville;
    }

    private Coordonnees trouverCoordonneesLocales(String lieu) {
        String normalise = normaliser(lieu);
        for (Map.Entry<String, Coordonnees> entry : COORDONNEES_TUNISIE.entrySet()) {
            if (normalise.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private ResultatMeteo formaterMeteo(String body, String nom, LocalDate date) {
        Double max = extraireNombre(body, "\"temperature_2m_max\"\\s*:\\s*\\[(-?\\d+(?:\\.\\d+)?)");
        Double min = extraireNombre(body, "\"temperature_2m_min\"\\s*:\\s*\\[(-?\\d+(?:\\.\\d+)?)");
        Integer code = extraireEntier(body, "\"weather_code\"\\s*:\\s*\\[(\\d+)");
        if (max == null || min == null || code == null) {
            return new ResultatMeteo("Meteo indisponible", "Aucune prevision pour " + date, "!");
        }
        String details = descriptionCodeMeteo(code) + " - " + Math.round(min) + " / " + Math.round(max) + " C";
        return new ResultatMeteo(nom == null ? "Meteo" : nom, details, iconCodeMeteo(code));
    }

    private Double extraireNombre(String texte, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(texte);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : null;
    }

    private Integer extraireEntier(String texte, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(texte);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private String extraireTexte(String texte, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(texte);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String normaliser(String mot) {
        return java.text.Normalizer.normalize(mot, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }

    private String descriptionCodeMeteo(int code) {
        if (code == 0) return "ciel clair";
        if (code <= 3) return "partiellement nuageux";
        if (code <= 48) return "brouillard";
        if (code <= 67) return "pluie";
        if (code <= 77) return "neige";
        if (code <= 82) return "averses";
        return "orage";
    }

    private String iconCodeMeteo(int code) {
        if (code == 0) return "SUN";
        if (code <= 3) return "CLD";
        if (code <= 48) return "FOG";
        if (code <= 67) return "RAN";
        if (code <= 77) return "SNW";
        if (code <= 82) return "SHW";
        return "STM";
    }

    private static Map<String, Coordonnees> creerCoordonneesTunisie() {
        Map<String, Coordonnees> coordonnees = new LinkedHashMap<>();
        coordonnees.put("tunis", new Coordonnees("Tunis", 36.8065, 10.1815));
        coordonnees.put("sfax", new Coordonnees("Sfax", 34.7406, 10.7603));
        coordonnees.put("sousse", new Coordonnees("Sousse", 35.8256, 10.6369));
        coordonnees.put("monastir", new Coordonnees("Monastir", 35.7770, 10.8262));
        coordonnees.put("nabeul", new Coordonnees("Nabeul", 36.4561, 10.7376));
        coordonnees.put("ariana", new Coordonnees("Ariana", 36.8625, 10.1956));
        coordonnees.put("ben arous", new Coordonnees("Ben Arous", 36.7531, 10.2189));
        coordonnees.put("bizerte", new Coordonnees("Bizerte", 37.2744, 9.8739));
        coordonnees.put("gabes", new Coordonnees("Gabes", 33.8815, 10.0982));
        coordonnees.put("kairouan", new Coordonnees("Kairouan", 35.6781, 10.0963));
        return coordonnees;
    }

    public record ResultatMeteo(String titre, String details, String icon) {
    }

    private record Coordonnees(String nom, double latitude, double longitude) {
    }
}
