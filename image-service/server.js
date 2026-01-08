require('dotenv').config();
const express = require('express');
const cors = require('cors');
const path = require('path');
const imageRoutes = require('./routes/imageRoutes');

// Initialize database
const db = require('./database/setup');

const app = express();
const PORT = process.env.PORT || 3001;

// CORS configuration - Allow frontend origins
const allowedOrigins = [
    'http://localhost:3000',
    'http://localhost:30000',
    'https://patientsystem-frontend.app.cloud.cbh.kth.se',
    process.env.FRONTEND_URL
].filter(Boolean);

app.use(cors({
    origin: function(origin, callback) {
        // Allow requests with no origin (mobile apps, curl, etc.)
        if (!origin) return callback(null, true);

        if (allowedOrigins.includes(origin)) {
            callback(null, true);
        } else {
            console.log('CORS blocked origin:', origin);
            callback(new Error('Not allowed by CORS'));
        }
    },
    credentials: true,
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization']
}));

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Routes - Now secured with JWT authentication
app.use('/api/images', imageRoutes);

// Health check - No authentication required
app.get('/health', (req, res) => {
    res.json({
        status: 'OK',
        service: 'Image Service',
        port: PORT,
        version: '2.1.0',
        security: 'JWT/Keycloak',
        features: [
            'Image upload with patient linking',
            'Patient-specific image retrieval',
            'Canvas-based image editing (client-side)',
            'JWT token authentication',
            'Role-based access control'
        ]
    });
});

// Root endpoint - No authentication required
app.get('/', (req, res) => {
    res.json({
        message: 'Image Service API',
        version: '2.1.0',
        description: 'Secured image service with JWT authentication',
        security: {
            authentication: 'Bearer JWT token required',
            issuer: 'Keycloak',
            roles: {
                upload: 'doctor',
                view: 'any authenticated user'
            }
        },
        endpoints: {
            upload: 'POST /api/images/upload - Upload new or edited images (doctor only)',
            getImage: 'GET /api/images/:filename - Retrieve an image (authenticated)',
            patientImages: 'GET /api/images/patient/:patientPersonnummer - Get all images for a patient (authenticated)'
        }
    });
});

// Error handling
app.use((error, req, res, next) => {
    console.error('Error:', error);

    // Handle CORS errors
    if (error.message === 'Not allowed by CORS') {
        return res.status(403).json({ error: 'CORS not allowed' });
    }

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
            console.log('✓ JWT authentication enabled');
            console.log('✓ Ready to accept image uploads');
            console.log('✓ Client-side editing enabled');
            console.log('✓ Allowed origins:', allowedOrigins);
        });
    } catch (error) {
        console.error('✗ Failed to start server:', error);
        process.exit(1);
    }
}

startServer();