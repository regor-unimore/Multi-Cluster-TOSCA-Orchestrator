# MinIO

**MinIO** is an S3-compatible storage server used in our project to store results from georeferencing, distance-time calculations, and TSP operations.

---

## Buckets
The following buckets are automatically created during initialization:
- `georeference`: Stores results from georeferencing operations.
- `distance-time-calculator`: Stores matrices of distances and travel times.

---

## MinIO Console
The MinIO console can be accessed at:
```bash
http://localhost:9001
```

Use the following credentials to log in:
- **Username**: `test`
- **Password**: `testtest`

The console allows you to manually manage buckets and stored data.
