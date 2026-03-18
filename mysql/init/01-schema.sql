CREATE DATABASE IF NOT EXISTS shop;
USE shop;

CREATE TABLE products (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(200) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  stock INT NOT NULL DEFAULT 0,
  description TEXT
);

INSERT INTO products (name, price, stock, description) VALUES
('High Concurrency Handbook', 99.00, 120, 'A demo product for cache-heavy read traffic.'),
('Distributed Cache Notes', 79.00, 80, 'Used to demonstrate cache hit, miss and rebuild.'),
('Nginx Practice Kit', 59.00, 66, 'A demo product for reverse proxy and static delivery.'),
('MySQL Replication Guide', 89.00, 50, 'Learn MySQL master-slave replication.'),
('ElasticSearch Essentials', 109.00, 35, 'Full-text search with ElasticSearch.');

CREATE USER IF NOT EXISTS 'repl'@'%' IDENTIFIED BY 'repl_pass';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
FLUSH PRIVILEGES;
