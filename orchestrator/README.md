# Orchestrator

The **Orchestrator** is a key component of the system, responsible for managing and coordinating requests across multiple microservices when invoked as a _chain of microservices_. This component ensures that operations are executed in the correct sequence and that data is appropriately transferred between microservices.

The Orchestrator invokes only the first two microservices in the chain, as the last two (RVRPTW Solver and Route Compactor Solver) are restricted by company policies.

---

## Available Endpoints
- `POST` `http://127.0.0.1:8085/api/v1/use-chain`
  Initiates a new chain of operations. Receives the initial data and forwards it to the first microservice in the workflow. Returns a unique `ID` to track the operation's status.

- `POST` `http://127.0.0.1:8085/api/v1/callback`
  Handles responses from microservices. Updates the state of the data and determines the next step in the chain. Continues the workflow by sending the data to the next microservice.

---

## Using the Chain

### Starting the Chain

To start the chain, send a `POST` request to _http://127.0.0.1:8085/api/v1/use-chain_ with the input data in JSON format. Example request body:
```json
{
    "addressesDescription": {
        "Unipr - Dipartimento di Ingegneria e Architettura - Sede didattica": {
            "id_customer":0,
            "node_type": "M",
            "Address": "Parco Area delle Scienze 59",
            "Area": "Parma",
            "District": "Parma",
            "ZipCode": 43124,
            "Region": "Emilia Romagna",
            "Country": "Ita",
            "num_aggregated_customers": 0,
            "time_window_lower_bound": 360,
            "time_window_upper_bound": 1140,
            "customer_demand_on_Monday": 0,
            "customer_demand_on_Tuesday": 0,
            "customer_demand_on_Wednesday": 0,
            "customer_demand_on_Thursday": 0,
            "customer_demand_on_Friday": 0,
            "customer_demand_on_Saturday": 0,
            "service_time_on_Monday": 0,
            "service_time_on_Tuesday": 0,
            "service_time_on_Wednesday": 0,
            "service_time_on_Thursday": 0,
            "service_time_on_Friday": 0,
            "service_time_on_Saturday": 0,
            "largest_vehicle_id": 0
        },
        "Dipartimento di Scienze e Metodi dell'Ingegneria": {
            "id_customer":1,
            "node_type": "T",
            "Address": "Via Giovanni Amendola 2",
            "Area": "Reggio Emilia",
            "District": "Reggio Emilia",
            "ZipCode": 42122 ,
            "Region": "Emilia Romagna",
            "Country": "Ita",
            "num_aggregated_customers": 1,
            "time_window_lower_bound": 480,
            "time_window_upper_bound": 600,
            "customer_demand_on_Monday": 1,
            "customer_demand_on_Tuesday": 1,
            "customer_demand_on_Wednesday": 35,
            "customer_demand_on_Thursday": 1,
            "customer_demand_on_Friday": 14,
            "customer_demand_on_Saturday": 0,
            "service_time_on_Monday": 15,
            "service_time_on_Tuesday": 15,
            "service_time_on_Wednesday": 15,
            "service_time_on_Thursday": 15,
            "service_time_on_Friday": 15,
            "service_time_on_Saturday": 0,
            "largest_vehicle_id": 0
        },
        "Dipartimento di Ingegneria Enzo Ferrari": {
            "id_customer":2,
            "node_type": "T",
            "Address": "Via Pietro Vivarelli 10",
            "Area": "Modena",
            "District": "Modena",
            "ZipCode": 41125 ,
            "Region": "Emilia Romagna",
            "Country": "Ita",
            "num_aggregated_customers": 1,
            "time_window_lower_bound": 840,
            "time_window_upper_bound": 1080,
            "customer_demand_on_Monday": 1,
            "customer_demand_on_Tuesday": 1,
            "customer_demand_on_Wednesday": 0,
            "customer_demand_on_Thursday": 1,
            "customer_demand_on_Friday": 1,
            "customer_demand_on_Saturday": 0,
            "service_time_on_Monday": 15,
            "service_time_on_Tuesday": 15,
            "service_time_on_Wednesday": 0,
            "service_time_on_Thursday": 15,
            "service_time_on_Friday": 15,
            "service_time_on_Saturday": 0,
            "largest_vehicle_id": 0
        },
        "Università di Bologna - Scuola di Ingegneria e Architettura": {
            "id_customer":3,
            "node_type": "T",
            "Address": "Viale del Risorgimento 2",
            "Area": "Bologna",
            "District": "Città Metropolitana di Bologna",
            "ZipCode": 40136,
            "Region": "Emilia Romagna",
            "Country": "Ita",
            "num_aggregated_customers": 1,
            "time_window_lower_bound": 480,
            "time_window_upper_bound": 840,
            "customer_demand_on_Monday": 0,
            "customer_demand_on_Tuesday": 2,
            "customer_demand_on_Wednesday": 1,
            "customer_demand_on_Thursday": 2,
            "customer_demand_on_Friday": 1,
            "customer_demand_on_Saturday": 0,
            "service_time_on_Monday": 0,
            "service_time_on_Tuesday": 15,
            "service_time_on_Wednesday": 15,
            "service_time_on_Thursday": 15,
            "service_time_on_Friday": 15,
            "service_time_on_Saturday": 0,
            "largest_vehicle_id": 0
        }
    },
    "vehicle_description": [
            {
                "id_vehicle": 0,
                "capacity": 690,
                "max_speed": 45,
                "availability": 1,
                "cost": 160
            }
        ],
    "max_number_of_vehicle_per_route": 1,
    "max_duration_of_route": 480
}
```

The orchestrator will return a unique `ID` to track the workflow. Example response:
```json
{
    "id": "dd4a49b8-b331-4534-af8a-c12758dfd215"
}
```

---

### Managing the Workflow
The orchestrator will handle communication with the Georeference and Distance Time Calculator microservices in the defined sequence. It ensures that data is passed correctly and operations are completed successfully.

Once the chain is complete, the results from the three microservices can be retrieved using their respective endpoints:
- `GET` `http://127.0.0.1:8080/api/v1/georef/{id}`
- `GET` `http://127.0.0.1:8081/api/v1/dm/{id}`

By default, after the chain completes, the orchestrator makes a call to an external endpoint to confirm the successful execution of the workflow.
