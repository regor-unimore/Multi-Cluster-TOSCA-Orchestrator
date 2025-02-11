import io
import json
from minio import Minio
import os

minio_host = os.environ.get('MINIO_HOST','minio')
minio_port = os.environ.get('MINIO_PORT','9000')


# Client minIO
minio_client = Minio(
    f"{minio_host}:{minio_port}",
    access_key="test",
    secret_key="testtest",
    secure=False
)


def saveData(output, id, bucket):
    try:
        # Convert the JSON object into a JSON string and then into an io.BytesIO object
        json_data = json.dumps(output)
        json_bytes = io.BytesIO(json_data.encode('utf-8'))
        
        # Save the data to the bucket
        minio_client.put_object(
            bucket,
            f"{id}",
            json_bytes,
            len(json_data),
            content_type="application/json",
        )
        print(f"Successfully uploaded {id} to {bucket}")

    except Exception as e:
        print(f"Error occurred in MinIO [SAV]: {e}")


def retrieveData(bucket, id):
    try:
        # Retrieve the object from the MinIO bucket
        response = minio_client.get_object(
            bucket,
            f"{id}"
        )
        
        content = response.read().decode('utf-8')
        content_json = json.loads(content)
        
        return content_json
    
    except Exception as err:
        print(f"Error occurred in MinIO [RET]: {err}")
        return None
    

def deleteData(bucket, id):
    try:
        res = minio_client.get_object(
            bucket,
            f"{id}"
        )

        if res == None:
            return False

        minio_client.remove_object(
            bucket,
            f"{id}"
        )

        return True
    
    except Exception as err:
        print(f"Error occurred in MinIO [DEL]: {err}")
        return False
