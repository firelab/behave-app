{
  description = "Behave7 build environment with Conveyor";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-25.05";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};

        # Import Conveyor package from separate file
        conveyor = pkgs.callPackage ./conveyor.nix { };
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            babashka
            clojure
            openjdk17
          ] ++ [ conveyor ];

          shellHook = ''
            echo "Behave7 build environment ready!"
            echo "Java: $(java -version 2>&1 | head -1)"
            echo "Clojure: $(clojure --version)"
            echo "Babashka: $(bb --version)"
            echo "Conveyor: $(which conveyor) -> ${conveyor}"
          '';
        };

        # Export Conveyor as a reusable package
        packages.conveyor = conveyor;

        # Package for building via Conveyor
        packages.default = pkgs.stdenv.mkDerivation {
          pname = "behave7";
          version = "7.1.3";
          src = ./.;

          nativeBuildInputs = with pkgs; [
            babashka
            clojure
            openjdk17
          ] ++ [ conveyor ];

          buildPhase = ''
            export HOME=$TMPDIR
            bb conveyor
          '';

          installPhase = ''
            mkdir -p $out
            cp -r output/* $out/
          '';
        };
      }
    );
}
