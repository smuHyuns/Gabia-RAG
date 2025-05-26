npm run build
docker buildx build --platform linux/amd64 -t dlawjddn/god-client:0.0.1 .
docker push dlawjddn/god-client:0.0.1