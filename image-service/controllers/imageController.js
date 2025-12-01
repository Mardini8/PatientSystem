const sharp = require('sharp');
const path = require('path');
const fs = require('fs').promises;
const { v4: uuidv4 } = require('uuid');
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
            isEdited: row.is_edited === 1,
            url: `/api/images/${row.filename}`,
            thumbnailUrl: `/api/images/${row.filename}?thumbnail=true`
        }));

        res.json({ images });
    } catch (error) {
        console.error('Error fetching patient images:', error);
        res.status(500).json({ error: 'Failed to fetch images' });
    }
};

// Get image metadata
exports.getImageMetadata = async (req, res) => {
    try {
        const { imageId } = req.params;

        const sql = `SELECT * FROM images WHERE id = ?`;
        const [rows] = await db.query(sql, [imageId]);

        if (rows.length === 0) {
            return res.status(404).json({ error: 'Image not found' });
        }

        const row = rows[0];
        res.json({
            id: row.id,
            filename: row.filename,
            originalFilename: row.original_filename,
            patientPersonnummer: row.patient_personnummer,
            uploadedBy: row.uploaded_by_username,
            uploadDate: row.upload_date,
            fileSize: row.file_size,
            mimeType: row.mime_type,
            description: row.description,
            tags: row.tags,
            isEdited: row.is_edited === 1,
            parentImageId: row.parent_image_id
        });
    } catch (error) {
        console.error('Error fetching metadata:', error);
        res.status(500).json({ error: 'Failed to fetch metadata' });
    }
};

// Add text to image
exports.addText = async (req, res) => {
    try {
        const filename = req.params.filename;
        const { text, x, y, fontSize, color, userId } = req.body;

        if (!text) {
            return res.status(400).json({ error: 'Text is required' });
        }

        const filepath = path.join(process.env.UPLOAD_DIR || './uploads', filename);
        const outputFilename = `edited-${uuidv4()}${path.extname(filename)}`;
        const outputPath = path.join(process.env.UPLOAD_DIR || './uploads', outputFilename);

        // Create SVG text overlay
        const svgText = `
            <svg width="2000" height="2000">
                <text x="${x || 10}" y="${y || 50}" 
                      font-size="${fontSize || 24}" 
                      fill="${color || 'red'}"
                      font-weight="bold"
                      font-family="Arial, sans-serif">
                    ${escapeXml(text)}
                </text>
            </svg>
        `;

        await sharp(filepath)
            .composite([{
                input: Buffer.from(svgText),
                top: 0,
                left: 0
            }])
            .toFile(outputPath);

        // Get original image metadata
        const imageId = path.parse(filename).name;
        const [originalRows] = await db.query('SELECT * FROM images WHERE id = ?', [imageId]);

        if (originalRows.length === 0) {
            return res.json({
                message: 'Text added successfully',
                image: {
                    filename: outputFilename,
                    url: `/api/images/${outputFilename}`
                }
            });
        }

        const original = originalRows[0];
        const newImageId = path.parse(outputFilename).name;

        // Save edited image to database
        const sql = `
            INSERT INTO images (
                id, filename, original_filename, path,
                patient_personnummer, uploaded_by_user_id, uploaded_by_username,
                upload_date, file_size, mime_type, description,
                is_edited, parent_image_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?)
        `;

        await db.query(sql, [
            newImageId,
            outputFilename,
            original.original_filename,
            outputPath,
            original.patient_personnummer,
            userId || original.uploaded_by_user_id,
            original.uploaded_by_username,
            new Date(),
            0, // Will be updated later
            original.mime_type,
            `${original.description || ''} [Text added]`,
            imageId
        ]);

        // Log the edit
        await db.query(
            'INSERT INTO image_edits (image_id, edit_type, edit_data, edited_by_user_id) VALUES (?, ?, ?, ?)',
            [newImageId, 'add_text', JSON.stringify({ text, x, y, fontSize, color }), userId]
        );

        res.json({
            message: 'Text added successfully',
            image: {
                id: newImageId,
                filename: outputFilename,
                url: `/api/images/${outputFilename}`
            }
        });
    } catch (error) {
        console.error('Add text error:', error);
        res.status(500).json({ error: 'Failed to add text to image' });
    }
};

// Draw on image (shapes)
exports.drawOnImage = async (req, res) => {
    try {
        const filename = req.params.filename;
        const { shape, x, y, width, height, color, userId, strokeWidth } = req.body;

        const filepath = path.join(process.env.UPLOAD_DIR || './uploads', filename);
        const outputFilename = `drawn-${uuidv4()}${path.extname(filename)}`;
        const outputPath = path.join(process.env.UPLOAD_DIR || './uploads', outputFilename);

        let svgShape = '';
        const sw = strokeWidth || 3;
        const shapeColor = color || 'red';

        if (shape === 'rectangle') {
            svgShape = `
                <svg width="2000" height="2000">
                    <rect x="${x || 10}" y="${y || 10}" 
                          width="${width || 100}" height="${height || 100}" 
                          fill="none" stroke="${shapeColor}" stroke-width="${sw}"/>
                </svg>
            `;
        } else if (shape === 'circle') {
            const radius = width || 50;
            svgShape = `
                <svg width="2000" height="2000">
                    <circle cx="${x || 50}" cy="${y || 50}" r="${radius}" 
                            fill="none" stroke="${shapeColor}" stroke-width="${sw}"/>
                </svg>
            `;
        } else if (shape === 'arrow') {
            const x2 = parseInt(x) + (parseInt(width) || 100);
            const y2 = parseInt(y) + (parseInt(height) || 0);
            svgShape = `
                <svg width="2000" height="2000">
                    <defs>
                        <marker id="arrowhead" markerWidth="10" markerHeight="10" 
                                refX="9" refY="3" orient="auto">
                            <polygon points="0 0, 10 3, 0 6" fill="${shapeColor}" />
                        </marker>
                    </defs>
                    <line x1="${x}" y1="${y}" x2="${x2}" y2="${y2}" 
                          stroke="${shapeColor}" stroke-width="${sw}" 
                          marker-end="url(#arrowhead)" />
                </svg>
            `;
        } else if (shape === 'line') {
            const x2 = parseInt(x) + (parseInt(width) || 100);
            const y2 = parseInt(y) + (parseInt(height) || 0);
            svgShape = `
                <svg width="2000" height="2000">
                    <line x1="${x}" y1="${y}" x2="${x2}" y2="${y2}" 
                          stroke="${shapeColor}" stroke-width="${sw}" />
                </svg>
            `;
        } else {
            return res.status(400).json({
                error: 'Invalid shape. Use: rectangle, circle, arrow, or line'
            });
        }

        await sharp(filepath)
            .composite([{
                input: Buffer.from(svgShape),
                top: 0,
                left: 0
            }])
            .toFile(outputPath);

        // Get original image metadata
        const imageId = path.parse(filename).name;
        const [originalRows] = await db.query('SELECT * FROM images WHERE id = ?', [imageId]);

        if (originalRows.length === 0) {
            return res.json({
                message: 'Drawing added successfully',
                image: {
                    filename: outputFilename,
                    url: `/api/images/${outputFilename}`
                }
            });
        }

        const original = originalRows[0];
        const newImageId = path.parse(outputFilename).name;

        const sql = `
            INSERT INTO images (
                id, filename, original_filename, path,
                patient_personnummer, uploaded_by_user_id, uploaded_by_username,
                upload_date, file_size, mime_type, description,
                is_edited, parent_image_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?)
        `;

        await db.query(sql, [
            newImageId,
            outputFilename,
            original.original_filename,
            outputPath,
            original.patient_personnummer,
            userId || original.uploaded_by_user_id,
            original.uploaded_by_username,
            new Date(),
            0,
            original.mime_type,
            `${original.description || ''} [${shape} added]`,
            imageId
        ]);

        await db.query(
            'INSERT INTO image_edits (image_id, edit_type, edit_data, edited_by_user_id) VALUES (?, ?, ?, ?)',
            [newImageId, 'draw', JSON.stringify({ shape, x, y, width, height, color }), userId]
        );

        res.json({
            message: 'Drawing added successfully',
            image: {
                id: newImageId,
                filename: outputFilename,
                url: `/api/images/${outputFilename}`
            }
        });
    } catch (error) {
        console.error('Draw error:', error);
        res.status(500).json({ error: 'Failed to draw on image' });
    }
};

// List all images
exports.listImages = async (req, res) => {
    try {
        const sql = `SELECT * FROM images ORDER BY upload_date DESC`;
        const [rows] = await db.query(sql);

        const images = rows.map(row => ({
            id: row.id,
            filename: row.filename,
            patientPersonnummer: row.patient_personnummer,
            uploadedBy: row.uploaded_by_username,
            uploadDate: row.upload_date,
            description: row.description,
            url: `/api/images/${row.filename}`
        }));

        res.json({ images });
    } catch (error) {
        console.error('List images error:', error);
        res.status(500).json({ error: 'Failed to list images' });
    }
};

// Delete image
exports.deleteImage = async (req, res) => {
    try {
        const { imageId } = req.params;

        // Get image info from database
        const [rows] = await db.query('SELECT * FROM images WHERE id = ?', [imageId]);

        if (rows.length === 0) {
            return res.status(404).json({ error: 'Image not found' });
        }

        const row = rows[0];

        // Delete file
        try {
            await fs.unlink(row.path);
        } catch (fileErr) {
            console.error('Error deleting file:', fileErr);
        }

        // Delete from database
        await db.query('DELETE FROM images WHERE id = ?', [imageId]);

        // Delete edit history (CASCADE will handle this automatically)
        // But we can be explicit:
        await db.query('DELETE FROM image_edits WHERE image_id = ?', [imageId]);

        res.json({ message: 'Image deleted successfully' });
    } catch (error) {
        console.error('Delete error:', error);
        res.status(500).json({ error: 'Failed to delete image' });
    }
};

// Helper function to escape XML special characters
function escapeXml(unsafe) {
    return unsafe.replace(/[<>&'"]/g, (c) => {
        switch (c) {
            case '<': return '&lt;';
            case '>': return '&gt;';
            case '&': return '&amp;';
            case '\'': return '&apos;';
            case '"': return '&quot;';
        }
    });
}

module.exports = exports;