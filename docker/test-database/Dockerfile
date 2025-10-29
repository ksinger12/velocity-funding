FROM mysql:8.0

ENV MYSQL_ROOT_PASSWORD=rootpass
ENV MYSQL_DATABASE=funding
ENV MYSQL_USER=velocity_user
ENV MYSQL_PASSWORD=velocity_pass
ENV MYSQL_TCP_PORT=3312

COPY funding-velocity.sql /docker-entrypoint-initdb.d/

EXPOSE 3312

CMD ["mysqld", "--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci"]
