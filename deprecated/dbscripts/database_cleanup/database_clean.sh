#! /bin/sh

DB_HOST="localhost"
DB_USER="root"
DB_PASSWORD="321"
DB_NAME="telco"
DB_TABLE="userinfo"


mysql -h $DB_HOST -u $DB_USER -p$DB_PASSWORD -e "source cleanup.sql"

