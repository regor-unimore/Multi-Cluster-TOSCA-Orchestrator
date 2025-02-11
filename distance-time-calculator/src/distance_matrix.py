import os
import json
import requests
import pandas as pd
from src.logging_config import configure_logger
from commons import get_db_connection, convert_numpy_types_in_structure

logger = configure_logger()

def get_route(origin:str, destination:str, start_coord:tuple, end_coord:tuple) -> tuple:
    """
    Retrieve or calculate the route between two locations.
    """
    conn, cur = get_db_connection('src/db/distancematrix.db')
    
    try:
        res = cur.execute(
            "SELECT * FROM distances WHERE origin = ? AND destination = ?",
            (origin, destination),
        ).fetchone()
    except:
        res = None

    if res is not None:
        # Retrieve the stored distance and duration
        distance_tuple = (res[2], res[3])

        # Update the access counter
        try:
            cur.execute(
                "UPDATE distances SET access_counter = access_counter + 1 WHERE origin = ? AND destination = ?",
                (origin, destination),
            )
        except:
            pass
    else:
        # Fetch new data from OSRM server
        osrm_server_host = os.getenv('OSRM_SERVER_HOST', 'osrm_container')
        osrm_server_port = os.getenv('OSRM_SERVER_PORT', '5000')
        osrm_server_api = os.getenv('OSRM_SERVER_API', '/route/v1/driving/')

        url = f"http://{osrm_server_host}:{osrm_server_port}{osrm_server_api}{start_coord[0]},{start_coord[1]};{end_coord[0]},{end_coord[1]}"
        logger.info(f"Send request to OSRM server: {url}")

        try:
            response = requests.get(url)
            response.raise_for_status()
            osrm_data = response.json()

            distance_tuple = (
                round(osrm_data['routes'][0]['distance'] / 1000, ndigits=3),
                round(osrm_data['routes'][0]['duration'] / 60, ndigits=3),
            )

            # Manage the database size and insert the new record
            try:
                if cur.execute(f"SELECT COUNT(*) FROM distances").fetchone()[0] == 250:
                    cur.execute('DELETE FROM distances WHERE access_counter = (SELECT MIN(access_counter) FROM distances) LIMIT 1')
                cur.execute('INSERT OR IGNORE INTO distances VALUES (?, ?, ?, ?, 1)',
                            (origin, destination, distance_tuple[0], distance_tuple[1]),
                )
            except:
                pass
        except:
            distance_tuple = (None, None)

    conn.commit()
    conn.close()
    return distance_tuple


def get_distance_time_matrices(addresses: dict) -> pd.DataFrame:
    """
    Generate distance and time matrices for the given addresses.
    """
    keys = list(addresses.keys())
    n = len(keys)

    # Initialize an empty DataFrame with keys as both rows and columns
    df = pd.DataFrame(index=keys, columns=keys)

    # Iterate over each pair of locations
    for i in range(n):
        for j in range(n):
            if i == j:
                df.iloc[i, j] = 0.0
            else:
                origin = addresses[keys[i]]
                destination = addresses[keys[j]]
                distance_and_time = get_route(
                    origin=origin["Address"],
                    destination=destination["Address"],
                    start_coord=(origin["Longitude"], origin["Latitude"]),
                    end_coord=(destination["Longitude"], destination["Latitude"])
                )
                df.iloc[i, j] = distance_and_time

    # Convert DataFrame to JSON format
    km_time_output_json = json.loads(df.to_json())
    return km_time_output_json


def convert_xls_to_json(path: str) -> dict:
    # Read the Excel file
    df = pd.read_excel(path)

    # Initialize the output_dict dictionary
    output_dict = {}

    # Iterate over each row in the DataFrame
    for _, row in df.iterrows():
        name = row['Name']
        output_dict[name] = {
            "Address": row['Address'],
            "Area": row['Area'],
            "District": row['District'],
            "ZipCode": row['ZipCode'],
            "Region": row['Region'],
            "Country": row['Country'],
            "Latitude": row['Latitude'],
            "Longitude": row['Longitude']
        }
    
    # Convert any numpy types to native Python types
    output_dict = convert_numpy_types_in_structure(output_dict)
    return output_dict


def convert_json_to_xls(input_data: dict, output_data: dict, id: str) -> str:
    # Create DataFrame for Geolocated Addresses
    geolocated_addresses = pd.DataFrame.from_dict(input_data, orient='index')
    geolocated_addresses.index.name = 'Name'
    geolocated_addresses.reset_index(inplace=True)
    
    # Create DataFrames for Distance Matrix (km) and Time Matrix (minutes)
    distance_matrix = pd.DataFrame(index=output_data.keys(), columns=output_data.keys())
    time_matrix = pd.DataFrame(index=output_data.keys(), columns=output_data.keys())
    for key, values in output_data.items():
        for sub_key, sub_values in values.items():
            distance_matrix.at[key, sub_key] = sub_values[0] if isinstance(sub_values, list) else sub_values
            time_matrix.at[key, sub_key] = sub_values[1] if isinstance(sub_values, list) else sub_values

    # Save to Excel with three sheets
    output_file = f'distance-time-calculator/xls/{id}.xlsx'
    with pd.ExcelWriter(output_file) as writer:
        geolocated_addresses.to_excel(writer, sheet_name='Geolocated Addresses', index=False)
        distance_matrix.to_excel(writer, sheet_name='Distance Matrix (km)')
        time_matrix.to_excel(writer, sheet_name='Time Matrix (minutes)')

    return output_file

