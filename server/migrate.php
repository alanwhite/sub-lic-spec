<?php
require_once __DIR__ . '/vendor/autoload.php';

use App\Database\Database;

$db = Database::getInstance();

// Get all migration files
$migrationFiles = glob(__DIR__ . '/database/migrations/*.sql');
sort($migrationFiles);

// Create migrations tracking table
$db->exec("
    CREATE TABLE IF NOT EXISTS migrations (
        id INT AUTO_INCREMENT PRIMARY KEY,
        migration VARCHAR(255) NOT NULL UNIQUE,
        executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_migration (migration)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
");

// Get executed migrations
$stmt = $db->query("SELECT migration FROM migrations");
$executed = $stmt->fetchAll(PDO::FETCH_COLUMN);

// Run pending migrations
foreach ($migrationFiles as $file) {
    $migration = basename($file);

    if (in_array($migration, $executed)) {
        echo "Skipping: $migration (already executed)\n";
        continue;
    }

    echo "Running: $migration\n";

    $sql = file_get_contents($file);

    try {
        $db->beginTransaction();
        $db->exec($sql);
        $db->exec("INSERT INTO migrations (migration) VALUES (?)", [$migration]);
        $db->commit();
        echo "Success: $migration\n";
    } catch (Exception $e) {
        $db->rollback();
        echo "Error in $migration: " . $e->getMessage() . "\n";
        exit(1);
    }
}

echo "All migrations completed successfully!\n";