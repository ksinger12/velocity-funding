create table inbound_log
(
    id               int auto_increment,
    trace_id         varchar(255)                           null,
    path             varchar(255)                           null,
    method           varchar(255)                           null,
    payload          json                                   null,
    created_datetime datetime default current_timestamp     not null,
    primary key (id)
);

create index idx_inbound_log_trace_id on inbound_log (trace_id);
