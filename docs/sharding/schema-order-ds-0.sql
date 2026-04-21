use order_ds_0;

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
