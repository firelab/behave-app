DROP DATABASE IF EXISTS datomic;
DROP ROLE IF EXISTS datomic;

CREATE ROLE datomic LOGIN PASSWORD 'datomic';

CREATE DATABASE datomic
 WITH OWNER = datomic
      TEMPLATE template0
      ENCODING = 'UTF8'
      TABLESPACE = pg_default
      LC_COLLATE = 'en_US.UTF-8'
      LC_CTYPE = 'en_US.UTF-8'
      CONNECTION LIMIT = -1;
