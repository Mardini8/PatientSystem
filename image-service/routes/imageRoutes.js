const express = require('express');
const router = express.Router();
const upload = require('../middleware/upload');
const imageController = require('../controllers/imageController');

// Upload image
router.post('/upload', upload.single('image'), imageController.uploadImage);

// Get image
router.get('/:filename', imageController.getImage);

// List all images
router.get('/', imageController.listImages);

// Add text to image
router.post('/:filename/text', imageController.addText);

// Draw on image
router.post('/:filename/draw', imageController.drawOnImage);

// Delete image
router.delete('/:filename', imageController.deleteImage);

module.exports = router;