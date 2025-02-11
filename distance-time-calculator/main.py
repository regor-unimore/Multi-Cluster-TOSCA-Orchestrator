from flask import Flask
from flask_cors import CORS
from flask_restful import Api
from src.distance_matrix_resource import DistanceMatrix, DistanceMatrixId, DistanceMatrixDownload

app = Flask(__name__)
CORS(app)
api = Api(app)

base_path = '/api/v1'

api.add_resource(DistanceMatrix, f'{base_path}/dm')
api.add_resource(DistanceMatrixId, f'{base_path}/dm/<string:id>')
api.add_resource(DistanceMatrixDownload, f'{base_path}/dm/download/<string:id>')


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8080, debug=True)