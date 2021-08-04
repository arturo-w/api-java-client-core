package com.taboola.rest.api.internal.serialization;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.taboola.rest.api.internal.config.SerializationConfig;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SerializationMapperCreatorTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void createObjectMapper_defaultSerializationConfig_objectMapperWithDefaultValues() {
        ObjectMapper objectMapper = SerializationMapperCreator.createObjectMapper(new SerializationConfig());

        Assert.assertEquals("Invalid property naming strategy", PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES, objectMapper.getPropertyNamingStrategy());
        Assert.assertEquals("Invalid mixin count", 0, objectMapper.mixInCount());
    }

    @Test
    public void createObjectMapper_serializationConfigWithMixins_objectMapperIsCreatedWithMixins() {
        Map<Class<?>, Class<?>> mixins = new HashMap<>();
        mixins.put(SampleApi.class, SampleMixIn.class);
        SerializationConfig serializationConfig = new SerializationConfig().setMixins(mixins);
        ObjectMapper objectMapper = SerializationMapperCreator.createObjectMapper(serializationConfig);

        Assert.assertEquals("Invalid mixin count", 1, objectMapper.mixInCount());
        Assert.assertEquals("Invalid property naming strategy", SampleMixIn.class, objectMapper.findMixInClassFor(SampleApi.class));
    }

    @Test
    public void createObjectMapper_defaultSerializationConfigAndApiObjectHasAnySetter_anySetterIsCalledOnSerialization() throws IOException {
        expectedException.expect(Exception.class);
        expectedException.expectMessage("unknown field");
        ObjectMapper objectMapper = SerializationMapperCreator.createObjectMapper(new SerializationConfig());

        objectMapper.readValue("{ \"id\": 1, \"test\": \"test\" }", SampleApi.class);
    }

    @Test
    public void createObjectMapper_serializationConfigWithoutAnySetterAndApiObjectHasAnySetter_anySetterIsCalledOnSerialization() throws IOException {
        expectedException.expect(Exception.class);
        expectedException.expectMessage("unknown field");
        ObjectMapper objectMapper = SerializationMapperCreator.createObjectMapper(new SerializationConfig());

        objectMapper.readValue("{ \"id\": 1, \"test\": \"test\" }", SampleApi.class);
    }

    @Test
    public void createObjectMapper_serializationConfigWithIgnoreAnySetterAndApiObjectHasAnySetter_anySetterIgnored() throws IOException {
        SerializationConfig serializationConfig = new SerializationConfig().setShouldIgnoreAnySetterAnnotation();
        ObjectMapper objectMapper = SerializationMapperCreator.createObjectMapper(serializationConfig);

        SampleApi sampleApi = objectMapper.readValue("{ \"id\": 1, \"test\": \"test\" }", SampleApi.class);
        Assert.assertEquals("Id parsed incorrectly", 1, sampleApi.id);
        Assert.assertNull("Name is parsed incorrectly", sampleApi.name);
    }

    @Test
    public void createObjectMapper_defaultSerializationConfigAndApiObjectHasValidEnum_enumParsedCorrectly() throws IOException {
        SerializationConfig serializationConfig = new SerializationConfig();
        ObjectMapper objectMapper = SerializationMapperCreator.createObjectMapper(serializationConfig);

        SampleApi sampleApi = objectMapper.readValue("{ \"letter\": \"B\" }", SampleApi.class);

        Assert.assertEquals("Unknown enum is parsed incorrectly", SampleEnum.B, sampleApi.letter);
    }

    @Test
    public void createObjectMapper_defaultSerializationConfigAndApiObjectHasInvalidEnum_defaultValueUsedOnSerialization() throws IOException {
        SerializationConfig serializationConfig = new SerializationConfig();
        ObjectMapper objectMapper = SerializationMapperCreator.createObjectMapper(serializationConfig);

        SampleApi sampleApi = objectMapper.readValue("{ \"letter\": \"D\" }", SampleApi.class);

        Assert.assertEquals("Unknown enum is parsed incorrectly", SampleEnum.UNKNOWN, sampleApi.letter);
    }

    @Test
    public void createObjectMapper_defaultSerializationConfigAndApiObjectHasEmptyEnumValue_defaultValueUsedOnSerialization() throws IOException {
        SerializationConfig serializationConfig = new SerializationConfig();
        ObjectMapper objectMapper = SerializationMapperCreator.createObjectMapper(serializationConfig);

        SampleApi sampleApi = objectMapper.readValue("{ \"letter\": \"D\" }", SampleApi.class);

        Assert.assertEquals("Unknown enum is parsed incorrectly", SampleEnum.UNKNOWN, sampleApi.letter);
    }

    @Test
    public void createObjectMapper_serializationConfigWithReadUnknownEnumsDisabledAndApiObjectHasValidEnum_enumParsedCorrectly() throws IOException {
        SerializationConfig serializationConfig = new SerializationConfig().setShouldDisableReadUnknownEnumValuesAsDefaultValue();
        ObjectMapper objectMapper = SerializationMapperCreator.createObjectMapper(serializationConfig);

        SampleApi sampleApi = objectMapper.readValue("{ \"letter\": \"B\" }", SampleApi.class);

        Assert.assertEquals("Unknown enum is parsed incorrectly", SampleEnum.B, sampleApi.letter);
    }

    @Test(expected = InvalidFormatException.class)
    public void createObjectMapper_serializationConfigWithReadUnknownEnumsDisabledAndApiObjectHasInvalidEnum_InvalidFormatExceptionIsThrown() throws IOException {
        SerializationConfig serializationConfig = new SerializationConfig().setShouldDisableReadUnknownEnumValuesAsDefaultValue();
        ObjectMapper objectMapper = SerializationMapperCreator.createObjectMapper(serializationConfig);

        objectMapper.readValue("{ \"letter\": \"D\" }", SampleApi.class);
    }

    @Test(expected = InvalidFormatException.class)
    public void createObjectMapper_serializationConfigWithReadUnknownEnumsDisabledAndApiObjectHasEmptyEnumValue_InvalidFormatExceptionIsThrown() throws IOException {
        SerializationConfig serializationConfig = new SerializationConfig().setShouldDisableReadUnknownEnumValuesAsDefaultValue();
        ObjectMapper objectMapper = SerializationMapperCreator.createObjectMapper(serializationConfig);

        objectMapper.readValue("{ \"letter\": \"\" }", SampleApi.class);
    }

    private enum SampleEnum {
        A, B, @JsonEnumDefaultValue UNKNOWN
    }

    private static class SampleApi {
        @JsonProperty("id")
        int id;

        @JsonProperty("letter")
        SampleEnum letter;

        @JsonProperty("name")
        String name;

        @JsonAnySetter
        public void handlerUnknownSetter(String field, Object value) throws Exception {
            throw new Exception("unknown field");
        }


    }

    private abstract class SampleMixIn {
        @JsonIgnore
        private String name;
    }
}
