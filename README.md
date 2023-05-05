# Kafka SMT - String Unicode Null

This SMT (Single Message Transform) can be configured to replace a target character (default: `null`, `\u0000`) in a
message with another (default: empty string) in a specified topic (default: all) and specified fields (default: all).

## Usage

You will need to package the `StripUnicodeNullTransform` class and all its dependencies into a JAR file and add it to 
the Kafka Connect worker's classpath.

* topic - A specific topic to process. Default: all
* fields - A comma separated list of fields to process. Default: all
* target - The character to replace. Default: `null`, `\u0000`
* replacement - The character to replace with. Default: empty string
```json
{
    "name": "example-1",
    "config": 
    {
        "transforms": "replace",
        "transforms.replace.type": "io.github.cyberjar09.strip_unicode_null_transform.StripUnicodeNullTransform",
        "transforms.replace.topic": "topicA",
        "transforms.replace.fields": "fieldB,fieldC"
    }
}
```

To process multiple topics, register more than one instance of the SMT:
```json
{
    "name": "example-2",
    "config": 
    {
        "transforms": "replaceA,replaceD",
        "transforms.replaceA.type": "io.github.cyberjar09.strip_unicode_null_transform.StripUnicodeNullTransform",
        "transforms.replaceA.topic": "topicA",
        "transforms.replaceA.fields": "fieldB,fieldC",
        "transforms.replaceD.type": "io.github.cyberjar09.strip_unicode_null_transform.StripUnicodeNullTransform",
        "transforms.replaceD.topic": "topicD",
        "transforms.replaceD.fields": "fieldE,fieldF"
    }
}
```

### Logging

The logging level can be set in the usual manner by access the Kafka Connect API. For example:
```shell
curl -s -X PUT -H "Content-Type:application/json" \
    http://KAFKA-CONNECT:PORT/admin/loggers/io.github.cyberjar09.strip_unicode_null_transform.StripUnicodeNullTransform \
    -d '{"level": "TRACE"}'
```

## Building

This can be quickly built and tested using Maven as normal:
```shell
mvn clean install
````

## Performance

Initial load testing with 400k records on a CPU-bound system showed an approximate increase of 15% in processing time
when the SMT is left to process **all** fields in **all** topics. Further testing on a more production capable system 
with 4 million records showed a 2-3% hit.

If performance is a critical concern,  it is advised that the SMT is configured to target only the affected fields of 
problem topics.
