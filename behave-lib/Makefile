ifndef WEBIDL
		WEBIDL := $(shell brew --prefix emscripten)/libexec/tools/webidl_binder
endif

BUILD_DIR = build
TEST_DIR = test-build
.DEFAULT_GOAL := install
.PHONY: all clean bind compile install test ctags

clean:
	rm -rf ${BUILD_DIR}
	rm -rf ${TEST_DIR}

all: clean install

# NOTE Make sure to set WEBIDL in Enviornment Variables
bind:
	${WEBIDL} include/idl/behave.idl include/js/glue
	mv include/js/glue.cpp include/cpp/emscripten/glue.cpp

compile: bind
	emcmake cmake -B ${BUILD_DIR}
	cmake --build ${BUILD_DIR}

install: compile
#	cp build/behave.js ../projects/behave/resources/public/js
	sed -i 's/\$$Module\$$\$$/Module/g' build/behave-min.js
	cp build/behave-min.* ../projects/behave/resources/public/js

test:
	cmake -B ${TEST_DIR} -DBEHAVE_WASM=OFF -DTEST_BEHAVE=ON
	cmake --build ${TEST_DIR}
	./${TEST_DIR}/testBehave
	./${TEST_DIR}/testSurface test/csv/surface.csv
	./${TEST_DIR}/testContain test/csv/contain.csv
	./${TEST_DIR}/testCrown test/csv/crown.csv

ctags:
	ctags -e -R --exclude=.git --exclude=build --exclude .ccls-cache .
