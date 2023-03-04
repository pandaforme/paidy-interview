# How do I design this service?
According to requirements:
> * The service returns an exchange rate when provided with 2 supported currencies
> * The rate should not be older than 5 minutes
> * The service should support at least 10,000 successful requests per day with 1 API token
> * The One-Frame service supports a maximum of 1000 requests per day for any given authentication token.

To avoid accessing the One-Frame service with every request, I am caching the currency rate obtained from the service, which expires after 5 minutes. Additionally, I have implemented a retry policy that uses an exponential backoff algorithm if the initial call fails.

However, let's consider the worst-case scenario: we support 9 currencies, resulting in a maximum of `9 * 8 = 72` unique combinations of request payload. In this scenario, a user may send 72 unique request payloads to our service every 5 minutes. As a result, we would need to call the One-Frame service `24 * 60 / 5 * 72 = 20736` times in one day, potentially exceeding the service's maximum of 1000 requests per day. To avoid this, we could consider using rate limiting or implementing a circuit breaker to prevent this situation.

Under normal circumstances, this design should be efficient in handling the workload.
 
# How to run all test cases?
```scala
sbt test
```

# How to run manually?
1. start One-Frame service
```scala
docker pull paidyinc/one-frame
docker run -p 8080:8080 paidyinc/one-frame
```

2. export environment variables
```shell
export HTTP_PORT=8888
export ONE_FRAME_KEY=10dc303535874aeccc86a8251e6992f5
export ONE_FRAME_HOST=http://localhost:8080
```

3. start service
```scala
sbt run
```

4. access service
```bash
curl -v 'http://localhost:8888/rates?from=USD&to=JPY'
```

# How to run in docker?

1. dockerize service
```scala
sbt docker:publishLocal
```

2. run docker-compose
```shell
docker-compose up
```

3. access service
```bash
curl -v 'http://localhost:8888/rates?from=USD&to=JPY'
```
