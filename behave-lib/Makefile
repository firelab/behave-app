
WEBIDL := /opt/homebrew/opt/emscripten/libexec/tools/webidl_binder
INSTALL_DIR := /opt/homebrew/opt/emscripten/libexec/tools/webidl_binder

all: compile

bind:
	${WEBIDL} include/idl/behave.idl include/js/glue
	mv include/js/glue.cpp include/cpp/emscripten/glue.cpp

compile: bind
	emcmake cmake -B build
	cmake --build build

install: compile
	cp build/behave.js 

ctags:
	ctags -e -R --exclude=.git --exclude=build --exclude .ccls-cache .
