# 프로젝트 최초 빌드 시 확인

## submodule

- 사용하는 모듈(라이브러리) 설치가 제대로 안되는 경우

- 사용하는 모듈의 프로젝트 폴더가 비어 있다면, 폴더를 삭제한 후에 아래 코드를 실행

```sh
git submodule init
git submodule update
```


# Crashlytics dSYMs 업로드
./Frameworks/Crashlytics.framework/upload-symbols -gsp ./GoogleService-Info.plist -p ios /Users/parkboo/Library/Developer/Xcode/Archives/2020-02-12/myloveactor\ 2020-02-12\ 3.02\ PM.xcarchive/dSYMs/*
archive 파일을 찾아서 패키지 내용보기 하고 dSYMs 디렉토리 찾으면 됨

## windows에서 빌드하려면
build variants에서 사용중인 symbolic link가 제대로 동작하기 위해서는 아래와 같이 클론하여야 한다.

```
git clone -c core.symlinks=true git@bitbucket.org:exodus9/idol_android.git
```

## FACEBOOK_CLIENT_ID, KAKAO_APP_KEY_FOR_MANIFEST
local.properties에 넣어야 합니다. 개발자에게 문의. 


## Apple Silicon 맥에서 빌드
[SO 참조](https://stackoverflow.com/questions/69541831/unknown-host-cpu-architecture-arm64-android-ndk-siliconm1-apple-macbook-pro)

사용중인 ndk 디렉토리로 이동 (~/Library/Android/sdk/ndk/...)
ndk-build 파일을 아래처럼 수정

```
arch -x86_64 /bin/bash $DIR/build/ndk-build "$@"
```
# idol_android
