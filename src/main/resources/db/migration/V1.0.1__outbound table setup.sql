create table outbound_log
(
    id               int auto_increment,
    trace_id         varchar(255)                           null,
    was_successful   tinyint(1)                             null,
    created_datetime datetime default current_timestamp     not null,
    primary key (id)
);

create index idx_outbound_log_trace_id on outbound_log (trace_id);
