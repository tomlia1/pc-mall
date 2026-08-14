package com.pcmall.service.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductCatalogMigrationContractTest {
    @Test
    void migrationSeparatesTheSer8MiniPcFromLaptopSearches() throws IOException {
        Path migration = Path.of("src/main/resources/db/migration/V14__correct_mini_pc_product_type.sql");

        assertTrue(Files.exists(migration));
        String sql = Files.readString(migration);
        assertTrue(sql.contains("MINI_PC"));
        assertTrue(sql.contains("零刻 SER8 迷你电脑主机"));
    }
}
