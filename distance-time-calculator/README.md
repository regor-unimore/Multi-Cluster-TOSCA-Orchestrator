# Distance Time Calculator Microservice

The **Distance Time Calculator** microservice computes matrices of distances and travel times between multiple addresses.

---

## Available APIs
- `POST` `http://127.0.0.1:8081/api/v1/dm`
- `GET` `http://127.0.0.1:8081/api/v1/dm/{id}`
- `DELETE` `http://127.0.0.1:8081/api/v1/dm/{id}`

---

### **Requests**
#### POST
To calculate distance and time matrices, send a `POST` request with the following input structure in the request body:
```json
{
    "resolutionType": "inline",
    "geocodedAddresses": {
        "Unipr - Dipartimento di Ingegneria e Architettura - Sede didattica": {
            "Address": "Parco Area delle Scienze 59, 43124, Parma",
            "Latitude": 44.763999382678,
            "Longitude": 10.312232164773
        },
        "Dipartimento di Scienze e Metodi dell'Ingegneria": {
            "Address": "Via Giovanni Amendola 2, 42122, Reggio Emilia",
            "Latitude": 44.686525517463,
            "Longitude": 10.665364354227
        },
        "Dipartimento di Ingegneria Enzo Ferrari": {
            "Address": "Via Pietro Vivarelli 10, 41125, Modena",
            "Latitude": 44.629339731591,
            "Longitude": 10.948345368238
        },
        "Università di Bologna - Scuola di Ingegneria e Architettura": {
            "Address": "Viale del Risorgimento 2, 40136, Bologna",
            "Latitude": 44.488176579814,
            "Longitude": 11.328324028909
        }
    }
}
```
---

**Resolution Types**\
The _resolutionType_ field supports the same options as the **Georeference Microservice**:
- `inline`: Synchronous request; the user waits for the response.
- `id`: Asynchronous request; the microservice responds with an _id_ to retrieve the output later.
- `url`: Asynchronous request with a _callbackUrl_ field in the request body, allowing the microservice to send results to the specified URL.

---

#### GET
Through a `GET` request, retrieve the results of a distance and time matrix calculation using the _id_ provided by the `POST` request with _id_ as resolution type:
```json
{
    "output": {
        "Unipr - Dipartimento di Ingegneria e Architettura - Sede didattica": {
            "Unipr - Dipartimento di Ingegneria e Architettura - Sede didattica": 0.0,
            "Dipartimento di Scienze e Metodi dell'Ingegneria": [
                40.721,
                43.687
            ],
            "Dipartimento di Ingegneria Enzo Ferrari": [
                64.121,
                58.795
            ],
            "Università di Bologna - Scuola di Ingegneria e Architettura": [
                98.901,
                76.437
            ]
        },
        "Dipartimento di Scienze e Metodi dell'Ingegneria": {
            "Unipr - Dipartimento di Ingegneria e Architettura - Sede didattica": [
                40.679,
                41.692
            ],
            "Dipartimento di Scienze e Metodi dell'Ingegneria": 0.0,
            "Dipartimento di Ingegneria Enzo Ferrari": [
                26.675,
                34.667
            ],
            "Università di Bologna - Scuola di Ingegneria e Architettura": [
                75.274,
                58.468
            ]
        },
        "Dipartimento di Ingegneria Enzo Ferrari": {
            "Unipr - Dipartimento di Ingegneria e Architettura - Sede didattica": [
                62.884,
                57.66
            ],
            "Dipartimento di Scienze e Metodi dell'Ingegneria": [
                25.674,
                33.928
            ],
            "Dipartimento di Ingegneria Enzo Ferrari": 0.0,
            "Università di Bologna - Scuola di Ingegneria e Architettura": [
                40.978,
                39.403
            ]
        },
        "Università di Bologna - Scuola di Ingegneria e Architettura": {
            "Unipr - Dipartimento di Ingegneria e Architettura - Sede didattica": [
                97.4,
                74.422
            ],
            "Dipartimento di Scienze e Metodi dell'Ingegneria": [
                73.216,
                57.17
            ],
            "Dipartimento di Ingegneria Enzo Ferrari": [
                39.47,
                37.395
            ],
            "Università di Bologna - Scuola di Ingegneria e Architettura": 0.0
        }
    },
    "extraVars": {
        "input": {
            "resolutionType": "ID",
            "geocodedAddresses": {
                "Unipr - Dipartimento di Ingegneria e Architettura - Sede didattica": {
                    "Address": "Parco Area delle Scienze 59, 43124, Parma",
                    "Latitude": 44.763999382678,
                    "Longitude": 10.312232164773
                },
                "Dipartimento di Scienze e Metodi dell'Ingegneria": {
                    "Address": "Via Giovanni Amendola 2, 42122, Reggio Emilia",
                    "Latitude": 44.686525517463,
                    "Longitude": 10.665364354227
                },
                "Dipartimento di Ingegneria Enzo Ferrari": {
                    "Address": "Via Pietro Vivarelli 10, 41125, Modena",
                    "Latitude": 44.629339731591,
                    "Longitude": 10.948345368238
                },
                "Università di Bologna - Scuola di Ingegneria e Architettura": {
                    "Address": "Viale del Risorgimento 2, 40136, Bologna",
                    "Latitude": 44.488176579814,
                    "Longitude": 11.328324028909
                }
            }
        }
    }
}
```

---

#### DELETE
To delete a specific output, send a `DELETE` request using the corresponding _id_:
```bash
DELETE http://127.0.0.1:8081/api/v1/dm/{id}
```

---

### **Storage**
Results generated with _id_ or _url_ resolution types are stored in **MinIO**. Refer to the `minio` folder's README for additional details.
