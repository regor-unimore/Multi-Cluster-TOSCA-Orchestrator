from flask import Flask
from flask_cors import CORS
from flask_restful import Api
from src.georeference_resource import Georeference, GeoreferenceId, GeoreferenceDownload

app = Flask(__name__)
CORS(app)
api = Api(app)

base_path = '/api/v1'

api.add_resource(Georeference, f'{base_path}/georef')
api.add_resource(GeoreferenceId, f'{base_path}/georef/<string:id>')
api.add_resource(GeoreferenceDownload, f'{base_path}/georef/download/<string:id>')


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8080, debug=True)