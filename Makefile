ENV_FILE_NAME=sample.env
OUT_PATH=dht/out

.PHONY: dht

build: dht_build
clean: dht_clean

dht: dht_build
	@$(OUT_PATH)/bin/dht-service

dht_build: dht_clean
	@echo "Building server..."
	@./gradlew -q dht:java-app:assembleDist
	@echo "Moving ang unpacking client executable..."
	@mkdir -p $(OUT_PATH)
	@cp dht/java-app/build/distributions/*.tar $(OUT_PATH)/dht.tar
	@tar -xf $(OUT_PATH)/dht.tar -C $(OUT_PATH)
	@rm $(OUT_PATH)/*.tar
	@mv -f $(OUT_PATH)/dht-*/* $(OUT_PATH)
	@rmdir $(OUT_PATH)/dht-*

dht_clean:
	@echo "Stopping service..."
	@echo "Cleaning up dht Gradle projects"
	@rm -rf $(OUT_PATH)
	@./gradlew -q dht:core:model:clean \
                  dht:core:service:clean \
                  dht:core:grpc:clean \
                  dht:java-app:clean
