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
        features: [
            'Image upload with patient linking',
            'Metadata storage',
            'Advanced image editing',
            'Patient-specific image retrieval'
        ]
    });
});

// Root endpoint
app.get('/', (req, res) => {
    res.json({
        message: 'Image Service API',
        version: '2.0.0',
        endpoints: {
            upload: 'POST /api/images/upload',
            getImage: 'GET /api/images/:filename',
            listImages: 'GET /api/images',
            patientImages: 'GET /api/images/patient/:patientPersonnummer',
            imageMetadata: 'GET /api/images/metadata/:imageId',
            addText: 'POST /api/images/:filename/text',
            draw: 'POST /api/images/:filename/draw',
            delete: 'DELETE /api/images/:imageId'
        }
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

        // Initialize tables (creates them if they don't exist)
        await db.initializeTables();

        // Start server
        app.listen(PORT, () => {
            console.log(`✓ Image Service running on http://localhost:${PORT}`);
            console.log('✓ Database initialized');
            console.log('✓ Ready to accept image uploads');
        });
    } catch (error) {
        console.error('✗ Failed to start server:', error);
        process.exit(1);
    }
}

startServer();