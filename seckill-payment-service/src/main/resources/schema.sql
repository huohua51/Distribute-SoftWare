create table if not exists payment_record (
    id bigint not null auto_increment primary key,
    payment_id bigint not null,
    order_id bigint not null,
    user_id bigint not null,
    amount_fen bigint not null,
    payment_status varchar(32) not null,
    fail_reason varchar(255) null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_payment_id (payment_id),
    unique key uk_order_id (order_id)
);
