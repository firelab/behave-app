{
  description = "Emscripten build environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            gnumake
            cmake
            llvm
            clang
            emscripten
            fd  # for finding webidl_binder.py
          ];

          shellHook = ''
            export EM_CACHE=$PWD/.em_cache
            export WEB_IDL=$(fd -1 webidl_binder.py /nix/store)
            export WEBIDL="${WEB_IDL%.*}"
            echo "Environment ready!"
            echo "EM_CACHE: $EM_CACHE"
            echo "WEB_IDL: $WEB_IDL"
          '';
        };

        # Convenience app to run the build
        apps.build = {
          type = "app";
          program = toString (pkgs.writeShellScript "build.sh" ''
            export EM_CACHE=$PWD/.em_cache
            export WEB_IDL=$(${pkgs.fd}/bin/fd -1 webidl_binder.py /nix/store)
            ${pkgs.gnumake}/bin/make install
          '');
        };
      }
    );
}
