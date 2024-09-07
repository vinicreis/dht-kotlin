ENV_FILE_NAME=sample.env
OUT_PATH=dht/out
SAMPLE_VAULT_OUT_PATH=sample-vault/out

.PHONY: dht

build: dht_build sample_vault_build
clean: dht_clean sample_vault_clean

sample_vault: sample_vault_build
	@$(SAMPLE_VAULT_OUT_PATH)/bin/dht-vault-sample

sample_vault_build: sample_vault_clean
	@echo "Building vault sample..."
	@./gradlew -q sample-vault:java-app:assembleDist
	@echo "Moving ang unpacking vault sample executable..."
	@mkdir -p $(SAMPLE_VAULT_OUT_PATH)
	@cp sample-vault/java-app/build/distributions/*.tar $(SAMPLE_VAULT_OUT_PATH)/sample-vault.tar
	@tar -xf $(SAMPLE_VAULT_OUT_PATH)/sample-vault.tar -C $(SAMPLE_VAULT_OUT_PATH)
	@rm $(SAMPLE_VAULT_OUT_PATH)/*.tar
	@mv -f $(SAMPLE_VAULT_OUT_PATH)/dht-vault-sample-*/* $(SAMPLE_VAULT_OUT_PATH)
	@rmdir $(SAMPLE_VAULT_OUT_PATH)/dht-vault-sample-*

sample_vault_clean:
	@echo "Cleaning up sample Gradle projects"
	@rm -rf $(SAMPLE_VAULT_OUT_PATH)
	@./gradlew -q sample-vault:core:core-model:clean

dht: dht_build
	@$(OUT_PATH)/bin/dht-service

dht_build: dht_clean
	@echo "Building server..."
	@./gradlew -q dht:server:assembleDist
	@echo "Moving ang unpacking client executable..."
	@mkdir -p $(OUT_PATH)
	@cp dht/server/build/distributions/*.tar $(OUT_PATH)/dht.tar
	@tar -xf $(OUT_PATH)/dht.tar -C $(OUT_PATH)
	@rm $(OUT_PATH)/*.tar
	@mv -f $(OUT_PATH)/dht-*/* $(OUT_PATH)
	@rmdir $(OUT_PATH)/dht-*

dht_clean:
	@echo "Cleaning up dht Gradle projects"
	@rm -rf $(OUT_PATH)
	@./gradlew -q dht:core:model:clean \
                  dht:core:service:clean \
                  dht:core:grpc:clean \
                  dht:server:clean
