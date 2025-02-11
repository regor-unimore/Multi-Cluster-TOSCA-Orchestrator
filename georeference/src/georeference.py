import pandas as pd
from arcgis.gis import GIS
from arcgis.geocoding import geocode
from typing import Optional
from commons import get_db_connection, convert_numpy_types_in_structure


from src.logging_config import configure_logger
logger = configure_logger()




def geocode_address(name: str) -> Optional[dict]:
    my_gis = GIS()
    try:
        res = geocode(name)
    except IndexError:
        return None
    return res[0]


def geocoding_result(addressToGeocode: str) -> Optional[dict]:
    res = geocode_address(addressToGeocode)
    if res is not None:
        if res['score'] > 77:
            result = {'Address': res['address'], 'Latitude': res['location']['y'], 'Longitude': res['location']['x']}
            return result


def retrieve_data(add:dict) -> Optional[dict]:
    address = add['Address']
    conn, cur = get_db_connection('src/db/georeference.db')
    try:
        res = cur.execute(f"SELECT * FROM resolutions WHERE address LIKE '{address}'").fetchone()
    except:
        res = None
    if res is None:
        address_to_geocode = f'{address} {add["Area"]} {add["District"]} {add["ZipCode"]} {add["Region"]}'
        geocode = geocoding_result(address_to_geocode)
        
        if geocode is None:
            return None
        try:
            cur.execute(f"SELECT COUNT(*) FROM resolutions").fetchone()[0]
            if cur.execute("SELECT COUNT(*) FROM resolutions").fetchone()[0] == 30:
                cur.execute('DELETE FROM resolutions WHERE access_counter = (SELECT MIN(access_counter) FROM resolutions) LIMIT 1')

            cur.execute(f'INSERT OR IGNORE INTO resolutions VALUES (\"{address}\", \"{geocode["address"]}\", {geocode["latitude"]}, {geocode["longitude"]}, 1)')
        except:
            pass
        
        conn.commit()
        conn.close()
        return geocode
    
    cur.execute(f"UPDATE resolutions SET access_counter = access_counter + 1 WHERE address LIKE '{address}'")
    resolution = {"address": res[1], "latitude": res[2], "longitude": res[3]}
    conn.commit()
    conn.close()
    return resolution


def get_georeferenced_addresses(addresses: dict) -> dict:
    output_res = {}
    for address in addresses.keys():
        res = retrieve_data(addresses[address])
        if isinstance(res, str):
            continue
        
        output_res[address] = res
    
    return output_res


def convert_xls_to_json(path: str) -> dict:
    columns = ["Name", "Address", "Area", "District", "ZipCode", "Region", "Country"]
    frame = pd.read_excel(path)
    addresses = frame[columns].to_dict(orient='records')
    addresses = convert_numpy_types_in_structure(addresses)
    
    json_output = {}
    for address in addresses:
        name = address.pop("Name")
        json_output[name] = address
    
    return json_output


def convert_json_to_xls(input_data: dict, output_data: dict, id: str) -> str:
    # Create DataFrame from input_data dictionary
    df = pd.DataFrame.from_dict(input_data, orient='index')
    
    # Add "Name" column
    df['Name'] = df.index

    # Add "Latitude" and "Longitude" columns from output_data dictionary
    df['Latitude'] = df.index.map(lambda x: output_data[x]['Latitude'])
    df['Longitude'] = df.index.map(lambda x: output_data[x]['Longitude'])
    
    # Reorder columns to have "Name" first
    columns = ['Name'] + [col for col in df.columns if col != 'Name']
    df = df[columns]
    
    # Write the DataFrame to an Excel file   
    fd = f'georeference/xls/{id}.xlsx'
    with pd.ExcelWriter(fd, engine='xlsxwriter') as writer:
        df.to_excel(writer, sheet_name='Geolocated Addresses', index=False)
    
    return fd
