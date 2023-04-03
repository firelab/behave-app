INSTALL_DIR := /opt/homebrew/opt/emscripten/libexec/tools/webidl_binder
.DEFAULT_GOAL := install

all: install

# NOTE Make sure to set WEBIDL in Enviornment Variables
bind:
	${WEBIDL} include/idl/behave.idl include/js/glue
	mv include/js/glue.cpp include/cpp/emscripten/glue.cpp

compile: bind
	emcmake cmake -B build
	cmake --build build

install: compile
#	cp build/behave.js ../projects/behave/resources/public/js
	cp build/behave-min.* ../projects/behave/resources/public/js

ctags:
	ctags -e -R --exclude=.git --exclude=build --exclude .ccls-cache .
