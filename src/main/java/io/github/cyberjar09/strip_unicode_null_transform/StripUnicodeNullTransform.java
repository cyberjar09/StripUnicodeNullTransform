package io.github.cyberjar09.strip_unicode_null_transform;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.transforms.Transformation;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.transforms.util.SimpleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.kafka.connect.transforms.util.Requirements.requireStruct;

public class StripUnicodeNullTransform<R extends ConnectRecord<R>> implements Transformation<R> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StripUnicodeNullTransform.class);
    /** The purpose of this SMT **/

    private static final String PURPOSE = "Strip null characters (\u0000) from Strings";

    /** Comma separated list of field names **/

    protected static final String TOPIC_CONFIG = "topic";
    protected static final String FIELDS_CONFIG = "fields";
    /** The character to search for **/
    protected static final String TARGET_CONFIG = "target";
    /** The character to replace with **/
    protected static final String REPLACEMENT_CONFIG = "replacement";
    protected static final String DEFAULT_ALL = "*";
    /* There's an annoying bug where \u0000 is not returned from ConfigDef, this is part of the workaround */
    protected static final String DEFAULT_TARGET_CHAR="NULL_CHAR";
    protected static final String ACTUAL_DEFAULT_TARGET_CHAR="\u0000";
    protected static final String DEFAULT_REPLACEMENT_CHAR="";

    private static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(TOPIC_CONFIG, ConfigDef.Type.STRING, DEFAULT_ALL, ConfigDef.Importance.MEDIUM, "Comma-separated list of fields to strip null characters from (default: all fields)")
            .define(FIELDS_CONFIG, ConfigDef.Type.STRING, DEFAULT_ALL, ConfigDef.Importance.MEDIUM, "Comma-separated list of fields to strip null characters from (default: all fields)")
            .define(TARGET_CONFIG, ConfigDef.Type.STRING, DEFAULT_TARGET_CHAR, ConfigDef.Importance.MEDIUM, "The character to look for. Remember to escape any \\ using \\\\. (default: '\\u0000')")
            .define(REPLACEMENT_CONFIG, ConfigDef.Type.STRING, DEFAULT_REPLACEMENT_CHAR, ConfigDef.Importance.MEDIUM, "The character to replace with, if any. Remember to escape any \\ using \\\\. (default: '')");

    private String target_char;
    private String escaped_target_char;
    private String replacement_char;
    private String escaped_replacement_char="";
    private String topic;
    private Set<String> fields;

    public void configure(Map<String, ?> configs) {
        final SimpleConfig config = new SimpleConfig(CONFIG_DEF, configs);

        topic = config.getString(TOPIC_CONFIG);
        if (isStringEmpty(topic) || topic.equals(DEFAULT_ALL)) {
            topic = null;
        }

        String fieldsAsString = config.getString(FIELDS_CONFIG);
        if (isStringEmpty(fieldsAsString) || fieldsAsString.equals(DEFAULT_ALL)) {
            // Include all fields
            fields = null;
        } else {
            fields = Arrays.stream(fieldsAsString.split(",")).collect(Collectors.toSet());
        }

        target_char = config.getString(TARGET_CONFIG);
        // Workaround for the bug where \u0000 does not get returned as a default value from ConfigDef
        if (isStringEmpty(target_char) || DEFAULT_TARGET_CHAR.equals(target_char)) {
            target_char=ACTUAL_DEFAULT_TARGET_CHAR;
        } else {
            if (target_char.length() > 1) {
                throw new IllegalArgumentException(TARGET_CONFIG + " cannot be more than one character");
            }
        }
        escaped_target_char=getEscapedChar(target_char.charAt(0));

        replacement_char = config.getString(REPLACEMENT_CONFIG);
        if (!isStringEmpty(replacement_char)) {
            if (replacement_char.length() > 1) {
                throw new IllegalArgumentException(REPLACEMENT_CONFIG + " cannot be more than one character");
            } else {
                escaped_replacement_char = getEscapedChar(replacement_char.charAt(0));
            }
        }
    }

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

    @Override
    public void close() {
        // No resources to release
    }

    @Override
    public R apply(R record) {
        logRecord(record, "START");

        // Minor duplication to avoid repeated null test
        if (topic == null) {
            processRecord(record);
        } else {
            if (topic.equals(record.topic())) {
                processRecord(record);
            }
        }

        logRecord(record, "END");
        return record;
    }

    public void processRecord(R record) {
        Struct struct = requireStruct(record.value(), PURPOSE);
        Schema schema = record.valueSchema();
        // We can only process strings, so let's grab those
        List<Field> stringFields = schema.fields().stream().filter(field ->
                field.schema().type().equals(Schema.Type.STRING)).collect(Collectors.toList());
        List<String> replacedFields = new ArrayList<>();

        // Minor duplication to avoid repeated null test
        if (fields == null) {
            for (Field field : stringFields) {
                processField(struct, field, replacedFields);
            }
        } else {
            for (Field field : stringFields) {
                if (fields.contains(field.name())) {
                    processField(struct, field, replacedFields);
                }
            }
        }

        if (replacedFields.size() > 0 && LOGGER.isWarnEnabled()) {
            LOGGER.warn("Record with key '{}' of topic '{}', partition '{}' needed '{}' replaced with '{}' in fields: '{}'.",
                    record.key(), record.topic(), record.kafkaPartition(), escaped_target_char, escaped_replacement_char,
                    String.join("', '", replacedFields));
        }
    }

    /**
     * Checks the field passed in and updates the Struct as required. If a field needs processed, it will be added to
     * {@code replacedFields} for later logging.
     * @param struct The complete record value
     * @param field The specific field to check
     * @param replacedFields A running list of any fields that needed updating
     */
    private void processField(final Struct struct, final Field field, final List<String> replacedFields) {
        // No need for type check, we know we are only getting String fields
        String raw = ((String) struct.get(field));
        if (raw != null && (raw.contains(escaped_target_char) || raw.contains(target_char))) {
            replacedFields.add(field.name());
            // https://stackoverflow.com/a/28990116/1310021
            String newValue = raw.replace(escaped_target_char, replacement_char).replace(target_char, replacement_char);
            struct.put(field, newValue);
        }
    }

    /**
     * Simple method to check if a String contains a value, avoids further dependencies
     * @param string The String to test
     * @return  Whether it is null/empty or not
     */
    private boolean isStringEmpty(final String string) {
        return (string == null || string.isEmpty());
    }

    /**
     * Returns the escaped character. For example the {@code null} character would become {@code "\u0000"}
     * @param theChar The character to process
     * @return The hexadecimal unicode equivalent
     */
    private String getEscapedChar(final char theChar) {
        return "\\u" + String.format("%04x", (int)theChar);
    }

    private void logRecord(R record, String tag) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("{} - Record with key '{}' of topic '{}', partition '{}'",
                    tag, record.key(), record.topic(), record.kafkaPartition());
        }
    }
}
