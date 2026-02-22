# Multi-stage Dockerfile for Next.js (ui)
# Builder stage
FROM node:20-slim AS builder

WORKDIR /app

# Copy package manifests first to take advantage of Docker layer caching
COPY package.json pnpm-lock.yaml ./

# Enable Corepack and install dependencies using pnpm (respects pnpm-lock.yaml)
RUN corepack enable && corepack prepare pnpm@latest --activate && pnpm install --frozen-lockfile

# Allow passing the public API URL at build time and expose it as an env var for Next.js build
ARG NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL}

# Copy the rest of the app and build
COPY . .
RUN pnpm build

# Production image
FROM node:20-slim AS runner

WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000

# Copy only dependency manifests and install production dependencies
COPY package.json pnpm-lock.yaml ./
RUN corepack enable && corepack prepare pnpm@latest --activate && pnpm install --frozen-lockfile --prod

# Copy build output and public assets from the builder stage
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/public ./public
COPY --from=builder /app/next.config.mjs ./next.config.mjs

EXPOSE 3000

# Start the Next.js production server
CMD ["pnpm", "start"]