{ pkgs ? import <nixpkgs> {} }: 
pkgs.mkShell {
  packages = [
    pkgs.python314Packages.pillow
    pkgs.python314
  ];
}
