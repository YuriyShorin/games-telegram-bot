package ru.gamestelegrambot

import jakarta.persistence.EntityManagerFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer

@Tag("integration")
@SpringBootTest
@Testcontainers
class StackIntegrationTest {
    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @Test
    fun `Liquibase creates the application schema and records the migration once`() {
        assertEquals(
            1,
            jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.schemata WHERE schema_name = 'games_bot'",
                Int::class.java,
            ),
        )
        assertEquals(
            1,
            jdbc.queryForObject(
                "SELECT count(*) FROM databasechangelog WHERE id = '001-create-application-schema'",
                Int::class.java,
            ),
        )
    }

    @Test
    fun `Hibernate can open a transaction against migrated PostgreSQL`() {
        entityManagerFactory.createEntityManager().use { entityManager ->
            entityManager.transaction.begin()
            try {
                assertEquals(1, entityManager.createNativeQuery("SELECT 1", Int::class.java).singleResult)
                entityManager.transaction.commit()
            } finally {
                if (entityManager.transaction.isActive) {
                    entityManager.transaction.rollback()
                }
            }
        }
        assertTrue(entityManagerFactory.isOpen)
    }

    companion object {
        @Container
        @JvmField
        val postgres = PostgreSQLContainer("postgres:18.6")

        @DynamicPropertySource
        @JvmStatic
        fun databaseProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
