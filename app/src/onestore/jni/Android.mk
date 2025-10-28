LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

ifeq (onestore, $(PRODUCT_FLAVOR))
    LOCAL_MODULE    := public_keys
    LOCAL_SRC_FILES := public_keys.c
    APP_ABI := all
    include $(BUILD_SHARED_LIBRARY)
endif

