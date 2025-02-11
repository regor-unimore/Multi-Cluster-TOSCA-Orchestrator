# Georeference Microservice

The **Georeference** microservice converts provided addresses into precise geographic coordinates (latitude and longitude).

---

## Available APIs
- `POST` `http://127.0.0.1:8080/api/v1/georef`
- `GET` `http://127.0.0.1:8080/api/v1/georef/{id}`
- `DELETE` `http://127.0.0.1:8080/api/v1/georef/{id}`

---

### **Requests**
#### POST
To georeference addresses, send a `POST` request with the input data in the request body:
```json
{
    "resolutionType": "inline",
    "addressesDescription": {
        "Unipr - Dipartimento di Ingegneria e Architettura - Sede didattica": {
            "Address": "Parco Area delle Scienze 59",
            "Area": "Parma",
            "District": "Parma",
            "ZipCode": 43124,
            "Region": "Emilia Romagna",
            "Country": "Ita"
        },
        "Dipartimento di Scienze e Metodi dell'Ingegneria": {
            "Address": "Via Giovanni Amendola 2",
            "Area": "Reggio Emilia",
            "District": "Reggio Emilia",
            "ZipCode": 42122 ,
            "Region": "Emilia Romagna",
            "Country": "Ita"
        },
        "Dipartimento di Ingegneria Enzo Ferrari": {
            "Address": "Via Pietro Vivarelli 10",
            "Area": "Modena",
            "District": "Modena",
            "ZipCode": 41125 ,
            "Region": "Emilia Romagna",
            "Country": "Ita"
        },
        "Università di Bologna - Scuola di Ingegneria e Architettura": {
            "Address": "Viale del Risorgimento 2",
            "Area": "Bologna",
            "District": "Città Metropolitana di Bologna",
            "ZipCode": 40136,
            "Region": "Emilia Romagna",
            "Country": "Ita"
        }
    }
}
```
---

**Resolution Types**
- `inline`: Synchronous request; the user waits for the response.
- `id`: Asynchronous request; the microservice responds with an _id_ to retrieve the output later.
- `url`: Asynchronous request with a _callbackUrl_ field in the request body, allowing the microservice to send a POST request with results to the specified URL.

---

#### GET
Through a `GET` request, retrieve the results of a georeferencing using the _id_ provided by the `POST` request with _id_ as resolution type:
```json
{
    "output": {
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
    },
    "extraVars": {
        "input": {
            "resolutionType": "ID",
            "addressesDescription": {
                "Unipr - Dipartimento di Ingegneria e Architettura - Sede didattica": {
                    "Address": "Parco Area delle Scienze 59",
                    "Area": "Parma",
                    "District": "Parma",
                    "ZipCode": 43124,
                    "Region": "Emilia Romagna",
                    "Country": "Ita"
                },
                "Dipartimento di Scienze e Metodi dell'Ingegneria": {
                    "Address": "Via Giovanni Amendola 2",
                    "Area": "Reggio Emilia",
                    "District": "Reggio Emilia",
                    "ZipCode": 42122,
                    "Region": "Emilia Romagna",
                    "Country": "Ita"
                },
                "Dipartimento di Ingegneria Enzo Ferrari": {
                    "Address": "Via Pietro Vivarelli 10",
                    "Area": "Modena",
                    "District": "Modena",
                    "ZipCode": 41125,
                    "Region": "Emilia Romagna",
                    "Country": "Ita"
                },
                "Università di Bologna - Scuola di Ingegneria e Architettura": {
                    "Address": "Viale del Risorgimento 2",
                    "Area": "Bologna",
                    "District": "Città Metropolitana di Bologna",
                    "ZipCode": 40136,
                    "Region": "Emilia Romagna",
                    "Country": "Ita"
                }
            }
        }
    }
}
```

---

#### DELETE
Delete a specific output by its _id_ using a `DELETE` request:
```bash
DELETE http://127.0.0.1:8080/api/v1/georef/{id}
```

---

### **Storage**
Results from _id_ and _url_ resolution types are stored in **MinIO**. Refer to the README in the `minio` folder for more details.
