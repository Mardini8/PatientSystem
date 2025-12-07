const mysql = require('mysql2/promise');

// Database configuration
const dbConfig = {
    host: process.env.DB_HOST || 'localhost',
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'patientsystemdb',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
};

// Create connection pool
const pool = mysql.createPool(dbConfig);

// Test connection
async function testConnection() {
    try {
        const connection = await pool.getConnection();
        console.log('✓ Connected to MySQL database:', dbConfig.database);
        connection.release();
    } catch (error) {
        console.error('✗ Error connecting to MySQL database:', error.message);
        throw error;
    }
}

// Initialize tables
async function initializeTables() {
    try {
        const connection = await pool.getConnection();

        // Create images table - simplified version
        await connection.query(`
            CREATE TABLE IF NOT EXISTS images (
                                                  id VARCHAR(255) PRIMARY KEY,
                filename VARCHAR(255) NOT NULL,
                original_filename VARCHAR(255),
                path TEXT NOT NULL,
                patient_personnummer VARCHAR(100) NOT NULL,
                uploaded_by_user_id INT NOT NULL,
                uploaded_by_username VARCHAR(100),
                upload_date DATETIME NOT NULL,
                file_size BIGINT,
                mime_type VARCHAR(100),
                description TEXT,
                tags TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_patient (patient_personnummer),
                INDEX idx_upload_date (upload_date),
                INDEX idx_uploaded_by (uploaded_by_user_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        `);

        console.log('✓ Images table ready');
        connection.release();
    } catch (error) {
        console.error('✗ Error creating tables:', error.message);
        throw error;
    }
}

// Helper function to convert callback-style to promise
function query(sql, params) {
    return pool.query(sql, params);
}

// Get a connection from the pool
function getConnection() {
    return pool.getConnection();
}

// Export pool and helper functions
module.exports = {
    pool,
    query,
    getConnection,
    testConnection,
    initializeTables
};