create table if not exists agent_order (
    order_id bigint primary key,
    user_id bigint not null,
    delivery_no varchar(64) not null,
    amount decimal(12, 2) not null
);
