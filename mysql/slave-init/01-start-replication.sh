#!/bin/bash
set -e

until mysql -h mysql-master -u root -prootpass -e "SELECT 1" &>/dev/null; do
  echo "Waiting for master..."
  sleep 2
done

mysql -u root -prootpass <<SQL
CHANGE REPLICATION SOURCE TO
  SOURCE_HOST='mysql-master',
  SOURCE_USER='repl',
  SOURCE_PASSWORD='repl_pass',
  SOURCE_AUTO_POSITION=1;
START REPLICA;
SQL

echo "Replication started."
