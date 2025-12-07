require('dotenv').config();
const express = require('express');
const cors = require('cors');
const path = require('path');
const imageRoutes = require('./routes/imageRoutes');

// Initialize database
const db = require('./database/setup');

const app = express();
const PORT = process.env.PORT || 3001;

// Middleware
app.use(cors({
    origin: ['http://localhost:3000', 'http://localhost:30000'],
    credentials: true
}));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Routes
app.use('/api/images', imageRoutes);

// Health check
app.get('/health', (req, res) => {
    res.json({
        status: 'OK',
        service: 'Image Service',
        port: PORT,
        version: '2.0.0',
        features: [
            'Image upload with patient linking',
            'Patient-specific image retrieval',
            'Canvas-based image editing (client-side)'
        ]
    });
});

// Root endpoint
app.get('/', (req, res) => {
    res.json({
        message: 'Image Service API',
        version: '2.0.0',
        description: 'Simplified image service - all editing done client-side',
        endpoints: {
            upload: 'POST /api/images/upload - Upload new or edited images',
            getImage: 'GET /api/images/:filename - Retrieve an image',
            patientImages: 'GET /api/images/patient/:patientPersonnummer - Get all images for a patient'
        },
        notes: [
            'Image editing is done client-side using HTML Canvas',
            'Edited images are saved as new files via the upload endpoint'
        ]
    });
});

// Error handling
app.use((error, req, res, next) => {
    console.error('Error:', error);
    res.status(error.status || 500).json({
        error: error.message || 'Internal server error'
    });
});

// Initialize database and start server
async function startServer() {
    try {
        // Test database connection
        await db.testConnection();

        // Initialize tables
        await db.initializeTables();

        // Start server
        app.listen(PORT, () => {
            console.log(`✓ Image Service running on http://localhost:${PORT}`);
            console.log('✓ Database initialized');
            console.log('✓ Ready to accept image uploads');
            console.log('✓ Client-side editing enabled');
        });
    } catch (error) {
        console.error('✗ Failed to start server:', error);
        process.exit(1);
    }
}

startServer();