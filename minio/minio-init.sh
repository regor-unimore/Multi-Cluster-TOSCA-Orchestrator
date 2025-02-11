#!/bin/sh

# Start MinIO server in the background
minio server /data --console-address :9001 &

# Wait for MinIO server to be ready
sleep 5

# Configure MinIO alias
mc alias set myminio http://localhost:9000 $MINIO_ROOT_USER $MINIO_ROOT_PASSWORD

# Function to create the bucket only if it does not exist
create_bucket_if_not_exists() {
  bucket_name=$1
  if mc ls myminio/$bucket_name > /dev/null 2>&1; then
    echo "Bucket '$bucket_name' already exists. Skipping creation."
  else
    mc mb myminio/$bucket_name
    echo "Bucket '$bucket_name' created successfully."
  fi
}

# Create buckets if they do not exist
create_bucket_if_not_exists georeference
create_bucket_if_not_exists distance-time-calculator

# Keep the container running
tail -f /dev/null