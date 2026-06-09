{
  description = "Lightweight CloudId providers for Akeyless (AWS, Azure, GCP) using JDK HTTP";
  inputs = {
    nixpkgs.follows = "substrate/nixpkgs";
    substrate = { url = "github:pleme-io/substrate";};
    flake-utils.url = "github:numtide/flake-utils";
  };
  outputs = inputs: (import "${inputs.substrate}/lib/repo-flake.nix" {
    inherit (inputs) nixpkgs flake-utils;
  }) {
    self = inputs.self;
    language = "java";
    builder = "package";
    pname = "cloudid-lightweight";
    mvnHash = "";
    description = "Lightweight CloudId providers for Akeyless (AWS, Azure, GCP) using JDK HTTP";
  };
}
