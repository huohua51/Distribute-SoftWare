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

create table if not exists orders (
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
