const express = require('express');
const router = express.Router();
const upload = require('../middleware/upload');
const imageController = require('../controllers/imageController');

// Upload image with metadata (used for both original and edited images)
router.post('/upload', upload.single('image'), imageController.uploadImage);

// Get image by filename
router.get('/:filename', imageController.getImage);

// Get images for a specific patient
router.get('/patient/:patientPersonnummer', imageController.getPatientImages);

module.exports = router;