create user 'svc_funding'@'%' identified by 'svc_funding';
grant select, insert, update, delete on `funding`.* to `svc_funding`@`%`;

create user 'svc_funding_flyway'@'%' identified by 'svc_funding_flyway';
grant select, insert, update, delete, create, drop, alter, index, references on `funding`.* to `svc_funding_flyway`@`%`;
