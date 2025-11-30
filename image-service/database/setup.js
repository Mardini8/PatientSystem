const sqlite3 = require('sqlite3').verbose();
const path = require('path');
const fs = require('fs');

const dbPath = process.env.DATABASE_PATH || './database/images.db';
const dbDir = path.dirname(dbPath);

// Ensure database directory exists
if (!fs.existsSync(dbDir)) {
    fs.mkdirSync(dbDir, { recursive: true });
}

const db = new sqlite3.Database(dbPath, (err) => {
    if (err) {
        console.error('Error opening database:', err);
    } else {
        console.log('✓ Connected to SQLite database');
    }
});

// Create tables
db.serialize(() => {
    // Images table
    db.run(`
        CREATE TABLE IF NOT EXISTS images (
            id TEXT PRIMARY KEY,
            filename TEXT NOT NULL,
            original_filename TEXT,
            path TEXT NOT NULL,
            patient_personnummer TEXT NOT NULL,
            uploaded_by_user_id INTEGER NOT NULL,
            uploaded_by_username TEXT,
            upload_date TEXT NOT NULL,
            file_size INTEGER,
            mime_type TEXT,
            description TEXT,
            tags TEXT,
            is_edited BOOLEAN DEFAULT 0,
            parent_image_id TEXT,
            created_at TEXT DEFAULT CURRENT_TIMESTAMP,
            updated_at TEXT DEFAULT CURRENT_TIMESTAMP
        )
    `, (err) => {
        if (err) {
            console.error('Error creating images table:', err);
        } else {
            console.log('✓ Images table ready');
        }
    });

    // Image edits table (for tracking edit history)
    db.run(`
        CREATE TABLE IF NOT EXISTS image_edits (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            image_id TEXT NOT NULL,
            edit_type TEXT NOT NULL,
            edit_data TEXT,
            edited_by_user_id INTEGER,
            edited_at TEXT DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (image_id) REFERENCES images(id)
        )
    `, (err) => {
        if (err) {
            console.error('Error creating image_edits table:', err);
        } else {
            console.log('✓ Image edits table ready');
        }
    });
});

module.exports = db;
