FROM node:18-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci --only=production

# Copy application files
COPY . .

# Create directories
RUN mkdir -p uploads database

# Expose port (will be overridden by PORT env var)
EXPOSE 3001

# Set environment variables with defaults
ENV NODE_ENV=production
ENV UPLOAD_DIR=/app/uploads

# Keycloak defaults (override in deployment)
ENV KEYCLOAK_JWKS_URI=https://patientsystem-keycloak.app.cloud.cbh.kth.se/realms/patientsystem/protocol/openid-connect/certs
ENV KEYCLOAK_ISSUER=https://patientsystem-keycloak.app.cloud.cbh.kth.se/realms/patientsystem

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD node -e "require('http').get('http://localhost:' + (process.env.PORT || 3001) + '/health', (r) => {process.exit(r.statusCode === 200 ? 0 : 1)})"

# Start the application
CMD ["node", "server.js"]