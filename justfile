crd-generate:
    ./gradlew generateCrds

crd-install:
    kubectl apply -f crd/

crd-delete:
    kubectl delete --ignore-not-found -f crd/

[working-directory('manifests')]
crd-import:
    cdk8s import ../crd/repositories.github.platform.benkeil.de-v1.yml

[working-directory('manifests/dist')]
clean:
    rm -f ./*

manifests: clean
    ./gradlew manifests:run

[working-directory('manifests')]
deploy:
    kubectl apply -f dist/

[working-directory('manifests')]
delete:
    kubectl delete -f dist/
