package com.capstone.fraudcase.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;
import java.util.TreeMap;

@Converter
public class StringMapConverter implements AttributeConverter<Map<String, String>, String> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        try {
            return MAPPER.writeValueAsString(attribute == null ? Map.of() : new TreeMap<>(attribute));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        try {
            return dbData == null || dbData.isBlank() ? Map.of() : MAPPER.readValue(dbData, new TypeReference<TreeMap<String, String>>() { });
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
