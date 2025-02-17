from flask import Flask, request, jsonify
import requests
from datetime import datetime
import os
#
#  PRE_REQUISITI
#
PORT = os.environ.get("port")
if not PORT:
  PORT = 9088
    
#
# SERVER in FLASK
#    
    
app = Flask(__name__)


@app.route('/result', methods=['POST'])
def georeference():
   
   data = request.get_json()
   print("@TIMELOG " + str(datetime.utcnow()), flush=True)
   print("\nresult is " + str(data), flush=True)
   return jsonify({"status": "received"})
   
if __name__ == '__main__':
   app.run(host="0.0.0.0", port=PORT, debug=True)

