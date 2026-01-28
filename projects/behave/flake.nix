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

        nativeBuildInputs = with pkgs; [
          unzip
        ];

        # Download Conveyor
        conveyor = pkgs.stdenv.mkDerivation rec {
          pname = "conveyor";
          version = "21.1";

          src = if pkgs.stdenv.isDarwin then
            pkgs.fetchurl {
              url = "https://downloads.hydraulic.dev/conveyor/conveyor-${version}-mac-aarch64.zip";
              hash = "sha256-7633655cb297f1b5317fcbc07ecf45fd4cd5b6e6911f3643d11758e552ae0e0c";
            }
          else
            pkgs.fetchurl {
              url = "https://downloads.hydraulic.dev/conveyor/conveyor-${version}-linux-amd64.tar.gz";
              hash = "sha256-3f4df066340fe8371a417443a97d5a56c660b408709e626ec0e477181cea3cee";
            };

          nativeBuildInputs = [ pkgs.unzip ];

          unpackPhase = if pkgs.stdenv.isDarwin then ''
            unzip $src
          '' else ''
            tar xzf $src
          '';

          installPhase = if pkgs.stdenv.isDarwin then ''
            mkdir -p $out/bin
            cp -r Conveyor.app $out/
            ln -s $out/Conveyor.app/Contents/Home/conveyor $out/bin/conveyor
          '' else ''
            mkdir -p $out/bin
            cp -r * $out/bin/
          '';
        };
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            babashka
            clojure
            corretto17
            gnumake
          ] ++ [ conveyor ];

          shellHook = ''
            echo "Behave7 build environment ready!"
            echo "Java: $(java -version 2>&1 | head -1)"
            echo "Clojure: $(clojure --version)"
            echo "Babashka: $(bb --version)"
          '';
        };

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
