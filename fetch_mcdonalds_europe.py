#!/usr/bin/env python3
"""
Fetch all McDonald's locations in Europe from OpenStreetMap.
This uses the Overpass API which has accurate, crowd-sourced data.

Run: pip install requests && python fetch_mcdonalds_europe.py
Output: mcdonalds_europe.csv (ready for MariaDB import)
"""

import requests
import csv
import time
import sys

# European countries with ISO codes
COUNTRIES = [
    ("DE", "Germany"),
    ("FR", "France"),
    ("GB", "United Kingdom"),
    ("ES", "Spain"),
    ("IT", "Italy"),
    ("NL", "Netherlands"),
    ("PL", "Poland"),
    ("BE", "Belgium"),
    ("AT", "Austria"),
    ("CH", "Switzerland"),
    ("PT", "Portugal"),
    ("SE", "Sweden"),
    ("NO", "Norway"),
    ("DK", "Denmark"),
    ("FI", "Finland"),
    ("IE", "Ireland"),
    ("CZ", "Czech Republic"),
    ("GR", "Greece"),
    ("HU", "Hungary"),
    ("RO", "Romania"),
    ("SK", "Slovakia"),
    ("HR", "Croatia"),
    ("SI", "Slovenia"),
    ("BG", "Bulgaria"),
    ("LT", "Lithuania"),
    ("LV", "Latvia"),
    ("EE", "Estonia"),
    ("LU", "Luxembourg"),
]

OVERPASS_URL = "https://overpass-api.de/api/interpreter"

def fetch_country(iso_code, country_name):
    """Fetch McDonald's from one country."""
    
    query = f'''
    [out:json][timeout:180];
    area["ISO3166-1"="{iso_code}"]->.searchArea;
    (
      node["brand"="McDonald's"](area.searchArea);
      way["brand"="McDonald's"](area.searchArea);
      node["name"="McDonald's"](area.searchArea);
      way["name"="McDonald's"](area.searchArea);
    );
    out center;
    '''
    
    print(f"Fetching {country_name} ({iso_code})...", end=" ", flush=True)
    
    try:
        response = requests.post(
            OVERPASS_URL, 
            data={"data": query},
            timeout=200
        )
        response.raise_for_status()
        data = response.json()
        elements = data.get("elements", [])
        print(f"found {len(elements)} locations")
        return elements
    except requests.exceptions.Timeout:
        print("TIMEOUT - retrying with smaller timeout...")
        time.sleep(10)
        return []
    except Exception as e:
        print(f"ERROR: {e}")
        return []

def extract_location(element):
    """Extract lat/lon from node or way."""
    if element["type"] == "node":
        return element.get("lat"), element.get("lon")
    elif element["type"] == "way" and "center" in element:
        return element["center"].get("lat"), element["center"].get("lon")
    return None, None

def main():
    all_locations = []
    
    print("=" * 60)
    print("Fetching McDonald's locations from OpenStreetMap")
    print("=" * 60)
    
    for iso_code, country_name in COUNTRIES:
        elements = fetch_country(iso_code, country_name)
        
        for el in elements:
            lat, lon = extract_location(el)
            if lat and lon:
                tags = el.get("tags", {})
                all_locations.append({
                    "id": el.get("id"),
                    "name": tags.get("name", "McDonald's"),
                    "latitude": round(lat, 8),
                    "longitude": round(lon, 8),
                    "street": tags.get("addr:street", ""),
                    "housenumber": tags.get("addr:housenumber", ""),
                    "city": tags.get("addr:city", ""),
                    "postcode": tags.get("addr:postcode", ""),
                    "country_code": iso_code,
                    "country": country_name,
                    "phone": tags.get("phone", ""),
                    "website": tags.get("website", ""),
                    "opening_hours": tags.get("opening_hours", ""),
                    "drive_through": "yes" if tags.get("drive_through") == "yes" else "no",
                })
        
        # Be nice to the API
        time.sleep(3)
    
    # Remove duplicates based on ID
    seen_ids = set()
    unique_locations = []
    for loc in all_locations:
        if loc["id"] not in seen_ids:
            seen_ids.add(loc["id"])
            unique_locations.append(loc)
    
    # Write CSV
    output_file = "mcdonalds_europe.csv"
    fieldnames = [
        "id", "name", "latitude", "longitude", "street", "housenumber",
        "city", "postcode", "country_code", "country", "phone", 
        "website", "opening_hours", "drive_through"
    ]
    
    with open(output_file, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(unique_locations)
    
    print("=" * 60)
    print(f"SUCCESS! Saved {len(unique_locations)} McDonald's locations")
    print(f"Output file: {output_file}")
    print("=" * 60)
    
    # Also create SQL import file
    sql_file = "mcdonalds_europe.sql"
    with open(sql_file, "w", encoding="utf-8") as f:
        f.write("""-- McDonald's Europe locations for MariaDB
-- Generated from OpenStreetMap data

CREATE TABLE IF NOT EXISTS mcdonalds (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL DEFAULT 'McDonald\\'s',
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    street VARCHAR(255),
    housenumber VARCHAR(50),
    city VARCHAR(255),
    postcode VARCHAR(20),
    country_code CHAR(2),
    country VARCHAR(100),
    phone VARCHAR(50),
    website VARCHAR(500),
    opening_hours VARCHAR(500),
    drive_through ENUM('yes', 'no') DEFAULT 'no',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_country (country_code),
    INDEX idx_location (latitude, longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

""")
        
        for loc in unique_locations:
            name = loc['name'].replace("'", "\\'")
            street = loc['street'].replace("'", "\\'") if loc['street'] else ""
            city = loc['city'].replace("'", "\\'") if loc['city'] else ""
            website = loc['website'].replace("'", "\\'") if loc['website'] else ""
            opening = loc['opening_hours'].replace("'", "\\'") if loc['opening_hours'] else ""
            
            f.write(f"INSERT INTO mcdonalds (id, name, latitude, longitude, street, housenumber, city, postcode, country_code, country, phone, website, opening_hours, drive_through) VALUES ({loc['id']}, '{name}', {loc['latitude']}, {loc['longitude']}, '{street}', '{loc['housenumber']}', '{city}', '{loc['postcode']}', '{loc['country_code']}', '{loc['country']}', '{loc['phone']}', '{website}', '{opening}', '{loc['drive_through']}');\n")
    
    print(f"SQL file: {sql_file}")

if __name__ == "__main__":
    main()