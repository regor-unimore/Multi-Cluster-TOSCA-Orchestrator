import uuid
import base64
import sqlite3


def create_uuid() -> str:
    return str(uuid.uuid4())


def is_valid_uuid(val:str) -> bool:
    try:
        uuid.UUID(val)
        return True
    except ValueError:
        return False


def validate_resolution_type(resolution_type:str) -> bool:
    return resolution_type in ['id', 'inline', 'url']


def convert_numpy_types_in_structure(data):
    if isinstance(data, dict):
        return {key: convert_numpy_types_in_structure(value) for key, value in data.items()}
    elif isinstance(data, list):
        return [convert_numpy_types_in_structure(item) for item in data]
    elif hasattr(data, 'dtype') and 'int' in data.dtype.name:
        return int(data)
    elif hasattr(data, 'dtype') and 'float' in data.dtype.name:
        return float(data)
    else:
        return data


def get_db_connection(path:str):
    conn = sqlite3.connect(path)
    cur = conn.cursor()
    return conn, cur


def decode_base64(file_content:str, file_path:str) -> None:
    # Decodifica la stringa Base64 in dati binari
    decoded_file = base64.b64decode(file_content)
    with open(file_path, 'wb') as file:
        file.write(decoded_file)
    return 1


# def inspect_data_types(obj, prefix=''):
#     """
#     Recursively inspect and print the types of all elements in a data structure.
    
#     Args:
#         obj: The object to inspect
#         prefix: String prefix for nested structures (used for indentation)
#     """
#     if isinstance(obj, dict):
#         print(f"{prefix}Dict containing:")
#         for key, value in obj.items():
#             print(f"{prefix}  Key '{key}':")
#             inspect_data_types(value, prefix + '    ')
#     elif isinstance(obj, list):
#         print(f"{prefix}List containing {len(obj)} items:")
#         for i, item in enumerate(obj):
#             print(f"{prefix}  Item {i}:")
#             inspect_data_types(item, prefix + '    ')
#     else:
#         print(f"{prefix}Type: {type(obj)}, Value: {obj}")