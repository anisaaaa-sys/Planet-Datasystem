#IN2090 - Innlevering 5

import os
import psycopg2
from psycopg2.extras import RealDictCursor
from contextlib import closing

#Henter miljøvariabler fra systemet

DB_HOST = "dbpg-ifi-kurs03.uio.no"
DB_PORT = "5432"
DB_NAME = "anisai"
USER = os.getenv("DB_USER")
PWD = os.getenv("DB_PASSWORD")

def main():
    try:
        with psycopg2.connect(
            host=DB_HOST,
            port=DB_PORT,
            dbname=DB_NAME,
            user=USER,
            password=PWD
        ) as conn:
            with conn.cursor() as cursor:
                while True:
                    print("\n--[ HUFFSA ]--")
                    print("Vennligst velg et alternativ:")
                    print("1. Søk etter planet")
                    print("2. Legg inn forsøksresultat")
                    print("3. Avslutt")
                    valg = input("Valg: ").strip()

                    if valg == "1":
                        planet_sok(conn)
                    elif valg == "2":
                        legg_inn_resultat(conn)
                    elif valg == "3":
                        print("Avslutter programmet.")
                        break
                    else:
                        print("Ugyldig valg. Vennligst prøv igjen.")
                        cursor.execute("SELECT version();")
                        db_version = cursor.fetchone()
                        print("PostgreSQL-versjon:", db_version[0])

    except psycopg2.OperationalError as e:
        print(f"Databasefeil: {e}")

def planet_sok(conn):
    molekyl1 = input("Molekyl 1: ").strip()
    if not molekyl1:
        print("Molekyl 1 må oppgis.")
        return
    molekyl2 = input("Molekyl 2: ").strip()

    query = """
    SELECT p.navn, p.masse AS planetmasse, s.masse AS stjermasse, s.avstand AS stjerndistanse,
           CASE WHEN p.liv = TRUE THEN 'Ja' ELSE 'Nei' END AS liv
    FROM planet p
    JOIN stjerne s ON p.stjerne = s.navn
    JOIN materie m1 ON p.navn = m1.planet
    WHERE m1.molekyl = %s
    """
    params = [molekyl1]

    if molekyl2:
        query += """
        AND EXISTS (
            SELECT 1 FROM materie m2
            WHERE m2.planet = p.navn AND m2.molekyl = %s
        ) 
        """
        params.append(molekyl2)

    query += "ORDER BY s.avstand ASC"

    with conn.cursor(cursor_factory=RealDictCursor) as cur:
        cur.execute(query, params)
        rows = cur.fetchall()

        if not rows:
            print("Ingen planeter fant som oppfyller søkekriteriene.")
        else:
            for row in rows:
                print("\n--Planet--")
                print(f"Navn: {row['navn']}")
                print(f"Planet-masse: {row['planetmasse']}")
                print(f"Stjerne-masse: {row['stjermasse']}")
                print(f"Stjerne-distanse: {row['stjerndistanse']}")
                print(f"Bekreftet liv: {row['liv']}")

def legg_inn_resultat(conn):
    planet_navn = input("Planet: ").strip()
    skummel_svar = input("Skummel (j/n): ").strip().lower()
    skummel = True if skummel_svar == 'j' else False

    intelligent_svar = input("Intelligent (j/n): ").strip().lower()
    intelligent = True if intelligent_svar == 'j' else False

    beskrivelse = input("Beskrivelse: ").strip()

    query = "UPDATE planet SET skummel = %s, intelligent = %s, beskrivelse = %s WHERE navn = %s"
    params = (skummel, intelligent, beskrivelse, planet_navn)

    with conn.cursor() as cursor:
        cursor.execute(query, params)
        conn.commit()

        rows_updated = cursor.rowcount

        if rows_updated > 0:
            print("Resultat lagt inn.")
        else:
            print(f"Ingen planet funnet med navnet '{planet_navn}'.")

if __name__ == "__main__":
    main()