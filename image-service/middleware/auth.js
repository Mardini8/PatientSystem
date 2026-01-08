const jwt = require('jsonwebtoken');
const jwksClient = require('jwks-rsa');

// JWKS client to fetch public keys from Keycloak
const client = jwksClient({
    jwksUri: process.env.KEYCLOAK_JWKS_URI || 'https://patientsystem-keycloak.app.cloud.cbh.kth.se/realms/patientsystem/protocol/openid-connect/certs',
    cache: true,
    cacheMaxEntries: 5,
    cacheMaxAge: 600000 // 10 minutes
});

// Function to get signing key
function getKey(header, callback) {
    client.getSigningKey(header.kid, (err, key) => {
        if (err) {
            console.error('Error getting signing key:', err);
            callback(err, null);
        } else {
            const signingKey = key.getPublicKey();
            callback(null, signingKey);
        }
    });
}

// JWT verification options
const jwtOptions = {
    issuer: process.env.KEYCLOAK_ISSUER || 'https://patientsystem-keycloak.app.cloud.cbh.kth.se/realms/patientsystem',
    algorithms: ['RS256']
};

/**
 * Middleware to authenticate JWT tokens from Keycloak
 */
const authenticateToken = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1]; // Bearer TOKEN

    if (!token) {
        console.log('No token provided');
        return res.status(401).json({ error: 'Access denied. No token provided.' });
    }

    jwt.verify(token, getKey, jwtOptions, (err, decoded) => {
        if (err) {
            console.error('Token verification failed:', err.message);
            return res.status(403).json({ error: 'Invalid or expired token.' });
        }

        // Extract user info from token
        req.user = {
            id: decoded.sub,
            username: decoded.preferred_username,
            email: decoded.email,
            roles: decoded.realm_access?.roles || []
        };

        console.log('Authenticated user:', req.user.username, 'Roles:', req.user.roles);
        next();
    });
};

/**
 * Middleware to check if user has required role
 * @param {string[]} allowedRoles - Array of roles that are allowed
 */
const requireRole = (allowedRoles) => {
    return (req, res, next) => {
        if (!req.user) {
            return res.status(401).json({ error: 'Not authenticated' });
        }

        const userRoles = req.user.roles.map(r => r.toLowerCase());
        const hasRole = allowedRoles.some(role => userRoles.includes(role.toLowerCase()));

        if (!hasRole) {
            console.log(`Access denied. User ${req.user.username} has roles [${userRoles}], needs one of [${allowedRoles}]`);
            return res.status(403).json({
                error: 'Access denied. Insufficient permissions.',
                required: allowedRoles,
                userRoles: userRoles
            });
        }

        next();
    };
};

/**
 * Middleware to check if user is a doctor
 */
const requireDoctor = requireRole(['doctor']);

/**
 * Middleware to check if user is doctor or staff
 */
const requireDoctorOrStaff = requireRole(['doctor', 'staff']);

/**
 * Middleware that allows any authenticated user
 */
const requireAuthenticated = (req, res, next) => {
    if (!req.user) {
        return res.status(401).json({ error: 'Not authenticated' });
    }
    next();
};

module.exports = {
    authenticateToken,
    requireRole,
    requireDoctor,
    requireDoctorOrStaff,
    requireAuthenticated
};