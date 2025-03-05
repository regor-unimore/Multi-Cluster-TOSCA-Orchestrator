from flask import Flask, request
import requests
import os
#
#  PRE_REQUISITI
#
PORT = os.environ.get("port")
if not PORT:
  PORT = 9088
SERVICE_PORT = os.environ.get("service_port")
SERVICE_ADDR = os.environ.get("service_addr")
ENDPOINT = os.environ.get("endpoint")
if not ENDPOINT:
   ENDPOINT = "/api/v1/georef"
if not SERVICE_PORT or not SERVICE_ADDR:
    #CRITICAL FAILURE
    #@TODO come la gestisco in TORCH? che meccanismo a disposizione per mandare l'errore alla Dashboard?
    raise EnvironmentError("service_port and service addr must be set")
    
#
# SERVER in FLASK
#    
    
app = Flask(__name__)


@app.route('/georef', methods=['POST'])
def georeference():
   data = request.get_json()
   print("\nservice_addr is " + SERVICE_ADDR + ":" + SERVICE_PORT)
   response = requests.post("http://" + SERVICE_ADDR + ":" +  SERVICE_PORT + ENDPOINT, json=data)
   return response.json()
   
if __name__ == '__main__':
   app.run(host="0.0.0.0", port=PORT)

