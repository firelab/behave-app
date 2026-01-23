{
  description = "Emscripten build environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-25.11";
    flake-utils.url = "github:numtide/flake-utils";
  };

  inputs.self.submodules = true;

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            gnumake
            cmakeMinimal
            llvm
            clang
            emscripten
            fd  # for finding webidl_binder.py
            python315
          ];

          shellHook = ''
            export EM_CACHE=$PWD/.em_cache
            export WEBIDL="python ${pkgs.emscripten}/share/emscripten/tools/webidl_binder.py"
            echo "Environment ready!"
            echo "EM_CACHE: $EM_CACHE"
            echo "WEBIDL: $WEBIDL"
          '';
        };

        # Convenience app to run the build
        apps.default = {
          type = "app";
          program = toString (pkgs.writeShellScript "build.sh" ''
            export EM_CACHE=$PWD/.em_cache
            export WEBIDL="python ${pkgs.emscripten}/share/emscripten/tools/webidl_binder.py"
            ${pkgs.gnumake}/bin/make install
          '');
        };

        # Convenience package for `nix build`
        packages.default = pkgs.stdenv.mkDerivation {
          pname = "behave-lib";
          version = "0.1.0";
          src = ./.;

          nativeBuildInputs = with pkgs; [
            gnumake
            cmakeMinimal
            llvm
            clang
            emscripten
            fd
            python315
          ];

          configurePhase = ''
            export EM_CACHE=$TMPDIR/.em_cache
            export WEBIDL="python ${pkgs.emscripten}/share/emscripten/tools/webidl_binder.py"
            mkdir -p build
            cd build
            ${pkgs.cmakeMinimal}/bin/cmake ..
          '';

          buildPhase = ''
            export EM_CACHE=$TMPDIR/.em_cache
            export WEBIDL="python ${pkgs.emscripten}/share/emscripten/tools/webidl_binder.py"
            ${pkgs.gnumake}/bin/make
          '';

          installPhase = ''
            mkdir -p $out
            cp -r * $out/
          '';
        };
      }
    );
}
