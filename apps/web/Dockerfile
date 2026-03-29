FROM node:20-alpine AS build

WORKDIR /workspace

COPY package.json package-lock.json nx.json tsconfig.base.json ./
COPY apps/web ./apps/web
COPY libs ./libs

RUN npm ci
RUN npx nx build web

FROM nginx:1.27-alpine AS runtime

COPY deploy/nginx/web.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/apps/web /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
