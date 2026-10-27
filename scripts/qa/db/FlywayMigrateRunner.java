import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

/**
 * 干净库迁移执行器（bug-⑦/⑧ 的回归闸门用）。
 *
 * 为什么需要它：本项目没有 flyway-maven-plugin（Flyway 只在应用启动时跑），
 * 而 preflight 要在「不启动整个应用」的前提下从零迁移一个干净库来验证迁移链完整。
 * 本类只做一件事：用与 application-pg.yml 等价的 Flyway 配置对目标库执行 migrate，
 * 然后把成功/失败与已执行条数打到 stdout 供 python 解析。
 *
 * 用法：java -cp <项目依赖classpath>:. FlywayMigrateRunner <dbName> <migrationsDir>
 */
public final class FlywayMigrateRunner {

    public static void main(String[] args) {
        String db = args[0];
        String dir = args[1];
        Flyway flyway = Flyway.configure()
                .dataSource("jdbc:postgresql://localhost:5432/" + db, "postgres", "123456")
                .locations("filesystem:" + dir)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .placeholderReplacement(false)
                .cleanDisabled(true)
                .outOfOrder(true)
                .validateOnMigrate(false)
                .load();
        MigrateResult r;
        try {
            r = flyway.migrate();
        } catch (Exception e) {
            System.out.println("FLYWAY_RESULT ok=false error=" + String.valueOf(e.getMessage()).replace('\n', ' '));
            return;
        }
        System.out.println("FLYWAY_RESULT ok=" + r.success
                + " executed=" + r.migrationsExecuted
                + " target=" + r.targetSchemaVersion);
    }

    private FlywayMigrateRunner() {
    }
}
