Embulk::JavaPlugin.register_input(
  "soql", "org.embulk.input.soql.SoqlInputFilePlugin",
  File.expand_path('../../../../classpath', __FILE__))
