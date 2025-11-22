const sharp = require('sharp');
const path = require('path');
const fs = require('fs').promises;

// Upload image
exports.uploadImage = async (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ error: 'No file uploaded' });
        }

        res.status(201).json({
            message: 'Image uploaded successfully',
            image: {
                id: path.parse(req.file.filename).name,
                filename: req.file.filename,
                path: req.file.path,
                size: req.file.size,
                mimetype: req.file.mimetype,
                url: `/api/images/${req.file.filename}`
            }
        });
    } catch (error) {
        console.error('Upload error:', error);
        res.status(500).json({ error: 'Failed to upload image' });
    }
};

// Get image
exports.getImage = async (req, res) => {
    try {
        const filename = req.params.filename;
        const filepath = path.join(process.env.UPLOAD_DIR || './uploads', filename);

        // Check if file exists
        await fs.access(filepath);

        res.sendFile(path.resolve(filepath));
    } catch (error) {
        console.error('Get image error:', error);
        res.status(404).json({ error: 'Image not found' });
    }
};

// Add text to image
exports.addText = async (req, res) => {
    try {
        const filename = req.params.filename;
        const { text, x, y, fontSize, color } = req.body;

        if (!text) {
            return res.status(400).json({ error: 'Text is required' });
        }

        const filepath = path.join(process.env.UPLOAD_DIR || './uploads', filename);
        const outputFilename = `edited-${Date.now()}-${filename}`;
        const outputPath = path.join(process.env.UPLOAD_DIR || './uploads', outputFilename);

        // Create SVG text overlay
        const svgText = `
            <svg width="2000" height="2000">
                <text x="${x || 10}" y="${y || 50}" 
                      font-size="${fontSize || 24}" 
                      fill="${color || 'red'}"
                      font-weight="bold">
                    ${text}
                </text>
            </svg>
        `;

        // Add text to image
        await sharp(filepath)
            .composite([{
                input: Buffer.from(svgText),
                top: 0,
                left: 0
            }])
            .toFile(outputPath);

        res.json({
            message: 'Text added successfully',
            image: {
                filename: outputFilename,
                url: `/api/images/${outputFilename}`
            }
        });
    } catch (error) {
        console.error('Add text error:', error);
        res.status(500).json({ error: 'Failed to add text to image' });
    }
};

// Draw on image (simple rectangle/circle)
exports.drawOnImage = async (req, res) => {
    try {
        const filename = req.params.filename;
        const { shape, x, y, width, height, color } = req.body;

        const filepath = path.join(process.env.UPLOAD_DIR || './uploads', filename);
        const outputFilename = `drawn-${Date.now()}-${filename}`;
        const outputPath = path.join(process.env.UPLOAD_DIR || './uploads', outputFilename);

        let svgShape = '';

        if (shape === 'rectangle') {
            svgShape = `
                <svg width="2000" height="2000">
                    <rect x="${x || 10}" y="${y || 10}" 
                          width="${width || 100}" height="${height || 100}" 
                          fill="none" stroke="${color || 'red'}" stroke-width="3"/>
                </svg>
            `;
        } else if (shape === 'circle') {
            const radius = width || 50;
            svgShape = `
                <svg width="2000" height="2000">
                    <circle cx="${x || 50}" cy="${y || 50}" r="${radius}" 
                            fill="none" stroke="${color || 'red'}" stroke-width="3"/>
                </svg>
            `;
        } else {
            return res.status(400).json({ error: 'Invalid shape. Use rectangle or circle' });
        }

        // Draw on image
        await sharp(filepath)
            .composite([{
                input: Buffer.from(svgShape),
                top: 0,
                left: 0
            }])
            .toFile(outputPath);

        res.json({
            message: 'Drawing added successfully',
            image: {
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
        const uploadDir = process.env.UPLOAD_DIR || './uploads';
        const files = await fs.readdir(uploadDir);

        const images = files
            .filter(file => /\.(jpg|jpeg|png|gif)$/i.test(file))
            .map(file => ({
                filename: file,
                url: `/api/images/${file}`
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
        const filename = req.params.filename;
        const filepath = path.join(process.env.UPLOAD_DIR || './uploads', filename);

        await fs.unlink(filepath);

        res.json({ message: 'Image deleted successfully' });
    } catch (error) {
        console.error('Delete error:', error);
        res.status(404).json({ error: 'Image not found' });
    }
};