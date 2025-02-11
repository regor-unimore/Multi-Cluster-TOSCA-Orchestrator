from flask import Flask
from flask_cors import CORS
from flask_restful import Api
from src.orchestrator_resource import UseChain, Callback

app = Flask(__name__)
CORS(app)
api = Api(app)

base_path = '/api/v1'

api.add_resource(UseChain, f'{base_path}/use-chain')
api.add_resource(Callback, f'{base_path}/callback')

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8080, debug=True)