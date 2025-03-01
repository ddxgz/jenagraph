docker buildx build -f Dockerfile_build -t jar-builder .

docker run --name jar-builder-container jar-builder

docker cp jar-builder-container:/output ./

docker rm jar-builder-container