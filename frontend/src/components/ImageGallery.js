import React, { useState, useEffect, useRef, useCallback } from 'react';
import API_CONFIG, { fetchWithAuth, fetchWithAuthFormData } from '../config/api';

function ImageGallery({ currentUser, patientPersonnummer }) {
    const [images, setImages] = useState([]);
    const [selectedImage, setSelectedImage] = useState(null);
    const [loading, setLoading] = useState(false);
    const [uploadFile, setUploadFile] = useState(null);
    const [uploadPreview, setUploadPreview] = useState(null);

    // Editor state
    const [showEditor, setShowEditor] = useState(false);
    const [editorImage, setEditorImage] = useState(null);
    const [tool, setTool] = useState('none'); // 'text', 'pen', 'rectangle', 'circle', 'arrow'
    const [color, setColor] = useState('#FF0000');
    const [text, setText] = useState('');
    const [textPosition, setTextPosition] = useState({ x: 50, y: 50 });
    const [fontSize, setFontSize] = useState(24);

    const canvasRef = useRef(null);
    const [isDrawing, setIsDrawing] = useState(false);
    const [drawStart, setDrawStart] = useState(null);

    const isDoctor = currentUser.role === 'DOCTOR';
    const isPatient = currentUser.role === 'PATIENT';

    const fetchImages = useCallback(async () => {
        setLoading(true);
        try {
            // Use fetchWithAuth instead of fetch
            const response = await fetchWithAuth(
                `${API_CONFIG.IMAGE_SERVICE}/api/images/patient/${patientPersonnummer}`
            );
            if (response.ok) {
                const data = await response.json();
                setImages(data.images || []);
                console.log('Images loaded:', data.images?.length || 0);
            } else {
                console.error('Failed to fetch images:', response.status);
            }
        } catch (error) {
            console.error('Error fetching images:', error);
        } finally {
            setLoading(false);
        }
    }, [patientPersonnummer]);

    useEffect(() => {
        if (patientPersonnummer) {
            fetchImages();
        }
    }, [patientPersonnummer, fetchImages]);

    const handleFileSelect = (e) => {
        const file = e.target.files[0];
        if (file) {
            setUploadFile(file);
            const reader = new FileReader();
            reader.onloadend = () => {
                setUploadPreview(reader.result);
            };
            reader.readAsDataURL(file);
        }
    };

    const handleUpload = async () => {
        if (!uploadFile) {
            alert('Please select an image');
            return;
        }

        const formData = new FormData();
        formData.append('image', uploadFile);
        formData.append('patientPersonnummer', patientPersonnummer);
        formData.append('userId', currentUser.id);
        formData.append('username', currentUser.username);

        console.log('Uploading image for patient:', patientPersonnummer);
        console.log('User ID:', currentUser.id);

        try {
            // Use fetchWithAuthFormData for file uploads
            const response = await fetchWithAuthFormData(`${API_CONFIG.IMAGE_SERVICE}/api/images/upload`, {
                method: 'POST',
                body: formData
            });

            if (response.ok) {
                const result = await response.json();
                console.log('Upload successful:', result);
                alert('Image uploaded successfully!');
                setUploadFile(null);
                setUploadPreview(null);
                fetchImages();
            } else {
                const errorData = await response.json();
                console.error('Upload failed:', errorData);
                alert(`Failed to upload image: ${errorData.error || 'Unknown error'}`);
            }
        } catch (error) {
            console.error('Upload error:', error);
            alert('Error uploading image: ' + error.message);
        }
    };

    const openEditor = (image) => {
        setEditorImage(image);
        setShowEditor(true);
        setTool('none');
        setText('');

        // Load image on canvas after modal opens
        setTimeout(() => {
            const canvas = canvasRef.current;
            if (canvas) {
                const ctx = canvas.getContext('2d');
                const img = new Image();
                img.crossOrigin = "anonymous";
                img.onload = () => {
                    canvas.width = img.width;
                    canvas.height = img.height;
                    ctx.drawImage(img, 0, 0);
                };
                img.src = `${API_CONFIG.IMAGE_SERVICE}${image.url}`;
            }
        }, 100);
    };

    const handleCanvasMouseDown = (e) => {
        if (tool === 'none' || tool === 'text') return;

        const canvas = canvasRef.current;
        const rect = canvas.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;

        setIsDrawing(true);
        setDrawStart({ x, y });
    };

    const handleCanvasMouseMove = (e) => {
        if (!isDrawing || tool === 'none' || tool === 'text') return;

        const canvas = canvasRef.current;
        const ctx = canvas.getContext('2d');
        const rect = canvas.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;

        if (tool === 'pen') {
            ctx.strokeStyle = color;
            ctx.lineWidth = 3;
            ctx.lineCap = 'round';
            ctx.beginPath();
            ctx.moveTo(drawStart.x, drawStart.y);
            ctx.lineTo(x, y);
            ctx.stroke();
            setDrawStart({ x, y });
        }
    };

    const handleCanvasMouseUp = async (e) => {
        if (!isDrawing) return;
        setIsDrawing(false);

        const canvas = canvasRef.current;
        const ctx = canvas.getContext('2d');
        const rect = canvas.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;

        ctx.strokeStyle = color;
        ctx.lineWidth = 3;

        if (tool === 'rectangle') {
            const width = x - drawStart.x;
            const height = y - drawStart.y;
            ctx.strokeRect(drawStart.x, drawStart.y, width, height);
        } else if (tool === 'circle') {
            const radius = Math.sqrt(
                Math.pow(x - drawStart.x, 2) + Math.pow(y - drawStart.y, 2)
            );
            ctx.beginPath();
            ctx.arc(drawStart.x, drawStart.y, radius, 0, 2 * Math.PI);
            ctx.stroke();
        } else if (tool === 'arrow') {
            drawArrow(ctx, drawStart.x, drawStart.y, x, y);
        }
    };

    const drawArrow = (ctx, fromX, fromY, toX, toY) => {
        const headLength = 15;
        const angle = Math.atan2(toY - fromY, toX - fromX);

        // Line
        ctx.beginPath();
        ctx.moveTo(fromX, fromY);
        ctx.lineTo(toX, toY);
        ctx.stroke();

        // Arrow head
        ctx.beginPath();
        ctx.moveTo(toX, toY);
        ctx.lineTo(
            toX - headLength * Math.cos(angle - Math.PI / 6),
            toY - headLength * Math.sin(angle - Math.PI / 6)
        );
        ctx.moveTo(toX, toY);
        ctx.lineTo(
            toX - headLength * Math.cos(angle + Math.PI / 6),
            toY - headLength * Math.sin(angle + Math.PI / 6)
        );
        ctx.stroke();
    };

    const handleCanvasClick = (e) => {
        if (tool !== 'text' || !text) return;

        const canvas = canvasRef.current;
        const ctx = canvas.getContext('2d');
        const rect = canvas.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;

        ctx.fillStyle = color;
        ctx.font = `${fontSize}px Arial`;
        ctx.fillText(text, x, y);
    };

    const handleAddText = () => {
        if (!text) {
            alert('Enter text first');
            return;
        }

        const canvas = canvasRef.current;
        const ctx = canvas.getContext('2d');

        ctx.fillStyle = color;
        ctx.font = `${fontSize}px Arial`;
        ctx.fillText(text, textPosition.x, textPosition.y);
    };

    const handleSaveEdited = async () => {
        const canvas = canvasRef.current;

        canvas.toBlob(async (blob) => {
            const formData = new FormData();
            formData.append('image', blob, `edited_${Date.now()}.png`);
            formData.append('patientPersonnummer', patientPersonnummer);
            formData.append('userId', currentUser.id);
            formData.append('username', currentUser.username);
            formData.append('description', `Edited version of ${editorImage.filename}`);

            try {
                // Use fetchWithAuthFormData for file uploads
                const response = await fetchWithAuthFormData(`${API_CONFIG.IMAGE_SERVICE}/api/images/upload`, {
                    method: 'POST',
                    body: formData
                });

                if (response.ok) {
                    alert('Edited image saved!');
                    setShowEditor(false);
                    setEditorImage(null);
                    fetchImages();
                } else {
                    alert('Failed to save edited image');
                }
            } catch (error) {
                console.error('Error saving edited image:', error);
                alert('Error saving image: ' + error.message);
            }
        }, 'image/png');
    };

    const styles = {
        container: {
            background: 'white',
            borderRadius: '8px',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
            padding: '20px'
        },
        header: {
            marginBottom: '20px',
            paddingBottom: '15px',
            borderBottom: '2px solid #e0e0e0'
        },
        uploadSection: {
            padding: '20px',
            background: '#f9f9f9',
            borderRadius: '8px',
            marginBottom: '30px'
        },
        fileInput: {
            marginBottom: '15px'
        },
        preview: {
            maxWidth: '200px',
            maxHeight: '200px',
            borderRadius: '8px',
            marginBottom: '15px'
        },
        gallery: {
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
            gap: '20px'
        },
        imageCard: {
            border: '1px solid #ddd',
            borderRadius: '8px',
            overflow: 'hidden',
            cursor: 'pointer',
            transition: 'transform 0.2s, box-shadow 0.2s'
        },
        thumbnail: {
            width: '100%',
            height: '150px',
            objectFit: 'cover'
        },
        button: {
            padding: '10px 20px',
            background: '#667eea',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            marginRight: '10px',
            fontSize: '14px'
        },
        modal: {
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: 'rgba(0,0,0,0.8)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000
        },
        modalContent: {
            background: 'white',
            padding: '30px',
            borderRadius: '8px',
            maxWidth: '90vw',
            maxHeight: '90vh',
            overflow: 'auto'
        },
        toolbar: {
            display: 'flex',
            flexWrap: 'wrap',
            gap: '10px',
            marginBottom: '15px',
            padding: '15px',
            background: '#f0f0f0',
            borderRadius: '8px'
        },
        toolButton: (active) => ({
            padding: '8px 16px',
            background: active ? '#667eea' : '#ddd',
            color: active ? 'white' : '#333',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer'
        }),
        canvas: {
            border: '2px solid #ddd',
            maxWidth: '100%',
            cursor: 'crosshair'
        },
        colorPicker: {
            width: '40px',
            height: '40px',
            padding: '0',
            border: 'none',
            cursor: 'pointer'
        },
        textInput: {
            padding: '8px',
            border: '1px solid #ddd',
            borderRadius: '4px',
            fontSize: '14px'
        }
    };

    return (
        <div style={styles.container}>
            <div style={styles.header}>
                <h2>Patient Images</h2>
                {isDoctor && (
                    <p style={{ color: '#666', fontSize: '14px', marginTop: '5px' }}>
                        Upload and edit medical images for this patient
                    </p>
                )}
                {isPatient && (
                    <p style={{ color: '#666', fontSize: '14px', marginTop: '5px' }}>
                        View your medical images
                    </p>
                )}
            </div>

            {/* Upload Section - Only for Doctors */}
            {isDoctor && (
                <div style={styles.uploadSection}>
                    <h3 style={{ marginBottom: '15px' }}>Upload New Image</h3>

                    <input
                        type="file"
                        accept="image/*"
                        onChange={handleFileSelect}
                        style={styles.fileInput}
                    />

                    {uploadPreview && (
                        <div>
                            <img src={uploadPreview} alt="Preview" style={styles.preview} />
                            <br />
                            <button style={styles.button} onClick={handleUpload}>
                                Upload Image
                            </button>
                            <button
                                style={{ ...styles.button, background: '#999' }}
                                onClick={() => {
                                    setUploadFile(null);
                                    setUploadPreview(null);
                                }}
                            >
                                Cancel
                            </button>
                        </div>
                    )}
                </div>
            )}

            {/* Image Gallery */}
            {loading ? (
                <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
                    Loading images...
                </div>
            ) : images.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
                    No images available
                </div>
            ) : (
                <div style={styles.gallery}>
                    {images.map((image, index) => (
                        <div
                            key={index}
                            style={styles.imageCard}
                            onClick={() => isDoctor ? openEditor(image) : setSelectedImage(image)}
                        >
                            <img
                                src={`${API_CONFIG.IMAGE_SERVICE}${image.url}`}
                                alt={`Medical image ${index + 1}`}
                                style={styles.thumbnail}
                            />
                            <div style={{ padding: '10px', textAlign: 'center', fontSize: '12px', color: '#666' }}>
                                {image.filename}
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Image Viewer Modal for Patients */}
            {isPatient && selectedImage && (
                <div style={styles.modal} onClick={() => setSelectedImage(null)}>
                    <div style={styles.modalContent} onClick={(e) => e.stopPropagation()}>
                        <img
                            src={`${API_CONFIG.IMAGE_SERVICE}${selectedImage.url}`}
                            alt="Medical image"
                            style={{ maxWidth: '100%', maxHeight: '70vh' }}
                        />
                        <div style={{ marginTop: '15px', textAlign: 'center' }}>
                            <button
                                style={styles.button}
                                onClick={() => setSelectedImage(null)}
                            >
                                Close
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Image Editor Modal for Doctors */}
            {isDoctor && showEditor && editorImage && (
                <div style={styles.modal}>
                    <div style={{ ...styles.modalContent, maxWidth: '95vw' }}>
                        <h3 style={{ marginBottom: '15px' }}>Edit Image</h3>

                        {/* Toolbar */}
                        <div style={styles.toolbar}>
                            <button
                                style={styles.toolButton(tool === 'pen')}
                                onClick={() => setTool('pen')}
                            >
                                ✏️ Pen
                            </button>
                            <button
                                style={styles.toolButton(tool === 'rectangle')}
                                onClick={() => setTool('rectangle')}
                            >
                                ▭ Rectangle
                            </button>
                            <button
                                style={styles.toolButton(tool === 'circle')}
                                onClick={() => setTool('circle')}
                            >
                                ○ Circle
                            </button>
                            <button
                                style={styles.toolButton(tool === 'arrow')}
                                onClick={() => setTool('arrow')}
                            >
                                → Arrow
                            </button>
                            <button
                                style={styles.toolButton(tool === 'text')}
                                onClick={() => setTool('text')}
                            >
                                T Text
                            </button>

                            <input
                                type="color"
                                value={color}
                                onChange={(e) => setColor(e.target.value)}
                                style={styles.colorPicker}
                                title="Choose color"
                            />

                            {tool === 'text' && (
                                <>
                                    <input
                                        type="text"
                                        value={text}
                                        onChange={(e) => setText(e.target.value)}
                                        placeholder="Enter text..."
                                        style={styles.textInput}
                                    />
                                    <input
                                        type="number"
                                        value={fontSize}
                                        onChange={(e) => setFontSize(parseInt(e.target.value))}
                                        min="12"
                                        max="72"
                                        style={{ ...styles.textInput, width: '60px' }}
                                        title="Font size"
                                    />
                                    <button style={styles.button} onClick={handleAddText}>
                                        Add Text
                                    </button>
                                </>
                            )}
                        </div>

                        {/* Canvas */}
                        <canvas
                            ref={canvasRef}
                            style={styles.canvas}
                            onMouseDown={handleCanvasMouseDown}
                            onMouseMove={handleCanvasMouseMove}
                            onMouseUp={handleCanvasMouseUp}
                            onClick={handleCanvasClick}
                        />

                        {/* Action Buttons */}
                        <div style={{ marginTop: '20px', textAlign: 'center' }}>
                            <button style={styles.button} onClick={handleSaveEdited}>
                                Save Edited Image
                            </button>
                            <button
                                style={{ ...styles.button, background: '#999' }}
                                onClick={() => {
                                    setShowEditor(false);
                                    setEditorImage(null);
                                }}
                            >
                                Close
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default ImageGallery;