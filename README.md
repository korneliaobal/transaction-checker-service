# Transaction Checker Service

Validates transactions from the `transaction-validation-request` Kafka topic and publishes results to `transaction-validation-result`.

## Railway deployment

`railway.toml` selects `Dockerfile.railway`. The image builds payment contracts from a pinned public source revision before packaging the service. No GitHub credentials or secret mounts are required.

Configure these service variables in Railway:

| Variable | Value |
| --- | --- |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | The Kafka broker address reachable from Railway, including its port. |
| `SPRING_KAFKA_PROPERTIES_SCHEMA_REGISTRY_URL` | The Schema Registry URL reachable from Railway. |

Configure any additional Kafka or Schema Registry authentication settings required by your infrastructure. Broker advertised listeners must also be reachable from the service.

This service consumes Kafka messages and does not expose an HTTP API. It does not need a public domain or an HTTP health check.

## Local verification

Run `./mvnw spotless:check clean test` with Java 21 and the matching payment contracts installed locally. The existing `Dockerfile` remains available for Docker Compose builds with the local contracts build context.
