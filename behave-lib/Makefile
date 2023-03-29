
WEBIDL := /opt/homebrew/opt/emscripten/libexec/tools/webidl_binder

all: compile

bind:
	${WEBIDL} include/idl/behave.idl include/js/glue

compile: bind
	emcmake cmake -B build
	cmake --build build
