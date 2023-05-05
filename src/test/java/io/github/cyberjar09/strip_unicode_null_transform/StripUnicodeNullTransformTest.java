package io.github.cyberjar09.strip_unicode_null_transform;


import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

public class StripUnicodeNullTransformTest {
    private static final byte[] BYTES = "foo\u0000".getBytes();
    @org.junit.jupiter.api.Test
    public void testTransformWithAllTopicsAllFields() {
        try (StripUnicodeNullTransform<SinkRecord> transform = new StripUnicodeNullTransform<>()) {
            transform.configure(new HashMap<String, String>());

            Schema schema = SchemaBuilder.struct()
                    .field("field0", Schema.STRING_SCHEMA)
                    .field("field1", Schema.STRING_SCHEMA)
                    .field("field2", Schema.STRING_SCHEMA)
                    .field("field3", Schema.STRING_SCHEMA)
                    .field("field4", Schema.BOOLEAN_SCHEMA)
                    .field("field5", Schema.BYTES_SCHEMA)
                    .field("field6", Schema.INT64_SCHEMA)
                    .build();

            Struct inputValue = new Struct(schema)
                    .put("field0", "foo\u0000bar")
                    .put("field1", "baz")
                    .put("field2", "qux\\u0000quux")
                    .put("field3", "ab\\u0000xy")
                    .put("field4", Boolean.TRUE)
                    .put("field5", BYTES)
                    .put("field6", 1234L);

            Struct expectedValue = new Struct(schema)
                    .put("field0", "foobar")
                    .put("field1", "baz")
                    .put("field2", "quxquux")
                    .put("field3", "abxy")
                    .put("field4", Boolean.TRUE)
                    .put("field5", BYTES)
                    .put("field6", 1234L);

            SinkRecord output = transform.apply(new SinkRecord("", 0, null, null, schema, inputValue, 0));

            assertEquals(expectedValue, output.value());
        }
    }

    @org.junit.jupiter.api.Test
    public void testTransformWithSelectedTopicSelectedFields() {
        Map<String, Object> config = new HashMap<>();
        config.put("topic","foo");
        config.put("fields", "field0,field2");

        try (StripUnicodeNullTransform<SinkRecord> transform = new StripUnicodeNullTransform<>()) {
            transform.configure(config);

            Schema schema = SchemaBuilder.struct()
                    .field("field0", Schema.STRING_SCHEMA)
                    .field("field1", Schema.OPTIONAL_STRING_SCHEMA)
                    .field("field2", Schema.STRING_SCHEMA)
                    .field("field3", Schema.STRING_SCHEMA)
                    .field("field4", Schema.BOOLEAN_SCHEMA)
                    .field("field5", Schema.BYTES_SCHEMA)
                    .field("field6", Schema.INT64_SCHEMA)
                    .build();

            Struct inputValue = new Struct(schema)
                    .put("field0", "foo\\u0000bar")
                    .put("field1", null)
                    .put("field2", "qux\u0000quux")
                    .put("field4", Boolean.TRUE)
                    .put("field5", BYTES)
                    .put("field6", 1234L);

            Struct expectedValue = new Struct(schema)
                    .put("field0", "foobar")
                    .put("field1", null)
                    .put("field2", "quxquux")
                    .put("field4", Boolean.TRUE)
                    .put("field5", BYTES)
                    .put("field6", 1234L);

            SinkRecord output = transform.apply(new SinkRecord("foo", 0, null, null, schema, inputValue, 0));

            assertEquals(expectedValue, output.value());
        }
    }

    @org.junit.jupiter.api.Test
    public void testTransformWithIgnoredTopicAllFields() {
        Map<String, Object> config = new HashMap<>();
        config.put("topic","foo");

        try (StripUnicodeNullTransform<SinkRecord> transform = new StripUnicodeNullTransform<>()) {
            transform.configure(config);

            Schema schema = SchemaBuilder.struct()
                    .field("field0", Schema.STRING_SCHEMA)
                    .field("field1", Schema.OPTIONAL_STRING_SCHEMA)
                    .field("field2", Schema.STRING_SCHEMA)
                    .field("field3", Schema.STRING_SCHEMA)
                    .field("field4", Schema.BOOLEAN_SCHEMA)
                    .field("field5", Schema.BYTES_SCHEMA)
                    .field("field6", Schema.INT64_SCHEMA)
                    .build();

            Struct inputValue = new Struct(schema)
                    .put("field0", "foo\\u0000bar")
                    .put("field1", null)
                    .put("field2", "qux\u0000quux")
                    .put("field4", Boolean.TRUE)
                    .put("field5", BYTES)
                    .put("field6", 1234L);

            Struct expectedValue = new Struct(schema)
                    .put("field0", "foo\\u0000bar")
                    .put("field1", null)
                    .put("field2", "qux\u0000quux")
                    .put("field4", Boolean.TRUE)
                    .put("field5", BYTES)
                    .put("field6", 1234L);

            SinkRecord output = transform.apply(new SinkRecord("bar", 0, null, null, schema, inputValue, 0));

            assertEquals(expectedValue, output.value());
        }
    }

    @org.junit.jupiter.api.Test
    public void testTransformWithSelectedTopicIgnoredFields() {
        Map<String, Object> config = new HashMap<>();
        config.put("topic","foo");
        config.put("fields","field0");

        try (StripUnicodeNullTransform<SinkRecord> transform = new StripUnicodeNullTransform<>()) {
            transform.configure(config);

            Schema schema = SchemaBuilder.struct()
                    .field("field0", Schema.STRING_SCHEMA)
                    .field("field1", Schema.OPTIONAL_STRING_SCHEMA)
                    .field("field2", Schema.STRING_SCHEMA)
                    .field("field3", Schema.STRING_SCHEMA)
                    .field("field4", Schema.BOOLEAN_SCHEMA)
                    .field("field5", Schema.BYTES_SCHEMA)
                    .field("field6", Schema.INT64_SCHEMA)
                    .build();

            Struct inputValue = new Struct(schema)
                    .put("field0", "foo\\u0000bar")
                    .put("field1", null)
                    .put("field2", "qux\u0000quux")
                    .put("field4", Boolean.TRUE)
                    .put("field5", BYTES)
                    .put("field6", 1234L);

            Struct expectedValue = new Struct(schema)
                    .put("field0", "foobar")
                    .put("field1", null)
                    .put("field2", "qux\u0000quux")
                    .put("field4", Boolean.TRUE)
                    .put("field5", BYTES)
                    .put("field6", 1234L);

            SinkRecord output = transform.apply(new SinkRecord("foo", 0, null, null, schema, inputValue, 0));

            assertEquals(expectedValue, output.value());
        }
    }

    @org.junit.jupiter.api.Test
    public void testTransformWithConfigdChars() {
        Map<String, Object> config = new HashMap<>();
        config.put("target", "A");
        config.put("replacement", "Z");

        try (StripUnicodeNullTransform<SinkRecord> transform = new StripUnicodeNullTransform<>()) {
            transform.configure(config);

            Schema schema = SchemaBuilder.struct()
                    .field("field0", Schema.BOOLEAN_SCHEMA)
                    .field("field1", Schema.BYTES_SCHEMA)
                    .field("field2", Schema.INT64_SCHEMA)
                    .field("field3", Schema.STRING_SCHEMA)
                    .field("field4", Schema.STRING_SCHEMA)
                    .field("field5", Schema.STRING_SCHEMA)
                    .field("field6", Schema.STRING_SCHEMA)
                    .build();

            Struct inputValue = new Struct(schema)
                    .put("field0", Boolean.TRUE)
                    .put("field1", BYTES)
                    .put("field2", 1234L)
                    .put("field3", "fooAbar")
                    .put("field4", "baz")
                    .put("field5", "qux\\u0041quux")
                    .put("field6", "ab\\u0041xy");

            Struct expectedValue = new Struct(schema)
                    .put("field0", Boolean.TRUE)
                    .put("field1", BYTES)
                    .put("field2", 1234L)
                    .put("field3", "fooZbar")
                    .put("field4", "baz")
                    .put("field5", "quxZquux")
                    .put("field6", "abZxy");

            SinkRecord output = transform.apply(new SinkRecord("", 0, null, null, schema, inputValue, 0));

            assertEquals(expectedValue, output.value());
        }
    }

    @org.junit.jupiter.api.Test
    public void testTransformWithEmptyReplacement() {
        Map<String, Object> config = new HashMap<>();
        config.put("replacement", "");

        try (StripUnicodeNullTransform<SinkRecord> transform = new StripUnicodeNullTransform<>()) {
            transform.configure(config);

            Schema schema = SchemaBuilder.struct()
                    .field("field0", Schema.STRING_SCHEMA)
                    .field("field1", Schema.STRING_SCHEMA)
                    .field("field2", Schema.STRING_SCHEMA)
                    .field("field3", Schema.STRING_SCHEMA)
                    .build();

            Struct inputValue = new Struct(schema)
                    .put("field0", "foo\\u0000bar")
                    .put("field1", "baz")
                    .put("field2", "qux\u0000quux")
                    .put("field3", "a\u0000b");

            Struct expectedValue = new Struct(schema)
                    .put("field0", "foobar")
                    .put("field1", "baz")
                    .put("field2", "quxquux")
                    .put("field3", "ab");

            SinkRecord output = transform.apply(new SinkRecord("", 0, null, null, schema, inputValue, 0));

            assertEquals(expectedValue, output.value());
        }
    }

    @org.junit.jupiter.api.Test
    public void testTransformBadTarget() {
        Map<String, Object> config = new HashMap<>();
        config.put("target", "ABC");

        try (StripUnicodeNullTransform<SinkRecord> transform = new StripUnicodeNullTransform<>()) {
            try {
                transform.configure(config);
                fail("Did not detect bad target");
            } catch (IllegalArgumentException iae) {
                assertTrue(iae.getMessage().startsWith(StripUnicodeNullTransform.TARGET_CONFIG));
            }
        }
    }

    @Test
    public void testTransformBadReplacement() {
        Map<String, Object> config = new HashMap<>();
        config.put("replacement", "ABC");

        try (StripUnicodeNullTransform<SinkRecord> transform = new StripUnicodeNullTransform<>()) {
            try {
                transform.configure(config);
                fail("Did not detect bad replacement");
            } catch (IllegalArgumentException iae) {
                assertTrue(iae.getMessage().startsWith(StripUnicodeNullTransform.REPLACEMENT_CONFIG));
            }
        }
    }
}
