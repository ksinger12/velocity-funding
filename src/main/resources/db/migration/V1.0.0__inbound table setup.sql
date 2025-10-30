create table inbound_log
(
    id               int auto_increment,
    trace_id         varchar(255)                           null,
    customer_id      int                                    null,
    payload          json                                   null,
    created_datetime datetime default current_timestamp     not null,
    primary key (id)
);

create index idx_inbound_log_trace_id on inbound_log (trace_id);
create index idx_inbound_log_customer_id on inbound_log (customer_id);
