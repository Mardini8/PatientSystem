const express = require('express');
const router = express.Router();
const upload = require('../middleware/upload');
const imageController = require('../controllers/imageController');

// Upload image with metadata
router.post('/upload', upload.single('image'), imageController.uploadImage);

// Get image by filename
router.get('/:filename', imageController.getImage);

// List all images
router.get('/', imageController.listImages);

// Get images for a specific patient
router.get('/patient/:patientPersonnummer', imageController.getPatientImages);

// Get image metadata
router.get('/metadata/:imageId', imageController.getImageMetadata);

// Add text to image
router.post('/:filename/text', imageController.addText);

// Draw on image
router.post('/:filename/draw', imageController.drawOnImage);

// Delete image
router.delete('/:imageId', imageController.deleteImage);

module.exports = router;