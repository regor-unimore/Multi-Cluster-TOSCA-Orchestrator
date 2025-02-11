import os
import requests
from http import HTTPStatus
from typing import Dict, Optional
from dataclasses import dataclass
from flask_restful import Resource
from abc import ABC, abstractmethod
from flask import request, send_from_directory
from multiprocessing import Pool, Manager, current_process

from src.storage import saveData, retrieveData, deleteData
from commons import create_uuid, validate_resolution_type, is_valid_uuid,decode_base64
from src.logging_config import configure_logger


logger = configure_logger()


# Base configuration
@dataclass
class SolverConfig:
    storage_bucket: str
    worker_pool_size: int = 2
    file_path: str = "solver/files"


# Base solver interface
class ProblemSolver(ABC):
    @abstractmethod
    def process_file_input(self, file_path: str) -> Dict:
        pass

    @abstractmethod
    def solve(self, problem_data: Dict) -> Dict:
        pass

    @abstractmethod
    def generate_output_file(self, problem_data: Dict, output_data: Dict, id: str) -> str:
        pass
    
    def _send_callback(self, url: str, output: Dict, input_data: Dict):
        logger.info(f"Async Resolution: callBack url {url}, output {output}, input {input_data}")
        try:
            requests.post(url, json={
                "output": output,
                "extraVars": {
                    "input": input_data,
                    "chainId": 1,
                    "requestId": 2
                }
            })
        except requests.RequestException as e:
            print(f"Callback error: {e}")


# Function for async resolution
def async_resolution(solver:ProblemSolver, inputs: Dict, outputs: Dict, config:SolverConfig, problem_data: Dict, callback_url: Optional[str] = None, 
                    generate_file: bool = False):
    
    logger.info(f"Process {os.getpid()} - Is Daemon: {current_process().daemon}")
    logger.info(f"Async Resolution: Starting for ID {problem_data['id']}")
    try:
        id = problem_data['id']
        if "extraVars" in inputs[id] and "invokedByOrchestrator" in inputs[id]["extraVars"]:
            new_id = inputs[id]["extraVars"]["id"]
            inputs[new_id] = inputs[id]
            del inputs[id]
            id = new_id

        outputs[id] = solver.solve(problem_data)

        logger.info(f"Async Resolution: Solved for ID {id}")
        result = {
            "output": outputs[id],
            "extraVars": {"input": inputs[id]}
        }

        if generate_file:
            logger.info(f"Async Resolution: Generating output file for ID {id}")
            solver.generate_output_file(problem_data, result["output"], id)

        saveData(result, id, config.storage_bucket)

        if callback_url:
            solver._send_callback(callback_url,outputs[id],inputs[id])

    except Exception as e:
        logger.error(f"Async Resolution: Error for ID {problem_data['id']}: {e}")
        raise 



class GenericSolverResource(Resource):
    def __init__(self, solver: ProblemSolver, config:SolverConfig):
        self.solver = solver
        self.config = config
        self.manager = Manager()
        self.inputs = self.manager.dict()
        self.outputs = self.manager.dict()
        self.workers = Pool(config.worker_pool_size)
    
    def on_error(self,error):
        logger.error(f"Async: returned an error: {error}")
    

    def post(self):
        try:
            data = request.json
            logger.info("POST: Received request")
            resolution_type = data['resolutionType'].lower()
            
            if not validate_resolution_type(resolution_type):
                logger.warning("POST: Invalid resolution type")
                return "Error: invalid resolution type", HTTPStatus.BAD_REQUEST
            
            if resolution_type == "url" and 'callbackUrl' not in data:
                logger.warning("POST: Missing callback URL")
                return "Error: callback url not present", HTTPStatus.BAD_REQUEST

            id = create_uuid()
            logger.info(f"POST : Arrived post request")

            problem_data = {'id': id, 'data': self._process_input(data, id)}
            callBack_url = data.get('callbackUrl') if resolution_type == 'url' else None
            is_file = 'file' in data

            if resolution_type == 'inline':
                logger.info(f"POST: Starting inline resolution for ID {id}")
                solution = self.solver.solve(problem_data)
                if is_file:
                    self.solver.generate_output_file(problem_data, solution, id)
                return {"output": solution, "extraVars": {"input": data}}, HTTPStatus.OK

            self.inputs[id] = data
            logger.info(f"POST: Queued async resolution for ID {id}")

            self.workers.apply_async(
                async_resolution,
                args=(self.solver,
                      self.inputs,
                      self.outputs,
                      self.config,
                      problem_data, 
                      callBack_url,
                      is_file
                    ), error_callback=self.on_error
            )

            return ({'callback_url': data['callbackUrl']} if resolution_type == 'url' else {'id': id}), HTTPStatus.OK

        except Exception as e:
            logger.error(f"POST Error: {e}")
            return str(e), HTTPStatus.INTERNAL_SERVER_ERROR

    def _process_input(self, data: Dict, id: str) -> Dict:
        if 'file' in data:
            if isinstance(data['file'], list):
                combined_data = {}
                for index, file_content in enumerate(data['file']):
                    input_file_path = f"{self.config.file_path}/input_{id}_{index}.xlsx"
                    if decode_base64(file_content, input_file_path) != 1:
                        raise ValueError(f"Failed to decode file at index {index}")
                    file_data = self.solver.process_file_input(input_file_path)
                    combined_data.update(file_data)
                return combined_data
            else:
                input_file_path = f"{self.config.file_path}/input_{id}.xlsx"
                if decode_base64(data['file'], input_file_path) != 1:
                    raise ValueError("Failed to decode file")
                return self.solver.process_file_input(input_file_path)
        return data.get('geocodedAddresses', {})
    


class GenericSolverId(Resource):
    def __init__(self, solver: GenericSolverResource):
        self.manager = solver

    def get(self, id):
        logger.info(f"GET: Request for ID {id}")
        if not is_valid_uuid(id):
            logger.warning(f"GET: Invalid ID {id}")
            return None, HTTPStatus.BAD_REQUEST
        
        content = retrieveData(self.manager.config.storage_bucket, id)
        if content:
            logger.info(f"GET: Data found for ID {id}")
            return content, HTTPStatus.OK
        else:
            logger.warning(f"GET: No data found for ID {id}")
            return None, HTTPStatus.NOT_FOUND

    def delete(self, id):
        logger.info(f"DELETE: Request for ID {id}")
        if not is_valid_uuid(id):
            logger.warning(f"DELETE: Invalid ID {id}")
            return None, HTTPStatus.BAD_REQUEST
        
        if deleteData(self.manager.config.storage_bucket, id):
            try:
                self.manager.outputs.pop(id, None)
                self.manager.inputs.pop(id, None)
            except Exception as e:
                logger.warning(f"DELETE: Error cleaning up for ID {id}: {e}")
            return None, HTTPStatus.NO_CONTENT
        logger.warning(f"DELETE: No data to delete for ID {id}")
        return None, HTTPStatus.NOT_FOUND


# Generic download file
class GenericDownloadResource(Resource):
    def __init__(self, solver: GenericSolverResource):
        self.manager = solver

    def get(self, id: str):
        if not is_valid_uuid(id):
            return None, HTTPStatus.BAD_REQUEST
        return send_from_directory(self.manager.config.file_path, f"{id}.xlsx")
