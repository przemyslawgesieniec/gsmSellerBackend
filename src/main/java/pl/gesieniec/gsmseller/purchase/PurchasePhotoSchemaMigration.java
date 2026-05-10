package pl.gesieniec.gsmseller.purchase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchasePhotoSchemaMigration implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!isPostgres()) {
            return;
        }

        allowCloudflareOnlyPurchasePhotos();
        normalizePurchaseCommentsSchema();
        normalizeLegacyPurchaseStatuses();
        refreshPurchaseStatusConstraint();
        log.info("Ensured purchase schema is compatible with Cloudflare photos and current statuses");
    }

    private void allowCloudflareOnlyPurchasePhotos() {
        if (!tableExists("purchase_photos")) {
            return;
        }

        jdbcTemplate.execute("ALTER TABLE purchase_photos ADD COLUMN IF NOT EXISTS image_id varchar(255)");
        if (columnExists("purchase_photos", "data")) {
            jdbcTemplate.execute("ALTER TABLE purchase_photos ALTER COLUMN data DROP NOT NULL");
        }
    }

    private void normalizePurchaseCommentsSchema() {
        if (!tableExists("purchase_comments")) {
            return;
        }

        jdbcTemplate.execute("ALTER TABLE purchase_comments ADD COLUMN IF NOT EXISTS author varchar(255)");
        if (columnExists("purchase_comments", "author_username")) {
            jdbcTemplate.update("""
                    UPDATE purchase_comments
                    SET author = author_username
                    WHERE author IS NULL
                      AND author_username IS NOT NULL
                    """);
            jdbcTemplate.update("""
                    UPDATE purchase_comments
                    SET author_username = author
                    WHERE author_username IS NULL
                      AND author IS NOT NULL
                    """);
            jdbcTemplate.execute("ALTER TABLE purchase_comments ALTER COLUMN author_username DROP NOT NULL");
        }
        jdbcTemplate.update("UPDATE purchase_comments SET author = 'unknown' WHERE author IS NULL");
        jdbcTemplate.execute("ALTER TABLE purchase_comments ALTER COLUMN author SET NOT NULL");
    }

    private void normalizeLegacyPurchaseStatuses() {
        if (!tableExists("purchases") || !columnExists("purchases", "status")) {
            return;
        }

        jdbcTemplate.execute("ALTER TABLE purchases DROP CONSTRAINT IF EXISTS purchases_status_check");
        int updatedRows = jdbcTemplate.update("UPDATE purchases SET status = 'OPEN' WHERE status = 'NEW'");
        if (updatedRows > 0) {
            log.info("Normalized {} legacy purchase rows from NEW to OPEN", updatedRows);
        }
    }

    private void refreshPurchaseStatusConstraint() {
        if (!tableExists("purchases") || !columnExists("purchases", "status")) {
            return;
        }

        jdbcTemplate.execute("ALTER TABLE purchases DROP CONSTRAINT IF EXISTS purchases_status_check");
        jdbcTemplate.execute("""
                ALTER TABLE purchases
                ADD CONSTRAINT purchases_status_check
                CHECK (status IN ('NEW', 'OPEN', 'PRICE_AGREED', 'PURCHASED', 'CLOSED'))
                NOT VALID
                """);
    }

    private boolean isPostgres() {
        try (Connection connection = dataSource.getConnection()) {
            return "PostgreSQL".equals(connection.getMetaData().getDatabaseProductName());
        } catch (SQLException e) {
            log.warn("Could not determine database type for purchase photo schema migration", e);
            return false;
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        Boolean exists = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.columns
                    WHERE table_schema = current_schema()
                      AND table_name = ?
                      AND column_name = ?
                )
                """,
                Boolean.class,
                tableName,
                columnName
        );
        return Boolean.TRUE.equals(exists);
    }

    private boolean tableExists(String tableName) {
        Boolean exists = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.tables
                    WHERE table_schema = current_schema()
                      AND table_name = ?
                )
                """,
                Boolean.class,
                tableName
        );
        return Boolean.TRUE.equals(exists);
    }
}
