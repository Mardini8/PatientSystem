import Keycloak from 'keycloak-js';

const keycloakConfig = {
    url: process.env.REACT_APP_KEYCLOAK_URL || 'https://patientsystem-keycloak.app.cloud.cbh.kth.se',
    realm: 'patientsystem',
    clientId: 'patientsystem',
};

const keycloak = new Keycloak(keycloakConfig);

export default keycloak;