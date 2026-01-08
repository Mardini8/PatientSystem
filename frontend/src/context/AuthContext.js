import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import keycloak from '../config/keycloak';
import API_CONFIG from '../config/api';

const AuthContext = createContext(null);

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};

export const AuthProvider = ({ children }) => {
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [user, setUser] = useState(null);
    const [token, setToken] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [needsProfileSetup, setNeedsProfileSetup] = useState(false);

    useEffect(() => {
        const initKeycloak = async () => {
            try {
                const authenticated = await keycloak.init({
                    onLoad: 'login-required',
                    checkLoginIframe: false,
                    pkceMethod: 'S256',
                });

                if (authenticated) {
                    setIsAuthenticated(true);
                    setToken(keycloak.token);

                    const keycloakId = keycloak.tokenParsed?.sub;

                    // Check if user has completed profile in user-service
                    try {
                        const response = await fetch(
                            `${API_CONFIG.USER_SERVICE}/api/users/keycloak/${keycloakId}`,
                            {
                                headers: {
                                    'Authorization': `Bearer ${keycloak.token}`
                                }
                            }
                        );

                        if (response.ok) {
                            // User exists in database - profile is complete
                            const userData = await response.json();
                            console.log('User found in database:', userData);

                            setUser({
                                id: userData.id,
                                keycloakId: keycloakId,
                                username: userData.username,
                                email: userData.email,
                                firstName: keycloak.tokenParsed?.given_name,
                                lastName: keycloak.tokenParsed?.family_name,
                                role: userData.role,
                                foreignId: userData.foreignId,
                            });
                            setNeedsProfileSetup(false);
                        } else if (response.status === 404) {
                            // User not in database - needs profile setup
                            console.log('User not found in database, needs profile setup');
                            setUser({
                                id: keycloakId,
                                keycloakId: keycloakId,
                                username: keycloak.tokenParsed?.preferred_username,
                                email: keycloak.tokenParsed?.email,
                                firstName: keycloak.tokenParsed?.given_name,
                                lastName: keycloak.tokenParsed?.family_name,
                                role: null,
                                foreignId: null,
                            });
                            setNeedsProfileSetup(true);
                        } else {
                            console.error('Error checking user profile:', response.status);
                            setNeedsProfileSetup(true);
                        }
                    } catch (error) {
                        console.error('Error fetching user profile:', error);
                        // If we can't reach user-service, assume profile setup needed
                        setUser({
                            id: keycloakId,
                            keycloakId: keycloakId,
                            username: keycloak.tokenParsed?.preferred_username,
                            email: keycloak.tokenParsed?.email,
                            firstName: keycloak.tokenParsed?.given_name,
                            lastName: keycloak.tokenParsed?.family_name,
                            role: null,
                            foreignId: null,
                        });
                        setNeedsProfileSetup(true);
                    }
                }
            } catch (error) {
                console.error('Keycloak init error:', error);
            } finally {
                setIsLoading(false);
            }
        };

        initKeycloak();

        // Token refresh
        const refreshInterval = setInterval(() => {
            if (keycloak.authenticated) {
                keycloak.updateToken(70)
                    .then((refreshed) => {
                        if (refreshed) {
                            setToken(keycloak.token);
                        }
                    })
                    .catch(() => {
                        console.error('Failed to refresh token');
                        keycloak.logout();
                    });
            }
        }, 60000);

        return () => clearInterval(refreshInterval);
    }, []);

    const logout = useCallback(() => {
        keycloak.logout({
            redirectUri: window.location.origin,
        });
    }, []);

    const getToken = useCallback(() => {
        return keycloak.token;
    }, []);

    const hasRole = useCallback((role) => {
        // Check role from user-service data (uppercase)
        const userRole = user?.role?.toUpperCase();
        const checkRole = role?.toUpperCase();
        return userRole === checkRole;
    }, [user]);

    const completeProfileSetup = useCallback((role, foreignId) => {
        // Update local state after profile is saved
        setUser(prev => ({
            ...prev,
            role: role.toUpperCase(),
            foreignId: foreignId,
        }));
        setNeedsProfileSetup(false);
    }, []);

    const value = {
        isAuthenticated,
        isLoading,
        user,
        token,
        needsProfileSetup,
        logout,
        getToken,
        hasRole,
        completeProfileSetup,
        keycloak,
    };

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
};

export default AuthProvider;