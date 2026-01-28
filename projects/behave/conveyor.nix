{ stdenv
, lib
, fetchurl
, unzip
, autoPatchelfHook
, makeWrapper
}:

stdenv.mkDerivation rec {
  pname = "conveyor";
  version = "21.1";

  src = if stdenv.isDarwin then
    fetchurl {
      url = "https://downloads.hydraulic.dev/conveyor/conveyor-${version}-mac-aarch64.zip";
      hash = "sha256-djNlXLKX8bUxf8vAfs9F/UzVtuaRHzZD0RdY5VKuDgw=";
    }
  else
    fetchurl {
      url = "https://downloads.hydraulic.dev/conveyor/conveyor-${version}-linux-amd64.tar.gz";
      hash = "sha256-P03wZjQP6DcaQXRDqX1aVsZgtAhwnmJuwOR3GBzqPO4=";
    };

  nativeBuildInputs = [ unzip makeWrapper ]
    ++ lib.optionals stdenv.isLinux [ autoPatchelfHook ];

  unpackPhase = if stdenv.isDarwin then ''
    unzip $src
  '' else ''
    tar xzf $src
  '';

  dontBuild = true;

  dontConfigure = true;

  installPhase = if pkgs.stdenv.isDarwin then ''
    mkdir -p $out/bin
    cp -r Conveyor.app $out/
    ln -s $out/Conveyor.app/Contents/Home/conveyor $out/bin/conveyor
  '' else ''
    mkdir -p $out/bin
    cp -r * $out/bin/
  '';

  dontFixup = true;

  meta = with lib; {
    description = "Conveyor - Software distribution tool";
    homepage = "https://www.hydraulic.dev";
    license = licenses.unfree;
    platforms = [ "x86_64-linux" "aarch64-darwin" "x86_64-darwin" ];
    maintainers = [ ];
  };
}
