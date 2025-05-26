cd ../..
./gradlew clean build -x test
docker buildx build --platform linux/amd64 --load -f deploy/local/Dockerfile -t dlawjddn/god:2.0.0 .
docker push dlawjddn/god:2.0.0
