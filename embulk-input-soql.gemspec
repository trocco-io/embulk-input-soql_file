
Gem::Specification.new do |spec|
  spec.name          = "embulk-input-soql"
  spec.version       = "0.1.4"
  spec.authors       = ["dododo8m"]
  spec.summary       = %[Soql input plugin for Embulk]
  spec.description   = %[Loads records from Soql.]
  spec.email         = ["taichiddt+8m@gmail.com"]
  spec.licenses      = ["MIT"]
  # TODO set this: spec.homepage      = "https://github.com/taichiddt+8m/embulk-input-soql"

  spec.files         = `git ls-files`.split("\n") + Dir["classpath/*.jar"]
  spec.test_files    = spec.files.grep(%r"^(test|spec)/")
  spec.require_paths = ["lib"]

  #spec.add_dependency 'YOUR_GEM_DEPENDENCY', ['~> YOUR_GEM_DEPENDENCY_VERSION']
  spec.add_development_dependency 'bundler', ['~> 1.0']
  spec.add_development_dependency 'rake', ['~> 12.0']
end
