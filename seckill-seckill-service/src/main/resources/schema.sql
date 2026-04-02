create table if not exists user_account (
    id bigint not null auto_increment primary key,
    user_id bigint not null,
    username varchar(64) not null,
    status varchar(32) not null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_user_id (user_id)
);

create table if not exists product (
    id bigint not null auto_increment primary key,
    product_id bigint not null,
    product_name varchar(128) not null,
    status varchar(32) not null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_product_id (product_id)
);
