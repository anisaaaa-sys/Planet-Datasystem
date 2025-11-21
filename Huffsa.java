//IN2090 - Innlevering 5

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Huffsa {
    //Konfigurerer databasevariabler via miljøvariabler
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String USER = System.getenv("DB_USER");
    private static final String PWD = System.getenv("DB_PASSWORD");

    public static void main(String[] args) {
        //Oppretter tilkobling til databasen
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PWD)) {
            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    //Meny for brukerinput
                    System.out.println("\n--[ HUFFSA ]--");
                    System.out.println("Vennligst velg et alternativ:");
                    System.out.println("1. Søk etter planet");
                    System.err.println("2. Legg inn forsøksresultat");
                    System.out.println("3. Avslutt");
                    System.out.print("Valg: ");

                    String valg = scanner.nextLine().trim();

                    switch (valg) {
                        case "1":
                            planetSok(conn); //Kjører funksjon for planetsøk
                            break;
                        case "2":
                            leggInnResultat(conn); //Kjører funksjon for å legge inn resultat
                            break;
                        case "3":
                            //Avslutter programmet
                            System.out.println("Avslutter programmet.");
                            scanner.close();
                            return;
                        default:
                            System.out.println("Ugyldig valg. Vennligst prøv igjen.");
                    }
                }
            }
        } catch (SQLException e) {
            //Håndterer databasefeil
            System.err.println("Databasefeil: " + e.getMessage());
        }
    }

    //Oppgave 1: Søker etter planeter basert på molekyler
    public static void planetSok(Connection conn) {
        @SuppressWarnings("resource")
        Scanner scanner = new Scanner(System.in);
        try {
            System.out.println("\n--[PLANET-SØK]--");
            System.out.print("Molekyl 1: ");
            String molekyl1 = scanner.nextLine().trim();
            if (molekyl1.isEmpty()) {
                System.out.print("Molekyl 1 må oppgis.");
                return;
            }
            System.out.print("Molekyl 2: ");
            String molekyl2 = scanner.nextLine().trim();

            //Byggel SQL-spørring for å finne planeter med gitte molekyler
            List<String> params = new ArrayList<>();
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT p.navn, p.masse AS planetmasse, s.masse AS stjermasse, s.avstand AS stjerndistanse, ")
                .append("CASE WHEN p.liv = TRUE THEN 'Ja' ELSE 'Nei' END AS liv ")
                .append("FROM planet p ")
                .append("JOIN stjerne s ON p.stjerne = s.navn ")
                .append("JOIN materie m1 ON p.navn = m1.planet ")
                .append("WHERE m1.molekyl = ? ");

            params.add(molekyl1);

            //Molekyl 2 må ikke oppgis
            if (!molekyl2.isEmpty()) {
                sql.append("AND EXISTS (")
                    .append("SELECT 1 FROM materie m2 ")
                    .append("WHERE m2.planet = p.navn AND m2.molekyl = ?")
                    .append(") ");
                params.add(molekyl2);
            }

            sql.append("ORDER BY s.avstand ASC");

            //Kjører SQL-spørring med parametre
            PreparedStatement pstmt = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                pstmt.setString(i + 1, params.get(i));
            }

            ResultSet rs = pstmt.executeQuery();

            boolean found = false;
            while (rs.next()) {
                //Skriver ut planetinformasjon hvis treff finnes
                found = true;
                String navn = rs.getString("navn");
                double planetmasse = rs.getDouble("planetmasse");
                double stjermasse = rs.getDouble("stjermasse");
                double stjerndistanse = rs.getDouble("stjerndistanse");
                String liv = rs.getString("liv");  // Bruk "liv" alias fra spørringen

                System.out.println("\n--Planet--");
                System.out.println("Navn: " + navn);
                System.out.println("Planet-masse: " + planetmasse);
                System.out.println("Stjerne-masse: " + stjermasse);
                System.out.println("Stjerne-distanse: " + stjerndistanse);
                System.out.println("Bekreftet liv: " + liv);
            }

            if (!found) {
                System.out.println("Ingen planeter fant som oppfyller sokekriteriene.");
            }

            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("Feil under planet-søk: " + e.getMessage());
            System.out.println();
        }
    }

    //Oppgave 2: Legger inn resultat om en planet
    public static void leggInnResultat(Connection conn) {
        @SuppressWarnings("resource")
        Scanner scanner = new Scanner(System.in);
        try {
            System.out.println("\n--[LEGG INN RESULTAT]--");
            System.out.print("Planet: ");
            String planetNavn = scanner.nextLine().trim();

            System.out.print("Skummel: ");
            String skummelSvar = scanner.nextLine().trim().toLowerCase();
            boolean skummel = skummelSvar.equals("j");

            System.out.print("Intelligent: ");
            String intelligentSvar = scanner.nextLine().trim().toLowerCase();
            boolean intelligent = intelligentSvar.equals("j");

            System.out.print("Beskrivelse: ");
            String beskrivelse = scanner.nextLine().trim();

            //Forbered SQL-setningen for å oppdatere planeten med nye verdier
            String sql = "UPDATE planet SET skummel = ?, intelligent = ?, beskrivelse = ?, liv = true WHERE navn = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setBoolean(1, skummel);
                pstmt.setBoolean(2, intelligent);
                pstmt.setString(3, beskrivelse);
                pstmt.setString(4, planetNavn);

                //Utfør oppdateringen og sjekk antall oppdaterte rader
                int rowsUpdated = pstmt.executeUpdate();
                //Bekreftelse om resultat er lagt inn
                System.out.println(rowsUpdated > 0 ? "Resultat lagt inn." : "Ingen planet funnet med navnet '" 
                + planetNavn + "'.\n");

                pstmt.close();
            }
        } catch (SQLException e) {
            System.err.println("Feil ved innsending av forsøksresultat: " + e.getMessage());
            System.out.println();
        }
    }
}