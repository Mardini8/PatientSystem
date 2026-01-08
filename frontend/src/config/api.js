// API Configuration for CBH Cloud Production
// This file should be used for production deployment

import keycloak from './keycloak';

const API_CONFIG = {
    // User Service - authentication and user management
    USER_SERVICE: process.env.REACT_APP_USER_SERVICE_URL || 'https://patientsystem-user.app.cloud.cbh.kth.se',

    // Message Service - messaging between users
    MESSAGE_SERVICE: process.env.REACT_APP_MESSAGE_SERVICE_URL || 'https://patientsystem-message.app.cloud.cbh.kth.se',

    // Clinical Service - FHIR resources (Patient, Practitioner, etc.)
    CLINICAL_SERVICE: process.env.REACT_APP_CLINICAL_SERVICE_URL || 'https://patientsystem-clinical.app.cloud.cbh.kth.se',

    // Image Service - medical image uploads and management
    IMAGE_SERVICE: process.env.REACT_APP_IMAGE_SERVICE_URL || 'https://patientsystem-image.app.cloud.cbh.kth.se',

    // Search Service - search functionality
    SEARCH_SERVICE: process.env.REACT_APP_SEARCH_SERVICE_URL || 'https://patientsystem-search.app.cloud.cbh.kth.se',
};

// Helper function to get base URLs
export const getApiUrl = (service) => {
    return API_CONFIG[service] || '';
};

/**
 * Fetch with automatic Bearer token injection
 * Use this for all authenticated API calls
 */
export const fetchWithAuth = async (url, options = {}) => {
    // Ensure token is fresh
    try {
        await keycloak.updateToken(30);
    } catch (error) {
        console.error('Failed to refresh token:', error);
        keycloak.logout();
        throw new Error('Session expired');
    }

    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    // Add Authorization header if authenticated
    if (keycloak.token) {
        headers['Authorization'] = `Bearer ${keycloak.token}`;
    }

    console.log('fetchWithAuth:', url, 'Token present:', !!keycloak.token);

    const response = await fetch(url, {
        ...options,
        headers,
    });

    // Handle 401 Unauthorized
    if (response.status === 401) {
        console.error('401 Unauthorized - logging out');
        keycloak.logout();
        throw new Error('Unauthorized');
    }

    // Handle 403 Forbidden
    if (response.status === 403) {
        console.error('403 Forbidden - insufficient permissions');
        throw new Error('Access denied - insufficient permissions');
    }

    return response;
};

/**
 * Fetch for file uploads with automatic Bearer token
 * Does NOT set Content-Type (let browser set it for FormData)
 */
export const fetchWithAuthFormData = async (url, options = {}) => {
    try {
        await keycloak.updateToken(30);
    } catch (error) {
        console.error('Failed to refresh token:', error);
        keycloak.logout();
        throw new Error('Session expired');
    }

    const headers = {
        ...options.headers,
    };

    if (keycloak.token) {
        headers['Authorization'] = `Bearer ${keycloak.token}`;
    }

    const response = await fetch(url, {
        ...options,
        headers,
    });

    if (response.status === 401) {
        keycloak.logout();
        throw new Error('Unauthorized');
    }

    return response;
};

// Endpoint helpers
export const endpoints = {
    // User Service endpoints
    login: () => `${API_CONFIG.USER_SERVICE}/api/users/login`,
    register: () => `${API_CONFIG.USER_SERVICE}/api/users/register`,
    users: () => `${API_CONFIG.USER_SERVICE}/api/users`,
    userById: (id) => `${API_CONFIG.USER_SERVICE}/api/users/${id}`,
    userByKeycloakId: (keycloakId) => `${API_CONFIG.USER_SERVICE}/api/users/keycloak/${keycloakId}`,
    setupProfile: () => `${API_CONFIG.USER_SERVICE}/api/users/setup-profile`,

    // Message Service endpoints
    messages: () => `${API_CONFIG.MESSAGE_SERVICE}/api/messages`,
    messagesByUser: (userId) => `${API_CONFIG.MESSAGE_SERVICE}/api/messages/user/${userId}`,
    conversation: (user1, user2) => `${API_CONFIG.MESSAGE_SERVICE}/api/messages/conversation/${user1}/${user2}`,

    // Clinical Service endpoints
    patients: () => `${API_CONFIG.CLINICAL_SERVICE}/api/patients`,
    patientById: (id) => `${API_CONFIG.CLINICAL_SERVICE}/api/patients/${id}`,
    patientByPersonnummer: (pnr) => `${API_CONFIG.CLINICAL_SERVICE}/api/patients/personnummer/${pnr}`,
    practitioners: () => `${API_CONFIG.CLINICAL_SERVICE}/api/practitioners`,
    practitionerById: (id) => `${API_CONFIG.CLINICAL_SERVICE}/api/practitioners/${id}`,
    observations: () => `${API_CONFIG.CLINICAL_SERVICE}/api/observations`,
    observationsByPatient: (patientId) => `${API_CONFIG.CLINICAL_SERVICE}/api/observations/patient/${patientId}`,
    conditions: () => `${API_CONFIG.CLINICAL_SERVICE}/api/conditions`,
    conditionsByPatient: (patientId) => `${API_CONFIG.CLINICAL_SERVICE}/api/conditions/patient/${patientId}`,
    encounters: () => `${API_CONFIG.CLINICAL_SERVICE}/api/encounters`,
    encountersByPatient: (patientId) => `${API_CONFIG.CLINICAL_SERVICE}/api/encounters/patient/${patientId}`,

    // Image Service endpoints
    imageUpload: () => `${API_CONFIG.IMAGE_SERVICE}/api/images/upload`,
    imageByFilename: (filename) => `${API_CONFIG.IMAGE_SERVICE}/api/images/${filename}`,
    imagesByPatient: (personnummer) => `${API_CONFIG.IMAGE_SERVICE}/api/images/patient/${personnummer}`,

    // Search Service endpoints
    search: () => `${API_CONFIG.SEARCH_SERVICE}/api/search`,
    searchPatients: (query) => `${API_CONFIG.SEARCH_SERVICE}/api/search/patients?q=${query}`,
};

export default API_CONFIG;