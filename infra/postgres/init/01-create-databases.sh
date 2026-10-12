#!/bin/bash
# Creates one database per service (runs once, on first container start).
set -e

for db in userdb productdb cartdb orderdb paymentdb inventorydb notificationdb; do
  echo "Creating database: $db"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" <<-EOSQL
    SELECT 'CREATE DATABASE $db'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db')\gexec
EOSQL
done
