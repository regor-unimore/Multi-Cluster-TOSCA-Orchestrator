from typing import Dict
from src.logging_config import configure_logger
from src.georeference import convert_xls_to_json, get_georeferenced_addresses, convert_json_to_xls
from src.problem_solver import ProblemSolver, GenericSolverResource, SolverConfig, GenericSolverId, GenericDownloadResource


logger = configure_logger()


class GeoreferenceSolver(ProblemSolver):
    def process_file_input(self, file_path):
        """
        Converts an input XLSX file into a usable JSON format.

        Args:
            file_path (str): Path to the input file.

        Returns:
            Dict: Data converted from the file.
        """
        logger.info('Georeference - Convert Excel File to JSON')
        return convert_xls_to_json(path=file_path)
    

    def solve(self, problem_data):
        """
        Solves the Georeference problem using the provided data.
        
        Args:
            problem_data (Dict): Problem data, including ID and input addresses.

        Returns:
            Dict: Solution to the Georeference problem.
        """
        logger.info('Georeference - Get Georeferenced Addresses')
        return get_georeferenced_addresses(problem_data['data'])
    

    def generate_output_file(self, problem_data: Dict, output_data: Dict, id: str):
        """
        Generates an XLSX file with the output data.

        Args:
            problem_data (Dict): Input data.
            output_data (Dict): Output data.
            id (str): Unique ID for the file.

        Returns:
            str: Path to the generated file.
        """
        logger.info('Georeference - Convert Output JSON to Excel')
        return convert_json_to_xls(problem_data['data'], output_data, id)



solver = GeoreferenceSolver()
config = SolverConfig(storage_bucket="georeference", file_path="georeference/xls")
manager = None  # Global Initialization with `None`


class Georeference(GenericSolverResource):
    def __init__(self):
        super().__init__(solver, config)

GeoreferenceGenericSolver = Georeference()


class GeoreferenceId(GenericSolverId):
    def __init__(self):
        super().__init__(GeoreferenceGenericSolver)


class GeoreferenceDownload(GenericDownloadResource):
    def __init__(self):
        super().__init__(GeoreferenceGenericSolver)