create table customer_transaction
(
    id               int auto_increment,
    trace_id         varchar(255)                           null,
    request_id       varchar(255)                           null,
    customer_id      varchar(255)                           null,
    load_amount      double                                 null,
    created_datetime datetime default current_timestamp     not null,
    primary key (id)
);

create index idx_customer_transactions_trace_id on customer_transaction (trace_id);
create index idx_customer_transactions_request_id on customer_transaction (request_id);
create index idx_customer_transactions_customer_id on customer_transaction (customer_id);
