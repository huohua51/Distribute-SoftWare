create database if not exists order_ds_0 default character set utf8mb4;
create database if not exists order_ds_1 default character set utf8mb4;

use order_ds_0;

create table if not exists product_stock (
    id bigint not null auto_increment primary key,
    product_id bigint not null,
    product_name varchar(128) not null,
    total_stock int not null,
    available_stock int not null,
    version int not null default 0,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_product_id (product_id)
);

create table if not exists order_task (
    id bigint not null auto_increment primary key,
    message_id varchar(64) not null,
    order_id bigint not null,
    user_id bigint not null,
    product_id bigint not null,
    task_status varchar(32) not null,
    fail_reason varchar(255) null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_message_id (message_id),
    key idx_order_id (order_id)
);

create table if not exists orders_0 (
    id bigint not null auto_increment primary key,
    order_id bigint not null,
    user_id bigint not null,
    product_id bigint not null,
    product_name varchar(128) not null,
    quantity int not null,
    status varchar(32) not null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_order_id (order_id),
    unique key uk_user_product (user_id, product_id),
    key idx_user_id (user_id)
);

create table if not exists orders_1 like orders_0;

use order_ds_1;

create table if not exists orders_0 (
    id bigint not null auto_increment primary key,
    order_id bigint not null,
    user_id bigint not null,
    product_id bigint not null,
    product_name varchar(128) not null,
    quantity int not null,
    status varchar(32) not null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_order_id (order_id),
    unique key uk_user_product (user_id, product_id),
    key idx_user_id (user_id)
);

create table if not exists orders_1 like orders_0;
