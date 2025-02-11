import os
import requests
from flask import request
from flask_restful import Resource
from multiprocessing import Pool
from commons import create_uuid
from src.logging_config import configure_logger

logger = configure_logger()


georeference_host = os.environ.get('GEOREF_HOST','georeference_container')
georeference_port = os.environ.get('GEOREF_PORT','8080')
georeference_api = os.environ.get('GEOREF_API','/api/v1/georef')

dm_host = os.environ.get('DM_HOST','distance_time_calculator_container')
dm_port = os.environ.get('DM_PORT','8080')
dm_api = os.environ.get('DM_API','/api/v1/dm')

res_host = os.environ.get('RESULT_HOST')
res_port = os.environ.get('RESULT_PORT','5001')
res_api = os.environ.get('RESULT_API','/callback')

orch_host = os.environ.get('ORCH_HOST','orchestrator_container')
orch_port = os.environ.get('ORCH_PORT','8080')
orch_api = os.environ.get('ORCH_API','/api/v1/callback')



def invokeService_RVRPTW(data):

    def find_key(data, target_key, results=None):
        if results is None:
            results = []

        # Se il dato è un dizionario, cerchiamo la chiave nei suoi elementi
        if isinstance(data, dict):
            for key, value in data.items():
                if key == target_key:
                    results.append(value)  # Aggiungi il valore associato alla chiave trovata
                else:
                    # Continua la ricerca nei valori
                    find_key(value, target_key, results)
        # Se il dato è una lista, esploriamo ogni elemento
        elif isinstance(data, list):
            for item in data:
                find_key(item, target_key, results)

        return results

    if data["extraVars"]["chainId"] == 1:
        if data["extraVars"]["requestId"] == 0:
            url = f"http://{georeference_host}:{georeference_port}{georeference_api}"
            logger.info(f"Invoking Georeference service: {url}")
            requests.post(url, json=data)
        
        elif data["extraVars"]["requestId"] == 1:
            data["geocodedAddresses"] = data.pop("output", None)
            url = f"http://{dm_host}:{dm_port}{dm_api}"
            logger.info(f"Invoking Distance-Time Calculator service: {url}")
            requests.post(url, json=data)
        
        elif data["extraVars"]["requestId"] == 2:
            url = f"http://{res_host}:{res_port}{res_api}"
            requests.post(url, json=data)

def on_error(error):
    logger.error(f"Async: returned an error: {error}")

class UseChain(Resource):

    def post(self):

        id = create_uuid()
        data = request.json
        data["resolutionType"] = "url"
        url = f"http://{orch_host}:{orch_port}{orch_api}"
        data["callbackUrl"] = url
        data["extraVars"] = {
            "id": id,
            "chainId": 1,
            "requestId": 0,
            "invokedByOrchestrator": True
            }

        workers = Pool(2)

        workers.apply_async(invokeService_RVRPTW, args=(data,), error_callback=on_error)

        return  {'id': id}, 202
            

class Callback(Resource):

    def post(self):
        
        response = request.json
        url = f"http://{orch_host}:{orch_port}{orch_api}"
        data = {
            "resolutionType" : "url",
            "callbackUrl": url,
            "output": response["output"],
            "extraVars": {
                "id": response["extraVars"]["input"]["extraVars"]["id"],
                "chainId": response["extraVars"]["chainId"],
                "requestId": response["extraVars"]["requestId"],
                "invokedByOrchestrator": True,
                "input": response["extraVars"]["input"]
            }
        }

        workers = Pool(2)

        workers.apply_async(invokeService_RVRPTW, args=(data,), error_callback=on_error)

        return  {'id': data["extraVars"]["id"]}, 202