package com.pcmall.migration;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealisticHardwareCatalogMigrationTest {
    private static final String MIGRATION = "db/migration/V10__expand_realistic_hardware_catalog.sql";

    @Test
    void seedsSevenAdditionalRealisticChoicesForEachBuildSlot() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(MIGRATION);
        assertNotNull(stream, "V10 catalog migration must exist");
        String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

        for (String productType : new String[]{"CPU", "MOTHERBOARD", "GPU", "RAM", "SSD", "PSU", "CASE"}) {
            assertEquals(7, count(sql, "'" + productType + "'"), productType + " should have seven new product rows");
        }
        assertEquals(49, count(sql, "'DIY 装机'"), "the catalog should add forty-nine hardware products");
        assertTrue(sql.contains("product_hardware_specs"), "each new product needs compatibility data");
    }

    @Test
    void correctsCatalogFactsWithANewMigrationAfterV10IsApplied() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("db/migration/V11__correct_realistic_hardware_specs.sql");
        assertNotNull(stream, "a correction after an applied V10 must be a new V11 migration");
        String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(sql.contains("SAPPHIRE PULSE Radeon RX 7800 XT 16GB 显卡"));
        assertTrue(sql.contains("gpu_length_mm = 280"));
        assertTrue(sql.contains("DeepCool CH370 机箱"));
        assertTrue(sql.contains("case_gpu_max_length_mm = 378"));
    }

    private int count(String value, String fragment) {
        Matcher matcher = Pattern.compile(Pattern.quote(fragment)).matcher(value);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }
}
