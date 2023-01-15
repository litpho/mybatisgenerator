MyBatis Generator Snowflake IdGenerator
-----------------------
An IdGenerator based on Twitter Snowflake

Usage
-----
Having this library on the classpath will automatically make it available through the ServiceLoader SPI.
The datacenterId and workerId properties of the Snowflake instance can be overriden by using
the environment variables SNOWFLAKE_DATACENTER_ID and SNOWFLAKE_WORKER_ID respectively.
