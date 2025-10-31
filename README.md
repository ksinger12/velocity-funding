# velocity-funding

To start the database:

```shell
cd docker/test-database
./build-image.sh
```

```shell
cd docker
docker compose up
```

To run Venn's interview test:

1. Ensure the database is running in docker
2. Start the project via the `VelocityFundingApplication.java` file
    - If the project was already started, be sure to clear the cache
3. Run the `LoadFundsHttpRunner.java` file

