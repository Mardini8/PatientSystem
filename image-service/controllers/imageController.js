const path = require('path');
const fs = require('fs').promises;
const db = require('../database/setup');

// Upload image with metadata
exports.uploadImage = async (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ error: 'No file uploaded' });
        }

        const { patientPersonnummer, userId, username, description, tags } = req.body;

        if (!patientPersonnummer) {
            await fs.unlink(req.file.path);
            return res.status(400).json({ error: 'Patient personnummer is required' });
        }

        if (!userId) {
            await fs.unlink(req.file.path);
            return res.status(400).json({ error: 'User ID is required' });
        }

        // Generate unique ID for the image
        const imageId = path.parse(req.file.filename).name;

        // Save to database using MySQL
        const sql = `
            INSERT INTO images (
                id, filename, original_filename, path,
                patient_personnummer, uploaded_by_user_id, uploaded_by_username,
                upload_date, file_size, mime_type, description, tags
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        `;

        await db.query(sql, [
            imageId,
            req.file.filename,
            req.file.originalname,
            req.file.path,
            patientPersonnummer,
            userId,
            username || 'Unknown',
            new Date(),
            req.file.size,
            req.file.mimetype,
            description || null,
            tags || null
        ]);

        res.status(201).json({
            message: 'Image uploaded successfully',
            image: {
                id: imageId,
                filename: req.file.filename,
                patientPersonnummer: patientPersonnummer,
                uploadedBy: username,
                uploadDate: new Date().toISOString(),
                url: `/api/images/${req.file.filename}`
            }
        });
    } catch (error) {
        console.error('Upload error:', error);
        res.status(500).json({ error: 'Failed to upload image' });
    }
};

// Get image by filename
exports.getImage = async (req, res) => {
    try {
        const filename = req.params.filename;
        const filepath = path.join(process.env.UPLOAD_DIR || './uploads', filename);

        await fs.access(filepath);
        res.sendFile(path.resolve(filepath));
    } catch (error) {
        res.status(404).json({ error: 'Image not found' });
    }
};

// Get images for a specific patient
exports.getPatientImages = async (req, res) => {
    try {
        const { patientPersonnummer } = req.params;

        const sql = `
            SELECT * FROM images
            WHERE patient_personnummer = ?
            ORDER BY upload_date DESC
        `;

        const [rows] = await db.query(sql, [patientPersonnummer]);

        const images = rows.map(row => ({
            id: row.id,
            filename: row.filename,
            originalFilename: row.original_filename,
            patientPersonnummer: row.patient_personnummer,
            uploadedBy: row.uploaded_by_username,
            uploadDate: row.upload_date,
            description: row.description,
            tags: row.tags,
            url: `/api/images/${row.filename}`
        }));

        res.json({ images });
    } catch (error) {
        console.error('Error fetching patient images:', error);
        res.status(500).json({ error: 'Failed to fetch images' });
    }
};

module.exports = exports;